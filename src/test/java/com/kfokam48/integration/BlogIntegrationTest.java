package com.kfokam48.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.dto.CreateArticleDTO;
import com.kfokam48.dto.CreateCommentDTO;
import com.kfokam48.entity.Article.ArticleStatus;
import com.kfokam48.repository.ArticleRepository;
import com.kfokam48.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        articleRepository.deleteAll();
    }

    private JsonNode createArticle(String title, String author, ArticleStatus status) throws Exception {
        String longContent = "A".repeat(150);
        CreateArticleDTO dto = new CreateArticleDTO();
        dto.setTitle(title);
        dto.setContent(longContent);
        dto.setAuthor(author);
        dto.setCategory("Tech");
        dto.setStatus(status);

        MvcResult result = mockMvc.perform(post("/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void fullArticleAndCommentFlow() throws Exception {
        // CREATE ARTICLE
        JsonNode article = createArticle("My First Article", "Author Name", ArticleStatus.PUBLISHED);
        long articleId = article.get("id").asLong();
        String slug = article.get("slug").asText();

        // GET ARTICLES
        mockMvc.perform(get("/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // GET BY SLUG
        mockMvc.perform(get("/articles/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount", is(1)));

        // ADD COMMENT
        CreateCommentDTO commentDto = new CreateCommentDTO("Great article!", "Reader", "reader@email.com", null);
        mockMvc.perform(post("/articles/" + articleId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName", is("Reader")));

        // GET COMMENTS (approved only - should be empty since PENDING)
        mockMvc.perform(get("/articles/" + articleId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // GET ALL COMMENTS (admin)
        mockMvc.perform(get("/articles/" + articleId + "/comments/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // SEARCH
        mockMvc.perform(get("/articles/search").param("query", "First"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // UPDATE ARTICLE
        String longContent = "B".repeat(150);
        CreateArticleDTO updateDto = new CreateArticleDTO();
        updateDto.setTitle("Updated Article Title");
        updateDto.setContent(longContent);
        updateDto.setStatus(ArticleStatus.PUBLISHED);

        mockMvc.perform(put("/articles/" + articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Article Title")));

        // DELETE ARTICLE
        mockMvc.perform(delete("/articles/" + articleId))
                .andExpect(status().isNoContent());

        // VERIFY DELETION
        mockMvc.perform(get("/articles/" + slug))
                .andExpect(status().isNotFound());
    }

    @Test
    void createArticle_withInvalidData_shouldReturn400() throws Exception {
        mockMvc.perform(post("/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"short\", \"content\": \"too short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getArticle_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/articles/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addComment_withInvalidData_shouldReturn400() throws Exception {
        mockMvc.perform(post("/articles/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addComment_toNonexistentArticle_shouldReturn404() throws Exception {
        CreateCommentDTO dto = new CreateCommentDTO("Comment", "Name", "e@e.com", null);
        mockMvc.perform(post("/articles/999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Cycle de modération complet : un commentaire créé en PENDING reste invisible
     * jusqu'à son approbation. Sans l'endpoint /status, aucun commentaire ne
     * pouvait jamais être publié.
     */
    @Test
    void commentModerationFlow() throws Exception {
        JsonNode article = createArticle("Moderation Test Article", "Author", ArticleStatus.PUBLISHED);
        long articleId = article.get("id").asLong();

        MvcResult created = mockMvc.perform(post("/articles/" + articleId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Merci pour cet article\", \"authorName\": \"Reader\", " +
                                 "\"authorEmail\": \"reader@email.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn();
        long commentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Invisible tant qu'il n'est pas approuvé
        mockMvc.perform(get("/articles/" + articleId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Un autre article ne peut pas modérer ce commentaire
        JsonNode other = createArticle("Another Article Entirely", "Author", ArticleStatus.PUBLISHED);
        mockMvc.perform(put("/articles/" + other.get("id").asLong() + "/comments/" + commentId + "/status")
                        .param("status", "APPROVED"))
                .andExpect(status().isNotFound());

        // Approbation
        mockMvc.perform(put("/articles/" + articleId + "/comments/" + commentId + "/status")
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        mockMvc.perform(get("/articles/" + articleId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].authorName", is("Reader")));
    }

    /** Le slug reste stable après modification du titre (permalien). */
    @Test
    void updatingTitle_shouldKeepSlug() throws Exception {
        JsonNode article = createArticle("Original Stable Title", "Author", ArticleStatus.PUBLISHED);
        long id = article.get("id").asLong();
        String slug = article.get("slug").asText();

        mockMvc.perform(put("/articles/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"A Brand New Title\", \"content\": \"" + "B".repeat(150) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug", is(slug)));

        mockMvc.perform(get("/articles/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("A Brand New Title")));
    }

    /** Le cahier des charges demande de pouvoir lire un article spécifique. */
    @Test
    void getArticleById_shouldReturnArticle() throws Exception {
        JsonNode article = createArticle("Readable By Id Article", "Author", ArticleStatus.PUBLISHED);
        long id = article.get("id").asLong();

        mockMvc.perform(get("/articles/id/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)))
                .andExpect(jsonPath("$.title", is("Readable By Id Article")));

        mockMvc.perform(get("/articles/id/999999"))
                .andExpect(status().isNotFound());
    }
}
