package id.baundang.storefront.controller;

import id.baundang.storefront.client.OrderApiClient;
import id.baundang.storefront.client.OrderApiClient.PublicOrderDTO;
import id.baundang.storefront.client.TemplateApiClient;
import id.baundang.storefront.client.TemplateApiClient.TemplatePage;
import id.baundang.storefront.client.TemplateSummaryDTO;
import id.baundang.storefront.config.MidtransProperties;
import id.baundang.storefront.config.PricingProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Controller
public class StorefrontController {

    private final PricingProperties pricing;
    private final TemplateApiClient templateClient;
    private final MidtransProperties midtrans;
    private final OrderApiClient orderClient;

    public StorefrontController(PricingProperties pricing, TemplateApiClient templateClient,
                                MidtransProperties midtrans, OrderApiClient orderClient) {
        this.pricing        = pricing;
        this.templateClient = templateClient;
        this.midtrans       = midtrans;
        this.orderClient    = orderClient;
    }

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("tiers", pricing.getTiers());
        TemplatePage featured = templateClient.fetchTemplates(0, 3, null);
        model.addAttribute("featuredTemplates", featured.content());
        return "landing";
    }

    @GetMapping("/templates")
    public String templates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String kategori,
            @RequestParam(defaultValue = "false") boolean htmx,
            Model model) {

        TemplatePage result = templateClient.fetchTemplates(page, size, kategori);
        model.addAttribute("templates", result.content());
        model.addAttribute("currentPage", result.page());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("isLast", result.last());
        model.addAttribute("nextPage", page + 1);
        model.addAttribute("kategori", kategori);

        if (htmx) {
            return "fragments/template-cards :: cards";
        }
        return "templates";
    }

    @GetMapping("/pesan")
    public String order(@RequestParam(required = false) String template, Model model) {
        model.addAttribute("tiers", pricing.getTiers());
        if (template != null && !template.isBlank()) {
            TemplatePage page = templateClient.fetchTemplates(0, 50, null);
            page.content().stream()
                    .filter(t -> template.equals(t.slug()))
                    .findFirst()
                    .ifPresent(t -> {
                        model.addAttribute("templateId", t.id());
                        model.addAttribute("templatePriceLevel", t.priceLevel());
                    });
        }
        if (!model.containsAttribute("templateId")) {
            model.addAttribute("templateId", "");
            model.addAttribute("templatePriceLevel", "");
        }
        return "pesan";
    }

    @GetMapping("/tentang")
    public String about() {
        return "tentang";
    }

    @GetMapping("/masuk")
    public String login() {
        return "masuk";
    }

    @GetMapping("/daftar")
    public String register() {
        return "daftar";
    }

    @GetMapping("/bayar/{orderId}")
    public String payment(@PathVariable String orderId, Model model) {
        model.addAttribute("orderId", orderId);
        model.addAttribute("midtransClientKey", midtrans.getClientKey());
        model.addAttribute("snapJsUrl", midtrans.getSnapJsUrl());
        return "bayar";
    }

    @GetMapping("/bayar/selesai")
    public String paymentFinish(
            @RequestParam(name = "order_id", required = false) String midtransOrderId,
            @RequestParam(name = "transaction_status", required = false) String transactionStatus,
            Model model) {
        model.addAttribute("orderId", midtransOrderId);
        model.addAttribute("transactionStatus", transactionStatus);
        enrichWithOrderDetail(midtransOrderId, model);
        return "bayar-selesai";
    }

    @GetMapping("/bayar/pending")
    public String paymentPending(
            @RequestParam(name = "order_id", required = false) String midtransOrderId,
            @RequestParam(name = "transaction_status", required = false) String transactionStatus,
            Model model) {
        model.addAttribute("orderId", midtransOrderId);
        model.addAttribute("transactionStatus", transactionStatus);
        enrichWithOrderDetail(midtransOrderId, model);
        return "bayar-pending";
    }

    @GetMapping("/bayar/gagal")
    public String paymentError(
            @RequestParam(name = "order_id", required = false) String midtransOrderId,
            @RequestParam(name = "transaction_status", required = false) String transactionStatus,
            Model model) {
        model.addAttribute("orderId", midtransOrderId);
        model.addAttribute("transactionStatus", transactionStatus);
        enrichWithOrderDetail(midtransOrderId, model);
        return "bayar-gagal";
    }

    private void enrichWithOrderDetail(String midtransOrderId, Model model) {
        if (midtransOrderId == null || midtransOrderId.isBlank()) return;
        try {
            String uuidStr = midtransOrderId.startsWith("BND-")
                    ? midtransOrderId.substring(4)
                    : midtransOrderId;
            UUID orderId = UUID.fromString(uuidStr);
            PublicOrderDTO order = orderClient.fetchPublicOrder(orderId);
            if (order != null) {
                model.addAttribute("order", order);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    @GetMapping("/templates/{slug}")
    public String templateDetail(@PathVariable String slug, Model model) {
        TemplatePage page = templateClient.fetchTemplates(0, 50, null);
        TemplateSummaryDTO template = page.content().stream()
                .filter(t -> slug.equals(t.slug()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("template", template);
        return "template-detail";
    }
}
