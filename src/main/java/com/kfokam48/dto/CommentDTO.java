package com.kfokam48.dto;

import com.kfokam48.entity.Comment.CommentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Représentation d'un commentaire renvoyée au client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private String content;
    private String authorName;
    private String authorEmail;
    private String authorWebsite;
    private CommentStatus status;
    private Long articleId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
