package com.sai.pumpkin.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

/**
 * Created by saipkri on 23/03/17.
 */
public class BitBucketUtils {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final RestTemplate restTemplate = new RestTemplate();

    public static String pullRequests(final String apiUrl, final String auth) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        SSLUtil.turnOffSslChecking();

        HttpEntity<String> request = securityHeader(auth);
        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, Map.class);
        return mapper.writeValueAsString(response.getBody());
    }

    private static HttpEntity<String> securityHeader(String auth) {
        byte[] plainCredsBytes = auth.getBytes();
        byte[] base64CredsBytes = Base64.getEncoder().encode(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Creds);
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        return new HttpEntity<>(headers);
    }
}
