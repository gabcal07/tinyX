package com.epita.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TimelinePost {
    public String postId;
    public String authorId; // ID de l'auteur ou du liker
    public String type;    // "POSTED" ou "LIKED"
    public Date date;
}
