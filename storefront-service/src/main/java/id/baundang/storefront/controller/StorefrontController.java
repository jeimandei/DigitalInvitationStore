package id.baundang.storefront.controller;

import id.baundang.storefront.client.TemplateApiClient;
import id.baundang.storefront.client.TemplateApiClient.TemplatePage;
import id.baundang.storefront.config.PricingProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StorefrontController {

    private final PricingProperties pricing;
    private final TemplateApiClient templateClient;

    public StorefrontController(PricingProperties pricing, TemplateApiClient templateClient) {
        this.pricing        = pricing;
        this.templateClient = templateClient;
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
    public String order(Model model) {
        model.addAttribute("tiers", pricing.getTiers());
        return "pesan";
    }

    @GetMapping("/tentang")
    public String about() {
        return "tentang";
    }
}
