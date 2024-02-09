package com.restaurantApp.serve.service.impl;

import com.restaurantApp.serve.domain.Confirmation;
import com.restaurantApp.serve.domain.Restaurant;
import com.restaurantApp.serve.domain.RestaurantPrincipal;
import com.restaurantApp.serve.enumuration.Role;
import com.restaurantApp.serve.exception.domain.*;
import com.restaurantApp.serve.repository.ConfirmationRepository;
import com.restaurantApp.serve.repository.RestaurantRepository;
import com.restaurantApp.serve.service.EmailService;
import com.restaurantApp.serve.service.LoginAttemptService;
import com.restaurantApp.serve.service.PhoneService;
import com.restaurantApp.serve.service.RestaurantService;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessagingException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.restaurantApp.serve.constant.FileConstant.*;
import static com.restaurantApp.serve.enumuration.Role.ROLE_USER;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Service
@Transactional
@Qualifier("userDetailsService")

public class RestaurantServiceImpl implements RestaurantService, UserDetailsService {
    public static final String EMAIL_ALREADY_EXISTS = "email already exists";
    public static final String USER_NAME_ALREADY_EXIST = "user name already exist";
    public static final String NO_USER_FOUND_BY_USER_NAME = "no user found by userName";
    public static final String FOUND_USER_BY_USERNAME = "found user by username";
    public static final String NO_USER_FOUND_BY_EMAIL = "no user found by email";

    private Logger LOGGER = LoggerFactory.getLogger(getClass());
    private RestaurantRepository repository;
    private BCryptPasswordEncoder passwordEncoder;
    private LoginAttemptService loginAttemptService;
    private EmailService emailService;
    private PhoneService phoneService;

    private final ConfirmationRepository confirmationRepository;
    private final TwoFactorAuthenticationService tfaService;

    @Autowired
    public RestaurantServiceImpl(RestaurantRepository repository, BCryptPasswordEncoder passwordEncoder, LoginAttemptService loginAttemptService, EmailService emailService, PhoneService phoneService, ConfirmationRepository confirmationRepository, TwoFactorAuthenticationService tfaService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
        this.phoneService = phoneService;
        this.confirmationRepository = confirmationRepository;
        this.tfaService = tfaService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        Restaurant restaurant = repository.findUserByUsername(username);
        if (restaurant == null){
            LOGGER.error(NO_USER_FOUND_BY_USER_NAME + username);
            throw new UsernameNotFoundException(NO_USER_FOUND_BY_USER_NAME + username);
        }else {
            validateLoginAttempt(restaurant);
            restaurant.setLastLoginDateDisplay(restaurant.getLastLoginDate());
            restaurant.setLastLoginDate(new Date());
            repository.save(restaurant);
            RestaurantPrincipal restaurantPrincipal = new RestaurantPrincipal(restaurant);
            LOGGER.info(FOUND_USER_BY_USERNAME + username);
            return restaurantPrincipal;
        }
    }

    public Restaurant register( String phone,String branch, String username, String email, String password) throws UserNotFoundException, UsernameExistException, EmailExistException, MessagingException, javax.mail.MessagingException, IOException {
        validateNewUsernameAndEmail(EMPTY, username, email);
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(generateUserId());
        //restaurant.setPassword(password);
        restaurant.setPhone(phone);
        restaurant.setUserBranch(branch);
        restaurant.setUsername(username);
        restaurant.setEmail(email);
        restaurant.setJoinDate(new Date());
        LOGGER.info("New user password: " + password);
        restaurant.setPassword(encodePassword(password));
        restaurant.setActive(true);
        restaurant.setNotLocked(true);
        restaurant.setRole(ROLE_USER.name());
        restaurant.setAuthorities(ROLE_USER.getAuthorities());
        restaurant.setProfileImageUrl(getTemporaryProfileImageUrl(username));
        /*restaurant.setMfaEnabled(true);
        if (restaurant.isMfaEnabled()) {
            restaurant.setSecret(tfaService.generateNewSecret());
            tfaService.generateQrCodeImageUri(restaurant.getSecret());
            restaurant.setMfaEnabled(true);
        }else {
            LOGGER.info("MFA is not enabled for this user.");
            restaurant.setMfaEnabled(false);
        }*/
        repository.save(restaurant);
        //phoneService.sendMessage(username, password, phone);
        Confirmation confirmation = new Confirmation(restaurant);
        confirmationRepository.save(confirmation);
        emailService.sendHtmlEmailWithEmbeddedFiles(restaurant.getUsername(), restaurant.getEmail(), confirmation.getToken());
        return restaurant;
    }

