package com.kfokam48.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Données reçues pour déposer un commentaire (contenu et identité de l'auteur).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentDTO {

    @NotBlank(message = "Content is required")
    @Size(min = 5, max = 5000, message = "Content must be between 5 and 5000 characters")
    private String content;

    @NotBlank(message = "Author name is required")
    @Size(min = 2, max = 100, message = "Author name must be between 2 and 100 characters")
    private String authorName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String authorEmail;

    @Size(max = 100, message = "Website must not exceed 100 characters")
    private String authorWebsite;
}
