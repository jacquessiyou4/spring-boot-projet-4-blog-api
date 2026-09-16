package com.kfokam48.repository;

import com.kfokam48.entity.Comment;
import com.kfokam48.entity.Comment.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Accès aux données des commentaires.
 *
 * Propose un comptage groupé pour plusieurs articles à la fois, afin d'éviter
 * une requête par article lors de l'affichage d'une page de résultats.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByArticleIdAndStatusOrderByCreatedAtDesc(Long articleId, CommentStatus status);
    List<Comment> findByArticleIdOrderByCreatedAtDesc(Long articleId);
    long countByArticleIdAndStatus(Long articleId, CommentStatus status);

    /** Comptage groupé : évite une requête par article lors du listing paginé. */
    @Query("SELECT c.article.id, COUNT(c) FROM Comment c " +
           "WHERE c.article.id IN :articleIds AND c.status = :status GROUP BY c.article.id")
    List<Object[]> countByArticleIds(@Param("articleIds") List<Long> articleIds,
                                     @Param("status") CommentStatus status);
}
