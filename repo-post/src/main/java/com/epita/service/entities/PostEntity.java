package com.epita.service.entities;

import com.epita.dto.PostType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.Date;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostEntity {
    private UUID authorId;
    private String authorUsername;
    private PostType type;
    private String text;
    private UUID parentPostId;
    private FileUpload media;
    private Date createdAt;

    @Override
    public String toString() {
        return "PostEntity{" +
                "authorId=" + authorId +
                ", type=" + type +
                ", text='" + text + '\'' +
                ", parentPostId=" + parentPostId +
                ", media=" + media +
                ", createdAt=" + createdAt +
                '}';
    }
}
