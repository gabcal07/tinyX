package com.epita.service;

import com.epita.repository.models.UserTimelineModel;
import com.epita.service.entities.UserTimelineEntity;

public class Converter {
    public static UserTimelineEntity convertTimelineModelToTimelineEntity(UserTimelineModel model){
        UserTimelineEntity entity = new UserTimelineEntity(model.getUsername(), model.getUserId(), model.getPosts(), model.getLastUpdated());
        return entity;
    }
}
