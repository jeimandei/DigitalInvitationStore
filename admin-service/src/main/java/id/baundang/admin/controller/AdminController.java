package id.baundang.admin.controller;

import id.baundang.admin.client.InvitationAdminClient;
import id.baundang.admin.client.OrderAdminClient;
import id.baundang.admin.client.TemplateAdminClient;
import id.baundang.admin.dto.*;
import id.baundang.admin.entity.AdminNote;
import id.baundang.admin.repository.AdminNoteRepository;
import id.baundang.admin.service.AdminDashboardService;
import id.baundang.admin.service.CsvExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final OrderAdminClient orderClient;
    private final InvitationAdminClient invitationClient;
    private final TemplateAdminClient templateClient;
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
        if (!model.containsAttribute("order")) return "redirect:/admin/orders";
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
