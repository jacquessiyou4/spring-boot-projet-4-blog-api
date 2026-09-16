package com.kfokam48.service;

import com.kfokam48.dto.ArticleDTO;
import com.kfokam48.dto.CreateArticleDTO;
import com.kfokam48.entity.Article;
import com.kfokam48.entity.Article.ArticleStatus;
import com.kfokam48.exception.ResourceNotFoundException;
import com.kfokam48.repository.ArticleRepository;
import com.kfokam48.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private ArticleService articleService;

    private Article sampleArticle;

    @BeforeEach
    void setUp() {
        sampleArticle = Article.builder()
                .id(1L)
                .title("Test Article")
                .slug("test-article")
                .content("This is a test article with enough content to pass validation checks for the minimum length requirement.")
                .author("Author")
                .category("Tech")
                .status(ArticleStatus.PUBLISHED)
                .viewCount(0)
                .build();
    }

    @Test
    void createArticle_shouldReturnDTO() {
        CreateArticleDTO dto = new CreateArticleDTO();
        dto.setTitle("Test Article");
        dto.setContent("This is a test article with enough content to pass validation checks for the minimum length requirement.");
        dto.setAuthor("Author");

        when(articleRepository.save(any(Article.class))).thenReturn(sampleArticle);

        ArticleDTO result = articleService.createArticle(dto);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Article");
        verify(articleRepository).save(any(Article.class));
    }

    @Test
    void getAllArticles_shouldReturnPage() {
        Page<Article> page = new PageImpl<>(Arrays.asList(sampleArticle));
        when(articleRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<ArticleDTO> result = articleService.getAllArticles(0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getArticleBySlug_whenExists_shouldReturnDTO() {
        when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(sampleArticle));

        ArticleDTO result = articleService.getArticleBySlug("test-article");

        assertThat(result).isNotNull();
        assertThat(result.getSlug()).isEqualTo("test-article");
        // Le compteur de vues est incrémenté par une requête atomique dédiée,
        // et non par un save() qui modifierait aussi updatedAt.
        verify(articleRepository).incrementViewCount(1L);
        verify(articleRepository, never()).save(any(Article.class));
    }

    @Test
    void getArticleBySlug_whenNotExists_shouldThrow() {
        when(articleRepository.findBySlug("not-found")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.getArticleBySlug("not-found"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /** Le slug est un permalien : il doit survivre à un changement de titre. */
    @Test
    void updateArticle_shouldNotChangeSlug() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(sampleArticle);

        CreateArticleDTO dto = new CreateArticleDTO(
                "A Completely Different Title",
                "C".repeat(150), null, null, null, null, null, null);
        articleService.updateArticle(1L, dto);

        assertThat(sampleArticle.getSlug()).isEqualTo("test-article");
        assertThat(sampleArticle.getTitle()).isEqualTo("A Completely Different Title");
    }

    @Test
    void updateArticle_whenExists_shouldReturnUpdated() {
        CreateArticleDTO dto = new CreateArticleDTO();
        dto.setTitle("Updated Title");
        dto.setContent("Updated content with enough length to pass validation checks for the minimum length requirement.");
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(sampleArticle);

        ArticleDTO result = articleService.updateArticle(1L, dto);

        assertThat(result).isNotNull();
        verify(articleRepository).save(any(Article.class));
    }

    @Test
    void deleteArticle_whenExists_shouldDelete() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));

        articleService.deleteArticle(1L);

        verify(articleRepository).delete(sampleArticle);
    }

    @Test
    void deleteArticle_whenNotExists_shouldThrow() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.deleteArticle(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
