package com.cliphub.security;

import com.cliphub.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String role;
    private final Long teamId;
    private final String displayName;
    private final Integer status;

    public UserPrincipal(Long id, String username, String password, String role, Long teamId,
                         String displayName, Integer status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.teamId = teamId;
        this.displayName = displayName;
        this.status = status;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getTeamId(),
                user.getDisplayName(),
                user.getStatus()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
