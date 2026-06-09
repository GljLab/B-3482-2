package com.cliphub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SettingRequest {

    @NotBlank(message = "配置键不能为空")
    private String settingKey;

    @NotBlank(message = "配置值不能为空")
    private String settingValue;

    private String description;
}
