package com.sai.pumpkin.rest;

import com.sai.pumpkin.domain.*;
import com.sai.pumpkin.notification.NotificationService;
import com.sai.pumpkin.repository.MavenGitVersionMappingRepository;
import com.sai.pumpkin.repository.ReleaseArtifactRepository;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class ReleaseArtifactResource {

    private final ReleaseArtifactRepository releaseArtifactRepository;
    private final MavenGitVersionMappingRepository mavenGitVersionMappingRepository;
    private final MongoTemplate mongoTemplate;
    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseArtifactResource.class);
    private final ExecutorService DIFF_WORKERS = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final DiffArtifactsResource diffArtifactsResource;
    private final NotificationService notificationService;

    @Inject
    public ReleaseArtifactResource(final ReleaseArtifactRepository releaseArtifactRepository, final MavenGitVersionMappingRepository mavenGitVersionMappingRepository, final MongoTemplate mongoTemplate, DiffArtifactsResource diffArtifactsResource, NotificationService notificationService) {
        this.releaseArtifactRepository = releaseArtifactRepository;
        this.mavenGitVersionMappingRepository = mavenGitVersionMappingRepository;
        this.mongoTemplate = mongoTemplate;
        this.diffArtifactsResource = diffArtifactsResource;
        this.notificationService = notificationService;
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
    @RequestMapping(value = "/release-dump", method = RequestMethod.PUT, produces = "application/json", consumes = "text/plain")
    public ResponseEntity<?> saveReleaseFromDump(@RequestParam("version") String version, @RequestParam("name") String releaseName, @RequestBody String releaseDump) {
        LOGGER.info("Saving the release dump for: {}", version);
        LOGGER.info("Dump is \n {} \n\n", releaseDump);
        List<ReleaseArtifact> allReleases = releaseArtifactRepository.findAll();

        List<MavenCoordinates> allMavenCoordinates = Stream.of(releaseDump.split("\n"))
                .filter(l -> l.trim().length() > 0)
                .map(s -> {
                    MavenCoordinates mavenCoordinates = new MavenCoordinates();
                    String[] tokens = s.trim().split(" ");
                    List<String> kv = Stream.of(tokens).filter(t -> t.contains("=")).collect(toList());
                    for (String kvp : kv) {
                        String value = kvp.split("=")[1];
                        if (kvp.contains("groupId")) {
                            mavenCoordinates.setGroupId(value.trim());
                        } else if (kvp.contains("artifactId")) {
                            mavenCoordinates.setArtifactId(value.trim());
                        } else if (kvp.contains("version")) {
                            mavenCoordinates.setVersion(value.trim());
                        }
                    }
                    return mavenCoordinates;
                }).collect(toList());
        ReleaseArtifact currRelease = new ReleaseArtifact();
        currRelease.setName(releaseName.trim());
        currRelease.setVersion(version);
        currRelease.setMavenArtifacts(allMavenCoordinates);
        Criteria c = Criteria.where("name").is(currRelease.getName()).and("version").is(currRelease.getVersion());
        mongoTemplate.remove(Query.query(c), ReleaseArtifact.class);
        mongoTemplate.save(currRelease);
        Map<String, String> json = new HashMap<>();
        json.put("status", "success");

        if (!allReleases.isEmpty()) {
            ReleaseArtifact prev = allReleases.get(allReleases.size() - 1);
            DIFF_WORKERS.submit(() -> diffArtifactsResource.releaseDiff(prev.getName() + ":" + prev.getVersion(), currRelease.getName() + ":" + currRelease.getVersion()));
        }
        notificationService.sendReleaseNotification();
        return new ResponseEntity<>(json, HttpStatus.CREATED);
    }

    @ApiOperation("Lists all releases")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/releases", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> releases() {
        return new ResponseEntity<>(releaseArtifactRepository.findAll(), HttpStatus.OK);
    }

    @Cacheable(cacheNames = "releaseMetaCache", key = "#p0.concat('releaseMetaCache').concat(#p1)")
    @ApiOperation("Lists all release meta")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/release-meta", method = RequestMethod.GET, produces = "application/json")
    public ReleaseMetadata releasemeta(@RequestParam("version") String version, @RequestParam("name") String releaseName) {
        Criteria c = Criteria.where("name").is(releaseName.trim()).and("version").is(version.trim());
        ReleaseArtifact release = mongoTemplate.findOne(Query.query(c), ReleaseArtifact.class);
        ReleaseMetadata meta = new ReleaseMetadata();
        meta.setReleaseName(releaseName.trim());
        meta.setVersion(version.trim());
        for (MavenCoordinates artifact : release.getMavenArtifacts()) {
            ArtifactCollection artifactCollection = new ArtifactCollection();
            artifactCollection.setMavenCoordinates(artifact);
            meta.getArtifacts().add(artifactCollection);
            List<MavenGitVersionMapping> byMavenCoordinates = mavenGitVersionMappingRepository.findByMavenCoordinates(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
            if (byMavenCoordinates != null && !byMavenCoordinates.isEmpty()) {
                artifactCollection.setStatus(ArtifactCollectionStatusType.COLLECTED);
            } else {
                artifactCollection.setStatus(ArtifactCollectionStatusType.NOT_REGISTERED);
            }
        }
        return meta;
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
