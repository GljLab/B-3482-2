package com.cliphub.dto;

import lombok.Data;

@Data
public class CopyProjectRequest {
    private String newName;
    private boolean copyMaterials;
}
