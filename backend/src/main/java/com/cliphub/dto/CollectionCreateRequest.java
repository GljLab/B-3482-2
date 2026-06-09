package com.cliphub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CollectionCreateRequest {

    @NotBlank(message = "素材集名称不能为空")
    private String name;
    private String description;
    private Long coverMaterialId;
    private String visibility = "PRIVATE";
}
