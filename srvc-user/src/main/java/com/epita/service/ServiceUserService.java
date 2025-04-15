package com.epita.service;

import com.epita.Converter;
import com.epita.events.UserActionEvent;
import com.epita.redis.UserActionPublisher;
import com.epita.repository.ServiceUserMongoRepo;
import com.epita.repository.models.UserModel;
import com.epita.service.entities.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Service class for managing user-related operations.
 */
@ApplicationScoped
public class ServiceUserService {
    @Inject
    Logger logger;

    @Inject
    ServiceUserMongoRepo serviceUserMongoRepo;

    @Inject
    UserActionPublisher userActionPublisher;

    /**
     * Creates a new user in both MongoDB and Neo4j.
     *
     * @param username the nickname of the user
     * @return true if the user is created successfully, otherwise false
     */
    public Boolean createUser(String username) {
        UUID uuid = UUID.randomUUID();
        logger.info("[CREATE USER][SERVICE]: creating user in mongo " + uuid + " " + username);
        Boolean success = serviceUserMongoRepo.createUser(uuid, username);
        if (success)
        {
            logger.info("[CREATE USER][SERVICE]: user created successfully");
            UserActionEvent event = new UserActionEvent();
            event.setUserId(uuid.toString());
            event.setActionType(UserActionEvent.ActionType.USER_CREATED);
            event.setUsername(username);
            logger.info("[CREATE USER][SERVICE]: sending user creation event to redis with user: " + event);
            userActionPublisher.publishAction(event, UserActionEvent.ActionType.USER_CREATED.getValue());
        } else {
            logger.error("[CREATE USER][SERVICE]: error while creating user");
        }
        return success;
    }

    /**
     * Clears all users in both MongoDB and Neo4j.
     *
     * @return true if all users are cleared successfully, otherwise false
     */
    public Boolean clearUsers() {
        logger.info("[CLEAR USERS][SERVICE]: clearing users");
        return serviceUserMongoRepo.clearUsers();
    }

    /**
     * Retrieves a user by userId.
     *
     * @param userId the UUID of the user to retrieve
     * @return the user entity if found, otherwise null
     */
    public UserEntity getUser(UUID userId) {
        logger.info("[GET USER][SERVICE]: getting user with nickname: " + userId);
        UserModel userModel = serviceUserMongoRepo.getUser(userId);
        if (userModel == null) {
            return null;
        }
        return Converter.convertUserModelToUserEntity(serviceUserMongoRepo.getUser(userId));
    }

    /**
     * Retrieves a user by username.
     *
     * @param username the username of the user to retrieve
     * @return the user entity if found, otherwise null
     */
    public UserEntity getUserWithUsername(String username) {
        logger.info("[GET USER][SERVICE]: getting user with username: " + username);
        UserModel userModel = serviceUserMongoRepo.getUserWithUsername(username);
        if (userModel == null) {
            return null;
        }
        return Converter.convertUserModelToUserEntity(userModel);
    }

    /**
     * Deletes a user by username in both MongoDB and Neo4j, and publishes a user deletion event.
     *
     * @param username the username of the user to delete
     * @return true if the user is deleted successfully, otherwise false
     */
    public Boolean deleteUserWithUsername(String username) {
        logger.info("[DELETE USER][SERVICE]: deleting user with username: " + username);
        Boolean success = serviceUserMongoRepo.deleteUserWithUsername(username);
        if (success) {
            logger.info("[DELETE USER][SERVICE]: user deleted successfully");
            UserActionEvent event = new UserActionEvent();
            event.setUsername(username);
            event.setActionType(UserActionEvent.ActionType.USER_DELETED);
            userActionPublisher.publishAction(event, UserActionEvent.ActionType.USER_DELETED.getValue());
        } else {
            logger.error("[DELETE USER][SERVICE]: error while deleting user");
        }
        return success;
    }

    /**
     * Deletes a user by userId in both MongoDB and Neo4j, and publishes a user deletion event.
     *
     * @param userId the UUID of the user to delete
     * @return true if the user is deleted successfully, otherwise false
     */
    public Boolean deleteUserWithUserId(UUID userId) {
        logger.info("[DELETE USER][SERVICE]: deleting user with userId: " + userId);
        Boolean success = serviceUserMongoRepo.deleteUserWithUserId(userId);
        if (success) {
            logger.info("[DELETE USER][SERVICE]: user deleted successfully");
            UserActionEvent event = new UserActionEvent();
            event.setUserId(userId.toString());
            event.setActionType(UserActionEvent.ActionType.USER_DELETED);
            userActionPublisher.publishAction(event, UserActionEvent.ActionType.USER_DELETED.getValue());
        } else {
            logger.error("[DELETE USER][SERVICE]: error while deleting user");
        }
        return success;
    }

    /**
     * Checks if a user exists by username.
     *
     * @param username the username of the user to check
     * @return true if the user exists, otherwise false
     */
    public boolean userExists(String username) {
        logger.info("[USER EXISTS][SERVICE]: checking if user exists with username: " + username);
        UserModel userModel = serviceUserMongoRepo.getUserWithUsername(username);
        return userModel != null;
    }
}