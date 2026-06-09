package com.cliphub.dto;

import lombok.Data;

@Data
public class CollectionUpdateRequest {

    private String name;
    private String description;
    private Long coverMaterialId;
    private String visibility;
    private Integer sortOrder;
}
