package com.sai.pumpkin.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by saipkri on 26/03/17.
 */
@Component
public class NotificationService {

    private final String apiToken;
    private final String apiUrl;
    private final String notificationChannelId;
    private final String webLink1;
    private final String webLink2;

    public NotificationService(@Value("apiToken") final String apiToken, @Value("apiUrl") final String apiUrl, @Value("notificationChannelId") final String notificationChannelId, @Value("webLink1") final String webLink1, @Value("webLink2") final String webLink2) {
        this.apiToken = apiToken;
        this.apiUrl = apiUrl;
        this.notificationChannelId = notificationChannelId;
        this.webLink1 = webLink1;
        this.webLink2 = webLink2;
    }

    public void sendReleaseNotification() {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> request = securityHeader(apiToken);
        restTemplate.exchange(apiUrl, HttpMethod.POST, request, Map.class);
    }

    private HttpEntity<?> securityHeader(String apiToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer:" + apiToken.trim());
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        Map<String, String> body = new HashMap<>();
        body.put("roomId", notificationChannelId);
        body.put("text", "[A new release has been loaded into Pumpkin]" + webLink1 + ", Verify your expectations here: " + webLink2);
        return new HttpEntity<>(body, headers);
    }
}
