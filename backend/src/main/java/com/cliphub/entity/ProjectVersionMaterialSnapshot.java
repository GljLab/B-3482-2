package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_version_material_snapshots")
public class ProjectVersionMaterialSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long versionId;
    private Long projectId;
    private Long materialId;
    private String materialTitle;
    private Long materialOwnerId;
    private String materialVisibility;
    private String materialSnapshot;
    private LocalDateTime createdAt;
}
