package com.epita.repository.models;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@MongoEntity(collection = "Users", database = "users_db")
public class UserModel {
    private String userId;
    private String username;
    private Date createdAt;
}