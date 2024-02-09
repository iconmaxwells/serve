package com.restaurantApp.serve.repository;

import com.restaurantApp.serve.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Restaurant findUserByUsername(String username);

    Restaurant findUserByEmail(String email);

    Restaurant findUserByPhone(String phone);

    Restaurant findEmailByUsername(String username);

}
