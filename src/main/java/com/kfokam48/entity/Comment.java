package com.kfokam48.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_article_id", columnList = "article_id")
})
/**
 * Entité JPA d'un commentaire (table « comments »), rattachée à un article.
 *
 * Un commentaire est créé au statut PENDING et n'est visible publiquement
 * qu'une fois passé à APPROVED par la modération.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(name = "author_email", nullable = false, length = 100)
    private String authorEmail;

    @Column(name = "author_website", length = 100)
    private String authorWebsite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CommentStatus status = CommentStatus.PENDING;

    /**
     * Exclu de toString() et equals()/hashCode() : la relation est bidirectionnelle.
     * Lombok @Data génère sinon des méthodes qui s'appellent mutuellement entre
     * les deux entités (article -> commentaires -> article -> ...), ce qui lève
     * un StackOverflowError dès qu'on journalise ou compare une entité chargée.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum CommentStatus {
        PENDING, APPROVED, REJECTED
    }
}
