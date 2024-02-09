package com.restaurantApp.serve.resource;

import com.restaurantApp.serve.domain.HttpResponse;
import com.restaurantApp.serve.domain.Restaurant;
import com.restaurantApp.serve.domain.RestaurantPrincipal;
import com.restaurantApp.serve.exception.ExceptionHandling;
import com.restaurantApp.serve.exception.domain.*;
import com.restaurantApp.serve.service.RestaurantService;
import com.restaurantApp.serve.service.impl.OtpService;
import com.restaurantApp.serve.utility.JWTTokenProvider;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.restaurantApp.serve.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@RestController
@RequestMapping(path = { "/","restaurants/v1/user"})
public class RestaurantResource extends ExceptionHandling {
    private RestaurantService restaurantService;
    private OtpService otpService;
    public static final String EMAIL_SENT = "An email with a new password was sent to: ";
    public static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully";
    private AuthenticationManager authenticationManager;
    private JWTTokenProvider jwtTokenProvider;

    private Logger LOGGER = LoggerFactory.getLogger(getClass());



    @Autowired
    public RestaurantResource(RestaurantService restaurantService, OtpService otpService, AuthenticationManager authenticationManager, JWTTokenProvider jwtTokenProvider) {
        this.restaurantService = restaurantService;
        this.otpService = otpService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;

    }

    @PostMapping("/login")
    public ResponseEntity<Restaurant> login(@RequestBody Restaurant restaurant) {
        authenticate(restaurant.getUsername(), restaurant.getPassword());
        Restaurant loginUser = restaurantService.findUserByUsername(restaurant.getUsername());
        RestaurantPrincipal restaurantPrincipal = new RestaurantPrincipal(loginUser);
        HttpHeaders jwtHeader = getJwtHeader(restaurantPrincipal);
        return new ResponseEntity<>(loginUser, jwtHeader, OK);
    }

    @PostMapping("/signUp")
    public ResponseEntity<Restaurant> register(@RequestBody Restaurant restaurant) throws UserNotFoundException, UsernameExistException, EmailExistException, MessagingException, javax.mail.MessagingException, IOException {
        Restaurant newUser = restaurantService.register(restaurant.getPhone(), restaurant.getUserBranch(), restaurant.getUsername(), restaurant.getEmail(),restaurant.getPassword());
        if (newUser.isMfaEnabled()) {
            return ResponseEntity.ok(newUser);
        }
        return new ResponseEntity<>(newUser, OK);
    }

    @PostMapping("/add")
    public ResponseEntity<Restaurant> addNewUser(@RequestParam("userBranch") String branch,
                                           @RequestParam("username") String username,
                                           @RequestParam("email") String email,
                                           @RequestParam("role") String role,
                                           @RequestParam("isActive") String isActive,
                                           @RequestParam("isNonLocked") String isNonLocked,
                                           @RequestParam("phone") String phone,
                                           @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        Restaurant newUser = restaurantService.addNewUser(phone, branch, username,email, role, Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive), profileImage);
        return new ResponseEntity<>(newUser, OK);
    }

    @PostMapping("/update")
    public ResponseEntity<Restaurant> update(@RequestParam("currentUsername") String currentUsername,
                                       @RequestParam("userBranch") String branch,
                                       @RequestParam("username") String username,
                                       @RequestParam("email") String email,
                                       @RequestParam("role") String role,
                                       @RequestParam("isActive") String isActive,
                                       @RequestParam("isNonLocked") String isNonLocked,
                                       @RequestParam("phone") String phone,
                                       @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        Restaurant updatedUser = restaurantService.updateUser(phone,currentUsername,  branch, username,email, role, Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive), profileImage);
        return new ResponseEntity<>(updatedUser, OK);
    }

    @GetMapping("/find/{username}")
    public ResponseEntity<Restaurant> getUser(@PathVariable("username") String username) {
        Restaurant restaurant = restaurantService.findUserByUsername(username);
        return new ResponseEntity<>(restaurant, OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<Restaurant>> getAllUsers() {
        List<Restaurant> users = restaurantService.getUsers();
        return new ResponseEntity<>(users, OK);
    }

    @GetMapping("/resetpassword/{phone}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("phone") String phone) throws MessagingException, EmailNotFoundException, javax.mail.MessagingException, IOException {
        restaurantService.resetPassword(phone);
        return response(OK, EMAIL_SENT + phone
        );
    }

    @DeleteMapping("/delete/{username}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("username") String username) throws IOException {
        restaurantService.deleteUser(username);
        return response(OK, USER_DELETED_SUCCESSFULLY);
    }

    @PostMapping("/updateProfileImage")
    public ResponseEntity<Restaurant> updateProfileImage(@RequestParam("username") String username, @RequestParam(value = "profileImage") MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException, NotAnImageFileException {
        Restaurant user = restaurantService.updateProfileImage(username, profileImage);
        return new ResponseEntity<>(user, OK);
    }

    @GetMapping(path = "/image/{username}/{fileName}", produces = IMAGE_JPEG_VALUE)
    public byte[] getProfileImage(@PathVariable("username") String username, @PathVariable("fileName") String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(System.getProperty("user.home") + "/PODVA/transportApp/v1/user/" + username + "/" + fileName));
    }

    @GetMapping(path = "/image/profile/{username}", produces = IMAGE_JPEG_VALUE)
    public byte[] getTempProfileImage(@PathVariable("username") String username) throws IOException {
        URL url = new URL("https://robohash.org/" + username);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = url.openStream()) {
            int bytesRead;
            byte[] chunk = new byte[1024];
            while((bytesRead = inputStream.read(chunk)) > 0) {
                byteArrayOutputStream.write(chunk, 0, bytesRead);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    @GetMapping
    public ResponseEntity<HttpResponse> confirmUserAccount(@RequestParam("token") String token) {
        Boolean isSuccess = restaurantService.verifyToken(token);
        ResponseEntity<HttpResponse> body = ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp1(LocalDateTime.now().toString())
                        .data(Map.of("Success", isSuccess))
                        .message("Account Verified")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
        return body;
    }

    @PostMapping(value = "verify")
    public ResponseEntity<HttpResponse> verifyOtp(@Valid @RequestBody Restaurant verifyTokenRequest)
    {
        String username = verifyTokenRequest.getUsername();
        Integer otp = verifyTokenRequest.getOtp();
        boolean isOtpValid = otpService.validateOTP(username, otp);
        if (!isOtpValid) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }



    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
        return new ResponseEntity<>(new HttpResponse(httpStatus.value(), httpStatus, httpStatus.getReasonPhrase().toUpperCase(),
                message), httpStatus);
    }

    private HttpHeaders getJwtHeader(RestaurantPrincipal restaurantPrincipal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(restaurantPrincipal));
        return headers;
    }

    private void authenticate(String username, String password) {
        LOGGER.info("Calling OTP service");
        otpService.generateOtp(username);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
}
