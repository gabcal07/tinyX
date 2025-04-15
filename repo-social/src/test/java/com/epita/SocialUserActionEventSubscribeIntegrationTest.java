package com.epita;

import com.epita.events.UserActionEvent;
import com.epita.redis.SocialActionPublisher;
import com.epita.repository.SocialRepoNeo4j;
import com.epita.repository.models.Node;
import com.epita.repository.models.Relationship;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class SocialUserActionEventSubscribeIntegrationTest {
    @Inject
    SocialActionPublisher publisher;

    @Inject
    SocialRepoNeo4j neo4jRepo;

    @Inject
    Logger logger;

    private String user1;
    private String user2;
    private String postId;

    @BeforeEach
    public void cleanupDatabase() {
        RestAssured.config = RestAssured.config().httpClient(
                HttpClientConfig.httpClientConfig()
                        .setParam("http.socket.timeout", 30000)
                        .setParam("http.connection.timeout", 30000)
        );
        logger.info("[TEST SETUP] Nettoyage de la base de données Neo4j...");
        neo4jRepo.clearDatabase();
        logger.info("[TEST SETUP] Base de données Neo4j nettoyée avec succès");
        // Création des données de test
        user1 = "testuser-" + UUID.randomUUID().toString().substring(0, 8);
        user2 = "testuser-" + UUID.randomUUID().toString().substring(0, 8);
        postId = UUID.randomUUID().toString();

        // Création des nœuds nécessaires
        neo4jRepo.createUser(user1);
        neo4jRepo.createUser(user2);
        neo4jRepo.createPostNode(postId);
    }

    @Test
    public void testPostCreationThroughEvents() {
        String postId = UUID.randomUUID().toString();
        createPostNode(postId);
    }

    @Test
    public void testUserCreationThroughEvents() {
        String username = "testuser-" + UUID.randomUUID().toString().substring(0, 8);
        createUserNode(username);
    }

    @Test
    public void testUserCreationAndPostCreation() {
        // Créer utilisateur
        String username = "author-" + UUID.randomUUID().toString().substring(0, 8);
        createUserNode(username);

        // Créer post
        String postId = UUID.randomUUID().toString();
        UserActionEvent postEvent = new UserActionEvent();
        postEvent.setActionType(UserActionEvent.ActionType.POST_CREATED);
        postEvent.setPostId(postId);
        postEvent.setUsername(username);
        publisher.publishAction(postEvent, UserActionEvent.ActionType.POST_CREATED.getValue());
        // Vérifier que la relation entre l'utilisateur et le post a été créée
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> neo4jRepo.postExists(postId));
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> neo4jRepo.nodeExists(username, Node.NodeType.USER));
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> neo4jRepo.postRelationshipExists(postId, username));
    }

    @Test
    public void testUserDeletionThroughEvents() {
        String username = "delete-user-" + UUID.randomUUID().toString().substring(0, 8);
        createUserNode(username);

        UserActionEvent deleteEvent = new UserActionEvent();
        deleteEvent.setActionType(UserActionEvent.ActionType.USER_DELETED);
        deleteEvent.setUsername(username);
        publisher.publishAction(deleteEvent, UserActionEvent.ActionType.USER_DELETED.getValue());

        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !neo4jRepo.nodeExists(username, Node.NodeType.USER));
    }

    @Test
    public void testPostDeletionThroughEvents() {
        String postId = UUID.randomUUID().toString();
        createPostNode(postId);

        UserActionEvent deleteEvent = new UserActionEvent();
        deleteEvent.setActionType(UserActionEvent.ActionType.POST_DELETED);
        deleteEvent.setPostId(postId);
        publisher.publishAction(deleteEvent, UserActionEvent.ActionType.POST_DELETED.getValue());

        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> !neo4jRepo.postExists(postId));
    }

    @Test
    public void testUserDeletionClearsAllRelationships() {
        // 1. Créer des relations pour l'utilisateur
        Node userNode1 = new Node(Node.NodeType.USER, user1);
        Node userNode2 = new Node(Node.NodeType.USER, user2);
        Node postNode = new Node(Node.NodeType.POST, postId);

        // Relation FOLLOWS entre user1 et user2
        neo4jRepo.createOrUpdateRelationship(new Relationship(
                userNode1, userNode2, Relationship.RelationshipType.FOLLOWS, System.currentTimeMillis()));

        // Relation BLOCKS de user2 vers user1
        neo4jRepo.createOrUpdateRelationship(new Relationship(
                userNode2, userNode1, Relationship.RelationshipType.BLOCKS, System.currentTimeMillis()));

        // Relation LIKES entre user1 et un post
        neo4jRepo.createOrUpdateRelationship(new Relationship(
                userNode1, postNode, Relationship.RelationshipType.LIKES, System.currentTimeMillis()));

        // Relation POSTED entre user1 et un post
        neo4jRepo.createOrUpdateRelationship(new Relationship(
                userNode1, postNode, Relationship.RelationshipType.POSTED, System.currentTimeMillis()));

        // Vérifier que les relations existent bien
        assertTrue(neo4jRepo.userRelationshipExists(user1, user2, "FOLLOWS"));
        assertTrue(neo4jRepo.userRelationshipExists(user2, user1, "BLOCKS"));

        assertTrue(neo4jRepo.likeRelationshipExists(postNode.nodeId(), userNode1.nodeId()));


        // 2. Simuler l'événement de suppression d'utilisateur
        UserActionEvent deleteUserEvent = new UserActionEvent();
        deleteUserEvent.setActionType(UserActionEvent.ActionType.USER_DELETED);
        deleteUserEvent.setUsername(user1);
        publisher.publishAction(deleteUserEvent, UserActionEvent.ActionType.USER_DELETED.getValue());

        // 3. Vérifier que les relations ont été supprimées
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Le nœud utilisateur ne devrait plus exister
            assertFalse(neo4jRepo.nodeExists(user1, Node.NodeType.USER));

            // Toutes les relations devraient être supprimées
            assertFalse(neo4jRepo.userRelationshipExists(user1, user2, "FOLLOWS"));

            // La relation POSTED et le post lui-même devraient être supprimés
            assertFalse(neo4jRepo.postRelationshipExists(postNode.nodeId(), userNode1.nodeId()));
        });
    }

    @Test
    public void testPostDeletionClearsAllRelationships() {
        // 1. Créer des relations pour le post
        Node userNode1 = new Node(Node.NodeType.USER, user1);
        Node userNode2 = new Node(Node.NodeType.USER, user2);
        Node postNode = new Node(Node.NodeType.POST, postId);

        // Relation LIKES entre user1 et le post
        neo4jRepo.createOrUpdateRelationship(new Relationship(
                userNode1, postNode, Relationship.RelationshipType.LIKES, System.currentTimeMillis()));

        // Relation LIKES entre user2 et le post
        neo4jRepo.createOrUpdateRelationship(new Relationship(
                userNode2, postNode, Relationship.RelationshipType.LIKES, System.currentTimeMillis()));

        // Relation POSTED entre user1 et le post
        neo4jRepo.createOrUpdateRelationship(new Relationship(
                userNode1, postNode, Relationship.RelationshipType.POSTED, System.currentTimeMillis()));

        // Vérifier que les relations existent bien
        assertTrue(neo4jRepo.likeRelationshipExists(postNode.nodeId(), userNode1.nodeId()));
        assertTrue(neo4jRepo.likeRelationshipExists(postNode.nodeId(), userNode2.nodeId()));
        assertTrue(neo4jRepo.postRelationshipExists(postNode.nodeId(), userNode1.nodeId()));

        // 2. Simuler l'événement de suppression de post
        UserActionEvent deletePostEvent = new UserActionEvent();
        deletePostEvent.setActionType(UserActionEvent.ActionType.POST_DELETED);
        deletePostEvent.setPostId(postId);
        publisher.publishAction(deletePostEvent, UserActionEvent.ActionType.POST_DELETED.getValue());

        // 3. Vérifier que le nœud et ses relations ont été supprimés
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Le nœud post ne devrait plus exister
            assertFalse(neo4jRepo.nodeExists(postId, Node.NodeType.POST));

            // Les relations LIKES devraient être supprimées
            assertFalse(neo4jRepo.likeRelationshipExists(postNode.nodeId(), userNode1.nodeId()));
            assertFalse(neo4jRepo.likeRelationshipExists(postNode.nodeId(), userNode2.nodeId()));

            // La relation POSTED devrait être supprimée
            assertFalse(neo4jRepo.postRelationshipExists(postNode.nodeId(), userNode1.nodeId()));
        });
    }

    // Méthodes utilitaires
    private void createUserNode(String username) {
        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_CREATED);
        event.setUsername(username);
        publisher.publishAction(event, UserActionEvent.ActionType.USER_CREATED.getValue());

        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> neo4jRepo.nodeExists(username, Node.NodeType.USER));
    }

    private void createPostNode(String postId) {
        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostId(postId);
        publisher.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> neo4jRepo.postExists(postId));
    }
}