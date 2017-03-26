package com.sai.pumpkin.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);
    private final ExecutorService WORKERS = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public NotificationService(@Value("${apiToken}") final String apiToken, @Value("${apiUrl}") final String apiUrl, @Value("${notificationChannelId}") final String notificationChannelId, @Value("${webLink1}") final String webLink1, @Value("${webLink2}") final String webLink2) {
        this.apiToken = apiToken;
        this.apiUrl = apiUrl;
        this.notificationChannelId = notificationChannelId;
        this.webLink1 = webLink1;
        this.webLink2 = webLink2;
    }

    public void sendReleaseNotification() {
        WORKERS.submit(() -> {
            try {
                Map<String, String> body = new HashMap<>();
                body.put("roomId", notificationChannelId);
                body.put("text", "[A new release has been loaded into Pumpkin]" + webLink1 + ", Verify your expectations here: " + webLink2);
                String payload = null;
                try {
                    payload = new ObjectMapper().writeValueAsString(body).replace("\n", "");
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                int exit = new ProcessExecutor().command("curl", "-v", "-H", "\"Content-type:application/json; charset=utf-8\"",
                        "-H \"Authorization Bearer " + apiToken + "\"", "-X", "POST", "-d", "'" + payload + "'", apiUrl)
                        .redirectOutput(new LogOutputStream() {
                            @Override
                            protected void processLine(String line) {
                                LOGGER.info(line);
                            }
                        })
                        .execute()
                        .getExitValue();
                LOGGER.info("Notification process exit status: {}", exit);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
