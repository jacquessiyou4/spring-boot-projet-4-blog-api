package com.kfokam48.service;

import com.kfokam48.dto.ArticleDTO;
import com.kfokam48.dto.CommentDTO;
import com.kfokam48.dto.CreateArticleDTO;
import com.kfokam48.entity.Article;
import com.kfokam48.entity.Article.ArticleStatus;
import com.kfokam48.entity.Comment;
import com.kfokam48.entity.Comment.CommentStatus;
import com.kfokam48.exception.ResourceNotFoundException;
import com.kfokam48.repository.ArticleRepository;
import com.kfokam48.repository.CommentRepository;
import com.kfokam48.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Couche Service : logique métier des articles.
 *
 * Génère un slug unique à la création, incrémente les vues à la consultation,
 * et construit deux formes de DTO : une vue détaillée (contenu et commentaires
 * approuvés) et une vue résumée pour les listes paginées.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public ArticleDTO createArticle(CreateArticleDTO dto) {
        String slug = generateUniqueSlug(dto.getTitle());

        Article article = Article.builder()
                .title(dto.getTitle())
                .slug(slug)
                .content(dto.getContent())
                .excerpt(dto.getExcerpt())
                .author(dto.getAuthor())
                .category(dto.getCategory())
                .tags(dto.getTags())
                .status(dto.getStatus() != null ? dto.getStatus() : ArticleStatus.DRAFT)
                .publicationDate(dto.getPublicationDate())
                .build();

        Article saved = articleRepository.save(article);
        log.info("Article created with id: {} and slug: {}", saved.getId(), saved.getSlug());
        return toDetailDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<ArticleDTO> getAllArticles(int page, int size) {
        return toSummaryPage(articleRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @Transactional
    public ArticleDTO getArticleBySlug(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));
        articleRepository.incrementViewCount(article.getId());
        // clearAutomatically vide le contexte de persistance : on relit la valeur à jour.
        article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));
        return toDetailDTO(article);
    }

    @Transactional
    public ArticleDTO getArticleById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        articleRepository.incrementViewCount(id);
        article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        return toDetailDTO(article);
    }

    @Transactional
    public ArticleDTO updateArticle(Long id, CreateArticleDTO dto) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        article.setTitle(dto.getTitle());
        // Le slug n'est PAS régénéré : il sert de permalien public et le changer
        // à chaque édition du titre casserait les liens déjà partagés.
        article.setContent(dto.getContent());
        article.setExcerpt(dto.getExcerpt());
        article.setAuthor(dto.getAuthor());
        article.setCategory(dto.getCategory());
        article.setTags(dto.getTags());
        if (dto.getStatus() != null) article.setStatus(dto.getStatus());
        if (dto.getPublicationDate() != null) article.setPublicationDate(dto.getPublicationDate());
        return toDetailDTO(articleRepository.save(article));
    }

    @Transactional
    public void deleteArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        articleRepository.delete(article);
        log.info("Article deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<ArticleDTO> searchArticles(String query, int page, int size) {
        return toSummaryPage(articleRepository.searchByQuery(query, PageRequest.of(page, size)));
    }

    private String generateUniqueSlug(String title) {
        return generateUniqueSlug(title, null);
    }

    private String generateUniqueSlug(String title, Long excludeId) {
        String slug = SlugGenerator.toSlug(title);
        // Fallback pour les titres sans caractères alphanumériques (ex: "!!!!!").
        if (slug == null || slug.isBlank()) {
            slug = "article";
        }
        int counter = 1;
        String baseSlug = slug;
        while (true) {
            Optional<Article> existing = articleRepository.findBySlug(slug);
            if (existing.isEmpty() || existing.get().getId().equals(excludeId)) {
                break;
            }
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }

    /**
     * Vue détaillée : contenu complet + commentaires approuvés.
     * Réservée aux accès à un seul article (création, mise à jour, lecture par slug).
     */
    private ArticleDTO toDetailDTO(Article article) {
        List<CommentDTO> comments = commentRepository
                .findByArticleIdAndStatusOrderByCreatedAtDesc(article.getId(), CommentStatus.APPROVED)
                .stream()
                .map(this::toCommentDTO)
                .collect(Collectors.toList());
        return buildDTO(article, comments.size(), comments);
    }

    /**
     * Vue liste : pas de commentaires embarqués et comptage groupé en une requête.
     * L'ancienne implémentation exécutait 2 requêtes supplémentaires PAR article
     * (soit 21 requêtes pour une page de 10) et renvoyait le contenu intégral de
     * chaque article accompagné de tous ses commentaires.
     */
    private Page<ArticleDTO> toSummaryPage(Page<Article> articles) {
        List<Long> ids = articles.getContent().stream().map(Article::getId).collect(Collectors.toList());
        Map<Long, Integer> counts = ids.isEmpty() ? Map.of() : commentRepository
                .countByArticleIds(ids, CommentStatus.APPROVED).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()));
        return articles.map(article -> buildDTO(article, counts.getOrDefault(article.getId(), 0), null));
    }

    private ArticleDTO buildDTO(Article article, int commentCount, List<CommentDTO> comments) {
        return new ArticleDTO(
                article.getId(), article.getTitle(), article.getSlug(),
                article.getContent(), article.getExcerpt(), article.getAuthor(),
                article.getCategory(), article.getTags(), article.getStatus(),
                article.getViewCount(), article.getPublicationDate(), commentCount, comments,
                article.getCreatedAt(), article.getUpdatedAt()
        );
    }

    private CommentDTO toCommentDTO(Comment comment) {
        return new CommentDTO(
                comment.getId(), comment.getContent(),
                comment.getAuthorName(), comment.getAuthorEmail(),
                comment.getAuthorWebsite(), comment.getStatus(),
                comment.getArticle().getId(),
                comment.getCreatedAt(), comment.getUpdatedAt()
        );
    }
}
