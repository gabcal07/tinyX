package com.epita.repository;

import com.epita.repository.models.UserModel;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Date;
import java.util.UUID;


/**
 * Repository for managing user data in MongoDB.
 */
@ApplicationScoped
public class ServiceUserMongoRepo implements PanacheMongoRepository<UserModel> {
    @Inject
    Logger logger;

    /**
     * Creates a new user in MongoDB.
     *
     * @param userId   the UUID of the user
     * @param username the username of the user
     * @return true if the user is created successfully, otherwise false
     */
    public Boolean createUser(UUID userId, String username) {
        try {
            if (find("username", username).firstResult() != null) {
                logger.error("[CREATE USER][REPO]: user already exists in mongo");
                return false;
            }
            UserModel user = new UserModel();
            user.setUserId(userId.toString());
            user.setUsername(username);
            user.setCreatedAt(new Date());
            persist(user);
            return true;
        } catch (Exception e) {
            logger.error("[CREATE USER][REPO]: error while creating user in mongo, error message is: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a user by userId in MongoDB.
     *
     * @param userId the UUID of the user to delete
     * @return true if the user is deleted successfully, otherwise false
     */
    public Boolean deleteUserWithUserId(UUID userId) {
        try {
            UserModel user = find("userId", userId.toString()).firstResult();
            if (user == null) {
                logger.error("[DELETE USER][REPO]: user not found in mongo");
                return false;
            }
            delete("userId", userId.toString());
            return true;
        } catch (Exception e) {
            logger.error("[DELETE USER][REPO]: error while deleting user in mongo, error message is: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a user by username in MongoDB.
     *
     * @param username the username of the user to delete
     * @return true if the user is deleted successfully, otherwise false
     */
    public Boolean deleteUserWithUsername(String username) {
        try {
            UserModel user = find("username", username).firstResult();
            if (user == null) {
                logger.error("[DELETE USER][REPO]: user not found in mongo");
                return false;
            }
            delete("username", username);
            return true;
        } catch (Exception e) {
            logger.error("[DELETE USER][REPO]: error while deleting user in mongo, error message is: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clears all users in MongoDB.
     *
     * @return true if all users are cleared successfully, otherwise false
     */
    public Boolean clearUsers() {
        try {
            deleteAll();
            return true;
        } catch (Exception e) {
            logger.error("[CLEAR USERS][REPO]: error while clearing users in mongo, error message is: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a user by userId in MongoDB.
     *
     * @param userId the UUID of the user to retrieve
     * @return the user model if found, otherwise null
     */
    public UserModel getUser(UUID userId) {
        try {
            return find("userId", userId.toString()).firstResult();
        } catch (Exception e) {
            logger.error("[GET USER][REPO]: error while getting user in mongo, error message is: " + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves a user by username in MongoDB.
     *
     * @param username the username of the user to retrieve
     * @return the user model if found, otherwise null
     */
    public UserModel getUserWithUsername(String username) {
        try {
            return find("username", username).firstResult();
        } catch (Exception e) {
            logger.error("[GET USER][REPO]: error while getting user in mongo, error message is: " + e.getMessage());
            return null;
        }
    }
}