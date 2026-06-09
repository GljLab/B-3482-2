package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("collections")
public class Collection {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Long coverMaterialId;
    private Long ownerId;
    private String visibility;
    private Integer sortOrder;
    private Integer isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
