package com.epita.controller;

import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import com.epita.repository.entity.HomeTimelines;
import com.epita.service.HomeTimelineService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

@Path("/timelines/home")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HomeTimelineController {

    @Inject
    HomeTimelineService hometimelineservice;

    @Inject
    Logger logger;

    @Operation(summary = "Get home timeline for a user",
            description = "Retrieve the home timeline for a specific user")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Home timeline retrieved successfully"),
            @APIResponse(responseCode = "404", description = "User not found")
    })
    @GET
    @Path("/{username}")
    public Response getHomeTimeline(@PathParam("username") String username) {
        if (username == null || username.isEmpty()) {
            logger.error("[GET USER TIMELINE][CONTROLLER]: username is null or empty");
            return Response.status(Response.Status.BAD_REQUEST).entity("Username cannot be empty").build();
        }

        logger.info("[GET USER TIMELINE][CONTROLLER]: Retrieving timeline for username: " + username);
        HomeTimelines homeTimeline = hometimelineservice.getTimeline(username);

        if (homeTimeline != null) {
            return Response.ok(homeTimeline).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Timeline not found for username: " + username)
                    .build();
        }
    }
}
