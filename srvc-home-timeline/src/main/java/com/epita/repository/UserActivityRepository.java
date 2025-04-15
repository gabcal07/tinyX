package com.epita.repository;

import java.util.List;

import com.epita.repository.entity.UserActivity;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserActivityRepository implements PanacheMongoRepositoryBase<UserActivity, String>{
    public UserActivity findActivity(String username) {
        return findById(username);
    }

    public List<String> getFollowers(String username) {
        return listAll()
                .stream()
                .filter(activity -> activity.getFollowed() != null && activity.getFollowed().contains(username))
                .map(UserActivity::getUsername)
                .toList();
    }
}