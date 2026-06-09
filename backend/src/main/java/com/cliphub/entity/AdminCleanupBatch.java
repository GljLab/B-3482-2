package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_cleanup_batches")
public class AdminCleanupBatch {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String operatorName;
    private Integer totalCount;
    private Integer cleanedCount;
    private Long totalBytes;
    private String status;
    private String materialIds;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
