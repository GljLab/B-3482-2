package com.cliphub.dto;

import lombok.Data;

@Data
public class CollectionShareRequest {

    private Integer expireDays = 7;
    private String password;
}
