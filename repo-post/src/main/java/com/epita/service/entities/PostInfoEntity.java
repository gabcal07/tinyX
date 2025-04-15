package com.epita.service.entities;

import com.epita.dto.PostType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostInfoEntity {
    private String postId;
    private String authorId;
    private String authorUsername;
    private String text;
    private String parentPostId;
    private PostType type;
    private String mediaUrl;
    private Date createdAt;

    @Override
    public String toString() {
        return "PostInfoEntity{" +
                "postId='" + postId + '\'' +
                ", authorId='" + authorId + '\'' +
                ", text='" + text + '\'' +
                ", parentPostId='" + parentPostId + '\'' +
                ", type=" + type +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
