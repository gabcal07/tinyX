package com.epita.repository.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Setter
@Getter

/**
 * Represents a Post entity that is indexed in Elasticsearch.
 */
public class Post {
    /** The unique identifier of the post. */
    private UUID postId;

    /** The full text of the post, including hashtags. */
    private String fullText;

    /** The text content of the post, excluding hashtags. */
    private String text;

    /** A list of hashtags associated with the post. */
    private List<String> hashtags;

    /**
     * Default constructor for JSON deserialization.
     */
    public Post() {}

    /**
     * Constructs a Post with the given ID, text, and hashtags.
     *
     * @param postId   The unique identifier of the post.
     * @param text     The text content of the post.
     * @param hashtags The list of hashtags associated with the post.
     */
    public Post(UUID postId, String text, List<String> hashtags) {
        this.postId = postId;
        this.text = text;
        this.fullText = text;
        this.hashtags = hashtags == null ? List.of() : hashtags;
    }

    /**
     * Constructs a Post from the given ID and text, extracting hashtags from the text.
     *
     * @param postId The unique identifier of the post.
     * @param text   The full text of the post, including hashtags.
     */
    public Post(UUID postId, String text) {
        this.postId = postId;
        List<String> tokens = List.of(text.split(" "));
        this.hashtags = tokens.stream().filter(t -> t.startsWith("#")).toList();
        // join the other words
        this.text = String.join(" " ,tokens.stream().filter(t -> !t.startsWith("#")).toList());
        this.fullText = text;

    }

}
