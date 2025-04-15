package com.epita.controller;

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

@Path("/social")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Relations sociales", description = "API pour gérer les relations sociales entre utilisateurs")
@ApplicationScoped
public class UpdateSocialController {

    @Inject
    SocialService socialService;

    @Inject
    Logger logger;

    @POST
    @Path("/{username}/follow/{targetUsername}")
    @Operation(summary = "Suivre un utilisateur",
            description = "Permet à un utilisateur d'en suivre un autre. Échoue si la relation existe déjà ou si l'utilisateur est bloqué.")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "L'utilisateur a bien été suivi"),
            @APIResponse(responseCode = "400", description = "Impossible de se suivre soi-même"),
            @APIResponse(responseCode = "403", description = "L'un des utilisateurs a bloqué l'autre"),
            @APIResponse(responseCode = "404", description = "L'un des utilisateurs n'existe pas"),
            @APIResponse(responseCode = "409", description = "L'utilisateur suit déjà la cible")
    })
    public Response followUser(
            @Parameter(description = "Nom d'utilisateur de celui qui suit", required = true)
            @PathParam("username") String username,
            @Parameter(description = "Nom d'utilisateur de celui qui est suivi", required = true)
            @PathParam("targetUsername") String targetUsername) {

        if (username.equals(targetUsername)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Un utilisateur ne peut pas se suivre lui-même").build();
        }
        // Vérification si l'utilisateur existe
        if (!socialService.userExists(username) || !socialService.userExists(targetUsername)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("L'un des utilisateurs n'existe pas").build();
        }
        // Vérification si l'un des utilisateurs est bloqué l'autre
        if (socialService.isUserBlocked(username, targetUsername) || socialService.isUserBlocked(targetUsername, username)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Action impossible car un des utilisateurs a bloqué l'autre").build();
        }
        // Vérification si l'utilisateur suit déjà l'autre
        if (socialService.isUserFollowing(username, targetUsername)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("L'utilisateur suit déjà cette personne").build();
        }

        socialService.userFollow(username, targetUsername);
        logger.info("[FOLLOW USER][CONTROLLER]: " + username + " suit " + targetUsername);

        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/{username}/unfollow/{targetUsername}")
    @Operation(summary = "Ne plus suivre un utilisateur",
            description = "Permet à un utilisateur de ne plus suivre un autre utilisateur")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "L'utilisateur n'est plus suivi"),
            @APIResponse(responseCode = "400", description = "Impossible de se désabonner de soi-même"),
            @APIResponse(responseCode = "404", description = "L'un des utilisateurs n'existe pas"),
            @APIResponse(responseCode = "409", description = "L'utilisateur ne suit pas déjà la cible")
    })
    public Response unfollowUser(
            @Parameter(description = "Nom d'utilisateur qui se désabonne", required = true)
            @PathParam("username") String username,
            @Parameter(description = "Nom d'utilisateur dont on se désabonne", required = true)
            @PathParam("targetUsername") String targetUsername) {
        // Vérifié que les utilisateurs existent
        if (!socialService.userExists(username) || !socialService.userExists(targetUsername)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("L'un des utilisateurs n'existe pas").build();
        }

        // Vérifie que l'utilisateur ne se désabonne pas lui-même
        if (username.equals(targetUsername)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Un utilisateur ne peut pas se désabonner de lui-même").build();
        }

        // Vérifie que l'utilisateur suit bien la cible
        if (!socialService.isUserFollowing(username, targetUsername)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("L'utilisateur ne suit pas cette personne").build();
        }

        logger.info("[UNFOLLOW USER][CONTROLLER]: " + username + " ne suit plus " + targetUsername);
        socialService.userUnfollow(username, targetUsername);
        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/{username}/block/{targetUsername}")
    @Operation(summary = "Bloquer un utilisateur",
            description = "Permet à un utilisateur d'en bloquer un autre et supprime les relations de suivi existantes")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "L'utilisateur a bien été bloqué"),
            @APIResponse(responseCode = "400", description = "Impossible de se bloquer soi-même"),
            @APIResponse(responseCode = "404", description = "L'un des utilisateurs n'existe pas"),
            @APIResponse(responseCode = "409", description = "L'utilisateur a déjà bloqué la cible")
    })
    public Response blockUser(
            @Parameter(description = "Nom d'utilisateur qui bloque", required = true)
            @PathParam("username") String username,
            @Parameter(description = "Nom d'utilisateur qui est bloqué", required = true)
            @PathParam("targetUsername") String targetUsername) {
        // Vérifié que les utilisateurs existent
        if (!socialService.userExists(username) || !socialService.userExists(targetUsername)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("L'un des utilisateurs n'existe pas").build();
        }

        // Vérifie que l'utilisateur ne se bloque pas lui-même
        if (username.equals(targetUsername)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Un utilisateur ne peut pas se bloquer lui-même").build();
        }

        // Vérifie que l'utilisateur n'a pas déjà bloqué la cible
        if (socialService.isUserBlocked(username, targetUsername)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("L'utilisateur a déjà bloqué cette personne").build();
        }

        socialService.userBlock(username, targetUsername);
        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/{username}/unblock/{targetUsername}")
    @Operation(summary = "Débloquer un utilisateur",
            description = "Permet à un utilisateur d'en débloquer un autre")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "L'utilisateur a bien été débloqué"),
            @APIResponse(responseCode = "400", description = "Impossible de se débloquer soi-même"),
            @APIResponse(responseCode = "404", description = "L'un des utilisateurs n'existe pas"),
            @APIResponse(responseCode = "409", description = "L'utilisateur n'a pas bloqué la cible")
    })
    public Response unblockUser(
            @Parameter(description = "Nom d'utilisateur qui débloque", required = true)
            @PathParam("username") String username,
            @Parameter(description = "Nom d'utilisateur qui est débloqué", required = true)
            @PathParam("targetUsername") String targetUsername) {
        // Vérifié que les utilisateurs existent
        if (!socialService.userExists(username) || !socialService.userExists(targetUsername)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("L'un des utilisateurs n'existe pas").build();
        }

        // Vérifie que l'utilisateur ne se débloque pas lui-même
        if (username.equals(targetUsername)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Un utilisateur ne peut pas se débloquer lui-même").build();
        }

        // Vérifie que l'utilisateur a bien bloqué la cible
        if (!socialService.isUserBlocked(username, targetUsername)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("L'utilisateur n'a pas bloqué cette personne").build();
        }

        socialService.userUnblock(username, targetUsername);
        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/{username}/like/{postId}")
    @Operation(summary = "Aimer un post",
            description = "Permet à un utilisateur d'aimer un post")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Le post a bien été aimé"),
            @APIResponse(responseCode = "400", description = "Format d'identifiant de post invalide"),
            @APIResponse(responseCode = "403", description = "L'utilisateur a bloqué l'auteur du post"),
            @APIResponse(responseCode = "404", description = "L'utilisateur ou le post n'existe pas"),
            @APIResponse(responseCode = "409", description = "L'utilisateur aime déjà ce post")
    })
    public Response likePost(
            @Parameter(description = "Nom d'utilisateur", required = true)
            @PathParam("username") String username,
            @Parameter(description = "Identifiant du post", required = true)
            @PathParam("postId") String postId) {
        // Vérifié que l'utilisateur existe
        if (!socialService.userExists(username)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("L'utilisateur n'existe pas").build();
        }

        try {
            // Vérifier si le post existe
            if (!socialService.postExists(postId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Le post n'existe pas").build();
            }

            // Vérifier si l'utilisateur a déjà aimé ce post
            if (socialService.hasUserLikedPost(username, postId)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("L'utilisateur aime déjà ce post").build();
            }

            // Vérifier si l'auteur du post a bloqué l'utilisateur ou inversement
            String postAuthor = socialService.getPostAuthor(postId);
            if (postAuthor != null && (socialService.isUserBlocked(username, postAuthor) ||
                    socialService.isUserBlocked(postAuthor, username))) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Action impossible car un blocage existe entre l'utilisateur et l'auteur").build();
            }

            socialService.postLike(username, postId);
            return Response.status(Response.Status.OK).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("ID du post invalide").build();
        }
    }

    @POST
    @Path("/{username}/unlike/{postId}")
    @Operation(summary = "Ne plus aimer un post",
            description = "Permet à un utilisateur de retirer son appréciation d'un post")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "L'appréciation du post a bien été retirée"),
            @APIResponse(responseCode = "400", description = "Format d'identifiant de post invalide"),
            @APIResponse(responseCode = "404", description = "L'utilisateur ou le post n'existe pas"),
            @APIResponse(responseCode = "409", description = "L'utilisateur n'aimait pas ce post")
    })
    public Response unlikePost(
            @Parameter(description = "Nom d'utilisateur", required = true)
            @PathParam("username") String username,
            @Parameter(description = "Identifiant du post", required = true)
            @PathParam("postId") String postId) {
        // Vérifié que l'utilisateur existe
        if (!socialService.userExists(username)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("L'utilisateur n'existe pas").build();
        }

        try {
            // Vérifier si le post existe
            if (!socialService.postExists(postId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Le post n'existe pas").build();
            }

            // Vérifier si l'utilisateur a bien aimé ce post
            if (!socialService.hasUserLikedPost(username, postId)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("L'utilisateur n'aime pas ce post").build();
            }

            socialService.postUnlike(postId, username);
            return Response.status(Response.Status.OK).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("ID du post invalide").build();
        }
    }
}