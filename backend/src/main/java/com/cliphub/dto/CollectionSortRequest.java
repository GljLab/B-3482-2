package com.cliphub.dto;

import lombok.Data;

import java.util.List;

@Data
public class CollectionSortRequest {

    private List<Long> orderedIds;
}
