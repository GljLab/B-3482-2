package com.cliphub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ProjectCreateRequest {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String description;
    private Long teamId;
    private String exportFormat;
    private List<Long> materialIds;
}
