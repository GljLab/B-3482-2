package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("projects")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String status;
    private Long teamId;
    private String exportFormat;
    private Long currentVersionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
