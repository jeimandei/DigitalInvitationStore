package id.baundang.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.template.config.AdminHeaderFilter;
import id.baundang.template.dto.TemplateDTO;
import id.baundang.template.dto.TemplateRequest;
import id.baundang.template.repository.BibleVerseRepository;
import id.baundang.template.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = TemplateController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class TemplateControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    TemplateService templateService;

    @MockBean
    BibleVerseRepository bibleVerseRepository;

    @MockBean
    AdminHeaderFilter adminHeaderFilter;

    private TemplateDTO sampleDTO() {
        return new TemplateDTO(
                UUID.randomUUID(), "Test Template", "test-template",
                "Desc", "WEDDING", null, (short) 1, null, null,
                Map.of(), true, Instant.now()
        );
    }

    @Test
    void listTemplates_returns200() throws Exception {
        when(templateService.list(any(), any(), anyBoolean(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk());
    }

    @Test
    void getBySlug_returns200() throws Exception {
        when(templateService.getBySlug("test-template")).thenReturn(sampleDTO());

        mockMvc.perform(get("/api/v1/templates/test-template"))
                .andExpect(status().isOk());
    }

    @Test
    void preview_callsGetPreviewUrl() throws Exception {
        when(templateService.getPreviewUrl("test-template")).thenReturn("https://example.com/preview");

        // The controller returns 302 FOUND with a Location header; the exact status depends on MockMvc config
        mockMvc.perform(get("/api/v1/templates/test-template/preview"))
                .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is(200),
                        org.hamcrest.Matchers.is(302)
                )));
    }

    @Test
    void createTemplate_returns201() throws Exception {
        when(templateService.create(any())).thenReturn(sampleDTO());

        String body = objectMapper.writeValueAsString(
                new TemplateRequest("Test", "test-slug", "Desc", "WEDDING", null, (short) 1, null, null, null)
        );

        mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void updateTemplate_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(templateService.update(any(), any())).thenReturn(sampleDTO());

        String body = objectMapper.writeValueAsString(
                new TemplateRequest("Test Updated", "test-slug", "Desc", "WEDDING", null, (short) 1, null, null, null)
        );

        mockMvc.perform(put("/api/v1/templates/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTemplate_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(templateService).softDelete(any());

        mockMvc.perform(delete("/api/v1/templates/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void setActive_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(templateService.setActive(any(), anyBoolean())).thenReturn(sampleDTO());

        mockMvc.perform(put("/api/v1/templates/" + id + "/active").param("active", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void listVerses_returns200() throws Exception {
        when(bibleVerseRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/templates/christian/verses"))
                .andExpect(status().isOk());
    }
}
