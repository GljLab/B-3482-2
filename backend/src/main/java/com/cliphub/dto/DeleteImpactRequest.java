package com.cliphub.dto;

import lombok.Data;

@Data
public class DeleteImpactRequest {
    private String strategy;
    private boolean notifyUsers;
}
