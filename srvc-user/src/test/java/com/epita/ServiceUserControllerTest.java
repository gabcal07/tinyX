package com.epita;

import com.epita.dto.contracts.CreateUserContract;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ServiceUserControllerTest {
    CreateUserContract validUser = new CreateUserContract("validUser");
    CreateUserContract invalidUser = new CreateUserContract("");

    @BeforeEach
    void setup() {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        // Clear the database before each test
        given()
                .contentType("application/json")
                .when().post("/users/clear")
                .then()
                .statusCode(200);
    }

    @Test
    void testCreateUser() {
        // Test creating a valid user
        given()
                .contentType("application/json")
                .body(validUser)
                .when().post("/users/create")
                .then()
                .statusCode(200);

        // Test creating an invalid user
        given()
                .contentType("application/json")
                .body(invalidUser)
                .when().post("/users/create")
                .then()
                .statusCode(400);
    }

    @Test
    void testGetUser() {
        // Create a user first
        given()
                .contentType("application/json")
                .body(validUser)
                .when().post("/users/create")
                .then()
                .statusCode(200);

        // Test getting the created user
        given()
                .when().get("/users/get/" + validUser.getUsername())
                .then()
                .statusCode(200)
                .body("username", is(validUser.getUsername()));

        // Test getting a non-existent user
        given()
                .when().get("/users/get/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteUser() {
        // Create a user first
        given()
                .contentType("application/json")
                .body(validUser)
                .when().post("/users/create")
                .then()
                .statusCode(200);

        // Test deleting the created user
        given()
                .when().delete("/users/delete/" + validUser.getUsername())
                .then()
                .statusCode(200);

        // Test deleting a non-existent user
        given()
                .when().delete("/users/delete/" + "asdgfasdasdgasd")
                .then()
                .statusCode(404);
    }

    @Test
    void testClearUsers() {
        given()
                .contentType("application/json")
                .body(validUser)
                .when().post("/users/create")
                .then()
                .statusCode(200);

        // Test clearing all users
        given()
                .contentType("application/json")
                .when().post("/users/clear")
                .then()
                .statusCode(200);

        given()
                .when().get("/users/get/" + validUser.getUsername())
                .then()
                .statusCode(404);
    }

    @Test
    void testCreateExistantUser() {
        given()
                .contentType("application/json")
                .body(validUser)
                .when().post("/users/create")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .body(validUser)
                .when().post("/users/create")
                .then()
                .statusCode(409);
    }

    @Test
    void testCreateUserWithTooLongUsername() {
        // Créer un utilisateur avec un nom dépassant 24 caractères
        CreateUserContract tooLongUser = new CreateUserContract("utilisateurAvecUnNomBeaucoupTropLongPourEtreAccepte");

        // Vérifier que le nom d'utilisateur dépasse bien la limite de 24 caractères
        assert tooLongUser.getUsername().length() > 24;

        // Tester la création d'un utilisateur avec un nom trop long
        given()
                .contentType("application/json")
                .body(tooLongUser)
                .when().post("/users/create")
                .then()
                .statusCode(400); // On s'attend à une erreur 400 Bad Request
    }
}