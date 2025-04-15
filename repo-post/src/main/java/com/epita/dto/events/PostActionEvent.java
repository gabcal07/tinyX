package com.epita.dto.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event representing post actions for communication between services.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostActionEvent {

    /**
     * Types of post actions that can be performed.
     */
    public enum ActionType {
        POST_CREATED,
        POST_DELETED,
    }

    private ActionType actionType;
    private String postId;
    private String authorId;
    private String authorUsername;
    private String parentPostId;
    private String postType; // ORIGINAL, REPOST, REPLY
}