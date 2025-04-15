package com.epita.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserActionEvent {
    @Getter
    public enum ActionType {
        USER_DELETED("user-deleted"), // srvc-user
        USER_CREATED("user-created"), // srvc-user
        USER_BLOCKED("user-blocked"), // repo-social
        USER_UNBLOCKED("user-unblocked"), // repo-social
        USER_FOLLOWED("user-followed"), // repo-social
        USER_UNFOLLOWED("user-unfollowed"), // repo-social
        POST_CREATED("post-created"), // repo-social
        POST_DELETED("post-deleted"), // repo-social
        POST_LIKED("post-liked"), // repo-social
        POST_UNLIKED("post-unliked"); // repo-social

        private final String value;

        ActionType(String value) {
            this.value = value;
        }
    }

    private ActionType actionType;
    private String userId;
    private String username; // Pour USER_CREATED, USER_DELETED, USER_BLOCKED, USER_FOLLOWED, USER_UNFOLLOWED
    private String targetId; // Pour USER_FOLLOWED, USER_UNFOLLOWED
    private String targetUsername; // Pour USER_FOLLOWED, USER_UNFOLLOWED
    private String postId; // Pour POST_DELETED, POST_CREATED, POST_LIKED
    private String postContent; // Pour POST_CREATED
    private Date timestamp;

    @Override
    public String toString() {
        return "UserActionEvent{" +
                "actionType=" + actionType +
                ", userId='" + userId + '\'' +
                ", targetId='" + targetId + '\'' +
                ", postId='" + postId + '\'' +
                ", postContent='" + postContent + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}