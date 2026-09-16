package com.kfokam48.controller;

import com.kfokam48.dto.CommentDTO;
import com.kfokam48.dto.CreateCommentDTO;
import com.kfokam48.entity.Comment.CommentStatus;
import com.kfokam48.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST des commentaires, imbriqués sous un article.
 *
 * L'identifiant d'article présent dans l'URL est systématiquement vérifié :
 * un commentaire appartenant à un autre article renvoie 404.
 */
@RestController
@RequestMapping("/articles/{articleId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Gestion des commentaires")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Ajouter un commentaire à un article")
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable Long articleId,
            @Valid @RequestBody CreateCommentDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(articleId, dto));
    }

    @GetMapping
    @Operation(summary = "Récupérer les commentaires approuvés d'un article")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Long articleId) {
        return ResponseEntity.ok(commentService.getApprovedComments(articleId));
    }

    @GetMapping("/all")
    @Operation(summary = "Récupérer tous les commentaires (admin)")
    public ResponseEntity<List<CommentDTO>> getAllComments(@PathVariable Long articleId) {
        return ResponseEntity.ok(commentService.getAllComments(articleId));
    }

    @PutMapping("/{commentId}/status")
    @Operation(summary = "Modérer un commentaire (APPROVED / REJECTED / PENDING)")
    public ResponseEntity<CommentDTO> updateStatus(
            @PathVariable Long articleId,
            @PathVariable Long commentId,
            @RequestParam CommentStatus status) {
        return ResponseEntity.ok(commentService.updateStatus(articleId, commentId, status));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Supprimer un commentaire")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long articleId,
            @PathVariable Long commentId) {
        commentService.deleteComment(articleId, commentId);
        return ResponseEntity.noContent().build();
    }
}
