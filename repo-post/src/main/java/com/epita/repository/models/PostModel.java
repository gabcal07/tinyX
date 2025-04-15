package com.epita.repository.models;

import com.epita.dto.PostType;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@MongoEntity(collection = "Posts", database = "posts_db")
public class PostModel {
    private String postId;
    private String authorId;
    private String authorUsername;
    private String text;
    private String parentPostId;
    private PostType postType;
    private String mediaUrl;
    private Date createdAt;

    @Override
    public String toString() {
        return "PostModel{" +
                "postId='" + postId + '\'' +
                ", authorId='" + authorId + '\'' +
                ", text='" + text + '\'' +
                ", parentPostId='" + parentPostId + '\'' +
                ", postType=" + postType +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
