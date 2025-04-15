package com.epita.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.epita.repository.entity.Post;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Repository for handling Elasticsearch operations related to {@link Post}.
 */
@ApplicationScoped
public class ElasticSearchRepository {
    @Inject
    ElasticsearchClient elasticsearchClient;

    @Inject
    Logger logger;

    /**
     * Indexes a post in Elasticsearch.
     *
     * @param post The post to be indexed.
     * @throws RuntimeException if indexing fails.
     */
    public void indexPost(Post post) {
        IndexRequest<Post> indexRequest = IndexRequest.of(request -> request
                .index("posts")
                .id(post.getPostId().toString())
                .document(post)
        );

        try {
            elasticsearchClient.index(indexRequest);
        } catch (IOException e) {
            throw new RuntimeException("Cant index the post: " + post + " error:  "+ e);
        }

    }

    /**
     * Deletes a post from Elasticsearch by its ID.
     *
     * @param postId The UUID of the post to be deleted.
     * @throws RuntimeException if deletion fails.
     */
    public void deletePost(UUID postId) {
        try {
            elasticsearchClient.delete(d -> d
                    .index("posts")
                    .id(postId.toString())
            );
        } catch (IOException e) {
            throw new RuntimeException("Cannot delete the post: " + postId + ", error: " + e);
        }
    }

    /**
     * Retrieves all posts from Elasticsearch (for tests purposes).
     *
     * @return A list of all posts.
     * @throws RuntimeException if retrieval fails.
     */
    public List<Post> getAllPosts(){
        try {
            SearchRequest allPostsRequest = SearchRequest.of(r -> r
                    .index("posts")
                    .query(q -> q.matchAll(m -> m))
            );
            var allPostsResponse = elasticsearchClient.search(allPostsRequest, Post.class);
            return allPostsResponse.hits().hits().stream()
                    .map(Hit::source)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Cannot fetch all posts, error: " + e);
        }
    }

    /**
     * Searches posts based on text and hashtag criteria.
     *
     * @param text A list of words to search for in post content.
     * @param hashtags A list of hashtags to filter posts.
     * @return A list of matching posts.
     * @throws RuntimeException if search fails.
     */
    public List<Post> searchPosts(List<String> text, List<String> hashtags) {
        try {
            final var request = SearchRequest.of(requestBuilder -> {
                requestBuilder.index("posts");
                return requestBuilder.query(queryBuilder -> {
                    BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
                    if (text != null && !text.isEmpty()) {
                        var textQueryBuilder = new BoolQuery.Builder();
                        for (String word : text) {
                            textQueryBuilder.should(createVagueTextQuery(word));
                        }
                        textQueryBuilder.minimumShouldMatch("1");
                        boolQueryBuilder.must(
                                Query.of(q -> q.bool(textQueryBuilder.build()))
                        );
                    }
                  if (hashtags != null && !hashtags.isEmpty()) {
                        for (String hashtag : hashtags) {
                            boolQueryBuilder.must(createHashtagQuery(hashtag));
                        }
                    }
                    return queryBuilder.bool(boolQueryBuilder.build());
                });
            });
            logger.info("Elasticsearch Query: " + request);
            final var response = elasticsearchClient.search(request, Post.class);
            logger.info("Elasticsearch Response Total Hits: " + response.hits().total().value());

            List<Post> results = response.hits().hits().stream().map(Hit::source).toList();
            logger.info("Mapped Results: " + results);
            return results;
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot search posts, error: " + e);
        }
    }

    /**
     * Creates a vague text query for searching posts.
     *
     * @param word The word to search for.
     * @return A query that finds posts containing at least one of the given words.
     */
    public Query createVagueTextQuery(String word) {
        return Query.of(q -> q
                .bool(b -> b
                        .must(m -> m
                                .match(MatchQuery.of(mq -> mq
                                        .field("text")
                                        .query(word)
                                ))
                        )
                        .mustNot(mn -> mn
                                .prefix(p -> p
                                        .field("text")
                                        .value("#" + word)  // Exclude any match that starts with the '#' + word
                                )
                        )
                )
        );
    }
    /**
     * Creates a strict hashtag query for searching posts.
     *
     * @param hashtag The hashtag to filter posts.
     * @return A query that ensures posts include all the searched hashtags.
     */
    private Query createHashtagQuery(String hashtag) {
        return Query.of(q -> q
                .term(termQuery -> termQuery
                        .field("hashtags.keyword")
                        .value(hashtag)
                )
        );
    }
}

