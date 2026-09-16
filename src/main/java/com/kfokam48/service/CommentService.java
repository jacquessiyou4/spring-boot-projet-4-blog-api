package com.kfokam48.service;

import com.kfokam48.dto.CommentDTO;
import com.kfokam48.dto.CreateCommentDTO;
import com.kfokam48.entity.Article;
import com.kfokam48.entity.Comment;
import com.kfokam48.entity.Comment.CommentStatus;
import com.kfokam48.exception.ResourceNotFoundException;
import com.kfokam48.repository.ArticleRepository;
import com.kfokam48.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Couche Service : logique métier des commentaires.
 *
 * Gère le dépôt d'un commentaire, sa modération (PENDING → APPROVED/REJECTED)
 * et sa suppression, en vérifiant toujours son rattachement à l'article visé.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;

    @Transactional
    public CommentDTO addComment(Long articleId, CreateCommentDTO dto) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + articleId));

        Comment comment = Comment.builder()
                .content(dto.getContent())
                .authorName(dto.getAuthorName())
                .authorEmail(dto.getAuthorEmail())
                .authorWebsite(dto.getAuthorWebsite())
                .article(article)
                .status(CommentStatus.PENDING)
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("Comment added to article {} by {}", articleId, dto.getAuthorName());
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentDTO> getApprovedComments(Long articleId) {
        return commentRepository.findByArticleIdAndStatusOrderByCreatedAtDesc(articleId, CommentStatus.APPROVED)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentDTO> getAllComments(Long articleId) {
        return commentRepository.findByArticleIdOrderByCreatedAtDesc(articleId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Modération : un commentaire est créé en PENDING et n'apparaît publiquement
     * qu'une fois APPROVED. Sans ce point d'entrée, aucun commentaire ne pouvait
     * jamais être publié.
     */
    @Transactional
    public CommentDTO updateStatus(Long articleId, Long commentId, CommentStatus status) {
        Comment comment = findCommentOfArticle(articleId, commentId);
        comment.setStatus(status);
        log.info("Comment {} moved to status {}", commentId, status);
        return toDTO(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long articleId, Long commentId) {
        commentRepository.delete(findCommentOfArticle(articleId, commentId));
        log.info("Comment deleted with id: {}", commentId);
    }

    /**
     * L'identifiant d'article de l'URL était ignoré : on pouvait supprimer via
     * /articles/1/comments/42 un commentaire appartenant à l'article 7.
     */
    private Comment findCommentOfArticle(Long articleId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        if (!comment.getArticle().getId().equals(articleId)) {
            throw new ResourceNotFoundException(
                    "Comment " + commentId + " does not belong to article " + articleId);
        }
        return comment;
    }

    private CommentDTO toDTO(Comment comment) {
        return new CommentDTO(
                comment.getId(), comment.getContent(),
                comment.getAuthorName(), comment.getAuthorEmail(),
                comment.getAuthorWebsite(), comment.getStatus(),
                comment.getArticle().getId(),
                comment.getCreatedAt(), comment.getUpdatedAt()
        );
    }
}
