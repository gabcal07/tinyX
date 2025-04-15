package com.epita.repository.models;

import com.epita.service.entities.PostReferenceEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@MongoEntity(collection = "UserTimelines", database = "user_timelines_db")
public class UserTimelineModel {
    private String userId;
    @BsonId
    private String username;
    private List<PostReferenceEntity> posts;
    private Date lastUpdated;
}