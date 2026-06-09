package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("share_links")
public class ShareLink {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private Long sharedBy;
    private String shareCode;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
}
