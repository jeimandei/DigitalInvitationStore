package id.baundang.template.repository;

import id.baundang.template.domain.BibleVerse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BibleVerseRepository extends JpaRepository<BibleVerse, UUID> {

    List<BibleVerse> findByTranslationAndCategory(
            BibleVerse.Translation translation, BibleVerse.Category category);

    List<BibleVerse> findByTranslation(BibleVerse.Translation translation);

    List<BibleVerse> findByCategory(BibleVerse.Category category);
}
