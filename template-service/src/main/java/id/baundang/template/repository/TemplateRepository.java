package id.baundang.template.repository;

import id.baundang.template.domain.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {

    @Query("""
            SELECT t FROM Template t
            WHERE t.active = true
              AND (:category IS NULL OR t.category = :category)
              AND (:priceLevel IS NULL OR t.priceLevel = :priceLevel)
            """)
    Page<Template> findAllActive(
            @Param("category")   Template.Category category,
            @Param("priceLevel") Short priceLevel,
            Pageable pageable);

    Optional<Template> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);
}
