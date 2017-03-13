package com.sai.pumpkin.rest;

import com.sai.pumpkin.domain.MavenGitVersionMapping;
import com.sai.pumpkin.domain.ReleaseArtifact;
import com.sai.pumpkin.repository.MavenGitVersionMappingRepository;
import com.sai.pumpkin.repository.ReleaseArtifactRepository;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class ReleaseArtifactResource {

    private final ReleaseArtifactRepository releaseArtifactRepository;
    private final MavenGitVersionMappingRepository mavenGitVersionMappingRepository;
    private final MongoTemplate mongoTemplate;

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

    @ApiOperation("Lists all releases")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/releases", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> releases() {
        return new ResponseEntity<>(releaseArtifactRepository.findAll(), HttpStatus.OK);
    }

    @ApiOperation("Lists all maven artifacts")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/artifacts", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> artifacts() {
        List<MavenGitVersionMapping> artifacts = mavenGitVersionMappingRepository.findAll();
        artifacts.sort((a, b) -> a.getMavenCoordinates().getArtifactId().compareTo(b.getMavenCoordinates().getArtifactId()));
        return new ResponseEntity<>(artifacts, HttpStatus.OK);
    }
}
