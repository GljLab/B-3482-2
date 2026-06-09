package com.cliphub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("password_reset_tokens")
public class PasswordResetToken {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String token;
    private LocalDateTime expireAt;
    private Integer used;
    private LocalDateTime createdAt;
}
