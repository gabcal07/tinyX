import com.epita.dto.PostType;
import com.epita.events.UserActionEvent;
import com.epita.redis.PostActionPublisher;
import com.epita.repository.PostRepoMongo;
import com.epita.repository.RegisteredUsersRepo;
import com.epita.repository.models.PostModel;
import com.epita.service.entities.PostEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RepoPostTest {

    @Inject
    RegisteredUsersRepo registeredUsersRepo;
    @Inject
    PostRepoMongo postRepoMongo;

    private PostModel postModel;

    @Inject
    PostActionPublisher publisher;

    private final String user1 = "user1";
    private final String user2 = "user2";
    private final String user3 = "user3";
    private final String blockedUser = "blocked_user";

    @BeforeAll
    public void init() {
        RestAssured.config = RestAssured.config().httpClient(
                HttpClientConfig.httpClientConfig()
                        .setParam("http.socket.timeout", 30000)
                        .setParam("http.connection.timeout", 30000)
        );
        registeredUsersRepo.clearUsers();
        postRepoMongo.clearPosts();

        // Enregistrement des utilisateurs avec des noms d'utilisateur
        UserActionEvent registerUser1Event = new UserActionEvent();
        registerUser1Event.setUsername(user1);
        registerUser1Event.setActionType(UserActionEvent.ActionType.USER_CREATED);
        publisher.publishAction(registerUser1Event, UserActionEvent.ActionType.USER_CREATED.getValue());

        UserActionEvent registerUser2Event = new UserActionEvent();
        registerUser2Event.setUsername(user2);
        registerUser2Event.setActionType(UserActionEvent.ActionType.USER_CREATED);
        publisher.publishAction(registerUser2Event, UserActionEvent.ActionType.USER_CREATED.getValue());

        UserActionEvent registerUser3Event = new UserActionEvent();
        registerUser3Event.setUsername(user3);
        registerUser3Event.setActionType(UserActionEvent.ActionType.USER_CREATED);
        publisher.publishAction(registerUser3Event, UserActionEvent.ActionType.USER_CREATED.getValue());

        // Enregistrement d'un utilisateur bloqué
        UserActionEvent registerBlockedUserEvent = new UserActionEvent();
        registerBlockedUserEvent.setUsername(blockedUser);
        registerBlockedUserEvent.setActionType(UserActionEvent.ActionType.USER_CREATED);
        publisher.publishAction(registerBlockedUserEvent, UserActionEvent.ActionType.USER_CREATED.getValue());

        // Enregistrement de l'action du bloqueur
        UserActionEvent blockUserEvent = new UserActionEvent();
        blockUserEvent.setUsername(user3);
        blockUserEvent.setActionType(UserActionEvent.ActionType.USER_BLOCKED);
        blockUserEvent.setTargetUsername(blockedUser);
        publisher.publishAction(blockUserEvent, UserActionEvent.ActionType.USER_BLOCKED.getValue());

        PostEntity postEntity = new PostEntity();
        postEntity.setAuthorUsername("blocked_user");
        postEntity.setType(PostType.ORIGINAL);
        postEntity.setText("I am the first one");
        postModel = postRepoMongo.createPost(postEntity);
    }

    @Test
    @Order(1)
    public void createReplyToBlockedUser() {
        String metadata = "{\"authorUsername\":\"user3\","
                + "\"type\":\"REPLY\","
                + "\"text\":\"Hello World\","
                + "\"parentPostId\":\"" + postModel.getPostId() + "\"}";

        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(2)
    public void testCreatePostWithValidData() {
        String metadata = "{\"authorUsername\":\"user2\","
                + "\"type\":\"ORIGINAL\","
                + "\"text\":\"Hello World\"}";

        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(200)
                .body("postId", notNullValue());
    }

    @Test
    @Order(3)
    public void testCreatePostInvalidMissingTextAndFile() {
        String metadata = "{" +
                "\\\"authorUsername\\\":\\\"user1\\\"," +
                "\\\"type\\\":\\\"ORIGINAL\\\"" +
                "}";
        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    public void testCreateReplyWithoutParentPostId() {
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"REPLY\","
                + "\"text\":\"Hello World\"}";

        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    public void testCreatePostWithTextExceeding160Characters() {
        // Création d'un texte de plus de 160 caractères
        String longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor.";

        // Vérification que le texte dépasse bien 160 caractères
        assert longText.length() > 160;

        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"ORIGINAL\","
                + "\"text\":\"" + longText + "\"}";

        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    public void testCreateReplyWithParentPostIdAndText() {
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"REPLY\","
                + "\"text\":\"Hello World\","
                + "\"parentPostId\":\"" + postModel.getPostId() + "\"}";

        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(200)
                .body("postId", notNullValue());
    }

    @Test
    @Order(7)
    public void testCreateReplyWithIncorrectParentPostIdAndText() {
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"REPLY\","
                + "\"text\":\"Hello World\","
                + "\"parentPostId\":\"" + "550e8400-e29b-41d4-a716-446655440008" + "\"}";

        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(8)
    public void testCreateRepostWithoutParentPostId() {
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"REPOST\","
                + "\"text\":\"Hello World\"}";

        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(9)
    public void testCreateRepostWithParentPostIdAndText() {
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"REPOST\","
                + "\"text\":\"Hello World\","
                + "\"parentPostId\":\"" + postModel.getPostId() + "\"}";

        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(200)
                .body("postId", notNullValue());
    }

    @Test
    @Order(10)
    public void testCreateRepostWithIncorrectParentPostIdAndText() {
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"REPOST\","
                + "\"text\":\"Hello World\","
                + "\"parentPostId\":\"550e8400-e29b-41d4-a716-446655440008\"}";

        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(11)
    public void testGetPostNotFound() {
        given()
                .when()
                .get("/posts/550e8400-e29b-41d4-a716-446655440000")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(12)
    public void testDeletePostNotFound() {
        given()
                .header("X-user-name", "user1")
                .when()
                .post("/posts/550e8400-e29b-41d4-a716-446655440000")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(13)
    public void testGetUserPostsNotFound() {
        given()
                .when()
                .get("/posts/user/nonexistent_user")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(14)
    public void testDeleteOwnPost() {
        // Créer d'abord un post
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"ORIGINAL\","
                + "\"text\":\"Post à supprimer\"}";

        String postId = given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(200)
                .extract().path("postId");

        // Supprimer le post avec un timeout augmenté
        given()
                .header("X-user-name", "user1")
                .config(RestAssured.config().httpClient(
                        HttpClientConfig.httpClientConfig()
                                .setParam("http.socket.timeout", 30000)
                                .setParam("http.connection.timeout", 30000)
                ))
                .when()
                .post("/posts/" + postId)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(15)
    public void testDeleteOtherUserPost() {
        // Créer un post avec un utilisateur
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"ORIGINAL\","
                + "\"text\":\"Post d'un autre utilisateur\"}";

        String postId = given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(200)
                .extract().path("postId");

        // Essayer de supprimer avec un autre utilisateur
        given()
                .header("X-user-name", "user2")
                .when()
                .post("/posts/" + postId)
                .then()
                .statusCode(401); // Unauthorized
    }

    @Test
    @Order(16)
    public void testGetUserPosts() {
        // Créer quelques posts pour l'utilisateur
        String metadata = "{\"authorUsername\":\"user3\","
                + "\"type\":\"ORIGINAL\","
                + "\"text\":\"Premier post\"}";

        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts");

        // Récupérer les posts de l'utilisateur
        given()
                .when()
                .get("/posts/user/user3")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(17)
    public void testGeUnregisteredtUserPosts() {
        // Créer quelques posts pour l'utilisateur
        String metadata = "{\"authorUsername\":\"user3\","
                + "\"type\":\"ORIGINAL\","
                + "\"text\":\"Premier post\"}";

        given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(200);

        // Récupérer les posts de l'utilisateur
        given()
                .when()
                .get("/posts/user/user16")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(18)
    public void testGetPostById() {
        // Créer un post pour le récupérer ensuite
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"ORIGINAL\","
                + "\"text\":\"Post to retrieve\"}";

        String postId = given()
                .multiPart("metadata", metadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(200)
                .extract().path("postId");

        // Récupérer le post par son ID
        given()
                .when()
                .get("/posts/" + postId)
                .then()
                .statusCode(200)
                .body("postId", notNullValue())
                .body("text", org.hamcrest.Matchers.equalTo("Post to retrieve"));
    }

    @Test
    @Order(19)
    public void testGetPostReplies() {
        // Créer un post parent
        String parentMetadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"ORIGINAL\","
                + "\"text\":\"Post parent\"}";

        String parentPostId = given()
                .multiPart("metadata", parentMetadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(200)
                .extract().path("postId");

        // Ajouter une réponse
        String replyMetadata = "{\"authorUsername\":\"user2\","
                + "\"type\":\"REPLY\","
                + "\"text\":\"Reponse au post\","
                + "\"parentPostId\":\"" + parentPostId + "\"}";

        given()
                .multiPart("metadata", replyMetadata, "application/json")
                .when()
                .post("/posts")
                .then()
                .statusCode(200);

        // Récupérer les réponses au post
        given()
                .when()
                .get("/posts/replies/" + parentPostId)
                .then()
                .statusCode(200)
                .body("[0].postId", notNullValue())
                .body("[0].text", org.hamcrest.Matchers.equalTo("Reponse au post"));
    }


    @Test
    @Order(20)
    public void testCreatePostWithTextAndMediaFile() {
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"ORIGINAL\","
                + "\"text\":\"Post avec média\"}";

        // Simuler un fichier média
        byte[] fileContent = "contenu du fichier".getBytes();

        given()
                .multiPart("metadata", metadata, "application/json")
                .multiPart("file", "image.jpg", fileContent, "image/jpeg")
                .when()
                .post("/posts")
                .then()
                .statusCode(200)
                .body("postId", notNullValue())
                .body("mediaUrl", notNullValue());
    }

    @Test
    @Order(21)
    public void testCreatePostWithTextAndRealMediaFile() throws IOException {
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"ORIGINAL\","
                + "\"text\":\"Post avec média\"}";


        // Nettoyer les fichiers existants
        given()
                .when()
                .post("/api/files/clear")
                .then()
                .statusCode(200);

        // Utiliser un fichier de code source comme média
        File fichierCode = new File("src/main/java/com/epita/repository/PostRepoMongo.java");

        given()
                .multiPart("metadata", metadata, "application/json")
                .multiPart("file", fichierCode, "text/plain")
                .when()
                .post("/posts")
                .then()
                .statusCode(200)
                .body("postId", notNullValue())
                .body("mediaUrl", notNullValue());
    }

    @Test
    @Order(22)
    public void testCreateAndGetPostWithMedia() throws IOException {
        // 1. Créer un post avec un fichier média
        String metadata = "{\"authorUsername\":\"user1\","
                + "\"type\":\"ORIGINAL\","
                + "\"text\":\"Post avec fichier à vérifier\"}";

        // Nettoyer les fichiers existants
        given()
                .when()
                .post("/api/files/clear")
                .then()
                .statusCode(200);

        File fichierTest = new File("src/main/java/com/epita/repository/RegisteredUsersRepo.java");

        // Récupérer le postId et mediaUrl lors de la création
        var response = given()
                .multiPart("metadata", metadata, "application/json")
                .multiPart("file", fichierTest, "text/plain")
                .when()
                .post("/posts")
                .then()
                .statusCode(200)
                .extract().response();

        String postId = response.path("postId");
        String mediaUrl = response.path("mediaUrl");

        // Vérifier qu'ils ne sont pas null
        assert postId != null;
        assert mediaUrl != null;

        // 2. Récupérer le post par son ID et vérifier le mediaUrl
        given()
                .when()
                .get("/posts/" + postId)
                .then()
                .statusCode(200)
                .body("postId", org.hamcrest.Matchers.equalTo(postId))
                .body("mediaUrl", org.hamcrest.Matchers.equalTo(mediaUrl))
                .body("mediaUrl", notNullValue());

        // 3. Télécharger le fichier média et vérifier son contenu
        byte[] downloadedContent = given()
                .when()
                .get(mediaUrl)
                .then()
                .statusCode(200)
                .extract().asByteArray();

        // Vérifier que le contenu n'est pas vide
        assert downloadedContent != null && downloadedContent.length > 0;

        // Comparer avec le contenu original du fichier
        byte[] originalContent = Files.readAllBytes(fichierTest.toPath());
        assertArrayEquals(originalContent, downloadedContent);
    }

//    @Test
//    @Order(Integer.MAX_VALUE)
//    public void testDeleteUserAndCheckPostsDeletion() {
//        String metadata = "{\"authorUsername\":\"user1\","
//                + "\"type\":\"ORIGINAL\","
//                + "\"text\":\"Post avec média\"}";
//
//        // Simuler un fichier média
//        byte[] fileContent = "contenu du fichier".getBytes();
//
//        given()
//                .multiPart("metadata", metadata, "application/json")
//                .multiPart("file", "image.jpg", fileContent, "image/jpeg")
//                .when()
//                .post("/posts")
//                .then()
//                .statusCode(200)
//                .body("postId", notNullValue())
//                .body("mediaUrl", notNullValue());
//
//        UserActionEvent event = new UserActionEvent();
//        event.setUsername(user1);
//        event.setActionType(UserActionEvent.ActionType.USER_DELETED);
//        publisher.publishAction(event, UserActionEvent.ActionType.USER_DELETED.getValue());
//        // Vérifier que la liste de posts est vide
//        given()
//                .when()
//                .get("/posts/user/" + user1)
//                .then()
//                .statusCode(200)
//                .body("posts", org.hamcrest.Matchers.empty());
//    }
}