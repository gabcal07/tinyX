package com.epita.repository.models;

import static com.epita.repository.models.Node.NodeType.USER;
import static com.epita.repository.models.Node.NodeType.POST;

import io.smallrye.common.constraint.NotNull;
import jakarta.ws.rs.BadRequestException;

public record Relationship(
        Node source,
        Node target,
        RelationshipType type,
        long timestamp
) {

    public enum RelationshipType {
        FOLLOWS,
        BLOCKS,
        LIKES,
        POSTED
    }

    public Relationship(
            final @NotNull Node source,
            final @NotNull Node target,
            final @NotNull RelationshipType type,
            final @NotNull long timestamp
    ) {
        validateTypes(source, target, type);
        this.source = source;
        this.target = target;
        this.type = type;
        this.timestamp = timestamp;
    }

    private void validateTypes(Node source, Node target, RelationshipType type) {
        switch(type) {
            case FOLLOWS:
            case BLOCKS:
                if(source.nodeType() != USER || target.nodeType() != USER) {
                    throw new BadRequestException(
                            type + " relationship requires User->User nodes"
                    );
                }
                break;
            case LIKES:
                if(source.nodeType() != USER || target.nodeType() != POST) {
                    throw new BadRequestException(
                            "LIKES relationship requires User->Post nodes"
                    );
                }
            case POSTED:
                if(source.nodeType() != USER || target.nodeType() != POST) {
                    throw new BadRequestException(
                            "POSTED relationship requires User->Post nodes"
                    );
                }
                break;
            default:
                throw new BadRequestException("Invalid relationship type");
        }
    }

    private String getPropertyName() {
        return switch(type) {
            case FOLLOWS, BLOCKS -> "since";
            case LIKES -> "likedAt";
            case POSTED -> "postedAt";
        };
    }

    public String createCypher() {
        String prop = getPropertyName();
        return String.format(
                "MATCH (a:%s {nodeId: '%s'}), (b:%s {nodeId: '%s'}) " +
                        "MERGE (a)-[r:%s]->(b) " +
                        "ON CREATE SET r.%s = %d " +
                        "ON MATCH SET r.%s = %d",
                source.nodeType(), source.nodeId(),
                target.nodeType(), target.nodeId(),
                type.name(),
                prop, timestamp,
                prop, timestamp
        );
    }

    public String deleteCypher() {
        return String.format(
                "MATCH (a:%s {nodeId: '%s'})-[r:%s]->(b:%s {nodeId: '%s'}) " +
                        "DELETE r",
                source.nodeType(), source.nodeId(),
                type.name(),
                target.nodeType(), target.nodeId()
        );
    }

    public static String findFollowersCypher(String userId) {
        return String.format(
                "MATCH (u:User {nodeId: '%s'})<-[:FOLLOWS]-(follower) " +
                        "RETURN follower.nodeId AS followerId, follower.nickname, r.since",
                userId
        );
    }
}