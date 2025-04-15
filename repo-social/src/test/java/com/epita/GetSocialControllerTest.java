package com.epita;

import com.epita.repository.SocialRepoNeo4j;
import com.epita.repository.models.Node;
import com.epita.repository.models.Relationship;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.empty;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class GetSocialControllerTest {
    @Inject
    SocialRepoNeo4j socialRepoNeo4j;

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
        socialRepoNeo4j.clearDatabase();
        logger.info("[TEST SETUP] Base de données Neo4j nettoyée avec succès");
        // Création des données de test
        user1 = "testuser-" + UUID.randomUUID().toString().substring(0, 8);
        user2 = "testuser-" + UUID.randomUUID().toString().substring(0, 8);
        postId = UUID.randomUUID().toString();

        // Création des nœuds nécessaires
        socialRepoNeo4j.createUser(user1);
        socialRepoNeo4j.createUser(user2);
        socialRepoNeo4j.createPostNode(postId);
    }

    @Test
    public void testGetLikeUsersSuccess() {
        // Créer deux utilisateurs
        String user1 = "userForPost";
        String user2 = "likerUser";
        socialRepoNeo4j.createUser(user1);
        socialRepoNeo4j.createUser(user2);
        socialRepoNeo4j.createPostNode(postId);
        Node userNode = new Node(Node.NodeType.USER, user1);
        Node post = new Node(Node.NodeType.POST, postId);
        Relationship relationship = new Relationship(userNode, post, Relationship.RelationshipType.POSTED, System.currentTimeMillis());
        socialRepoNeo4j.createOrUpdateRelationship(relationship);

        // user2 aime le post
        given()
                .contentType("application/json")
                .when().post("/social/" + user2 + "/like/" + postId)
                .then().statusCode(200);
        assertTrue(socialRepoNeo4j.likeRelationshipExists(postId, user2));

        // Vérifier que l'appel GET /posts/{postId}/likeUsers retourne user2 comme ayant aimé le post
        given()
                .contentType("application/json")
                .when().get("/social/posts/" + postId + "/likeUsers")
                .then().statusCode(200)
                .body("postId", containsString(postId))
                .body("likeUsers", hasItem(user2));
    }

    @Test
    public void testGetLikeUsersNotFound() {
        // Appel avec un post inexistant
        String randomPostId = UUID.randomUUID().toString();
        given()
                .contentType("application/json")
                .when().get("/social/posts/" + randomPostId + "/likeUsers")
                .then().statusCode(404);
    }

    @Test
    public void testGetLikedPostsSuccess() {
        // Créer deux utilisateurs
        String author = "authorUser";
        String liker = "likerUser";
        socialRepoNeo4j.createUser(author);
        socialRepoNeo4j.createUser(liker);
        socialRepoNeo4j.createPostNode(postId);
        Node authorNode = new Node(Node.NodeType.USER, author);
        Node post = new Node(Node.NodeType.POST, postId);
        Relationship relationship = new Relationship(authorNode, post, Relationship.RelationshipType.POSTED, System.currentTimeMillis());
        socialRepoNeo4j.createOrUpdateRelationship(relationship);

        // Liker le post par le liker
        given()
                .contentType("application/json")
                .when().post("/social/" + liker + "/like/" + postId)
                .then().statusCode(200);

        // Vérifier que GET /users/{userId}/likedPosts retourne le post
        given()
                .contentType("application/json")
                .when().get("/social/users/" + liker + "/likedPosts")
                .then().statusCode(200)
                .body("username", containsString(liker))
                .body("likedPosts", hasItem(postId));
    }

    @Test
    public void testGetLikedPostsUserNotFound() {
        String randomUser = "nonexistentUser";
        given()
                .contentType("application/json")
                .when().get("/social/users/" + randomUser + "/likedPosts")
                .then().statusCode(404)
                .body(containsString("L'utilisateur n'existe pas"));
    }

    @Test
    public void testGetFollowersSuccess() {
        // Créer deux utilisateurs
        String follower = "followerUser";
        String target = "targetUser";
        socialRepoNeo4j.createUser(follower);
        socialRepoNeo4j.createUser(target);

        // follower suit target
        given().contentType("application/json")
                .when().post("/social/" + follower + "/follow/" + target)
                .then().statusCode(200);

        // Vérifier que GET /users/{userId}/followers pour target retourne follower
        given().contentType("application/json")
                .when().get("/social/users/" + target + "/followers")
                .then().statusCode(200)
                .body("username", containsString(target))
                .body("followers", hasItem(follower));
    }

    @Test
    public void testGetFollowersUserNotFound() {
        String randomUser = "nonexistentUser";
        given()
                .when().get("/social/users/" + randomUser + "/followers")
                .then().statusCode(404)
                .body(containsString("L'utilisateur n'existe pas"));
    }

    @Test
    public void testGetFollowsSuccess() {
        // Créer deux utilisateurs
        String follower = "followerUser";
        String followed = "followedUser";
        socialRepoNeo4j.createUser(follower);
        socialRepoNeo4j.createUser(followed);

        // follower suit followed
        given()
                .contentType("application/json")
                .when().post("/social/" + follower + "/follow/" + followed)
                .then().statusCode(200);

        // Vérifier que GET /users/{userId}/follows pour follower retourne followed
        given()
                .contentType("application/json")
                .when().get("/social/users/" + follower + "/follows")
                .then().statusCode(200)
                .body("username", containsString(follower))
                .body("followers", hasItem(followed));
    }

    @Test
    public void testGetFollowsUserNotFound() {
        String randomUser = "nonexistentUser";
        given()
                .when().get("/social/users/" + randomUser + "/follows")
                .then().statusCode(404)
                .body(containsString("L'utilisateur n'existe pas"));
    }

    @Test
    public void testGetBlockedUsersSuccess() {
        // Créer deux utilisateurs
        String blocker = "blockerUser";
        String blocked = "blockedUser";
        socialRepoNeo4j.createUser(blocker);
        socialRepoNeo4j.createUser(blocked);

        // blocker bloque blocked
        given()
                .contentType("application/json")
                .when().post("/social/" + blocker + "/block/" + blocked)
                .then().statusCode(200);

        // Vérifier que GET /users/{userId}/blocked retourne blocked pour blocker
        given()
                .contentType("application/json")
                .when().get("/social/users/" + blocker + "/blocked")
                .then().statusCode(200)
                .body("username", containsString(blocker))
                .body("blockedUsers", hasItem(blocked));
    }

    @Test
    public void testGetBlockedUsersUserNotFound() {
        String randomUser = "nonexistentUser";
        given()
                .when().get("/social/users/" + randomUser + "/blocked")
                .then().statusCode(404)
                .body(containsString("L'utilisateur n'existe pas"));
    }

    @Test
    public void testGetBlockingUsersSuccess() {
        // Créer deux utilisateurs
        String blocker = "blockerUser";
        String blocked = "blockedUser";
        socialRepoNeo4j.createUser(blocker);
        socialRepoNeo4j.createUser(blocked);

        // blocker bloque blocked
        given()
                .contentType("application/json")
                .when().post("/social/" + blocker + "/block/" + blocked)
                .then().statusCode(200);

        // Vérifier que GET /users/{userId}/isblocked pour blocked retourne blocker
        given()
                .contentType("application/json")
                .when().get("/social/users/" + blocked + "/isblocked")
                .then().statusCode(200)
                .body("username", containsString(blocked))
                .body("isBlockedBy", hasItem(blocker));
    }

    @Test
    public void testGetBlockingUsersUserNotFound() {
        String randomUser = "nonexistentUser";
        given()
                .contentType("application/json")
                .when().get("/social/users/" + randomUser + "/isblocked")
                .then().statusCode(404)
                .body(containsString("L'utilisateur n'existe pas"));
    }
}