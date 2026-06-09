package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("material_modification_histories")
public class MaterialModificationHistory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private Long userId;
    private String username;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;
}
