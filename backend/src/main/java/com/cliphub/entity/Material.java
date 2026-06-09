package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("materials")
public class Material {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String type;
    private Long categoryId;
    private Long ownerId;
    private String visibility;
    private String fileName;
    private String storagePath;
    private String previewPath;
    private String mimeType;
    private String format;
    private Long sizeBytes;
    private Integer durationSeconds;
    private String resolution;
    private Long downloadCount;
    private Long favoriteCount;
    private Long shareCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
