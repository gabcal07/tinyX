package com.epita.controller;

import com.epita.service.ServiceUserTimelineService;
import com.epita.service.entities.UserTimelineEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.logging.Logger;

/**
 * REST controller for managing user timeline operations.
 */
@Path("/timelines")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ServiceUserTimelineController {

    @Inject
    ServiceUserTimelineService serviceUserService;

    @Inject
    Logger logger;

    /**
     * Retrieves a user timeline by username.
     *
     * @param username the username of the user whose timeline to retrieve
     * @return HTTP 200 OK with the timeline information if found, otherwise HTTP 404 Not Found
     */
    @GET
    @Path("/user/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtenir la timeline de l'utilisateur",
            description = "Retourne la timeline contenant la liste des posts likés et postés par l'utilisateur")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Timeline récupérée avec succès"),
            @APIResponse(responseCode = "404", description = "L'utilisateur n'existe pas")
    })
    public Response getUser(@PathParam("username") String username) {
        if (username == null || username.isEmpty()) {
            logger.error("[GET USER TIMELINE][CONTROLLER]: username is null or empty");
            return Response.status(Response.Status.BAD_REQUEST).entity("Username cannot be empty").build();
        }

        logger.info("[GET USER TIMELINE][CONTROLLER]: Retrieving timeline for username: " + username);
        UserTimelineEntity userTimeline = serviceUserService.getUserTimeline(username);

        if (userTimeline != null) {
            return Response.ok(userTimeline).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Timeline not found for username: " + username)
                    .build();
        }
    }
}
