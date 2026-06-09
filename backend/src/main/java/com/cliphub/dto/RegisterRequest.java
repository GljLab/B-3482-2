package com.cliphub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 24, message = "用户名长度需要在4-24之间")
    private String username;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 30, message = "密码长度需要在8-30之间")
    private String password;

    @NotBlank(message = "显示名称不能为空")
    private String displayName;
}
