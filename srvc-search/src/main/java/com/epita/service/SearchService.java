package com.epita.service;

import com.epita.events.UserActionEvent;
import com.epita.repository.ElasticSearchRepository;
import com.epita.repository.entity.Post;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;


/**
 * Service responsible for handling search operations and post management.
 */
@ApplicationScoped
public class SearchService {

    @Inject
    ElasticSearchRepository elasticSearchRepository;

    @Inject
    Logger logger;

    /**
     * Creates a new post from a user action event and indexes it in Elasticsearch.
     *
     * @param userActionEvent The event containing post details.
     */
    public void createPost(UserActionEvent userActionEvent) {
        UUID postId = UUID.fromString(userActionEvent.getPostId());
        Post p = new Post(postId, userActionEvent.getPostContent());
        elasticSearchRepository.indexPost(p);
    }

    /**
     * Deletes a post based on the provided user action event.
     *
     * @param userActionEvent The event containing the post ID to be deleted.
     */
    public void deletePost(UserActionEvent userActionEvent) {
        UUID postId = UUID.fromString(userActionEvent.getPostId());
        elasticSearchRepository.deletePost(postId);
    }

    /**
     * Searches for posts based on a query string.
     *
     * @param query The search query containing words and/or hashtags.
     * @return A list of posts matching the search criteria.
     */
    public List<Post> searchPost(String query) {
        List<String> tokens = Arrays.stream(query.split(" ")).toList();
        List<String> hashtags = tokens.stream().filter(s -> s.startsWith("#")).toList();
        List<String> text = tokens.stream().filter(s -> !s.startsWith("#")).toList();

        logger.info("Searching for posts with text: " + text + " and hashtags: " + hashtags);
        return elasticSearchRepository.searchPosts(text, hashtags);
    }

    /**
     * Retrieves all posts stored in Elasticsearch (for tests purposes ).
     *
     * @return A list of all indexed posts.
     */
    public List<Post> getAllPosts() {
        return elasticSearchRepository.getAllPosts();
    }
}