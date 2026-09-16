package com.kfokam48.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA d'un article (table « articles »).
 *
 * Porte le titre, le contenu et la date de publication exigés, ainsi qu'un slug
 * unique servant de permalien, un statut (brouillon / publié), un compteur de
 * vues et la liste de ses commentaires.
 */
@Entity
@Table(name = "articles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 250)
    private String slug;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 500)
    private String excerpt;

    @Column(length = 100)
    private String author;

    @Column(length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ArticleStatus status = ArticleStatus.DRAFT;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "publication_date")
    private LocalDateTime publicationDate;

    /**
     * Exclu de toString() et equals()/hashCode() : la relation est bidirectionnelle.
     * Lombok @Data génère sinon des méthodes qui s'appellent mutuellement entre
     * les deux entités (article -> commentaires -> article -> ...), ce qui lève
     * un StackOverflowError dès qu'on journalise ou compare une entité chargée.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ArticleStatus {
        DRAFT, PUBLISHED
    }
}
