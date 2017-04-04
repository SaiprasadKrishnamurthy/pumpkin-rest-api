package com.sai.pumpkin.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.pumpkin.repository.ArtifactConfigRepository;
import io.swagger.annotations.ApiOperation;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class SubmitCollectionJobResource {

    private final ArtifactConfigRepository artifactConfigRepository;
    private final JmsTemplate jmsTemplate;
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    public SubmitCollectionJobResource(final ArtifactConfigRepository artifactConfigRepository, final JmsTemplate jmsTemplate) {
        this.artifactConfigRepository = artifactConfigRepository;
        this.jmsTemplate = jmsTemplate;
    }

    @Scheduled(fixedRate = 1000 * 60 * 20)
    @ApiOperation("Submits collection job request for all configs")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/collectall", method = RequestMethod.PUT, produces = "application/json")
    public void collectAll() {
        artifactConfigRepository.findAll().forEach(c -> jmsTemplate.send(session -> {
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
}
