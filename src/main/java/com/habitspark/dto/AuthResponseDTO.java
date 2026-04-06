package com.habitspark.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String username;
    private String role;
    private String displayName;
}
