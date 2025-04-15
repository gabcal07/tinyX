package com.epita.repository.entity;

import java.util.Date;
import java.util.List;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@MongoEntity(collection = "HomeTimelines", database = "home_timelines_db")
public class HomeTimelines {
    @BsonId

    public String username;
    public List<TimelinePost> posts;
    public Date lastUpdated;
}
