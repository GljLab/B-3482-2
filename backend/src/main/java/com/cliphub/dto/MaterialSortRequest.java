package com.cliphub.dto;

import lombok.Data;

import java.util.List;

@Data
public class MaterialSortRequest {

    private List<MaterialSortItem> items;

    @Data
    public static class MaterialSortItem {
        private Long materialId;
        private Integer sortOrder;
    }
}
