package id.baundang.storefront.controller;

import id.baundang.storefront.client.TemplateApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = SitemapController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class SitemapControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TemplateApiClient templateClient;

    @Test
    void sitemap_returns200WithXml() throws Exception {
        TemplateApiClient.TemplatePage emptyPage =
                new TemplateApiClient.TemplatePage(Collections.emptyList(), 0, 50, 0, 0, true);
        when(templateClient.fetchTemplates(anyInt(), anyInt(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<urlset")));
    }

    @Test
    void sitemap_containsStaticPages() throws Exception {
        TemplateApiClient.TemplatePage emptyPage =
                new TemplateApiClient.TemplatePage(Collections.emptyList(), 0, 50, 0, 0, true);
        when(templateClient.fetchTemplates(anyInt(), anyInt(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("baundang.id")));
    }
}
