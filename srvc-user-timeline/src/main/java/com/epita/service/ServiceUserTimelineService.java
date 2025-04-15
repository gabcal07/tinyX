package com.epita.service;

import com.epita.redis.UserTimelineActionSubscriber;
import com.epita.repository.ServiceUserTimelineMongoRepo;
import com.epita.repository.models.UserTimelineModel;
import com.epita.service.entities.UserTimelineEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ServiceUserTimelineService {
    @Inject
    Logger logger;

    @Inject
    ServiceUserTimelineMongoRepo serviceUserMongoRepo;

    @Inject
    UserTimelineActionSubscriber userTimelineActionSubscriber;

    /**
     * Get the UserTimeline from the given username
     * @param username the username to get the UserTimeline from
     */

    public UserTimelineEntity getUserTimeline(String username){
        logger.info("[GET USERTIMELINE][SERVICE]: getting usertimeline from username: " + username);
        UserTimelineModel userTimelineModel = serviceUserMongoRepo.getUserTimelineWithUsername(username);
        if (userTimelineModel == null) {
            return null;
        }
        return Converter.convertTimelineModelToTimelineEntity(userTimelineModel);
    }

}
