package com.restaurantApp.serve.service;

import com.restaurantApp.serve.domain.Restaurant;
import com.restaurantApp.serve.exception.domain.*;
import org.springframework.messaging.MessagingException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface RestaurantService {
    Restaurant register(String phone, String userBranch, String username, String email,String password) throws UserNotFoundException, UsernameExistException, EmailExistException, MessagingException, javax.mail.MessagingException, IOException;

    List<Restaurant> getUsers();

    Restaurant findUserByUsername(String username);

    String findEmailByUsername(String username);

    Restaurant findUserByEmail(String email);
    Restaurant findUserByPhone(String phone);
    Restaurant addNewUser( String phone,String branch, String username, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException;

    Restaurant updateUser(String phone,String currentUsername, String newBranch, String newUsername, String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException;

    void deleteUser(String username) throws IOException;

    void resetPassword(String phone) throws MessagingException, EmailNotFoundException, javax.mail.MessagingException, IOException;
    Boolean verifyToken(String token);

    Restaurant updateProfileImage(String username, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException, NotAnImageFileException;



}
