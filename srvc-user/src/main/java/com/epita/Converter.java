package com.epita;

import com.epita.repository.models.UserModel;
import com.epita.service.entities.UserEntity;

public class Converter {
    public static UserEntity convertUserModelToUserEntity(UserModel userModel) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(userModel.getUserId());
        userEntity.setUsername(userModel.getUsername());
        userEntity.setCreatedAt(userModel.getCreatedAt());
        return userEntity;
    }
}
