package com.cliphub.dto;

import lombok.Data;

import java.util.List;

@Data
public class CleanupUnusedRequest {
    private List<Long> materialIds;
    private Boolean dryRun;
}
