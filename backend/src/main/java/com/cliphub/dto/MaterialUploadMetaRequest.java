package com.cliphub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class MaterialUploadMetaRequest {

    @NotBlank(message = "素材标题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "素材类型不能为空")
    private String type;

    private Long categoryId;

    @NotBlank(message = "权限类型不能为空")
    private String visibility;

    private Integer durationSeconds;
    private String resolution;
    private List<Long> tagIds;
}
