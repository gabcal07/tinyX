// PostRepoMongo.java
package com.epita.repository;

import com.epita.Converter;
import com.epita.repository.models.PostModel;
import com.epita.service.entities.PostEntity;
import com.epita.service.entities.PostInfoEntity;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PostRepoMongo implements PanacheMongoRepository<PostModel> {

    @Inject
    MongoClient mongoClient;

    @Inject
    FileStorageRepo fileStorageRepo;

    @Inject
    Converter converter;

    @Inject
    Logger logger;

    /**
     * Crée un post de manière transactionnelle.
     * Stocke également le fichier joint s'il est présent.
     *
     * @param postEntity l'entité du post à créer
     * @return le modèle du post créé
     */
    public PostModel createPost(PostEntity postEntity) {
        logger.info("[CREATE POST][REPOSITORY]: début de la création du post");
        PostModel postModel = converter.PostEntityModel(postEntity);
        String fileId = null;

        try (ClientSession clientSession = mongoClient.startSession()) {
            clientSession.startTransaction();

            try {
                // Étape 1: Stocker le fichier si présent
                if (postEntity.getMedia() != null) {
                    logger.info("[CREATE POST][REPOSITORY]: stockage du fichier joint");
                    fileId = fileStorageRepo.storeFile(postEntity.getMedia());
                    postModel.setMediaUrl(fileStorageRepo.getFileUrl(fileId));
                    logger.info("[CREATE POST][REPOSITORY]: fichier stocké avec succès, fileId: " + fileId);
                }

                // Étape 2: Persister le post dans MongoDB
                logger.info("[CREATE POST][REPOSITORY]: persistance du post dans MongoDB");
                mongoCollection().insertOne(clientSession, postModel);

                // Validation de la transaction
                clientSession.commitTransaction();
                logger.info("[CREATE POST][REPOSITORY]: post créé avec succès, id: " + postModel.getPostId());

                return postModel;
            } catch (Exception e) {
                // Annulation de la transaction en cas d'erreur
                clientSession.abortTransaction();
                logger.error("[CREATE POST][REPOSITORY]: erreur lors de la création du post: " + e.getMessage(), e);

                // Nettoyage des ressources en cas d'échec
                if (fileId != null) {
                    logger.info("[CREATE POST][REPOSITORY]: suppression du fichier après échec de la transaction");
                    fileStorageRepo.deleteFile(fileId);
                }

                throw new RuntimeException("Échec de la création du post: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error("[CREATE POST][REPOSITORY]: erreur lors du stockage du fichier: " + e.getMessage(), e);
            throw new RuntimeException("Échec du chargement du fichier", e);
        }
    }

    /**
     * Supprime un post de manière transactionnelle.
     * Supprime également le fichier référencé par le post s'il existe.
     * Met aussi à jour les posts qui référencent ce post comme parent.
     *
     * @param postId identifiant du post à supprimer
     * @param authorUsername identifiant de l'auteur demandant la suppression
     * @return true si la suppression a réussi, false sinon
     */
    public Boolean deletePost(String postId, String authorUsername) {
        logger.info("[DELETE POST][REPOSITORY]: tentative de suppression du post: " + postId + " par l'auteur: " + authorUsername);

        // Vérification préalable que le post existe et appartient à l'auteur
        PostModel postModel = find("postId", postId).firstResultOptional().orElse(null);
        if (postModel == null) {
            logger.error("[DELETE POST][REPOSITORY]: post non trouvé: " + postId);
            throw new RuntimeException("Post not found: " + postId);
        }

        if (!postModel.getAuthorUsername().equals(authorUsername)) {
            logger.error("[DELETE POST][REPOSITORY]: utilisateur non autorisé à supprimer le post: " + postId);
            throw new RuntimeException("Unauthorized to delete post: " + postId);
        }

        try (ClientSession clientSession = mongoClient.startSession()) {
            clientSession.startTransaction();
            logger.info("[DELETE POST][REPOSITORY]: transaction démarrée pour supprimer le post: " + postId);

            try {
                // Supprimer le média associé au post si présent
                if (postModel.getMediaUrl() != null && !postModel.getMediaUrl().isEmpty()) {
                    String fileId = fileStorageRepo.getFileIdFromUrl(postModel.getMediaUrl());
                    logger.info("[DELETE POST][REPOSITORY]: suppression du média associé, fileId: " + fileId);
                    fileStorageRepo.deleteFile(fileId);
                }

                // Mettre à jour les posts qui référencent ce post comme parent
                var updateResult = mongoCollection().updateMany(
                        clientSession,
                        Filters.eq("parentPostId", postId),
                        new org.bson.Document("$set", new org.bson.Document("parentPostId", null))
                );
                logger.info("[DELETE POST][REPOSITORY]: " + updateResult.getModifiedCount() + " posts référençant ce post comme parent mis à jour");

                // Supprimer le post lui-même
                var deleteResult = mongoCollection().deleteOne(clientSession, Filters.eq("postId", postId));
                boolean success = deleteResult.getDeletedCount() > 0;

                if (success) {
                    clientSession.commitTransaction();
                    logger.info("[DELETE POST][REPOSITORY]: transaction validée, post supprimé avec succès: " + postId);
                } else {
                    clientSession.abortTransaction();
                    logger.error("[DELETE POST][REPOSITORY]: échec de la suppression du post: " + postId);
                }

                return success;

            } catch (Exception e) {
                clientSession.abortTransaction();
                logger.error("[DELETE POST][REPOSITORY]: erreur lors de la suppression du post: " + postId, e);
                throw new RuntimeException("Failed to delete post: " + e.getMessage(), e);
            }
        }
    }

    public List<PostInfoEntity> getPostReplies(UUID postId) {
        logger.info("[GET POST REPLIES][REPOSITORY]: getting replies for post: " + postId);
        return find("parentPostId = ?1 and postType = ?2", postId.toString(), "REPLY").stream()
                .map(converter::PostModeltoPostInfoEntity)
                .toList();
    }

    /**
     * Supprime tous les posts de la collection MongoDB.
     * Utilisé principalement pour les tests.
     */
    public void clearPosts()
    {
        logger.info("[CLEAR POSTS][REPOSITORY]: clearing all posts");
        deleteAll();
        logger.info("[CLEAR POSTS][REPOSITORY]: all posts cleared");
    }

    /**
     * Supprime tous les posts appartenant à un utilisateur spécifique dans MongoDB.
     * Gère également la suppression des fichiers médias associés aux posts.
     *
     * @param authorUsername l'UUID de l'utilisateur dont les posts doivent être supprimés
     */
    public void deleteUsersPosts(String authorUsername) {
        logger.info("[DELETE USER POSTS][REPOSITORY]: début de la suppression des posts pour l'utilisateur: " + authorUsername);
        try (ClientSession clientSession = mongoClient.startSession()) {
            logger.info("[DELETE USER POSTS][REPOSITORY]: session MongoDB démarrée");
            clientSession.startTransaction();
            logger.info("[DELETE USER POSTS][REPOSITORY]: transaction démarrée");
            try {
                // Opération 1: Récupérer les posts
                List<PostModel> userPosts = find("authorUsername", authorUsername).list();
                logger.info("[DELETE USER POSTS][REPOSITORY]: " + userPosts.size() + " posts récupérés pour l'utilisateur: " + authorUsername);

                // Opération 2: Supprimer les médias associés
                int mediaDeletedCount = 0;
                for (PostModel post : userPosts) {
                    if (post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()) {
                        String fileId = fileStorageRepo.getFileIdFromUrl(post.getMediaUrl());
                        fileStorageRepo.deleteFile(fileId);
                        mediaDeletedCount++;
                        logger.info("[DELETE USER POSTS][REPOSITORY]: média supprimé pour le post: " + post.getPostId() + ", fileId: " + fileId);
                    }
                }
                logger.info("[DELETE USER POSTS][REPOSITORY]: " + mediaDeletedCount + "/" + userPosts.size() + " fichiers médias supprimés");

                // Opération 3: Supprimer les posts
                logger.info("[DELETE USER POSTS][REPOSITORY]: suppression des posts pour l'utilisateur: " + authorUsername);
                mongoCollection().deleteMany(clientSession, Filters.eq("authorUsername", authorUsername));
                logger.info("[DELETE USER POSTS][REPOSITORY]: posts supprimés pour l'utilisateur: " + authorUsername);

                // Operation 4: Set le parentPostId à null pour les posts qui sont des réponses
                logger.info("[DELETE USER POSTS][REPOSITORY]: mise à jour des références parentPostId: " + authorUsername);
                var updateResult = mongoCollection().updateMany(clientSession, Filters.eq("parentPostId", authorUsername),
                        new org.bson.Document("$set", new org.bson.Document("parentPostId", null)));
                logger.info("[DELETE USER POSTS][REPOSITORY]: " + updateResult.getModifiedCount() + " références parentPostId mises à jour");

                clientSession.commitTransaction();
                logger.info("[DELETE USER POSTS][REPOSITORY]: transaction validée avec succès");
            } catch (Exception e) {
                logger.error("[DELETE USER POSTS][REPOSITORY]: erreur lors de la suppression des posts: " + e.getMessage(), e);
                clientSession.abortTransaction();
                logger.info("[DELETE USER POSTS][REPOSITORY]: transaction annulée");
                throw e;
            }
        }
        logger.info("[DELETE USER POSTS][REPOSITORY]: fin de la suppression des posts pour l'utilisateur: " + authorUsername);
    }
}