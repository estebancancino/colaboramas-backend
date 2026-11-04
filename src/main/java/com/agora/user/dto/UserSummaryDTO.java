package com.agora.user.dto;

import com.agora.util.ImageUrlSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Data;

@Data
public class UserSummaryDTO {
    private Long id;
    private String username;
    private String email;
    @JsonSerialize(using = ImageUrlSerializer.class)
    private String imageUrl;

    public UserSummaryDTO(Long id, String username, String email, String imageUrl) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.imageUrl = imageUrl;
    }
}
