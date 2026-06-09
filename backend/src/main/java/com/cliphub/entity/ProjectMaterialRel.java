package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_material_rel")
public class ProjectMaterialRel {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long materialId;
    private Long addedBy;
    private LocalDateTime addedAt;
    private String sourceType;
    private String sourceNote;
    private Long materialOwnerId;
    private String materialStatus;
    private LocalDateTime createdAt;
}
