package com.kfokam48.service;

import com.kfokam48.dto.CommentDTO;
import com.kfokam48.dto.CreateCommentDTO;
import com.kfokam48.entity.Article;
import com.kfokam48.entity.Comment;
import com.kfokam48.entity.Comment.CommentStatus;
import com.kfokam48.exception.ResourceNotFoundException;
import com.kfokam48.repository.ArticleRepository;
import com.kfokam48.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private CommentService commentService;

    private Article sampleArticle;
    private Comment sampleComment;

    @BeforeEach
    void setUp() {
        sampleArticle = Article.builder()
                .id(1L).title("Article").slug("article").content("Content").build();

        sampleComment = Comment.builder()
                .id(1L)
                .content("Great article!")
                .authorName("Reader")
                .authorEmail("reader@email.com")
                .status(CommentStatus.PENDING)
                .article(sampleArticle)
                .build();
    }

    @Test
    void addComment_shouldReturnCommentDTO() {
        CreateCommentDTO dto = new CreateCommentDTO("Great article!", "Reader", "reader@email.com", null);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));
        when(commentRepository.save(any(Comment.class))).thenReturn(sampleComment);

        CommentDTO result = commentService.addComment(1L, dto);

        assertThat(result).isNotNull();
        assertThat(result.getAuthorName()).isEqualTo("Reader");
        assertThat(result.getStatus()).isEqualTo(CommentStatus.PENDING);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_whenArticleNotFound_shouldThrow() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(99L,
                        new CreateCommentDTO("text", "Name", "e@e.com", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getApprovedComments_shouldReturnFilteredList() {
        Comment approved = Comment.builder()
                .id(2L).content("Approved").status(CommentStatus.APPROVED)
                .article(sampleArticle).build();
        when(commentRepository.findByArticleIdAndStatusOrderByCreatedAtDesc(1L, CommentStatus.APPROVED))
                .thenReturn(Arrays.asList(approved));

        List<CommentDTO> result = commentService.getApprovedComments(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(CommentStatus.APPROVED);
    }

    @Test
    void getAllComments_shouldReturnAll() {
        when(commentRepository.findByArticleIdOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(sampleComment));

        List<CommentDTO> result = commentService.getAllComments(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void deleteComment_whenExists_shouldDelete() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(sampleComment));

        commentService.deleteComment(1L, 1L);

        verify(commentRepository).delete(sampleComment);
    }

    @Test
    void deleteComment_whenNotExists_shouldThrow() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /** L'identifiant d'article de l'URL doit être vérifié, pas ignoré. */
    @Test
    void deleteComment_whenCommentBelongsToAnotherArticle_shouldThrow() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(sampleComment));

        assertThatThrownBy(() -> commentService.deleteComment(7L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not belong to article 7");
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void updateStatus_shouldApproveComment() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(sampleComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(sampleComment);

        CommentDTO result = commentService.updateStatus(1L, 1L, CommentStatus.APPROVED);

        assertThat(result.getStatus()).isEqualTo(CommentStatus.APPROVED);
        verify(commentRepository).save(sampleComment);
    }

    @Test
    void updateStatus_whenCommentBelongsToAnotherArticle_shouldThrow() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(sampleComment));

        assertThatThrownBy(() -> commentService.updateStatus(7L, 1L, CommentStatus.APPROVED))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(commentRepository, never()).save(any(Comment.class));
    }
}
