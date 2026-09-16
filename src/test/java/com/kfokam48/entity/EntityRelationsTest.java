package com.kfokam48.entity;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Régression : Article et Comment se référencent mutuellement. Avec @Data,
 * Lombok générait des toString()/equals()/hashCode() qui s'appelaient en
 * boucle d'une entité à l'autre, provoquant un StackOverflowError dès qu'on
 * journalisait ou comparait un article chargé avec ses commentaires.
 * Les deux extrémités de la relation sont désormais exclues.
 */
class EntityRelationsTest {

    private Article articleWithComment() {
        Article article = Article.builder()
                .id(1L).title("Titre").slug("titre").content("Contenu").build();
        Comment comment = Comment.builder()
                .id(1L).content("Commentaire").authorName("Marie")
                .authorEmail("marie@example.com").article(article).build();
        article.setComments(List.of(comment));
        return article;
    }

    @Test
    void toString_onBidirectionalRelation_shouldNotOverflow() {
        Article article = articleWithComment();

        assertThatCode(article::toString).doesNotThrowAnyException();
        assertThatCode(() -> article.getComments().get(0).toString()).doesNotThrowAnyException();
        // La relation ne doit pas être dépliée dans la représentation textuelle.
        assertThat(article.toString()).doesNotContain("Commentaire");
    }

    @Test
    void hashCode_onBidirectionalRelation_shouldNotOverflow() {
        Article article = articleWithComment();

        assertThatCode(article::hashCode).doesNotThrowAnyException();
        assertThatCode(() -> article.getComments().get(0).hashCode()).doesNotThrowAnyException();
    }

    /** Cas concret : placer des entités dans un Set déclenchait le débordement. */
    @Test
    void entitiesCanBeStoredInHashSet() {
        Article article = articleWithComment();

        Set<Article> articles = new HashSet<>();
        Set<Comment> comments = new HashSet<>();
        assertThatCode(() -> {
            articles.add(article);
            comments.add(article.getComments().get(0));
        }).doesNotThrowAnyException();

        assertThat(articles).hasSize(1);
        assertThat(comments).hasSize(1);
    }
}
