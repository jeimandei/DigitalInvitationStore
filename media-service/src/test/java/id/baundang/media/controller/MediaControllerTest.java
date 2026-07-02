package id.baundang.media.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.media.config.GatewayHeaderFilter;
import id.baundang.media.dto.PresignDownloadResponse;
import id.baundang.media.dto.PresignUploadRequest;
import id.baundang.media.dto.PresignUploadResponse;
import id.baundang.media.dto.UploadedObjectResponse;
import id.baundang.media.service.MinioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = MediaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    MinioService minioService;

    @MockBean
    GatewayHeaderFilter gatewayHeaderFilter;

    @Test
    void presignUpload_returns2xx() throws Exception {
        PresignUploadResponse resp = new PresignUploadResponse("https://minio.url/presign", "couples/test-slug/file.jpg", 15);
        when(minioService.presignUpload(any())).thenReturn(resp);

        String body = objectMapper.writeValueAsString(
                new PresignUploadRequest("file.jpg", "image/jpeg", "couples/test-slug")
        );

        mockMvc.perform(post("/api/v1/media/upload/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void presignUpload_emptyBody_returnsAny() throws Exception {
        // An empty body is missing required fields, so @Valid must reject it with 400.
        when(minioService.presignUpload(any())).thenReturn(
                new PresignUploadResponse("https://minio.url/presign", "couples/test/file.jpg", 15)
        );

        mockMvc.perform(post("/api/v1/media/upload/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void presignDownload_returns200() throws Exception {
        when(minioService.presignDownload(any())).thenReturn(new PresignDownloadResponse("https://minio.url/download", 1));

        mockMvc.perform(get("/api/v1/media/download/some-file")
                        .param("objectKey", "couples/test/file.jpg"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteObject_returns200() throws Exception {
        doNothing().when(minioService).deleteObject(any());

        mockMvc.perform(delete("/api/v1/media/some-file")
                        .param("objectKey", "couples/test/file.jpg"))
                .andExpect(status().isOk());
    }

    @Test
    void uploadTemplate_returns2xx() throws Exception {
        UploadedObjectResponse resp = new UploadedObjectResponse("templates/thumbnails/test.jpg", "https://cdn.url/test.jpg");
        when(minioService.uploadTemplate(any(), any())).thenReturn(resp);

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/media/template/upload").file(file))
                .andExpect(status().is2xxSuccessful());
    }
}
