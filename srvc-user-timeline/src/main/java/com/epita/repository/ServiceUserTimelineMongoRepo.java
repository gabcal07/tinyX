package com.epita.repository;

import com.epita.repository.models.UserTimelineModel;
import com.epita.service.Converter;
import com.epita.service.entities.PostReferenceEntity;
import com.epita.service.entities.UserTimelineEntity;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.jboss.logging.Logger;

import java.util.Date;
import java.util.List;
import java.util.UUID;


/**
 * Repository for managing user data in MongoDB.
 */
@ApplicationScoped
public class ServiceUserTimelineMongoRepo implements PanacheMongoRepositoryBase<UserTimelineModel, String> {
    @Inject
    Logger logger;

    public Boolean createUserTimeline(String username, String userId, List<PostReferenceEntity> posts) {
        try {
            if (find("userId", userId).firstResult() != null) {
                logger.error("[CREATE USERTIMELINE][REPO]: user timeline already exists in mongo");
                return false;
            }
            UserTimelineModel userTimeline = new UserTimelineModel();
            userTimeline.setUserId(userId);
            userTimeline.setPosts(posts);
            userTimeline.setUsername(username);
            userTimeline.setLastUpdated(new Date());
            logger.info("[CREATE USERTIMELINE][REPO]: creating user timeline in mongo");
            persist(userTimeline);
            return true;
        } catch (Exception e) {
            logger.error("[CREATE USERTIMELINE][REPO]: error while creating user timeline in mongo, error message is: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a userTimeline by userId in MongoDB.
     *
     * @param userId the UUID of the user to delete
     * @return true if the userTimeline is deleted successfully, otherwise false
     */
    public Boolean deleteUserTmelineWithUserId(UUID userId) {
        try {
            UserTimelineModel userTimeline = find("userId", userId.toString()).firstResult();
            if (userTimeline == null) {
                logger.error("[DELETE USERTIMELINE][REPO]: user timeline not found in mongo");
                return false;
            }
            delete("userId", userId.toString());
            return true;
        } catch (Exception e) {
            logger.error("[DELETE USERTIMELINE][REPO]: error while deleting user timeline in mongo, error message is: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a userTimeline by username in MongoDB.
     *
     * @param username the name of the user to delete
     * @return true if the userTimeline is deleted successfully, otherwise false
     */
    public Boolean deleteUserTmelineWithUsername(String username) {
        try {
            UserTimelineModel userTimeline = findById(username);
            if (userTimeline == null) {
                logger.error("[DELETE USERTIMELINE][REPO]: user timeline not found in mongo");
                return false;
            }
            deleteById(username);
            return true;
        } catch (Exception e) {
            logger.error("[DELETE USERTIMELINE][REPO]: error while deleting user timeline in mongo, error message is: " + e.getMessage());
            return false;
        }
    }


    /**
     * Clears all userTimelines in MongoDB.
     *
     * @return true if all userTimelines are cleared successfully, otherwise false
     */
    public Boolean clearUserTimelines() {
        try {
            deleteAll();
            return true;
        } catch (Exception e) {
            logger.error("[CLEAR USERTIMELINES][REPO]: error while clearing user timelines in mongo, error message is: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a userTimeline by userId in MongoDB.
     *
     * @param userId the UUID of the user to retrieve
     * @return the userTimeline model if found, otherwise null
     */
    public UserTimelineModel getUserTimeline(UUID userId) {
        try {
            return find("userId", userId.toString()).firstResult();
        } catch (Exception e) {
            logger.error("[GET USERTIMELINE][REPO]: error while getting user timeline in mongo, error message is: " + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves a user by username in MongoDB.
     *
     * @param username the username of the user to retrieve
     * @return the user model if found, otherwise null
     */
    public UserTimelineModel getUserTimelineWithUsername(String username) {
        try {
            return findById(username);
        } catch (Exception e) {
            logger.error("[GET USERTIMELINE][REPO]: error while getting user timeline in mongo, error message is: " + e.getMessage());
            return null;
        }
    }

    /**
     * Update a user timeline by userId in MongoDB.
     *
     * @param userId the id of the user to update
     * @param posts list of posts to update in the database
     * @return true if operation succeeded or false
     */

    public boolean updateUserTimeLineWithUserId(UUID userId, List<JsonObject> posts){
        try {
            update("posts=?1 where userid=?2", posts, userId.toString());
            return true;
        }
        catch (Exception e) {
            logger.error("[UPDATE USERTIMELINE][REPO]: error while updating user timeline in mongo, error message is: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update a user timeline by userId in MongoDB.
     *
     * @param username the name of the user to update
     * @param posts list of posts to update in the database
     * @return true if operation succeeded or false
     */

    public boolean updateUserTimeLineWithUsername(String username, List<PostReferenceEntity> posts) {
        try {
            UserTimelineModel model = find("_id", username).firstResult();
            if (model == null) {
                logger.warn("No timeline found for user " + username);
                return false;
            }
            model.setPosts(posts);
            persistOrUpdate(model); // or persistOrUpdate()
            UserTimelineModel model2 = find("_id", username).firstResult();
            return true;
        } catch (Exception e) {
            logger.error("Error updating timeline: " + e.getMessage(), e);
            return false;
        }
    }
}