package com.epita.repository;

import com.epita.repository.models.UserModel;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.UUID;

@ApplicationScoped
public class RegisteredUsersRepo implements PanacheMongoRepository<UserModel> {
    @Inject
    Logger logger;

    /**
     * Registers a user by their UUID.
     *
     * @param username the UUID of the user to register
     */
    public void registerUser(String username) {
        logger.infof("[REGISTER USER][REPO]: registering user %s", username);
        if (find("userId", username).firstResult() != null) {
            logger.infof("[REGISTER USER][REPO]: user %s already registered", username);
            return; // User already registered
        }
        logger.infof("[REGISTER USER][REPO]: user %s not found, creating new user", username);
        UserModel userModel = new UserModel();
        userModel.setUsername(username);
        userModel.setBlockedUsers(new ArrayList<>());
        persist(userModel);
        UserModel model = find("username", username).firstResult();
        logger.info("[REGISTER USER][REPO] find the recently added user "  + model);
    }

    /**
     * Checks if a user is registered.
     *
     * @param username the UUID of the user to check
     */
    public void addBlockedUser(String username, String blockedUsername) {
        logger.infof("[BLOCK USER][REPO]: blocking user %s for user %s", blockedUsername, username);
        UserModel userModel = find("username", username).firstResult();
        userModel.getBlockedUsers().add(blockedUsername);
        update(userModel);
    }

    /**
     * Unblocks a user for a given user.
     *
     * @param username the UUID of the user who is unblocking
     * @param blockedUsername the UUID of the user to be unblocked
     */
    public void removeBlockedUser(String username, String blockedUsername) {
        logger.infof("[UNBLOCK USER][REPO]: unblocking user %s for user %s", blockedUsername, username);
        UserModel userModel = find("username", username).firstResult();
        userModel.getBlockedUsers().remove(blockedUsername);
        update(userModel);
    }

    /**
     * Removes a user from the repository.
     *
     * @param username the UUID of the user to remove
     */
    public void removeUser(UUID username) {
        logger.infof("[DELETE USER][REPO]: deleting user %s", username);
        UserModel userModel = find("username", username).firstResult();
        if (userModel != null) {
            delete(userModel);
        } else {
            logger.warnf("[DELETE USER][REPO]: user %s not found", username);
        }
    }

    /**
     * Clears all users from the RegisteredUsers Collection.
     */
    public void clearUsers() {
        logger.info("[CLEAR USERS][REPO]: clearing all users");
        deleteAll();
    }

    /**
     * Retrieves a user by their username.
     *
     * @param username the username of the user to retrieve
     */
    public void deleteUserWithUsername(String username) {
        logger.infof("[DELETE USER][REPO]: deleting user %s", username);
        UserModel userModel = find("username", username).firstResult();
        if (userModel != null) {
            delete(userModel);
        } else {
            logger.warnf("[DELETE USER][REPO]: user %s not found", username);
        }
    }
}
