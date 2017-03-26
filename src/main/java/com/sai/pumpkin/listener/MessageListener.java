package com.sai.pumpkin.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.pumpkin.core.MavenGitVersionCollector;
import com.sai.pumpkin.domain.ArtifactConfig;
import com.sai.pumpkin.domain.CollectionJob;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
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
    private final MongoTemplate mongoTemplate;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    public MessageListener(final MavenGitVersionCollector mavenGitVersionCollector, final MongoTemplate mongoTemplate) {
        this.mavenGitVersionCollector = mavenGitVersionCollector;
        this.mongoTemplate = mongoTemplate;
    }

    @JmsListener(destination = "artifact.collection.queue", concurrency = "${collectorConcurrency}")
    public void onMessage(String config) {
        try {
            CollectionOptions options = new CollectionOptions(10000000, 800, true);
            if (!mongoTemplate.collectionExists(CollectionJob.class)) {
                mongoTemplate.createCollection(CollectionJob.class, options);
            }
            CollectionJob job = new CollectionJob();
            job.setConfigName(config.trim());
            job.setStartTime(System.currentTimeMillis());
            mavenGitVersionCollector.collect(OBJECT_MAPPER.readValue(config, ArtifactConfig.class));
            job.setEndTime(System.currentTimeMillis());
            job.setTotalTime(job.getEndTime() - job.getStartTime());
            mongoTemplate.save(job);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

    }
}
