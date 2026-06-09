package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("material_tag_rel")
public class MaterialTagRel {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private Long tagId;
}
