package com.epita.service;

import com.epita.Converter;
import com.epita.events.UserActionEvent;
import com.epita.redis.PostActionPublisher;
import com.epita.repository.PostRepoMongo;
import com.epita.repository.RegisteredUsersRepo;
import com.epita.repository.models.PostModel;
import com.epita.repository.models.UserModel;
import com.epita.service.entities.PostEntity;
import com.epita.service.entities.PostInfoEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Service gérant les opérations liées aux posts des utilisateurs.
 * Ce service coordonne les interactions entre la couche contrôleur et les repositories,
 * gère le stockage dans MongoDB et Neo4j, et publie des événements dans Redis.
 */
@ApplicationScoped
public class PostService {
    @Inject
    PostRepoMongo postRepoMongo;

    @Inject
    PostActionPublisher publisher;

    @Inject
    Converter converter;

    @Inject
    Logger logger;
    @Inject
    PostActionPublisher postActionPublisher;
    @Inject
    RegisteredUsersRepo registeredUsersRepo;

    /**
     * Crée un nouveau post dans le système.
     * Sauvegarde le post dans MongoDB, crée un nœud dans Neo4j et publie un événement dans Redis.
     *
     * @param postEntity l'entité contenant les informations du post à créer
     * @return les informations du post créé, ou null en cas d'échec
     */
    public PostInfoEntity createPost(PostEntity postEntity) {
        logger.info("[CREATE POST][SERVICE]: creating post: " + postEntity.toString());
        PostInfoEntity postInfoEntity = converter.PostModeltoPostInfoEntity(postRepoMongo.createPost(postEntity));
        if (postInfoEntity == null) {
            return null;
        }
        logger.info("[CREATE POST][SERVICE]: created post: " + postInfoEntity);
        // Publish post creation event to Redis
        
        UserActionEvent event = new UserActionEvent();
        event.setUserId(postInfoEntity.getAuthorId());
        event.setUsername(postInfoEntity.getAuthorUsername());
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent(postInfoEntity.getText());
        event.setPostId(postInfoEntity.getPostId());
        event.setTimestamp(postInfoEntity.getCreatedAt());
        logger.info("[CREATE POST][SERVICE]: publishing post creation event to Redis: " + event);
        publisher.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());
        
        return postInfoEntity;
    }

    /**
     * Supprime un post existant.
     * Vérifie que l'utilisateur demandant la suppression est l'auteur du post.
     *
     * @param postId identifiant unique du post à supprimer
     * @param authorUsername identifiant de l'utilisateur demandant la suppression
     * @return true si la suppression a réussi, false sinon
     */
    public Boolean deletePost(UUID postId, String authorUsername) {
        logger.info("[DELETE POST][SERVICE]: deleting post: " + postId);

        Boolean success = postRepoMongo.deletePost(postId.toString(), authorUsername);

        if (success) {
            logger.info("[DELETE POST][SERVICE]: deleted post node successfully: " + postId);
            UserActionEvent event = new UserActionEvent();
            event. setUsername(authorUsername);
            event.setActionType(UserActionEvent.ActionType.POST_DELETED);
            event.setPostId(postId.toString());
            logger.info("[DELETE POST][SERVICE]: publishing post deletion event to Redis: " + event);
            postActionPublisher.publishAction(event, UserActionEvent.ActionType.POST_DELETED.getValue());
        }
        else {
            logger.error("[DELETE POST][SERVICE]: failed to delete post node: " + postId);
        }
        return success;
    }

    /**
     * Récupère un post par son identifiant.
     *
     * @param postId identifiant unique du post à récupérer
     * @return l'entité contenant les informations du post, ou null si le post n'existe pas
     */
    public PostInfoEntity getPost(UUID postId) {
        logger.info("[GET POST][SERVICE]: getting post: " + postId);
        PostModel model = postRepoMongo.find("postId", postId.toString()).firstResultOptional().orElse(null);
        if (model == null) {
            logger.error("[GET POST][SERVICE]: post not found: " + postId);
            return null;
        }
        return converter.PostModeltoPostInfoEntity(model);
    }

    /**
     * Récupère les réponses à un post spécifique.
     *
     * @param postId identifiant unique du post parent
     * @return la liste des réponses au post
     */
    public List<PostInfoEntity> getPostReplies(UUID postId) {
        logger.info("[GET POST REPLIES][SERVICE]: getting post replies for post: " + postId);
        return postRepoMongo.getPostReplies(postId);
    }

    /**
     * Récupère tous les posts d'un utilisateur spécifique.
     *
     * @param username identifiant unique de l'utilisateur
     * @return la liste des posts de l'utilisateur
     */
    public List<PostInfoEntity> getUserPosts(String username) {
        logger.info("[GET USER POSTS][SERVICE]: getting posts for user: " + username);
        return postRepoMongo.find("authorUsername", username).list().stream()
                .map(converter::PostModeltoPostInfoEntity)
                .toList();
    }

    /**
     * Vérifie si un post est créé par un utilisateur bloqué.
     *
     * @param authorUsername identifiant de l'utilisateur qui tente d'interagir
     * @param parentPostId identifiant du post parent
     * @return true si l'auteur du post parent a bloqué l'utilisateur, false sinon
     * @throws RuntimeException si le post parent n'existe pas
     */
    public boolean isPostFromBlockedUser(String authorUsername, String parentPostId) {
        PostModel post = postRepoMongo.find("postId", parentPostId).firstResult();
        logger.info("[IS POST FROM BLOCKED USER][SERVICE]: checking if post is from blocked user: " + authorUsername);
        UserModel user = registeredUsersRepo.find("username", authorUsername).firstResult();
        if (user.getBlockedUsers().contains(post.getAuthorUsername()))
        {
            logger.info("[IS POST FROM BLOCKED USER][SERVICE]: user is blocked: " + parentPostId);
            return true;
        }
        return false;
    }

    /**
     * Vérifie si un utilisateur existe dans le système.
     *
     * @param username identifiant unique de l'utilisateur à vérifier
     * @return true si l'utilisateur existe, false sinon
     */
    public boolean userExists(String username) {
        return registeredUsersRepo.find("username", username).count() > 0;
    }

    /**
     * Vérifie si un post existe dans le système.
     *
     * @param postId identifiant unique du post à vérifier
     * @return true si le post existe, false sinon
     */
    public boolean postExists(@NotBlank(message = "postId is required") UUID postId) {
        return postRepoMongo.find("postId", postId.toString()).count() > 0;
    }
}