package com.restaurantApp.serve.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class RestaurantPrincipal implements UserDetails {
    private Restaurant restaurant;

    public RestaurantPrincipal(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return stream(this.restaurant.getAuthorities()).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return this.restaurant.getPassword();
    }

    @Override
    public String getUsername() {
        return this.restaurant.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.restaurant.isNotLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.restaurant.isActive();
    }
}
