package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("collection_material_rel")
public class CollectionMaterialRel {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long collectionId;
    private Long materialId;
    private Integer sortOrder;
    private String note;
    private LocalDateTime createdAt;
}
