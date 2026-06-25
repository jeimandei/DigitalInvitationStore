package id.baundang.storefront.controller;

import id.baundang.storefront.client.TemplateApiClient;
import id.baundang.storefront.client.TemplateSummaryDTO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
public class SitemapController {

    private static final String BASE = "https://baundang.id";

    private final TemplateApiClient templateClient;

    public SitemapController(TemplateApiClient templateClient) {
        this.templateClient = templateClient;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robots() {
        return "User-agent: *\nAllow: /\nSitemap: https://baundang.id/sitemap.xml\n";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static pages
        appendUrl(xml, BASE + "/", "weekly", "1.0");
        appendUrl(xml, BASE + "/templates", "weekly", "0.9");
        appendUrl(xml, BASE + "/pesan", "monthly", "0.8");
        appendUrl(xml, BASE + "/tentang", "monthly", "0.7");

        // Template detail pages
        List<TemplateSummaryDTO> templates = fetchAllTemplates();
        for (TemplateSummaryDTO t : templates) {
            appendUrl(xml, BASE + "/templates/" + t.slug(), "monthly", "0.6");
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String loc, String changefreq, String priority) {
        xml.append("  <url>\n")
                .append("    <loc>").append(loc).append("</loc>\n")
                .append("    <changefreq>").append(changefreq).append("</changefreq>\n")
                .append("    <priority>").append(priority).append("</priority>\n")
                .append("  </url>\n");
    }

    private List<TemplateSummaryDTO> fetchAllTemplates() {
        List<TemplateSummaryDTO> all = new ArrayList<>();
        int page = 0;
        boolean last;
        do {
            TemplateApiClient.TemplatePage result = templateClient.fetchTemplates(page, 50, null);
            all.addAll(result.content());
            last = result.last();
            page++;
        } while (!last && page < 20);
        return all;
    }
}
