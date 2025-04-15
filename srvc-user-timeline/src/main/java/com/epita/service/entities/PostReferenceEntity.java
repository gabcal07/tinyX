package com.epita.service.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostReferenceEntity {
        public String postId;
        public String type;
        public Date date; // or `Instant date` if you want proper time handling
}
