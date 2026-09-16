package com.kfokam48.repository;

import com.kfokam48.entity.Article;
import com.kfokam48.entity.Article.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accès aux données des articles.
 *
 * Fournit la recherche par slug, le filtrage par statut ou catégorie, une
 * recherche plein texte, et l'incrément atomique du compteur de vues.
 */
@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySlug(String slug);
    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);
    Page<Article> findByCategory(String category, Pageable pageable);

    /**
     * Incrément atomique : l'ancienne lecture-modification-écriture perdait des
     * vues en concurrence et déclenchait @UpdateTimestamp, si bien que updatedAt
     * changeait à chaque simple consultation.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT a FROM Article a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Article> searchByQuery(@Param("query") String query, Pageable pageable);
}
