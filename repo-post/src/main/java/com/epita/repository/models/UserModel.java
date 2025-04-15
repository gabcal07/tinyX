package com.epita.repository.models;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@MongoEntity(collection = "RegisteredUsers", database = "posts_db")
public class UserModel {

    @BsonId
    private ObjectId id = new ObjectId();
    private String username;
    private List<String> blockedUsers;
    private Date registeredAt;

    @Override
    public String toString() {
        return "UserModel{" +
                "userId='" + username + '\'' +
                ", blockedUsers=" + blockedUsers +
                ", registeredAt=" + registeredAt +
                '}';
    }
}
