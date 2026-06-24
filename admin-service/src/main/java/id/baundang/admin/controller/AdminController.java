package id.baundang.admin.controller;

import id.baundang.admin.client.InvitationAdminClient;
import id.baundang.admin.client.NotificationAdminClient;
import id.baundang.admin.client.OrderAdminClient;
import id.baundang.admin.client.TemplateAdminClient;
import id.baundang.admin.dto.BroadcastRequest;
import id.baundang.admin.dto.GiftAccountDTO;
import id.baundang.admin.dto.InvitationDTO;
import id.baundang.admin.dto.OrderDTO;
import id.baundang.admin.dto.PagedResult;
import id.baundang.admin.dto.TemplateCreateRequest;
import id.baundang.admin.dto.TemplateDTO;
import id.baundang.admin.entity.AdminNote;
import id.baundang.admin.repository.AdminNoteRepository;
import id.baundang.admin.service.AdminDashboardService;
import id.baundang.admin.service.CsvExportService;
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

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final OrderAdminClient orderClient;
    private final InvitationAdminClient invitationClient;
    private final TemplateAdminClient templateClient;
    private final NotificationAdminClient notificationClient;
    private final AdminNoteRepository noteRepository;
    private final CsvExportService csvExportService;

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.buildStats());
        return "admin/dashboard";
    }

    // ─── Orders ───────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String orders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {
        PagedResult<OrderDTO> result = orderClient.listOrders(page, size, status, search);
        model.addAttribute("orders", result);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentPage", page);
        return "admin/orders/list";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable UUID id, Model model) {
        orderClient.getOrder(id).ifPresent(order -> {
            model.addAttribute("order", order);
            model.addAttribute("revisions", orderClient.getRevisions(id));
            model.addAttribute("notes",
                    noteRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("ORDER", id.toString()));
        });
        if (!model.containsAttribute("order")) {
            return "redirect:/admin/orders";
        }
        return "admin/orders/detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam(required = false) String midtransId) {
        orderClient.updateStatus(id, status, midtransId);
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/orders/{id}/notes")
    public String addOrderNote(
            @PathVariable UUID id,
            @RequestParam String note,
            HttpServletRequest request) {
        String adminId = (String) request.getAttribute("userId");
        AdminNote n = new AdminNote();
        n.setEntityType("ORDER");
        n.setEntityId(id.toString());
        n.setNote(note);
        n.setCreatedBy(adminId != null ? adminId : "admin");
        noteRepository.save(n);
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/orders/export.csv")
    public void exportOrders(
            @RequestParam(required = false) String status,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"orders.csv\"");
        csvExportService.exportOrders(response.getWriter(), status);
    }

    // ─── Templates ────────────────────────────────────────────────────────────

    @GetMapping("/templates")
    public String templates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        model.addAttribute("templates", templateClient.listTemplates(page, size));
        model.addAttribute("currentPage", page);
        return "admin/templates/list";
    }

    @GetMapping("/templates/create")
    public String templateCreateForm() {
        return "admin/templates/create";
    }

    @PostMapping("/templates/create")
    public String templateCreate(
            @RequestParam String name,
            @RequestParam String slug,
            @RequestParam(required = false) String description,
            @RequestParam String category,
            @RequestParam(required = false) String stylePreset,
            @RequestParam short priceLevel,
            @RequestParam(required = false) String thumbnailUrl,
            Model model) {
        TemplateCreateRequest req = new TemplateCreateRequest(
                name, slug, description, category, stylePreset, priceLevel, thumbnailUrl);
        boolean ok = templateClient.createTemplate(req);
        if (ok) {
            return "redirect:/admin/templates";
        }
        model.addAttribute("error", "Gagal membuat template. Periksa data dan coba lagi.");
        model.addAttribute("form", req);
        return "admin/templates/create";
    }

    @GetMapping("/templates/{id}/edit")
    public String templateEditForm(@PathVariable String id, Model model) {
        TemplateDTO t = templateClient.getTemplate(id);
        if (t == null) {
            return "redirect:/admin/templates";
        }
        model.addAttribute("template", t);
        return "admin/templates/edit";
    }

    @PostMapping("/templates/{id}/edit")
    public String templateEdit(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam String slug,
            @RequestParam(required = false) String description,
            @RequestParam String category,
            @RequestParam(required = false) String stylePreset,
            @RequestParam short priceLevel,
            @RequestParam(required = false) String thumbnailUrl,
            Model model) {
        TemplateCreateRequest req = new TemplateCreateRequest(
                name, slug, description, category, stylePreset, priceLevel, thumbnailUrl);
        boolean ok = templateClient.updateTemplate(id, req);
        if (ok) {
            return "redirect:/admin/templates";
        }
        model.addAttribute("error", "Gagal memperbarui template.");
        model.addAttribute("template", new TemplateDTO(id, name, slug, description, category, stylePreset,
                priceLevel, thumbnailUrl, true));
        return "admin/templates/edit";
    }

    @PostMapping("/templates/{id}/delete")
    public String templateDelete(@PathVariable UUID id) {
        templateClient.deleteTemplate(id);
        return "redirect:/admin/templates";
    }

    @PostMapping("/templates/{id}/toggle")
    public String toggleTemplate(@PathVariable String id, @RequestParam boolean active) {
        templateClient.toggleActive(id, active);
        return "redirect:/admin/templates";
    }

    // ─── Invitations ──────────────────────────────────────────────────────────

    @GetMapping("/invitations")
    public String invitations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        model.addAttribute("invitations", invitationClient.listInvitations(page, size));
        model.addAttribute("currentPage", page);
        return "admin/invitations/list";
    }

    @GetMapping("/invitations/{id}")
    public String invitationDetail(@PathVariable UUID id, Model model) {
        InvitationDTO inv = invitationClient.getInvitation(id);
        if (inv == null) {
            return "redirect:/admin/invitations";
        }
        model.addAttribute("invitation", inv);
        return "admin/invitations/detail";
    }

    @PostMapping("/invitations/{id}/status")
    public String updateInvitationStatus(@PathVariable UUID id, @RequestParam String status) {
        invitationClient.updateStatus(id, status);
        return "redirect:/admin/invitations/" + id;
    }

    @GetMapping("/invitations/{id}/rsvp")
    public String rsvp(@PathVariable UUID id, Model model) {
        model.addAttribute("invitationId", id);
        model.addAttribute("entries", invitationClient.listRsvp(id));
        return "admin/invitations/rsvp";
    }

    @GetMapping("/invitations/{id}/guests")
    public String guests(@PathVariable UUID id, Model model) {
        model.addAttribute("invitationId", id);
        model.addAttribute("guests", invitationClient.listGuests(id));
        return "admin/invitations/guests";
    }

    @PostMapping("/invitations/{id}/guests")
    public String addGuest(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam(required = false) String groupLabel,
            @RequestParam(required = false) String tableNo,
            @RequestParam(defaultValue = "1") short allottedCount) {
        invitationClient.addGuest(id, Map.of(
                "name", name,
                "groupLabel", groupLabel != null ? groupLabel : "",
                "tableNo", tableNo != null ? tableNo : "",
                "allottedCount", allottedCount
        ));
        return "redirect:/admin/invitations/" + id + "/guests";
    }

    @PostMapping("/invitations/{id}/guests/{guestId}/delete")
    public String deleteGuest(@PathVariable UUID id, @PathVariable UUID guestId) {
        invitationClient.deleteGuest(id, guestId);
        return "redirect:/admin/invitations/" + id + "/guests";
    }

    @GetMapping("/invitations/{id}/attendance")
    public String attendance(@PathVariable UUID id, Model model) {
        model.addAttribute("invitationId", id);
        model.addAttribute("attendance", invitationClient.getAttendance(id));
        return "admin/invitations/attendance";
    }

    @GetMapping("/invitations/{id}/gifts")
    public String gifts(@PathVariable UUID id, Model model) {
        model.addAttribute("invitationId", id);
        model.addAttribute("gifts", invitationClient.getGifts(id));
        return "admin/invitations/gifts";
    }

    @GetMapping("/invitations/{id}/gift-account")
    public String giftAccountForm(@PathVariable UUID id, Model model) {
        model.addAttribute("invitationId", id);
        model.addAttribute("account", invitationClient.getGiftAccount(id));
        return "admin/invitations/gift-account";
    }

    @PostMapping("/invitations/{id}/gift-account")
    public String saveGiftAccount(
            @PathVariable UUID id,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String accountHolder,
            @RequestParam(required = false) String gopayNumber,
            @RequestParam(required = false) String ovoNumber,
            @RequestParam(required = false) String qrisImageUrl) {
        GiftAccountDTO req = new GiftAccountDTO(
                bankName, accountNumber, accountHolder, gopayNumber, ovoNumber, qrisImageUrl);
        invitationClient.setGiftAccount(id, req);
        return "redirect:/admin/invitations/" + id;
    }

    @GetMapping("/invitations/{id}/guestbook")
    public String guestbook(@PathVariable UUID id, Model model) {
        model.addAttribute("invitationId", id);
        model.addAttribute("entries", invitationClient.listAllGuestbook(id));
        return "admin/invitations/guestbook";
    }

    @PostMapping("/invitations/{invitationId}/guestbook/{entryId}/approve")
    public String approveGuestbook(@PathVariable UUID invitationId, @PathVariable UUID entryId) {
        invitationClient.approveGuestbook(invitationId, entryId);
        return "redirect:/admin/invitations/" + invitationId + "/guestbook";
    }

    @GetMapping("/rsvp/{invitationId}/export.csv")
    public void exportRsvp(@PathVariable UUID invitationId, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"rsvp-" + invitationId + ".csv\"");
        csvExportService.exportRsvp(response.getWriter(), invitationId);
    }

    // ─── Revisions ────────────────────────────────────────────────────────────

    @PostMapping("/revisions/{revisionId}/complete")
    public String completeRevision(
            @PathVariable UUID revisionId,
            @RequestParam UUID orderId,
            @RequestParam(required = false) UUID invitationId,
            @RequestParam(required = false) String contentJson,
            @RequestParam(required = false) String note,
            HttpServletRequest request) {

        orderClient.completeRevision(revisionId);

        if (invitationId != null && contentJson != null && !contentJson.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Object patch = mapper.readValue(contentJson, Object.class);
                invitationClient.updateContent(invitationId, patch);
            } catch (Exception e) {
                // invalid JSON — skip content update
            }
        }

        if (note != null && !note.isBlank()) {
            String adminId = (String) request.getAttribute("userId");
            AdminNote n = new AdminNote();
            n.setEntityType("ORDER");
            n.setEntityId(orderId.toString());
            n.setNote(note);
            n.setCreatedBy(adminId != null ? adminId : "admin");
            noteRepository.save(n);
        }

        return "redirect:/admin/orders/" + orderId;
    }

    // ─── Broadcast WA ────────────────────────────────────────────────────────

    @GetMapping("/broadcast/wa")
    public String broadcastForm() {
        return "admin/broadcast/wa";
    }

    @PostMapping("/broadcast/wa")
    public String sendBroadcast(
            @RequestParam String targetGroup,
            @RequestParam String message,
            Model model) {
        try {
            notificationClient.broadcast(new BroadcastRequest(targetGroup, message));
            model.addAttribute("success", "Broadcast berhasil dikirim ke grup " + targetGroup);
        } catch (Exception e) {
            model.addAttribute("error", "Gagal mengirim broadcast: " + e.getMessage());
        }
        return "admin/broadcast/wa";
    }

    // ─── Buyers ───────────────────────────────────────────────────────────────

    @GetMapping("/buyers")
    public String buyers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        PagedResult<OrderDTO> result = orderClient.listOrders(page, size, "PAID", search);
        model.addAttribute("orders", result);
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentPage", page);
        return "admin/buyers/list";
    }
}
