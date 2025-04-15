package com.epita.repository;

import com.epita.repository.models.Node;
import com.epita.repository.models.Relationship;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for managing post data in Neo4j.
 */
@ApplicationScoped
public class SocialRepoNeo4j {

    @Inject
    Driver neo4jDriver;

    @Inject
    Logger logger;

    /**
     * Creates a new post node in Neo4j.
     *
     * @param postId   the UUID of the post
     * @return true if the post node is created successfully, otherwise false
     */
    public Boolean createPostNode(String postId) {
        Node postNode = new Node(Node.NodeType.POST, postId);
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run(postNode.createCypher());
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Deletes a post node by postId in Neo4j.
     *
     * @param postId the UUID of the post to delete
     * @return true if the post node is deleted successfully, otherwise false
     */
    public boolean deletePostWithPostId(String postId) {
        Node postNode = new Node(Node.NodeType.POST, postId);
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                // Synchronous deletion of the node only (hybrid - step 1)
                tx.run(postNode.deleteCypher());
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clears all post nodes in Neo4j.
     *
     * @return true if all post nodes are cleared successfully, otherwise false
     */
    public boolean clearPosts() {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run("MATCH (n:Post) DETACH DELETE n");
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates or updates a relationship in Neo4j.
     *
     * @param relationship the relationship to create or update
     */
    public void createOrUpdateRelationship(Relationship relationship) {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                // Create nodes if necessary
                tx.run(relationship.source().createCypher());
                tx.run(relationship.target().createCypher());

                // Create/Update the relationship with timestamp
                tx.run(relationship.createCypher());
                return null;
            });
        }
    }

