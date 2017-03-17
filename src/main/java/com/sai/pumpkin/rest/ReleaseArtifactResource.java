package com.sai.pumpkin.rest;

import com.sai.pumpkin.domain.MavenGitVersionMapping;
import com.sai.pumpkin.domain.ReleaseArtifact;
import com.sai.pumpkin.repository.MavenGitVersionMappingRepository;
import com.sai.pumpkin.repository.ReleaseArtifactRepository;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class ReleaseArtifactResource {

    private final ReleaseArtifactRepository releaseArtifactRepository;
    private final MavenGitVersionMappingRepository mavenGitVersionMappingRepository;
    private final MongoTemplate mongoTemplate;
    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseArtifactResource.class);

    @Inject
    public ReleaseArtifactResource(final ReleaseArtifactRepository releaseArtifactRepository, final MavenGitVersionMappingRepository mavenGitVersionMappingRepository, final MongoTemplate mongoTemplate) {
        this.releaseArtifactRepository = releaseArtifactRepository;
        this.mavenGitVersionMappingRepository = mavenGitVersionMappingRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @ApiOperation("Saves a release artifact")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/release", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<?> saveRelease(@RequestBody ReleaseArtifact releaseArtifact) {
        Criteria c = Criteria.where("name").is(releaseArtifact.getName()).and("version").is(releaseArtifact.getVersion());
        mongoTemplate.remove(Query.query(c), ReleaseArtifact.class);
        mongoTemplate.save(releaseArtifact);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @ApiOperation("Saves a release artifact from a dump")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/release", method = RequestMethod.PUT, produces = "application/json", consumes = "text/plain")
    public ResponseEntity<?> saveReleaseFromDump(@RequestParam("version") String version, @RequestBody String releaseDump) {
        LOGGER.info("Saving the release dump for: {}", version);
        LOGGER.info("Dump is \n {} \n\n", releaseDump);
        Map<String, String> json = new HashMap<>();
        json.put("status", "success");
        return new ResponseEntity<>(json, HttpStatus.CREATED);
    }

    @ApiOperation("Lists all releases")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/releases", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> releases() {
        return new ResponseEntity<>(releaseArtifactRepository.findAll(), HttpStatus.OK);
    }

    @ApiOperation("Lists all maven artifacts")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/artifacts", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> artifacts(@RequestParam("artifactId") final String artifactId) {
        List<MavenGitVersionMapping> artifacts = mavenGitVersionMappingRepository.findByMavenCoordinates(artifactId.trim());
        artifacts.sort((a, b) -> a.getMavenCoordinates().getArtifactId().compareTo(b.getMavenCoordinates().getArtifactId()));
        return new ResponseEntity<>(artifacts, HttpStatus.OK);
    }
}
