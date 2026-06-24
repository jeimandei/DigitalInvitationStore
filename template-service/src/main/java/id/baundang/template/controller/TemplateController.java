package id.baundang.template.controller;

import id.baundang.common.ApiResponse;
import id.baundang.common.PagedResponse;
import id.baundang.template.domain.BibleVerse;
import id.baundang.template.dto.BibleVerseDTO;
import id.baundang.template.dto.TemplateDTO;
import id.baundang.template.dto.TemplateListDTO;
import id.baundang.template.dto.TemplateRequest;
import id.baundang.template.repository.BibleVerseRepository;
import id.baundang.template.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService service;
    private final BibleVerseRepository bibleVerseRepository;

    public TemplateController(TemplateService service, BibleVerseRepository bibleVerseRepository) {
        this.service = service;
        this.bibleVerseRepository = bibleVerseRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TemplateListDTO>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Short priceLevel,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        var pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by("createdAt").descending());
        var result = service.list(category, priceLevel, includeInactive, pageable);
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

    @PutMapping("/{id}/active")
    public ResponseEntity<ApiResponse<TemplateDTO>> setActive(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        TemplateDTO dto = service.setActive(id, active);
        return ResponseEntity.ok(ApiResponse.ok(dto,
                active ? "Template activated" : "Template deactivated"));
    }

    @GetMapping("/christian/verses")
    public ResponseEntity<ApiResponse<List<BibleVerseDTO>>> listVerses(
            @RequestParam(required = false) String translation,
            @RequestParam(required = false) String category) {

        List<BibleVerse> verses;
        BibleVerse.Translation t = parseEnum(BibleVerse.Translation.class, translation);
        BibleVerse.Category    c = parseEnum(BibleVerse.Category.class, category);

        if (t != null && c != null) {
            verses = bibleVerseRepository.findByTranslationAndCategory(t, c);
        } else if (t != null) {
            verses = bibleVerseRepository.findByTranslation(t);
        } else if (c != null) {
            verses = bibleVerseRepository.findByCategory(c);
        } else {
            verses = bibleVerseRepository.findAll();
        }

        return ResponseEntity.ok(ApiResponse.ok(
                verses.stream().map(BibleVerseDTO::from).toList()));
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(cls, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
