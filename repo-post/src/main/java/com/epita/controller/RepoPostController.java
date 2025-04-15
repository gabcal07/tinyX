package com.epita.controller;

import com.epita.Converter;
import com.epita.dto.contracts.CreatePostContract;
import com.epita.dto.responses.CreatePostReponse;
import com.epita.service.PostService;
import com.epita.service.entities.PostEntity;
import com.epita.service.entities.PostInfoEntity;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
public class RepoPostController {
    @Inject
    PostService postService;

    @Inject
    Converter converter;

    @Inject
    Logger logger;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new post", description = "Creates a new post with the given data")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Post created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreatePostReponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid post data"),
            @APIResponse(responseCode = "403", description = "Trying to reference a blocked user's post"),
            @APIResponse(responseCode = "404", description = "Author not found, or referenced post not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response createPost(@Valid CreatePostContract contract) {
        if (!contract.IsValid())
            return Response.status(Response.Status.BAD_REQUEST).build();

        // Check if author exists
        if (!postService.userExists(contract.getData().getAuthorUsername()))
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Author: " + contract.getData().getAuthorUsername() + "does not exist")
                    .build();

        if (contract.getData().getText() != null && contract.getData().getText().length() > 160)
            return Response.status(Response.Status.BAD_REQUEST).build();

        // Check if referenced post
        if (contract.getData().getParentPostId() != null)
        {
            // Check if referenced post exists
            if (!postService.postExists(contract.getData().getParentPostId()))
                return Response.status(Response.Status.NOT_FOUND).entity("Post not found").build();

            UUID parentPostId = contract.getData().getParentPostId();
            String authorUsername = contract.getData().getAuthorUsername();
            try {
                if (postService.isPostFromBlockedUser(authorUsername, parentPostId.toString()))
                    return Response.status(Response.Status.FORBIDDEN).build();
            }
            catch (RuntimeException e) {
                return Response.status(Response.Status.NOT_FOUND).entity("Post not found" + e.getMessage()).build();
            }
        }

        logger.info("[CREATE POST][CONTROLLER]: creating post with authorUsername: " + contract.getData().getAuthorUsername() + " and text: " + contract.getData().getText());
        PostEntity postEntity = converter.PostCreateContractToPostEntity(contract);
        PostInfoEntity postInfo = postService.createPost(postEntity);

        if (postInfo.getPostId() != null) {
            CreatePostReponse response = converter.PostEntityInfoToCreatePostReponse(postInfo);
            return Response.ok(response).build();
        }
        else
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete a post", description = "Deletes a post with the given data")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Post deleted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreatePostReponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid post data"),
            @APIResponse(responseCode = "403", description = "Unauthorized to delete post"),
            @APIResponse(responseCode = "404", description = "Post not found"),
            @APIResponse(responseCode = "500", description = "Internal server error")
    })
    @Path("/{postId}")
    public Response deletePost(@HeaderParam("X-user-name")String username, UUID postId) {
        if (postId == null || username == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        if (!postService.userExists(username))
            return Response.status(Response.Status.BAD_REQUEST).build();

        Boolean success = false;
        try {
            success = postService.deletePost(postId, username);
        }
        catch (RuntimeException e) {
            if (e.getMessage().contains("not found"))
            {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            else if (e.getMessage().contains("Unauthorized"))
            {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        }

        if (success) {
            return Response.ok().build();
        }
        else
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a post", description = "Get a post with the given postId")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Post retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreatePostReponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid post data"),
            @APIResponse(responseCode = "404", description = "Post Not Found")
    })
    @Path("/{postId}")
    public Response getPost(UUID postId) {
        if (postId == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        if (!postService.postExists(postId))
            return Response.status(Response.Status.NOT_FOUND).build();
        
        PostInfoEntity postInfo = postService.getPost(postId);

        if (postInfo != null && postInfo.getPostId() != null) {
            CreatePostReponse response = converter.PostEntityInfoToCreatePostReponse(postInfo);
            return Response.ok(response).build();
        }
        else
            return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a post's replies", description = "Get a post's reply with the given postId")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Post retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreatePostReponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid post data"),
            @APIResponse(responseCode = "404", description = "Post Not Found")
    })
    @Path("/replies/{postId}")
    public Response getPostReplies(UUID postId) {
        if (postId == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        List<PostInfoEntity> postsInfo = postService.getPostReplies(postId);

        if (postsInfo != null) {
            return Response.ok(postsInfo).build();
        }
        else
            return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a user's posts", description = "Get a user's posts with the given username")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Posts retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreatePostReponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid user data"),
            @APIResponse(responseCode = "404", description = "User Not Found")
    })
    @Path("/user/{username}")
    public Response getUserPosts(String username) {
        if (username == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        if (!postService.userExists(username))
            return Response.status(Response.Status.NOT_FOUND).build();

        List<PostInfoEntity> postsInfo = postService.getUserPosts(username);

        if (postsInfo != null) {
            return Response.ok(postsInfo).build();
        }
        else
            return Response.status(Response.Status.NOT_FOUND).build();
    }
}