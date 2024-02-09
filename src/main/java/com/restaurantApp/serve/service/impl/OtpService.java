package com.restaurantApp.serve.service.impl;

import com.restaurantApp.serve.domain.Restaurant;
import com.restaurantApp.serve.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Description(value = "Service responsible for handling OTP related functionality.")
@Service
public class OtpService {

    private final Logger LOGGER = LoggerFactory.getLogger(OtpService.class);

    private OtpGenerator otpGenerator;
    private EmailService emailService;
    private RestaurantServiceImpl userService;
    private Restaurant restaurant;

    /**
     * Constructor dependency injector
     *
     * @param otpGenerator - otpGenerator dependency
     * @param emailService - email service dependency
     * @param userService  - user service dependency
     * @param restaurant
     */
    @Autowired
    public OtpService(OtpGenerator otpGenerator, EmailService emailService, RestaurantServiceImpl userService, Restaurant restaurant)
    {
        this.otpGenerator = otpGenerator;
        this.emailService = emailService;
        this.userService = userService;
        this.restaurant = restaurant;
    }

    /**
     * Method for generate OTP number
     *
     * @param key - provided key (username in this case)
     * @return boolean value (true|false)
     */
    public Boolean generateOtp(String key)
    {
        // generate otp
        Integer otpValue = otpGenerator.generateOTP(key);
        if (otpValue == -1)
        {
            LOGGER.error("OTP generator is not working...");
            return  false;
        }

        LOGGER.info("Generated OTP: {}", otpValue);

        // fetch user e-mail from database

        LOGGER.info("the email is" + userService.findEmailByUsername(key));
        String userMail = userService.findEmailByUsername(key);
        emailService.sendOtpMail(userMail,otpValue.toString());
        // send generated e-mail
        return true;
    }

    /**
     * Method for validating provided OTP
     *
     * @param key - provided key
     * @param otpNumber - provided OTP number
     * @return boolean value (true|false)
     */
    public Boolean validateOTP(String key, Integer otpNumber)
    {
        // get OTP from cache
        Integer cacheOTP = otpGenerator.getOPTByKey(key);
        if (cacheOTP!=null && cacheOTP.equals(otpNumber))
        {
            otpGenerator.clearOTPFromCache(key);
            return true;
        }
        return false;
    }
}
