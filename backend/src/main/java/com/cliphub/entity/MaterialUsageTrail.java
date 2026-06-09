package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("material_usage_trails")
public class MaterialUsageTrail {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private Long userId;
    private String username;
    private String action;
    private String actionType;
    private String targetType;
    private String targetId;
    private String targetName;
    private String detail;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;
}
