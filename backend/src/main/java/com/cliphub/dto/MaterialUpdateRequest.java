package com.cliphub.dto;

import lombok.Data;

import java.util.List;

@Data
public class MaterialUpdateRequest {

    private String title;
    private String description;
    private String visibility;
    private Long categoryId;
    private Integer durationSeconds;
    private String resolution;
    private List<Long> tagIds;
}
