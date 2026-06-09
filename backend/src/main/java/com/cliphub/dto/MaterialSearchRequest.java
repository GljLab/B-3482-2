package com.cliphub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialSearchRequest {
    private String keyword;
    private Long categoryId;
    private Long tagId;
    private String type;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long collectionId;
    private Boolean inMyCollections;
    private Integer page = 1;
    private Integer size = 10;
}
