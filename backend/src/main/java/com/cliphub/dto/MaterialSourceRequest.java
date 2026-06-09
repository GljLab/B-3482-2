package com.cliphub.dto;

import lombok.Data;

@Data
public class MaterialSourceRequest {
    private String sourceType;
    private String sourceNote;
    private String ownershipType;
    private Long departmentId;
}
