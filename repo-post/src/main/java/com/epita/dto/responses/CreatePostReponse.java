package com.epita.dto.responses;

import com.epita.dto.PostType;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Date;

@Data
@RegisterForReflection
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Post response")
public class CreatePostReponse {

    @Schema(description = "Unique post ID")
    private String postId;

    @Schema(description = "Author ID")
    private String authorId;

    @Schema(description = "Author username")
    private String authorUsername;

    @Schema(description = "Post type")
    private PostType postType;

    @Schema(description = "Post content text")
    private String text;

    @Schema(description = "Parent post ID")
    private String parentPostId;

    @Schema(description = "Media URL")
    private String mediaUrl;

    @Schema(description = "Creation timestamp")
    private Date createdAt;
}
