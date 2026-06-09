package com.cliphub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotBlank(message = "等级不能为空")
    private String level;

    @NotBlank(message = "状态不能为空")
    private String status;

    private LocalDateTime publishAt;
}
