package id.baundang.invitation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.dto.ChristianContentSchema;
import id.baundang.invitation.dto.EventDTO;
import id.baundang.invitation.dto.GuestDTO;
import id.baundang.invitation.service.InvitationService;
import id.baundang.invitation.service.PinGateService;
import id.baundang.invitation.service.PreviewTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/i")
@RequiredArgsConstructor
public class InvitationPageController {

    private final InvitationService invitationService;
    private final PinGateService pinGateService;
    private final PreviewTokenService previewTokenService;

    @org.springframework.beans.factory.annotation.Value("${app.midtrans.snap-js-url}")
    private String snapJsUrl;

    @org.springframework.beans.factory.annotation.Value("${app.midtrans.client-key}")
    private String midtransClientKey;

    @GetMapping("/{slug}")
    public String viewInvitation(@PathVariable String slug,
                                 @RequestParam(name = "to", required = false) String to,
                                 @RequestParam(name = "preview", required = false) String preview,
                                 HttpServletRequest request,
                                 Model model) {
        String greeting = to != null ? to.trim() : "";

        // Gate before anything is read into the model: a protected invitation must not
        // put its content — or its PIN — into the response at all.
        Invitation gated = invitationService.getBySlug(slug);
        boolean previewing = previewTokenService.isValid(preview, slug);
        // A valid preview token proves the owner minted this link, so it also stands in
        // for the PIN — the couple should not have to enter their own gate to proofread.
        if (!previewing && isGateClosed(gated, request, slug)) {
            return renderGate(gated, slug, greeting, false, model);
        }

        // Previews are the owner proofreading their own page; they neither count as a
        // view nor read from the cache, which still holds the published version.
        Invitation inv = previewing ? gated : invitationService.getBySlugAndIncrementView(slug);
        JsonNode content = previewing
                ? invitationService.previewContent(inv.getId())
                : inv.getContent();

        model.addAttribute("guestGreeting", greeting);
        model.addAttribute("previewing", previewing);

        model.addAttribute("slug", slug);
        model.addAttribute("invitationId", inv.getId());
        model.addAttribute("templateId", inv.getTemplateId());
        model.addAttribute("status", inv.getStatus().name());
        model.addAttribute("activeUntil", inv.getActiveUntil());
        model.addAttribute("viewCount", inv.getViewCount());
        // The raw content node carries accessPin; expose a copy without it so no
        // future template can render the PIN into a guest-facing page.
        model.addAttribute("content", withoutPin(content));

        // Convenience fields extracted from JSONB for use in templates
        model.addAttribute("coupleName", textOf(content, "coupleName", slug));
        model.addAttribute("brideFullName", textOf(content, "brideFullName", ""));
        model.addAttribute("groomFullName", textOf(content, "groomFullName", ""));
        model.addAttribute("akadDate", textOf(content, "matrimonyDate", textOf(content, "akadDate", "")));
        model.addAttribute("akadTime", textOf(content, "matrimonyTime", textOf(content, "akadTime", "")));
        model.addAttribute("akadVenue", textOf(content, "matrimonyVenue", textOf(content, "akadVenue", "")));
        model.addAttribute("receptionDate", textOf(content, "receptionDate", ""));
        model.addAttribute("receptionTime", textOf(content, "receptionTime", ""));
        model.addAttribute("receptionVenue", textOf(content, "receptionVenue", ""));
        model.addAttribute("loveStory", textOf(content, "loveStory", ""));
        model.addAttribute("coverPhotoUrl", textOf(content, "coverPhotoUrl", ""));
        model.addAttribute("mapsEmbedUrl", textOf(content, "mapsEmbedUrl", ""));
        model.addAttribute("events", extractEvents(content));
        model.addAttribute("christian", ChristianContentSchema.from(content));

        String stylePreset = textOf(content, "stylePreset", "GRACE");
        model.addAttribute("stylePreset", stylePreset);

        // Midtrans Snap for the floating gift (amplop) button
        model.addAttribute("snapJsUrl", snapJsUrl);
        model.addAttribute("midtransClientKey", midtransClientKey);

        return "invitation/view";
    }

