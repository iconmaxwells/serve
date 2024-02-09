package com.restaurantApp.serve.constant;

public class SecurityConstant {
    public static final long EXPIRATION_TIME = 86_400_000; // 5 days expressed in milliseconds
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String JWT_TOKEN_HEADER = "Jwt-Token";
    public static final String TOKEN_CANNOT_BE_VERIFIED = "Token cannot be verified";
    public static final String RESTAURANT_SERVE_LLC = "Restaurant Serve, LLC";
    public static final String RESTAURANT_ADMINISTRATION = "Restaurant Serving Portal";
    public static final String AUTHORITIES = "authorities";
    public static final String FORBIDDEN_MESSAGE = "You need to log in to access this page";
    public static final String ACCESS_DENIED_MESSAGE = "You do not have permission to access this page";
    public static final String OPTIONS_HTTP_METHOD = "OPTIONS";
    //public static final String[] PUBLIC_URLS = { "/transportApp/v1/user/login", "/transportApp/v1/user/image/**","/transportApp/v1/booking/findTicket" };
    public static final String[] PUBLIC_URLS = { "**" }; //"/transportApp/v1/user/register",
}
