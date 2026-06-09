package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_versions")
public class ProjectVersion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Integer versionNo;
    private String versionName;
    private String contentJson;
    private Long createdBy;
    private Integer isCurrent;
    private LocalDateTime createdAt;
}
