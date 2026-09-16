package com.kfokam48.dto;

import com.kfokam48.entity.Article.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Données reçues pour créer ou mettre à jour un article.
 *
 * Le titre et le contenu sont obligatoires et bornés ; la date de publication
 * est facultative, conformément au cahier des charges.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateArticleDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 100, max = 50000, message = "Content must be between 100 and 50000 characters")
    private String content;

    @Size(max = 500, message = "Excerpt must not exceed 500 characters")
    private String excerpt;

    @Size(max = 100, message = "Author must not exceed 100 characters")
    private String author;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    private String tags;

    private ArticleStatus status;

    // Date de publication (facultative). Si non fournie, la date de création est utilisée.
    private LocalDateTime publicationDate;
}
