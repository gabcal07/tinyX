package com.epita.dto.contracts;

import com.epita.dto.PostType;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Data
@RegisterForReflection
public class CreatePostContract {

    @Schema(description = "Post metadata in JSON format")
    @FormParam("metadata")
    @PartType(MediaType.APPLICATION_JSON)
    @Valid
    @NotNull(message = "Metadata is required")
    private JsonData data;

    @Schema(description = "Media file upload")
    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private FileUpload file;

    public boolean IsValid() {
        if (data == null || !data.isValid()) {return false;}
        int count = 0;
        if (file != null) count++;
        if (data.getText() != null) count++;
        if (data.getType() == PostType.REPOST) count++;
        return count >= 1 && count <= 2;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Schema
    @Getter
    @RegisterForReflection
    public static class JsonData {

        @NotNull(message = "username is required")
        @Schema(description = "Author's username", example = "john_doe", maxLength = 24)
        private String authorUsername;

        @NotNull(message = "type is required")
        @Schema(description = "Type of post", example = "ORIGINAL")
        private PostType type;

        @Schema(description = "Post text content", maxLength = 160)
        private String text;

        @Schema(description = "Parent post ID for replies/reposts")
        private UUID parentPostId;

        public boolean isValid() {
            return ((type == PostType.ORIGINAL && parentPostId == null) ||
                    (type != PostType.ORIGINAL && parentPostId != null));
        }
    }
}