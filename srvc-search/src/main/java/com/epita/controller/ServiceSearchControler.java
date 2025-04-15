package com.epita.controller;


import com.epita.repository.entity.Post;
import com.epita.service.SearchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * REST controller for managing search-related operations.
 */
@Path("/search")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ServiceSearchControler {


    @Inject
    Logger logger;

    @Inject
    SearchService searchService;

    /**
     * Creates a new user.
     *
     * @param query the string to search for
     * @return a list of posts matching the search query
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPosts(
            @QueryParam("query") String query)
    {

        if (query == null || query.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Query search must be provided")
                    .build();
        }
        try {
            List<Post> results = searchService.searchPost(query);
            logger.info("Results : " + results);
            return Response.ok(results).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An unexpected error occurred")
                    .build();
        }
    }
}
