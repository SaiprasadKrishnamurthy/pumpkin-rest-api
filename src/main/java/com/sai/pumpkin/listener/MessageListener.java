package com.sai.pumpkin.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.pumpkin.core.MavenGitVersionCollector;
import com.sai.pumpkin.domain.ArtifactConfig;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Created by saipkri on 08/03/17.
 */
@Component
public class MessageListener {
    private final MavenGitVersionCollector mavenGitVersionCollector;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    public MessageListener(final MavenGitVersionCollector mavenGitVersionCollector) {
        this.mavenGitVersionCollector = mavenGitVersionCollector;
    }

    @JmsListener(destination = "artifact.collection.queue", concurrency = "${collectorConcurrency}")
    public void receiveOrder(String config) {
        try {
            mavenGitVersionCollector.collect(OBJECT_MAPPER.readValue(config, ArtifactConfig.class));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

    }
}
