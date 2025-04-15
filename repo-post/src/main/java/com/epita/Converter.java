package com.epita;

import com.epita.dto.contracts.CreatePostContract;
import com.epita.dto.responses.CreatePostReponse;
import com.epita.repository.models.PostModel;
import com.epita.service.entities.PostEntity;
import com.epita.service.entities.PostInfoEntity;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@ApplicationScoped
public class Converter {
    public PostEntity PostCreateContractToPostEntity(CreatePostContract contract) {
        PostEntity postEntity = new PostEntity();
        postEntity.setAuthorUsername(contract.getData().getAuthorUsername());
        postEntity.setText(contract.getData().getText());
        postEntity.setType(contract.getData().getType());
        postEntity.setParentPostId(contract.getData().getParentPostId());
        postEntity.setMedia(contract.getFile());
        return postEntity;
    }

    public PostInfoEntity PostModeltoPostInfoEntity(PostModel post) {
        PostInfoEntity postInfoEntity = new PostInfoEntity();
        postInfoEntity.setAuthorUsername(post.getAuthorUsername());
        postInfoEntity.setPostId(post.getPostId());
        postInfoEntity.setText(post.getText());
        postInfoEntity.setType(post.getPostType());
        postInfoEntity.setParentPostId(post.getParentPostId());
        postInfoEntity.setMediaUrl(post.getMediaUrl());
        postInfoEntity.setCreatedAt(post.getCreatedAt());
        return postInfoEntity;
    }

    public PostModel PostEntityModel(PostEntity entity) {
        PostModel model = new PostModel();
        model.setAuthorUsername(entity.getAuthorUsername());
        model.setPostId(UUID.randomUUID().toString());
        model.setText(entity.getText());
        model.setPostType(entity.getType());
        model.setParentPostId(entity.getParentPostId() != null ?
                entity.getParentPostId().toString() : null);
        model.setCreatedAt(new Date());
        return model;
    }

    public CreatePostReponse PostEntityInfoToCreatePostReponse(PostInfoEntity entity) {
        CreatePostReponse response = new CreatePostReponse();
        response.setAuthorUsername(entity.getAuthorUsername());
        response.setPostId(entity.getPostId());
        response.setText(entity.getText());
        response.setParentPostId(entity.getParentPostId());
        response.setPostType(entity.getType());
        response.setMediaUrl(entity.getMediaUrl());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
