package com.cliphub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    private String bio;
    private String avatarUrl;
}
