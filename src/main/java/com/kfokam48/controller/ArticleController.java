package com.kfokam48.controller;

import com.kfokam48.dto.ArticleDTO;
import com.kfokam48.dto.CreateArticleDTO;
import com.kfokam48.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints REST des articles.
 *
 * Couvre les fonctionnalités demandées : création, lecture de tous les articles
 * (paginée), lecture d'un article précis (par slug ou par identifiant), mise à
 * jour, suppression et recherche plein texte.
 */
@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
@Tag(name = "Articles", description = "Gestion des articles de blog")
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping
    @Operation(summary = "Créer un article")
    public ResponseEntity<ArticleDTO> createArticle(@Valid @RequestBody CreateArticleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articleService.createArticle(dto));
    }

    @GetMapping
    @Operation(summary = "Récupérer tous les articles (paginés)")
    public ResponseEntity<Page<ArticleDTO>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(articleService.getAllArticles(page, size));
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "Récupérer un article par identifiant")
    public ResponseEntity<ArticleDTO> getArticleById(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Récupérer un article par slug")
    public ResponseEntity<ArticleDTO> getArticleBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(articleService.getArticleBySlug(slug));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un article")
    public ResponseEntity<ArticleDTO> updateArticle(
            @PathVariable Long id, @Valid @RequestBody CreateArticleDTO dto) {
        return ResponseEntity.ok(articleService.updateArticle(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un article")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des articles")
    public ResponseEntity<Page<ArticleDTO>> searchArticles(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(articleService.searchArticles(query, page, size));
    }
}
