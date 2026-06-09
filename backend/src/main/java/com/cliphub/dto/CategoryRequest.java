package com.cliphub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private Long parentId;
    private String description;
}
