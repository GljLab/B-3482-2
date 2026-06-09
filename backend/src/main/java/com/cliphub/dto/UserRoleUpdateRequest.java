package com.cliphub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRoleUpdateRequest {

    @NotBlank(message = "角色不能为空")
    private String role;
}