    /**
     * Deletes a relationship in Neo4j.
     *
     * @param relationship the relationship to delete
     * @return true if the relationship is deleted successfully, otherwise false
     */
    public boolean deleteRelationship(Relationship relationship) {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run(relationship.deleteCypher());
                return null;
            });
            return true;
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de la relation", e);
            return false;
        }
    }

    /**
     * Deletes all relationships for a post in Neo4j.
     *
     * @param postId the UUID of the post
     */
    public void deleteRelationshipsForPost(UUID postId) {
        logger.info("[DELETE POST RELATIONS][REPOSITORY]: Suppression des relations pour le post: " + postId);
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                // Asynchronous cleanup of relationships (hybrid - step 2)
                String cypher = String.format(
                        "MATCH (p:Post {nodeId: '%s'})-[r]-() DELETE r",
                        postId.toString()
                );
                tx.run(cypher);
                return null;
            });
        }
    }

    /**
     * Supprime les relations de suivi (FOLLOWS) entre deux utilisateurs dans les deux sens.
     *
     * @param username1 le nom d'utilisateur du premier utilisateur
     * @param username2 le nom d'utilisateur du deuxième utilisateur
     * @return true si la suppression a réussi, false sinon
     */
    public boolean removeFollowRelationsBetweenUsers(String username1, String username2) {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                // Suppression dans les deux sens
                String cypherFromFirst = String.format(
                        "MATCH (a:User {nodeId: '%s'})-[r:FOLLOWS]->(b:User {nodeId: '%s'}) DELETE r",
                        username1, username2
                );

                String cypherFromSecond = String.format(
                        "MATCH (a:User {nodeId: '%s'})-[r:FOLLOWS]->(b:User {nodeId: '%s'}) DELETE r",
                        username2, username1
                );

                tx.run(cypherFromFirst);
                tx.run(cypherFromSecond);
                return null;
            });
            return true;
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression des relations de suivi entre utilisateurs", e);
            return false;
        }
    }

    /**
     * Checks if a post node exists in Neo4j.
     *
     * @param postId the ID of the post
     * @return true if the post node exists, otherwise false
     */
    public boolean postExists(String postId) {
        return nodeExists(postId, Node.NodeType.POST);
    }

    /**
     * Checks if a node exists in Neo4j.
     *
     * @param nodeId the ID of the node
     * @param type   the type of the node
     * @return true if the node exists, otherwise false
     */
    public boolean nodeExists(String nodeId, Node.NodeType type) {
        String cypher = String.format(
                "MATCH (n:%s {nodeId: $nodeId}) RETURN COUNT(n) > 0 AS exists",
                type.toString()
        );

        try (Session session = neo4jDriver.session()) {
            return session.executeRead(tx -> tx.run(cypher,
                            Map.of("nodeId", nodeId))
                    .single().get("exists").asBoolean()
            );
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a user relationship exists in Neo4j.
     *
     * @param user1          the nodeId of the first user
     * @param user2          the nodeId of the second user
     * @param relationshipType the type of relationship (e.g., FOLLOWS, BLOCKS)
     * @return true if the relationship exists, otherwise false
     */
    public boolean userRelationshipExists(String user1, String user2, String relationshipType) {
        String cypher = String.format(
                "MATCH (a:User {nodeId: '%s'})-[r:%s]->(b:User {nodeId: '%s'}) RETURN COUNT(r) > 0 AS exists",
                user1, relationshipType, user2
        );

        try (Session session = neo4jDriver.session()) {
            return session.executeRead(tx -> tx.run(cypher)
                    .single().get("exists").asBoolean()
            );
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a post relationship exists in Neo4j.
     *
     * @param postId the ID of the post
     * @param userId the ID of the user
     * @return true if the relationship exists, otherwise false
     */
    public boolean postRelationshipExists(String postId, String userId) {
        String cypher = String.format(
                "MATCH (a:User {nodeId: '%s'})-[r:POSTED]->(b:Post {nodeId: '%s'}) RETURN COUNT(r) > 0 AS exists",
                userId, postId
        );

        try (Session session = neo4jDriver.session()) {
            return session.executeRead(tx -> tx.run(cypher)
                    .single().get("exists").asBoolean()
            );
        } catch (Exception e) {
            return false;
        }
    }

    public boolean likeRelationshipExists(String postId, String userId) {
        String cypher = String.format(
                "MATCH (a:User {nodeId: '%s'})-[r:LIKES]->(b:Post {nodeId: '%s'}) RETURN COUNT(r) > 0 AS exists",
                userId, postId
        );

        try (Session session = neo4jDriver.session()) {
            return session.executeRead(tx -> tx.run(cypher)
                    .single().get("exists").asBoolean()
            );
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Supprime tous les nœuds posts appartenant à un utilisateur spécifique dans Neo4j.
     *
     * @param username Le nodeId de l'utilisateur dont les posts doivent être supprimés
     */
    public void deleteUsersPosts(String username) {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                // Supprimer toutes les relations entrantes et sortantes des posts de l'utilisateur
                String deleteRelationsCypher = String.format(
                        "MATCH (u:User {nodeId: '%s'})-->(p:Post)-[r]-() " +
                                "DELETE r",
                        username
                );
                tx.run(deleteRelationsCypher);

                // Supprimer les relations entre l'utilisateur et ses posts
                String deleteUserPostRelationsCypher = String.format(
                        "MATCH (u:User {nodeId: '%s'})-[r]->(p:Post) " +
                                "DELETE r",
                        username
                );
                tx.run(deleteUserPostRelationsCypher);

                // Supprimer les nœuds des posts
                String deletePostsCypher = String.format(
                        "MATCH (u:User {nodeId: '%s'})-->(p:Post) " +
                                "DELETE p",
                        username
                );
                tx.run(deletePostsCypher);

                return null;
            });
        } catch (Exception e) {
            logger.error("[DELETE USER POSTS][REPOSITORY]: échec de la suppression pour l'utilisateur: " + username, e);
        }
    }

    /**
     * Creates a new user node in Neo4j.
     *
     * @param username the nodeId of the user
     * @return true if the user node is created successfully, otherwise false
     */
    public Boolean createUser(String username) {
        Node userNode = new Node(Node.NodeType.USER, username);
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run(userNode.createCypher());
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clears all user nodes in Neo4j.
     *
     * @return true if all user nodes are cleared successfully, otherwise false
     */
    public boolean clearUsers() {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run("MATCH (n:User) DETACH DELETE n");
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Deletes a user node by userId in Neo4j.
     *
     * @param username the nodeId of the user to delete
     * @return true if the user node is deleted successfully, otherwise false
     */
    public boolean deleteUserWithUserId(String username) {
        Node userNode = new Node(Node.NodeType.USER, username);
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                // Synchronous deletion of the node only (hybrid - step 1)
                tx.run(userNode.deleteCypher());
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Deletes all relationships for a user in Neo4j.
     *
     * @param username the UUID of the user
     */
    public void deleteRelationshipsForUser(String username) {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                // Asynchronous cleanup of relationships (hybrid - step 2)
                String cypher = String.format(
                        "MATCH (u:User {nodeId: '%s'})-[r]-() DELETE r",
                        username
                );
                tx.run(cypher);
                return null;
            });
        }
    }

    /**
     * Deletes a like relationship between a user and a post in Neo4j.
     *
     * @param userId the nodeId of the user
     * @param postId the nodeId of the post
     * @return true if the relationship was deleted successfully, otherwise false
     */
    public boolean deleteLikeRelationship(String userId, String postId) {
        String cypher = String.format(
                "MATCH (a:User {nodeId: '%s'})-[r:LIKES]->(b:Post {nodeId: '%s'}) " +
                        "DELETE r " +
                        "RETURN COUNT(r) > 0 AS wasDeleted",
                userId, postId
        );

        try (Session session = neo4jDriver.session()) {
            return session.executeWrite(tx -> tx.run(cypher)
                    .single().get("wasDeleted").asBoolean());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clears the entire Neo4j database.
     */
    public void clearDatabase() {
        try (Session session = neo4jDriver.session()) {
            session.executeWrite(tx -> {
                tx.run("MATCH (n) DETACH DELETE n");
                return null;
            });
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de la base de données", e);
        }
    }

    public boolean relationshipExists(Relationship relationship) {
        String cypher = String.format(
                "MATCH (a:User {nodeId: '%s'})-[r:%s]->(b:User {nodeId: '%s'}) RETURN COUNT(r) > 0 AS exists",
                relationship.source().nodeId(), relationship.type(), relationship.target().nodeId()
        );

        try (Session session = neo4jDriver.session()) {
            return session.executeRead(tx -> tx.run(cypher)
                    .single().get("exists").asBoolean()
            );
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Récupère l'auteur d'un post.
     *
     * @param postNode le nœud du post
     * @return le nom d'utilisateur de l'auteur
     */
    public String getPostAuthor(Node postNode) {
        try (Session session = neo4jDriver.session()) {
            logger.info("[GET POST AUTHOR][REPOSITORY]: Récupération de l'auteur du post: " + postNode.nodeId());
            String postId = postNode.nodeId();
            String query = String.format("MATCH (post {nodeType: 'Post', nodeId: '%s'})<-[:POSTED]-(user {nodeType: 'User'}) RETURN user.nodeId AS userId", postId);

            Result result = session.run(query, Map.of("postId", postId));
            return result.list().stream()
                    .map(record -> record.get("userId").asString())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche de l'auteur du post: " + e.getMessage());
            return null;
        }
    }

    /**
     * Récupère la liste des utilisateurs qui ont aimé un post.
     *
     * @param postId l'identifiant du post
     * @return la liste des noms d'utilisateur des utilisateurs qui ont aimé le post
     */
    public List<String> getPostLikeUsers(String postId
    ) {
        try (Session session = neo4jDriver.session()) {
            logger.info("[GET POST LIKE USERS][REPOSITORY]: Récupération des utilisateurs qui ont aimé le post: " + postId);
            String query = String.format("MATCH (user {nodeType: 'User'})-[r:LIKES]->(post {nodeType: 'Post', nodeId: '%s'}) RETURN user.nodeId AS userId", postId);

            Result result = session.run(query, Map.of("postId", postId));
            return result.list().stream()
                    .map(record -> record.get("userId").asString())
                    .toList();
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des utilisateurs qui ont aimé le post: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Récupère la liste des posts aimés par un utilisateur.
     *
     * @param userId le username de l'utilisateur
     * @return la liste des IDs des posts aimés par l'utilisateur
     */
    public List<String> getUserLikedPosts(String userId) {
        try (Session session = neo4jDriver.session()) {
            logger.info("[GET USER LIKED POSTS][REPOSITORY]: Récupération des posts aimés par l'utilisateur: " + userId);
            String query = String.format("MATCH (user {nodeType: 'User', nodeId: '%s'})-[r:LIKES]->(post {nodeType: 'Post'}) RETURN post.nodeId AS postId", userId);

            Result result = session.run(query, Map.of("userId", userId));
            return result.list().stream()
                    .map(record -> record.get("postId").asString())
                    .toList();
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des posts aimés par l'utilisateur: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Récupère la liste des abonnés d'un utilisateur.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return la liste des noms d'utilisateur des abonnés
     */
    public List<String> getUserFollowers(String userId) {
        try (Session session = neo4jDriver.session()) {
            logger.info("[GET USER FOLLOWERS][REPOSITORY]: Récupération des abonnés de l'utilisateur: " + userId);
            String query = String.format("MATCH (user {nodeType: 'User', nodeId: '%s'})<-[:FOLLOWS]-(follower {nodeType: 'User'}) RETURN follower.nodeId AS followerId", userId);

            Result result = session.run(query, Map.of("userId", userId));
            return result.list().stream()
                    .map(record -> record.get("followerId").asString())
                    .toList();
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des abonnés de l'utilisateur: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Récupère la liste des utilisateurs suivis par un utilisateur.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return la liste des noms d'utilisateur suivis
     */
    public List<String> getUserFollows(String userId) {
        try (Session session = neo4jDriver.session()) {
            logger.info("[GET USER FOLLOWS][REPOSITORY]: Récupération des utilisateurs suivis par: " + userId);
            String query = String.format("MATCH (user {nodeType: 'User', nodeId: '%s'})-[:FOLLOWS]->(followed {nodeType: 'User'}) RETURN followed.nodeId AS followedId", userId);

            Result result = session.run(query, Map.of("userId", userId));
            return result.list().stream()
                    .map(record -> record.get("followedId").asString())
                    .toList();
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des utilisateurs suivis par l'utilisateur: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Récupère la liste des utilisateurs bloqués par un utilisateur.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return la liste des noms d'utilisateur bloqués
     */
    public List<String> getUserBlockedUsers(String userId) {
        try (Session session = neo4jDriver.session()) {
            logger.info("[GET USER BLOCKED USERS][REPOSITORY]: Récupération des utilisateurs bloqués par: " + userId);
            String query = String.format("MATCH (user {nodeType: 'User', nodeId: '%s'})-[:BLOCKS]->(blocked {nodeType: 'User'}) RETURN blocked.nodeId AS blockedId", userId);

            Result result = session.run(query, Map.of("userId", userId));
            return result.list().stream()
                    .map(record -> record.get("blockedId").asString())
                    .toList();
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des utilisateurs bloqués par l'utilisateur: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Récupère la liste des utilisateurs qui bloquent un utilisateur.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return la liste des noms d'utilisateur qui bloquent
     */
    public List<String> getUserBlockingUsers(String userId) {
        try (Session session = neo4jDriver.session()) {
            logger.info("[GET USER BLOCKING USERS][REPOSITORY]: Récupération des utilisateurs qui bloquent: " + userId);
            String query = String.format("MATCH (user {nodeType: 'User', nodeId: '%s'})<-[:BLOCKS]-(blocker {nodeType: 'User'}) RETURN blocker.nodeId AS blockerId", userId);

            Result result = session.run(query, Map.of("userId", userId));
            return result.list().stream()
                    .map(record -> record.get("blockerId").asString())
                    .toList();
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des utilisateurs qui bloquent l'utilisateur: " + e.getMessage());
            return List.of();
        }
    }
}