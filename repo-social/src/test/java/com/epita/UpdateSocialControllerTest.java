package com.epita;

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

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class UpdateSocialControllerTest {

    @Inject
    SocialRepoNeo4j neo4jRepo;

    @Inject
    Logger logger;

    private String user1;
    private String user2;
    private String postId;

    @BeforeEach
    public void setup() {
        // Augmenter le timeout car les write sont transactionnels
        RestAssured.config = RestAssured.config().httpClient(
                HttpClientConfig.httpClientConfig()
                        .setParam("http.socket.timeout", 30000)
                        .setParam("http.connection.timeout", 30000)
        );

        // Nettoyage de la base
        neo4jRepo.clearDatabase();

        // Création des données de test
        user1 = "testuser1-" + UUID.randomUUID().toString().substring(0, 8);
        user2 = "testuser2-" + UUID.randomUUID().toString().substring(0, 8);
        postId = UUID.randomUUID().toString();

        // Création des nœuds nécessaires
        neo4jRepo.createUser(user1);
        neo4jRepo.createUser(user2);
        neo4jRepo.createPostNode(postId);

        logger.info("Test setup completed with users: " + user1 + ", " + user2);
    }

    @Test
    public void testFollowUser() {
        // Quand
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/follow/" + user2)
                .then()
                .statusCode(200);

        // Alors - Vérifier que la relation a été créée dans Neo4j
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertTrue(neo4jRepo.userRelationshipExists(user1, user2, "FOLLOWS"));
        });

        // Test avec un utilisateur inexistant
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/follow/" + "nonexistentuser")
                .then()
                .statusCode(404);

        // Test avec un utilisateur déjà suivi
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/follow/" + user2)
                .then()
                .statusCode(409);

        // Test avec un utilisateur qui se suit lui-même
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/follow/" + user1)
                .then()
                .statusCode(400);

        // Test avec un utilisateur qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + "nonexistentuser" + "/follow/" + user2)
                .then()
                .statusCode(404);
    }

    @Test
    public void testUnfollowUser() {
        // Prérequis: l'utilisateur suit déjà
        Node userNode1 = new Node(Node.NodeType.USER, user1);
        Node userNode2 = new Node(Node.NodeType.USER, user2);
        Relationship relationship = new Relationship(userNode1, userNode2, Relationship.RelationshipType.FOLLOWS, System.currentTimeMillis());
        neo4jRepo.createOrUpdateRelationship(relationship);

        // Vérifier que la relation existe bien
        assertTrue(neo4jRepo.userRelationshipExists(user1, user2, "FOLLOWS"));

        // Quand
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unfollow/" + user2)
                .then()
                .statusCode(200);

        // Alors - Vérifier que la relation a été supprimée
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertFalse(neo4jRepo.userRelationshipExists(user1, user2, "FOLLOWS"));
        });

        // Test avec un utilisateur qui ne suit pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unfollow/" + user2)
                .then()
                .statusCode(409);

        // Test avec un utilisateur qui se suit lui-même
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unfollow/" + user1)
                .then()
                .statusCode(400);

        // Test avec un utilisateur qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + "nonexistentuser" + "/unfollow/" + user2)
                .then()
                .statusCode(404);

        // Test avec un utilisateur qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unfollow/" + "nonexistentuser")
                .then()
                .statusCode(404);
    }

    @Test
    public void testBlockUser() {
        // Créer d'abord une relation FOLLOWS pour vérifier qu'elle est supprimée
        Node userNode1 = new Node(Node.NodeType.USER, user1);
        Node userNode2 = new Node(Node.NodeType.USER, user2);
        Relationship followRelationship = new Relationship(userNode1, userNode2, Relationship.RelationshipType.FOLLOWS, System.currentTimeMillis());
        neo4jRepo.createOrUpdateRelationship(followRelationship);

        // Quand
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/block/" + user2)
                .then()
                .statusCode(200);

        // Alors - Vérifier que la relation FOLLOWS a été supprimée et que BLOCKS existe
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertFalse(neo4jRepo.userRelationshipExists(user1, user2, "FOLLOWS"));
            assertTrue(neo4jRepo.userRelationshipExists(user1, user2, "BLOCKS"));
        });

        // Test avec un utilisateur déjà bloqué
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/block/" + user2)
                .then()
                .statusCode(409);

        // Test avec un utilisateur qui se bloque lui-même
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/block/" + user1)
                .then()
                .statusCode(400);

        // Test avec un utilisateur qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + "nonexistentuser" + "/block/" + user2)
                .then()
                .statusCode(404);

        // Test avec un utilisateur qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/block/" + "nonexistentuser")
                .then()
                .statusCode(404);
    }

    @Test
    public void testUnblockUser() {
        // Prérequis: l'utilisateur a déjà bloqué
        Node userNode1 = new Node(Node.NodeType.USER, user1);
        Node userNode2 = new Node(Node.NodeType.USER, user2);
        Relationship relationship = new Relationship(userNode1, userNode2, Relationship.RelationshipType.BLOCKS, System.currentTimeMillis());
        neo4jRepo.createOrUpdateRelationship(relationship);

        // Quand
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unblock/" + user2)
                .then()
                .statusCode(200);

        // Alors - Vérifier que la relation a été supprimée
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertFalse(neo4jRepo.userRelationshipExists(user1, user2, "BLOCKS"));
        });

        // Test avec un utilisateur qui n'est pas bloqué
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unblock/" + user2)
                .then()
                .statusCode(409);

        // Test avec un utilisateur qui se débloque lui-même
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unblock/" + user1)
                .then()
                .statusCode(400);

        // Test avec un utilisateur qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + "nonexistentuser" + "/unblock/" + user2)
                .then()
                .statusCode(404);

        // Test avec un utilisateur qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unblock/" + "nonexistentuser")
                .then()
                .statusCode(404);
    }

    @Test
    public void testLikePost() {
        // Quand
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/like/" + postId)
                .then()
                .statusCode(200);

        // Alors - Vérifier que la relation LIKES existe
        Node userNode = new Node(Node.NodeType.USER, user1);
        Node postNode = new Node(Node.NodeType.POST, postId);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertTrue(neo4jRepo.likeRelationshipExists(postNode.nodeId(), userNode.nodeId()));
        });

        // Test avec un post déjà aimé
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/like/" + postId)
                .then()
                .statusCode(409);

        // Test avec un post qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/like/" + "nonexistentpost")
                .then()
                .statusCode(404);

        // Test avec un utilisateur qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + "nonexistentuser" + "/like/" + postId)
                .then()
                .statusCode(404);

        // Test avec un utilisateur qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/like/" + "nonexistentpost")
                .then()
                .statusCode(404);

        // Test avec un utilisateur bloqué
        neo4jRepo.deleteLikeRelationship(user1, postId);
        Node userNode2 = new Node(Node.NodeType.USER, user2);
        Relationship authorshipRelationship = new Relationship(userNode2, postNode, Relationship.RelationshipType.POSTED, System.currentTimeMillis());
        Relationship blockRelationship = new Relationship(userNode, userNode2, Relationship.RelationshipType.BLOCKS, System.currentTimeMillis());
        neo4jRepo.createOrUpdateRelationship(blockRelationship);
        neo4jRepo.createOrUpdateRelationship(authorshipRelationship);
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/like/" + postId)
                .then()
                .statusCode(403);
    }

    @Test
    public void testUnlikePost() {
        // Prérequis: l'utilisateur aime déjà le post
        Node userNode = new Node(Node.NodeType.USER, user1);
        Node postNode = new Node(Node.NodeType.POST, postId);
        Relationship likesRelationship = new Relationship(userNode, postNode, Relationship.RelationshipType.LIKES, System.currentTimeMillis());
        neo4jRepo.createOrUpdateRelationship(likesRelationship);

        // Quand
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unlike/" + postId)
                .then()
                .statusCode(200);

        // Alors - Vérifier que la relation a été supprimée
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertFalse(neo4jRepo.likeRelationshipExists(user1, postId));
        });

        // Test avec un post qui n'est pas aimé
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unlike/" + postId)
                .then()
                .statusCode(409);

        // Test avec un post qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unlike/" + "nonexistentpost")
                .then()
                .statusCode(404);

        // Test avec un utilisateur qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + "nonexistentuser" + "/unlike/" + postId)
                .then()
                .statusCode(404);

        // Test avec un utilisateur qui n'existe pas
        given()
                .contentType("application/json")
                .when()
                .post("/social/" + user1 + "/unlike/" + "nonexistentpost")
                .then()
                .statusCode(404);
    }
}