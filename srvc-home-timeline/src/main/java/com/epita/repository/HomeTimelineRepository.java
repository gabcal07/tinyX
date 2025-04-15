package com.epita.repository;

import com.epita.repository.entity.HomeTimelines;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HomeTimelineRepository implements PanacheMongoRepositoryBase<HomeTimelines, String> {
    public HomeTimelines findTimeline(String username) {
        return findById(username);
    }
}
