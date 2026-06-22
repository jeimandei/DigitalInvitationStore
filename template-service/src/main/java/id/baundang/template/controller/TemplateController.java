package id.baundang.template.controller;

import id.baundang.common.ApiResponse;
import id.baundang.common.PagedResponse;
import id.baundang.template.dto.TemplateDTO;
import id.baundang.template.dto.TemplateListDTO;
import id.baundang.template.dto.TemplateRequest;
import id.baundang.template.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService service;

    public TemplateController(TemplateService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TemplateListDTO>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Short priceLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        var pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by("createdAt").descending());
        var result = service.list(category, priceLevel, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(result)));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<TemplateDTO>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(service.getBySlug(slug)));
    }

    @GetMapping("/{slug}/preview")
    public ResponseEntity<Void> preview(@PathVariable String slug) {
        String url = service.getPreviewUrl(slug);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TemplateDTO>> create(
            @Valid @RequestBody TemplateRequest req) {
        TemplateDTO created = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Template created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateDTO>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TemplateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req), "Template updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.softDelete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Template deactivated"));
    }
}
