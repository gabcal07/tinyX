package com.epita.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.common.MongoEntity;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@MongoEntity(collection = "UserActivity", database = "home_timelines_db")
public class UserActivity {
    @BsonId

    private String username;
    private List<String> followed;
    private List<Posts> posts;

    @Getter
    @Setter
    public static class Posts {
        private String authorId; // ID de l'auteur ou du liker
        private String postId;
        private String type; // "POSTED" ou "LIKED"
        private Date date;
    }
}
