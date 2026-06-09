package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("collection_shares")
public class CollectionShare {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long collectionId;
    private Long sharedBy;
    private String shareCode;
    private String password;
    private LocalDateTime expireAt;
    private Long accessCount;
    private LocalDateTime lastAccessAt;
    private String status;
    private LocalDateTime createdAt;
}
