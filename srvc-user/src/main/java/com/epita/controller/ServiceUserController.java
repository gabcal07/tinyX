package com.epita.controller;

import com.epita.dto.contracts.CreateUserContract;
import com.epita.service.ServiceUserService;
import com.epita.service.entities.UserEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * REST controller for managing user-related operations.
 */
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ServiceUserController {

    @Inject
    ServiceUserService serviceUserService;

    @Inject
    Logger logger;

    /**
     * Creates a new user.
     *
     * @param createUserContract the user creation contract containing userId and nickname
     * @return HTTP 200 OK if the user is created successfully, otherwise HTTP 500 Internal Server Error
     */
    @POST
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(CreateUserContract createUserContract) {
        if (createUserContract.getUsername() == null || createUserContract.getUsername().isBlank()) {
            logger.error("[CREATE USER][CONTROLLER]: userId or nickname is null or empty");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (createUserContract.getUsername().length() > 20) {
            logger.error("[CREATE USER][CONTROLLER]: nickname is too long");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        logger.info("[CREATE USER][CONTROLLER]: triggering service layer for user creation");
        Boolean success = serviceUserService.createUser(createUserContract.getUsername());

        // If the creation fails this means that another user with the same username exists
        return success ? Response.ok().build() : Response.status(Response.Status.CONFLICT).build();
    }

    /**
     * Retrieves a user by userId.
     *
     * @param userId the UUID of the user to retrieve
     * @return HTTP 200 OK with the user information if found, otherwise HTTP 404 Not Found
     */
    @GET
    @Path("/get/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("userId") UUID userId) {
        if (userId == null) {
            logger.error("[GET USER][CONTROLLER]: userId is null or empty");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        logger.info("[GET USER][CONTROLLER]: triggering service layer for getting user with userId: " + userId);
        UserEntity userInfo = serviceUserService.getUser(userId);
        return userInfo != null ? Response.ok(userInfo).build() : Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Retrieves a user by username.
     *
     * @param username the username of the user to retrieve
     * @return HTTP 200 OK with the user information if found, otherwise HTTP 404 Not Found
     */
    @GET
    @Path("/get/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("username") String username) {
        if (username == null) {
            logger.error("[GET USER][CONTROLLER]: userId is null or empty");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        logger.info("[GET USER][CONTROLLER]: triggering service layer for getting user with userId: " + username);
        UserEntity userInfo = serviceUserService.getUserWithUsername(username);
        return userInfo != null ? Response.ok(userInfo).build() : Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Clears all users.
     *
     * @return HTTP 200 OK if users are cleared successfully, otherwise HTTP 500 Internal Server Error
     */
    @POST
    @Path("/clear")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearUsers() {
        logger.info("[CLEAR USERS][CONTROLLER]: triggering service layer for clearing users");
        Boolean success = serviceUserService.clearUsers();

        return success ? Response.ok().build() : Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Deletes a user by userId.
     *
     * @param userId the UUID of the user to delete
     * @return HTTP 200 OK if the user is deleted successfully, otherwise HTTP 404 Not Found
     */
    @DELETE
    @Path("/delete/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("userId") UUID userId) {
        if (userId == null) {
            logger.error("[DELETE USER][CONTROLLER]: userId is null or empty");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        logger.info("[DELETE USER][CONTROLLER]: triggering service layer for deleting user with userId: " + userId);
        Boolean success = serviceUserService.deleteUserWithUserId(userId);
        return success ? Response.ok().build() : Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Deletes a user by username.
     *
     * @param username the username of the user to delete
     * @return HTTP 200 OK if the user is deleted successfully, otherwise HTTP 404 Not Found
     */
    @DELETE
    @Path("/delete/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("username") String username) {
        if (username == null) {
            logger.error("[DELETE USER][CONTROLLER]: userId is null or empty");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        logger.info("[DELETE USER][CONTROLLER]: triggering service layer for deleting user with userId: " + username);
        Boolean success = serviceUserService.deleteUserWithUsername(username);
        return success ? Response.ok().build() : Response.status(Response.Status.NOT_FOUND).build();
    }
}