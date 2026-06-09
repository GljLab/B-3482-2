package com.cliphub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveVersionRequest {

    @NotBlank(message = "版本名称不能为空")
    private String versionName;

    @NotBlank(message = "版本内容不能为空")
    private String contentJson;
}
