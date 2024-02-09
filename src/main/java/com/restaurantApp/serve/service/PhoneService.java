package com.restaurantApp.serve.service;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PhoneService {
    private Logger LOGGER = LoggerFactory.getLogger(getClass());

    public void sendMessage(String userName, String password, String phone) throws IOException {
        createMessage(userName, password, phone);
    }
    private String createMessage(String userName, String password, String phone) throws IOException {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        String requestBody = "{\n" +
                "    \"key\": \"!ea48d_nrzi2lnz1u6emq#78tak#jlri(e5y2y763p(7o5xt#x63vnyajnlsz0ue\",\n" +
                "    \"msisdn\": \""+phone+"\",\n" +
                "    \"message\": \"hello your username =: "+userName+" and your password is =: "+password+"\",\n" +
                "    \"sender_id\": \"PETMAX-TECH\"\n" +
                "}";

        RequestBody body = RequestBody.create(mediaType, requestBody);

        Request request = new Request.Builder()
                .url("https://sms.nalosolutions.com/smsbackend/Nal_resl/send-message/")
                .method("POST", body)
                .build();

        try {
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                System.out.println("Request successful. Response: " + response.body().string());
            } else {
                System.err.println("Request failed. Response code: " + response.code());
            }
        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
        return null;
    }
}
