package com.cliphub.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cliphub.entity.User;
import com.cliphub.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return UserPrincipal.from(user);
    }
}
