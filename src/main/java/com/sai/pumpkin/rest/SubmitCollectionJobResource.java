package com.sai.pumpkin.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.pumpkin.core.MavenGitVersionCollector;
import com.sai.pumpkin.domain.ArtifactConfig;
import com.sai.pumpkin.repository.ArtifactConfigRepository;
import io.swagger.annotations.ApiOperation;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class SubmitCollectionJobResource {

    private final ArtifactConfigRepository artifactConfigRepository;
    private final JmsTemplate jmsTemplate;
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final MavenGitVersionCollector mavenGitVersionCollector;

    private final ExecutorService WORKERS = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Inject
    public SubmitCollectionJobResource(final ArtifactConfigRepository artifactConfigRepository, final JmsTemplate jmsTemplate, MavenGitVersionCollector mavenGitVersionCollector) {
        this.artifactConfigRepository = artifactConfigRepository;
        this.jmsTemplate = jmsTemplate;
        this.mavenGitVersionCollector = mavenGitVersionCollector;
    }

    //@Scheduled(fixedRate = 1000 * 60 * 20)
    @ApiOperation("Submits collection job request for all configs")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/collectall", method = RequestMethod.PUT, produces = "application/json")
    public void collectAll() {
        List<ArtifactConfig> all = artifactConfigRepository.findAll();

        // Randomize the order for git command crashes (probabilistic).
        Collections.shuffle(all);

        all.forEach(c -> jmsTemplate.send(session -> {
            try {
                return session.createTextMessage(OBJECT_MAPPER.writeValueAsString(c));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @ApiOperation("Submits collection job request for a specific artifact")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/collect", method = RequestMethod.PUT, produces = "application/json")
    public void collect(@RequestParam("gitRepoUrl") String gitRepoUrl) {
        artifactConfigRepository.findAll().stream()
                .filter(ac -> ac.getRepoUrl().equals(gitRepoUrl.trim()))
                .forEach(c -> jmsTemplate.send(session -> {
                    try {
                        return session.createTextMessage(OBJECT_MAPPER.writeValueAsString(c));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }

    @ApiOperation("Submits collection job request that tracks the pom on all the inbound branches for a specific artifact")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/collect-one-off", method = RequestMethod.PUT, produces = "application/json")
    public void collectOneOff(@RequestParam("gitRepoUrl") String gitRepoUrl) {
        artifactConfigRepository.findAll().stream()
                .filter(ac -> ac.getRepoUrl().equals(gitRepoUrl.trim()))
                .forEach(c -> WORKERS.submit(() -> mavenGitVersionCollector.collectOneOff(c)));
    }
}
