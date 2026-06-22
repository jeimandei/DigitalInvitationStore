package id.baundang.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.common.exception.NotFoundException;
import id.baundang.common.exception.ValidationException;
import id.baundang.template.domain.Template;
import id.baundang.template.domain.TemplateFeature;
import id.baundang.template.dto.TemplateDTO;
import id.baundang.template.dto.TemplateListDTO;
import id.baundang.template.dto.TemplateRequest;
import id.baundang.template.repository.TemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TemplateService {

    private final TemplateRepository repo;
    private final MinioPresignService presignService;
    private final ObjectMapper objectMapper;

    public TemplateService(TemplateRepository repo,
                           MinioPresignService presignService,
                           ObjectMapper objectMapper) {
        this.repo           = repo;
        this.presignService = presignService;
        this.objectMapper   = objectMapper;
    }

    public Page<TemplateListDTO> list(String categoryParam, Short priceLevel, Pageable pageable) {
        Template.Category category = null;
        if (categoryParam != null && !categoryParam.isBlank()) {
            try {
                category = Template.Category.valueOf(categoryParam.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid category: " + categoryParam);
            }
        }
        return repo.findAllActive(category, priceLevel, pageable)
                   .map(TemplateListDTO::from);
    }

    public TemplateDTO getBySlug(String slug) {
        return repo.findBySlugAndActiveTrue(slug)
                   .map(TemplateDTO::from)
                   .orElseThrow(() -> new NotFoundException("Template", slug));
    }

    public String getPreviewUrl(String slug) {
        Template t = repo.findBySlugAndActiveTrue(slug)
                         .orElseThrow(() -> new NotFoundException("Template", slug));
        String objectKey = "previews/" + t.getSlug();
        return presignService.presignedPreviewUrl(objectKey);
    }

    @Transactional
    public TemplateDTO create(TemplateRequest req) {
        validateSlugUnique(req.slug(), null);
        Template t = applyRequest(new Template(), req);
        return TemplateDTO.from(repo.save(t));
    }

    @Transactional
    public TemplateDTO update(UUID id, TemplateRequest req) {
        Template t = repo.findById(id)
                         .orElseThrow(() -> new NotFoundException("Template", id));
        if (!t.getSlug().equals(req.slug())) {
            validateSlugUnique(req.slug(), id);
        }
        return TemplateDTO.from(repo.save(applyRequest(t, req)));
    }

    @Transactional
    public void softDelete(UUID id) {
        Template t = repo.findById(id)
                         .orElseThrow(() -> new NotFoundException("Template", id));
        t.setActive(false);
    }

    private Template applyRequest(Template t, TemplateRequest req) {
        t.setName(req.name());
        t.setSlug(req.slug());
        t.setDescription(req.description());
        t.setCategory(parseEnum(Template.Category.class, req.category(), "category"));
        t.setStylePreset(req.stylePreset() != null
                ? parseEnum(Template.StylePreset.class, req.stylePreset(), "stylePreset")
                : null);
        t.setPriceLevel(req.priceLevel());
        t.setThumbnailUrl(req.thumbnailUrl());
        t.setConfig(req.config() != null ? req.config() : objectMapper.createObjectNode());

        validateStylePreset(t);

        t.getFeatures().clear();
        if (req.features() != null) {
            for (Map.Entry<String, String> entry : req.features().entrySet()) {
                t.getFeatures().add(new TemplateFeature(t, entry.getKey(), entry.getValue()));
            }
        }
        return t;
    }

    private void validateSlugUnique(String slug, UUID excludeId) {
        if (repo.existsBySlug(slug)) {
            throw new ValidationException("Slug already in use: " + slug);
        }
    }

    private void validateStylePreset(Template t) {
        if (t.getStylePreset() != null && t.getCategory() != Template.Category.CHRISTIAN) {
            throw new ValidationException("stylePreset is only allowed for CHRISTIAN category");
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String value, String field) {
        try {
            return Enum.valueOf(cls, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid " + field + ": " + value);
        }
    }
}
