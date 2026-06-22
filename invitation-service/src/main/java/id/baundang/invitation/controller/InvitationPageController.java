package id.baundang.invitation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.dto.EventDTO;
import id.baundang.invitation.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/i")
@RequiredArgsConstructor
public class InvitationPageController {

    private final InvitationService invitationService;

    @GetMapping("/{slug}")
    public String viewInvitation(@PathVariable String slug, Model model) {
        Invitation inv = invitationService.getBySlugAndIncrementView(slug);
        JsonNode content = inv.getContent();

        model.addAttribute("slug", slug);
        model.addAttribute("invitationId", inv.getId());
        model.addAttribute("templateId", inv.getTemplateId());
        model.addAttribute("status", inv.getStatus().name());
        model.addAttribute("activeUntil", inv.getActiveUntil());
        model.addAttribute("viewCount", inv.getViewCount());
        model.addAttribute("content", content);

        // Convenience fields extracted from JSONB for use in templates
        model.addAttribute("coupleName",    textOf(content, "coupleName", slug));
        model.addAttribute("brideFullName", textOf(content, "brideFullName", ""));
        model.addAttribute("groomFullName", textOf(content, "groomFullName", ""));
        model.addAttribute("akadDate",      textOf(content, "akadDate", ""));
        model.addAttribute("akadTime",      textOf(content, "akadTime", ""));
        model.addAttribute("akadVenue",     textOf(content, "akadVenue", ""));
        model.addAttribute("receptionDate", textOf(content, "receptionDate", ""));
        model.addAttribute("receptionTime", textOf(content, "receptionTime", ""));
        model.addAttribute("receptionVenue",textOf(content, "receptionVenue", ""));
        model.addAttribute("loveStory",     textOf(content, "loveStory", ""));
        model.addAttribute("coverPhotoUrl", textOf(content, "coverPhotoUrl", ""));
        model.addAttribute("mapsEmbedUrl",  textOf(content, "mapsEmbedUrl", ""));
        model.addAttribute("events", extractEvents(content));

        return "invitation/view";
    }

    private String textOf(JsonNode node, String field, String fallback) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText(fallback) : fallback;
    }

    private List<EventDTO> extractEvents(JsonNode content) {
        List<EventDTO> events = new ArrayList<>();
        if (content == null || !content.hasNonNull("events")) return events;
        JsonNode arr = content.get("events");
        if (!arr.isArray()) return events;
        for (JsonNode item : arr) {
            events.add(EventDTO.from(item));
        }
        return events;
    }
}
