package com.epita.repository.models;

import io.smallrye.common.constraint.NotNull;
import jakarta.ws.rs.InternalServerErrorException;

public record Node(NodeType nodeType, String nodeId) {
    public enum NodeType {
        USER("User"),
        POST("Post");

        private final String typeStr;

        NodeType(final String typeStr) {
            this.typeStr = typeStr;
        }

        @Override
        public String toString() {
            return typeStr;
        }

        public static NodeType fromString(final @NotNull String str) {
            for (final var value : values()) {
                if (value.toString().equals(str))
                    return value;
            }
            return null;
        }
    }

    public Node(final @NotNull NodeType nodeType, final @NotNull String nodeId) {
        this.nodeType = nodeType;
        this.nodeId = nodeId; // username or postId
    }

    public static Node from(final @NotNull org.neo4j.driver.types.Node neo4jNode) {
        final var nodeType = neo4jNode.get("nodeType").asString();
        final var nodeId = neo4jNode.get("nodeId").asString();

        if (nodeType == null || nodeType.isEmpty() || nodeId == null || nodeId.isEmpty()) {
            throw new InternalServerErrorException("Neo4J error: Incomplete node");
        }

        return new Node(NodeType.fromString(nodeType), nodeId);
    }

    @Override
    public String toString() {
        return String.format("Node{nodeType=%s, nodeId='%s'}", nodeType, nodeId);
    }

    public String createCypher() {
        return String.format(
                "MERGE (n:%s {nodeId: '%s', nodeType: '%s'})",
                nodeType.toString(),
                nodeId,
                nodeType.toString()
        );
    }

    public String findCypher() {
        return String.format(
                "MATCH (n:%s {nodeId: '%s'}) RETURN n",
                nodeType.toString(),
                nodeId
        );
    }

    // This method is used to delete a node and all its relationships
    public String forceDeleteCypher() {
        return String.format(
                "MATCH (n:%s {nodeId: '%s'}) DETACH DELETE n",
                nodeType.toString(),
                nodeId
        );
    }

    // This method is used to delete a node with the idea that the relationships will be suppressed asynchronously later
    public String deleteCypher() {
        return String.format(
                "MATCH (n:%s {nodeId: '%s'}) DELETE n",
                nodeType.toString(),
                nodeId
        );
    }

    // Lists all relationships suppressions implied in a potential suppression
    public String debugDeleteCypher() {
        return String.format(
                "MATCH (n:%s {nodeId: '%s'}) " +
                        "OPTIONAL MATCH (n)-[r]-() " +
                        "WITH n, COLLECT(r) AS relations " +
                        "FOREACH (rel IN relations | DELETE rel) " +
                        "DELETE n",
                nodeType.toString(),
                nodeId
        );
    }
}