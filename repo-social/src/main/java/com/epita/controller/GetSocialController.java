package com.epita.controller;

import com.epita.dto.responses.*;
import com.epita.service.SocialService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/social")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Relations sociales", description = "API pour consulter les relations sociales entre utilisateurs")
@ApplicationScoped
public class GetSocialController {
    @Inject
    SocialService socialService;

    @Inject
    Logger logger;

    @GET
    @Path("/posts/{postId}/likeUsers")
    @Operation(summary = "Obtenir les utilisateurs qui ont aimé un post",
            description = "Retourne la liste des utilisateurs qui ont aimé le post spécifié")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Liste des utilisateurs récupérée avec succès"),
            @APIResponse(responseCode = "400", description = "Format d'identifiant de post invalide"),
            @APIResponse(responseCode = "404", description = "Le post n'existe pas")
    })
    public Response getLikeUsers(
            @Parameter(description = "Identifiant du post", required = true)
            @PathParam("postId") String postId) {
        try {
            if (!socialService.postExists(postId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Le post n'existe pas").build();
            }

            List<String> likeUsers = socialService.getPostLikeUsers(postId);
            logger.info("[GET LIKE USERS][CONTROLLER]: Récupération des utilisateurs qui ont aimé le post " + postId);
            GetLikeUsersResponse response = new GetLikeUsersResponse(postId, likeUsers);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("ID du post invalide").build();
        }
    }

    @GET
    @Path("/users/{userId}/likedPosts")
    @Operation(summary = "Obtenir les posts aimés par un utilisateur",
            description = "Retourne la liste des posts aimés par l'utilisateur spécifié")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Liste des posts récupérée avec succès"),
            @APIResponse(responseCode = "404", description = "L'utilisateur n'existe pas")
    })
    public Response getLikedPosts(
            @Parameter(description = "Identifiant de l'utilisateur", required = true)
            @PathParam("userId") String userId) {
        if (!socialService.userExists(userId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("L'utilisateur n'existe pas").build();
        }

        List<String> likedPosts = socialService.getUserLikedPosts(userId);
        GetLikedPostsResponse response = new GetLikedPostsResponse(userId, likedPosts);
        logger.info("[GET LIKED POSTS][CONTROLLER]: Récupération des posts aimés par l'utilisateur " + userId);
        return Response.ok(response).build();
    }

    @GET
    @Path("/users/{userId}/followers")
    @Operation(summary = "Obtenir les abonnés d'un utilisateur",
            description = "Retourne la liste des utilisateurs qui suivent l'utilisateur spécifié")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Liste des abonnés récupérée avec succès"),
            @APIResponse(responseCode = "404", description = "L'utilisateur n'existe pas")
    })
    public Response getFollowers(
            @Parameter(description = "Identifiant de l'utilisateur", required = true)
            @PathParam("userId") String userId) {
        if (!socialService.userExists(userId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("L'utilisateur n'existe pas").build();
        }

        List<String> followers = socialService.getUserFollowers(userId);
        GetFollowersResponse response = new GetFollowersResponse(userId, followers);
        logger.info("[GET FOLLOWERS][CONTROLLER]: Récupération des abonnés de l'utilisateur " + userId);
        return Response.ok(response).build();
    }

    @GET
    @Path("/users/{userId}/follows")
    @Operation(summary = "Obtenir les utilisateurs suivis",
            description = "Retourne la liste des utilisateurs suivis par l'utilisateur spécifié")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Liste des utilisateurs suivis récupérée avec succès"),
            @APIResponse(responseCode = "404", description = "L'utilisateur n'existe pas")
    })
    public Response getFollows(
            @Parameter(description = "Identifiant de l'utilisateur", required = true)
            @PathParam("userId") String userId) {
        if (!socialService.userExists(userId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("L'utilisateur n'existe pas").build();
        }

        List<String> follows = socialService.getUserFollows(userId);
        GetFollowersResponse response = new GetFollowersResponse(userId, follows);
        logger.info("[GET FOLLOWS][CONTROLLER]: Récupération des utilisateurs suivis par " + userId);
        return Response.ok(response).build();
    }

    @GET
    @Path("/users/{userId}/blocked")
    @Operation(summary = "Obtenir les utilisateurs bloqués",
            description = "Retourne la liste des utilisateurs bloqués par l'utilisateur spécifié")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Liste des utilisateurs bloqués récupérée avec succès"),
            @APIResponse(responseCode = "404", description = "L'utilisateur n'existe pas")
    })
    public Response getBlockedUsers(
            @Parameter(description = "Identifiant de l'utilisateur", required = true)
            @PathParam("userId") String userId) {
        if (!socialService.userExists(userId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("L'utilisateur n'existe pas").build();
        }

        List<String> blockedUsers = socialService.getUserBlockedUsers(userId);
        GetBlockedUsersResponse response = new GetBlockedUsersResponse(userId, blockedUsers);
        logger.info("[GET BLOCKED USERS][CONTROLLER]: Récupération des utilisateurs bloqués par " + userId);
        return Response.ok(response).build();
    }

    @GET
    @Path("/users/{userId}/isblocked")
    @Operation(summary = "Obtenir les utilisateurs qui ont bloqué",
            description = "Retourne la liste des utilisateurs qui ont bloqué l'utilisateur spécifié")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Liste des utilisateurs bloquants récupérée avec succès"),
            @APIResponse(responseCode = "404", description = "L'utilisateur n'existe pas")
    })
    public Response getBlockingUsers(
            @Parameter(description = "Identifiant de l'utilisateur", required = true)
            @PathParam("userId") String userId) {
        if (!socialService.userExists(userId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("L'utilisateur n'existe pas").build();
        }

        List<String> blockingUsers = socialService.getUserBlockingUsers(userId);
        GetBlockingUsersResponse response = new GetBlockingUsersResponse(userId, blockingUsers);
        logger.info("[GET BLOCKING USERS][CONTROLLER]: Récupération des utilisateurs qui ont bloqué " + userId);
        return Response.ok(response).build();
    }
}