    /**
     * Verifies a submitted PIN and, on success, issues the short-lived pass cookie.
     * The PIN is compared server-side and never rendered into any response.
     */
    @PostMapping("/{slug}/pin")
    public String submitPin(@PathVariable String slug,
                            @RequestParam(name = "pin", required = false) String pin,
                            @RequestParam(name = "to", required = false) String to,
                            HttpServletResponse response,
                            Model model) {
        Invitation inv = invitationService.getBySlug(slug);
        String storedPin = textOf(inv.getContent(), "accessPin", "");
        String greeting = to != null ? to.trim() : "";

        if (!pinGateService.matches(storedPin, pin)) {
            return renderGate(inv, slug, greeting, true, model);
        }

        Cookie pass = new Cookie(pinGateService.cookieName(slug), pinGateService.issue(slug));
        pass.setHttpOnly(true);
        pass.setSecure(true);
        pass.setPath("/i/" + slug);
        pass.setMaxAge(pinGateService.cookieMaxAgeSeconds());
        pass.setAttribute("SameSite", "Lax");
        response.addCookie(pass);

        String target = UriComponentsBuilder.fromPath("/i/{slug}").buildAndExpand(slug).toUriString();
        return "redirect:" + (greeting.isBlank()
                ? target
                : target + "?to=" + URLEncoder.encode(greeting, StandardCharsets.UTF_8));
    }

    @GetMapping("/{slug}/gift")
    public String giftPage(@PathVariable String slug, HttpServletRequest request, Model model) {
        Invitation gated = invitationService.getBySlug(slug);
        // The gift page exposes the couple's bank/GoPay/OVO/QRIS details, so it is
        // gated alongside the invitation itself.
        if (isGateClosed(gated, request, slug)) {
            return renderGate(gated, slug, "", false, model);
        }
        Invitation inv = invitationService.getBySlugAndIncrementView(slug);
        JsonNode content = inv.getContent();
        model.addAttribute("slug", slug);
        model.addAttribute("invitationId", inv.getId());
        model.addAttribute("coupleName", textOf(content, "coupleName", slug));
        model.addAttribute("snapJsUrl", snapJsUrl);
        model.addAttribute("midtransClientKey", midtransClientKey);
        model.addAttribute("giftAccount", invitationService.getGiftAccount(slug));
        return "invitation/gift";
    }

    @GetMapping("/{slug}/scan")
    public String scanPage(@PathVariable String slug, Model model) {
        Invitation inv = invitationService.getBySlugAndIncrementView(slug);
        JsonNode content = inv.getContent();
        model.addAttribute("slug", slug);
        model.addAttribute("coupleName", textOf(content, "coupleName", slug));
        return "invitation/scanner";
    }

    @GetMapping("/{slug}/checkin/{code}")
    public String checkInPage(@PathVariable String slug, @PathVariable String code, Model model) {
        Invitation inv = invitationService.getBySlugAndIncrementView(slug);
        GuestDTO guest = invitationService.getGuestByCode(code);
        JsonNode content = inv.getContent();

        model.addAttribute("slug", slug);
        model.addAttribute("coupleName", textOf(content, "coupleName", slug));
        model.addAttribute("inviteCode", code);
        model.addAttribute("guestName", guest.name());
        model.addAttribute("groupLabel", guest.groupLabel());
        model.addAttribute("tableNo", guest.tableNo());
        model.addAttribute("allottedCount", guest.allottedCount());
        model.addAttribute("alreadyCheckedIn", guest.checkedIn());
        model.addAttribute("checkedInCount", guest.checkedInCount());
        return "invitation/checkin";
    }

    private JsonNode withoutPin(JsonNode content) {
        if (content == null || !content.isObject()) {
            return content;
        }
        ObjectNode copy = (ObjectNode) content.deepCopy();
        copy.remove("accessPin");
        return copy;
    }

    /** True when this invitation has a PIN and the request has not yet cleared it. */
    private boolean isGateClosed(Invitation inv, HttpServletRequest request, String slug) {
        String storedPin = textOf(inv.getContent(), "accessPin", "");
        return pinGateService.isProtected(storedPin) && !pinGateService.hasValidPass(request, slug);
    }

    /** Renders the gate with nothing in the model beyond the couple's name. */
    private String renderGate(Invitation inv, String slug, String greeting,
                              boolean pinError, Model model) {
        model.addAttribute("slug", slug);
        model.addAttribute("coupleName", textOf(inv.getContent(), "coupleName", slug));
        model.addAttribute("guestGreeting", greeting);
        model.addAttribute("pinError", pinError);
        return "invitation/pin-gate";
    }

    private String textOf(JsonNode node, String field, String fallback) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText(fallback) : fallback;
    }

    private List<EventDTO> extractEvents(JsonNode content) {
        List<EventDTO> events = new ArrayList<>();
        if (content == null || !content.hasNonNull("events")) {
            return events;
        }
        JsonNode arr = content.get("events");
        if (!arr.isArray()) {
            return events;
        }
        for (JsonNode item : arr) {
            events.add(EventDTO.from(item));
        }
        return events;
    }
}
