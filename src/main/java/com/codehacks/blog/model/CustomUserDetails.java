package com.codehacks.blog.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;

@Data
@Getter
@Setter
public class CustomUserDetails implements UserDetails {

    private final String username;
    private final String password;
    private final String email;
    private final Role role;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public CustomUserDetails(String username, String password, String email, Role role,
                             Collection<? extends GrantedAuthority> authorities, boolean enabled) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
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
        return this.enabled;
    }
}
