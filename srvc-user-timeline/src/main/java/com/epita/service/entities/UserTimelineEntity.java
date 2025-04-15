package com.epita.service.entities;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
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
@NoArgsConstructor
@Getter
@Setter
public class UserTimelineEntity {
    private String Id;
    private String userId;
    private List<PostReferenceEntity> posts;
    private Date lastUpdated;
}
