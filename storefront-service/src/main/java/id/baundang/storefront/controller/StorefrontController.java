package id.baundang.storefront.controller;

import id.baundang.storefront.client.TemplateApiClient;
import id.baundang.storefront.client.TemplateApiClient.TemplatePage;
import id.baundang.storefront.client.TemplateSummaryDTO;
import id.baundang.storefront.config.PricingProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/masuk")
    public String login() {
        return "masuk";
    }

    @GetMapping("/daftar")
    public String register() {
        return "daftar";
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
