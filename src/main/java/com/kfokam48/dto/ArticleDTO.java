package com.kfokam48.dto;

import com.kfokam48.entity.Article.ArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Représentation d'un article renvoyée au client.
 *
 * Le champ « comments » n'est renseigné que pour la lecture d'un article seul ;
 * les listes paginées ne portent que « commentCount », afin d'éviter de charger
 * les commentaires de chaque article (problème N+1).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDTO {
    private Long id;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private String author;
    private String category;
    private String tags;
    private ArticleStatus status;
    private Integer viewCount;
    private LocalDateTime publicationDate;
    private Integer commentCount;
    private List<CommentDTO> comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
