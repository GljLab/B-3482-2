package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("material_transfer_records")
public class MaterialTransferRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private Long fromUserId;
    private Long toUserId;
    private String transferNote;
    private Long operatorId;
    private LocalDateTime createdAt;
}
