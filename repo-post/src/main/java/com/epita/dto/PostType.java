package com.epita.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PostType {
    @JsonProperty("ORIGINAL") ORIGINAL,
    @JsonProperty("REPLY") REPLY,
    @JsonProperty("REPOST") REPOST
}