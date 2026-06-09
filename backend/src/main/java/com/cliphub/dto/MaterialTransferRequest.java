package com.cliphub.dto;

import lombok.Data;

import java.util.List;

@Data
public class MaterialTransferRequest {
    private Long toUserId;
    private String transferNote;
    private List<Long> materialIds;
}
