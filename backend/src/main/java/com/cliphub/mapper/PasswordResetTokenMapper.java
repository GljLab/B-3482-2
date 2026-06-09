package com.cliphub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cliphub.entity.PasswordResetToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PasswordResetTokenMapper extends BaseMapper<PasswordResetToken> {
}