    @Override
    public Restaurant addNewUser(String phone, String branch, String username, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage)
            throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        validateNewUsernameAndEmail(EMPTY, username, email);
        Restaurant restaurant = new Restaurant();
        String password = generatePassword();
        restaurant.setRestaurantId(generateUserId());
        restaurant.setPhone(phone);
        restaurant.setUserBranch(branch);
        restaurant.setJoinDate(new Date());
        restaurant.setUsername(username);
        restaurant.setEmail(email);
        restaurant.setPassword(encodePassword(password));
        restaurant.setActive(isActive);
        restaurant.setNotLocked(isNonLocked);
        restaurant.setRole(getRoleEnumName(role).name());
        restaurant.setAuthorities(getRoleEnumName(role).getAuthorities());
        restaurant.setProfileImageUrl(getTemporaryProfileImageUrl(username));
        repository.save(restaurant);
        saveProfileImage(restaurant, profileImage);
        LOGGER.info("Your userName is "+ username);
        LOGGER.info("New user password: " + password);
        phoneService.sendMessage(username,password,phone);
        return restaurant;
    }



    @Override
    public List<Restaurant> getUsers() {
        return repository.findAll();
    }

    @Override
    public Restaurant findUserByUsername(String username) {
        return repository.findUserByUsername(username);
    }

    public String findEmailByUsername(String username) {
        Restaurant user = repository.findEmailByUsername(username);
        return user != null ? user.getEmail() : null;
    }


    @Override
    public Restaurant findUserByEmail(String email) {
        return repository.findUserByEmail(email);
    }

    @Override
    public Restaurant findUserByPhone(String phone) {
        return repository.findUserByPhone(phone);
    }


    @Override
    public Restaurant updateUser(String phone,String currentUsername, String newUserBranch, String newUsername, String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        Restaurant currentUser = validateNewUsernameAndEmail(currentUsername, newUsername, newEmail);
        currentUser.setUserBranch(newUserBranch);
        currentUser.setUsername(newUsername);
        currentUser.setEmail(newEmail);
        currentUser.setActive(isActive);
        currentUser.setNotLocked(isNonLocked);
        currentUser.setRole(getRoleEnumName(role).name());
        currentUser.setAuthorities(getRoleEnumName(role).getAuthorities());
        repository.save(currentUser);
        saveProfileImage(currentUser, profileImage);

        return currentUser;
    }

    private String getTemporaryProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH + username).toUriString();
    }

    @Override
    public void deleteUser(String username) throws IOException {
        Restaurant restaurant = repository.findUserByUsername(username);
        Path userFolder = Paths.get(USER_FOLDER+ restaurant.getUsername()).toAbsolutePath().normalize();
        FileUtils.deleteDirectory(new File(userFolder.toString()));
        repository.deleteById(restaurant.getId());
    }



    private void saveProfileImage(Restaurant restaurant, MultipartFile profileImage) throws IOException, NotAnImageFileException {
        if (profileImage != null) {
            if(!Arrays.asList("image/jpeg", "image/png", "image/gif").contains(profileImage.getContentType())) {
                throw new NotAnImageFileException(profileImage.getOriginalFilename() + "is not an image file. Please upload an image file");
            }
            Path userFolder = Paths.get(USER_FOLDER + restaurant.getUsername()).toAbsolutePath().normalize();
            if(!Files.exists(userFolder)) {
                Files.createDirectories(userFolder);
                LOGGER.info("Created directory for: " + userFolder);
            }
            Files.deleteIfExists(Paths.get(userFolder + restaurant.getUsername() + "." + "jpg"));
            Files.copy(profileImage.getInputStream(), userFolder.resolve(restaurant.getUsername() + "." + "jpg"), REPLACE_EXISTING);
            restaurant.setProfileImageUrl(setProfileImageUrl(restaurant.getUsername()));
            repository.save(restaurant);
            LOGGER.info("Saved file in file system by name: " + profileImage.getOriginalFilename());
        }
    }

    private String setProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(USER_IMAGE_PATH +" " + username + "/"
                + username + "." + "jpg").toUriString();
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }



    @Override
    public void resetPassword(String phone) throws MessagingException, EmailNotFoundException, javax.mail.MessagingException, IOException {
        Restaurant restaurant = repository.findUserByPhone(phone);
        if (restaurant == null) {
            throw new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL + phone);
        }
        String password = generatePassword();
        restaurant.setPassword(encodePassword(password));
        repository.save(restaurant);
        LOGGER.info("New user password: " + password);
        //emailService.sendNewPasswordEmail(user.getUserBranch(), password, user.getEmail());
        phoneService.sendMessage(restaurant.getUsername(),password,phone);
    }

    @Override
    public Boolean verifyToken(String token) {
        Confirmation confirmation = confirmationRepository.findByToken(token);
        Restaurant user = repository.findUserByEmail(confirmation.getUser().getEmail());
        user.setEnabled(true);
        repository.save(user);
        //confirmationRepository.delete(confirmation);
        return Boolean.TRUE;
    }


    @Override
    public Restaurant updateProfileImage(String username, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        Restaurant restaurant = validateNewUsernameAndEmail(username, null, null);
        saveProfileImage(restaurant, profileImage);
        return restaurant;
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }



    private void validateLoginAttempt(Restaurant restaurant) {
        if(restaurant.isNotLocked()) {
            if(loginAttemptService.hasExceededMaxAttempts(restaurant.getUsername())) {
                restaurant.setNotLocked(false);
            } else {
                restaurant.setNotLocked(true);
            }
        } else {
            loginAttemptService.evictUserFromLoginAttemptCache(restaurant.getUsername());
        }
    }

    private Restaurant validateNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail) throws UserNotFoundException, UsernameExistException, EmailExistException {
        Restaurant userByNewUsername = findUserByUsername(newUsername);
        Restaurant userByNewEmail = findUserByEmail(newEmail);
        if(StringUtils.isNotBlank(currentUsername)) {
            Restaurant currentUser = findUserByUsername(currentUsername);
            if(currentUser == null) {
                throw new UserNotFoundException(NO_USER_FOUND_BY_USER_NAME + currentUsername);
            }
            if(userByNewUsername != null && !currentUser.getId().equals(userByNewUsername.getId())) {
                throw new UsernameExistException(USER_NAME_ALREADY_EXIST);
            }
            if(userByNewEmail != null && !currentUser.getId().equals(userByNewEmail.getId())) {
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return currentUser;
        } else {
            if(userByNewUsername != null) {
                throw new UsernameExistException(USER_NAME_ALREADY_EXIST);
            }
            if(userByNewEmail != null) {
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return null;
        }
    }



}
