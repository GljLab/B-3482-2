package com.cliphub.dto;

import lombok.Data;

import java.util.List;

@Data
public class CollectionMaterialRequest {

    private List<Long> collectionIds;
}
