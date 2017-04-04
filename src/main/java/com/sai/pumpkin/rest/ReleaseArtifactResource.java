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
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
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
    public ResponseEntity<?> saveReleaseFromDump(@RequestParam("version") String version, @RequestParam("name") String releaseName,
                                                 @RequestParam(value = "timestamp", required = false, defaultValue = "") String timestamp,
                                                 @RequestParam(value = "shouldNotify", required = false, defaultValue = "true") boolean shouldNotify, @RequestBody String releaseDump) throws Exception {
        LOGGER.info("Saving the release dump for: {}", version);
        LOGGER.info("Dump is \n {} \n\n", releaseDump);
        List<ReleaseArtifact> allReleases = releaseArtifactRepository.findAll();
        allReleases.sort((a, b) -> a.getVersion().compareTo(b.getVersion()));
        allReleases = allReleases.stream().filter(r -> r.getSnapshot() == null || !r.getSnapshot()).collect(Collectors.toList());

        ReleaseArtifact currRelease = processDump(version, releaseName, releaseDump, "", false);
        Map<String, String> json = new HashMap<>();
        json.put("status", "success");

        if (allReleases.size() > 1) {
            ReleaseArtifact prev = allReleases.get(allReleases.size() - 1);
            DIFF_WORKERS.submit(() -> diffArtifactsResource.releaseDiff(prev.getName() + ":" + prev.getVersion(), currRelease.getName() + ":" + currRelease.getVersion()));
        }
        if (shouldNotify) {
            notificationService.sendReleaseNotification();
        }
        return new ResponseEntity<>(json, HttpStatus.CREATED);
    }

    @ApiOperation("Appends a dependency dump to a release. This should be of the format of the output from the maven dependency plugin from resolve goal.")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/dependency-dump", method = RequestMethod.PUT, produces = "application/json", consumes = "text/plain")
    public ResponseEntity<?> saveDependencyDumpForRelease(@RequestParam("version") String version, @RequestParam("name") String releaseName,
                                                          @RequestParam(value = "timestamp", required = false, defaultValue = "") String timestamp,
                                                          @RequestParam(value = "shouldNotify", required = false, defaultValue = "true") boolean shouldNotify, @RequestBody String releaseDump) throws Exception {
        try {
            LOGGER.info("Saving the dependency dump for: {}", version);
            LOGGER.info("Dump is \n {} \n\n", releaseDump);
            ReleaseArtifact release = releaseArtifactRepository.findRelease(releaseName, version);
            if (release == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            List<String> lines = Arrays.asList(releaseDump.split("\n"));
            List<MavenCoordinates> dependencies = lines.stream()
                    .map(s -> {
                        LOGGER.info("Sai: {} ", s);
                        String[] tokens = s.trim().split(":");
                        String groupId = tokens[0];
                        String artifactId = tokens[1];
                        String aVersion = tokens[tokens.length - 2];
                        return new MavenCoordinates(groupId, artifactId, aVersion, release.getBuiltTimestamp() == null ? -1 : release.getBuiltTimestamp());
                    }).collect(toList());

            release.getMavenArtifacts().addAll(dependencies);

            // Update it.
            Query query = new Query();
            query.addCriteria(Criteria.where("name").is(release.getName()).and("version").is(release.getVersion()));
            Update update = new Update();
            update.set("mavenArtifacts", release.getMavenArtifacts());
            mongoTemplate.updateFirst(query, update, ReleaseArtifact.class);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @ApiOperation("Saves a snapshot artifact from a dump")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/snapshot-dump", method = RequestMethod.PUT, produces = "application/json", consumes = "text/plain")
    public ResponseEntity<?> saveSnapshotFromDump(@RequestParam("version") String version,
                                                  @RequestParam(value = "timestamp", required = false, defaultValue = "") String timestamp,
                                                  @RequestParam("name") String releaseName, @RequestParam(value = "shouldNotify", required = false, defaultValue = "true") boolean shouldNotify, @RequestBody String releaseDump) throws Exception {
        LOGGER.info("Saving the release dump for: {}", version);
        LOGGER.info("Dump is \n {} \n\n", releaseDump);
        List<ReleaseArtifact> allReleases = releaseArtifactRepository.findAll();
        allReleases.sort((a, b) -> a.getVersion().compareTo(b.getVersion()));
        allReleases = allReleases.stream().filter(r -> r.getSnapshot() != null && r.getSnapshot()).collect(Collectors.toList());

        ReleaseArtifact currRelease = processDump(version, releaseName, releaseDump, timestamp, true);
        Map<String, String> json = new HashMap<>();
        json.put("status", "success");

        if (shouldNotify) {
            notificationService.sendSnapshotNotification();
        }
        return new ResponseEntity<>(json, HttpStatus.CREATED);
    }

    private ReleaseArtifact processDump(@RequestParam("version") String version, @RequestParam("name") String releaseName, @RequestBody String releaseDump, String timestamp, boolean isSnapshot) throws Exception {
        List<MavenCoordinates> allMavenCoordinates = Stream.of(releaseDump.split("\n"))
                .filter(l -> l.trim().length() > 0)
                .map(s -> {
                    Optional<Date> builtDate = builtDate(s);
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
                    if (builtDate.isPresent()) {
                        mavenCoordinates.setBuiltTimestamp(builtDate.get().getTime());
                    }
                    return mavenCoordinates;
                }).collect(toList());
        ReleaseArtifact currRelease = new ReleaseArtifact();
        currRelease.setName(releaseName.trim());
        currRelease.setVersion(version);
        currRelease.setMavenArtifacts(allMavenCoordinates);
        currRelease.setSnapshot(isSnapshot);
        if (StringUtils.hasText(timestamp)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh_mm_ss.S_Z");
            currRelease.setBuiltTimestamp(sdf.parse(timestamp).getTime());

        }
        Criteria c = Criteria.where("name").is(currRelease.getName()).and("version").is(currRelease.getVersion());
        mongoTemplate.remove(Query.query(c), ReleaseArtifact.class);
        mongoTemplate.save(currRelease);
        return currRelease;
    }

    private Optional<Date> builtDate(String line) {
        final SimpleDateFormat fmt = new SimpleDateFormat("EEE MMM dd hh:mm:ss ZZ yyyy");
        if (line.contains("#") && line.contains("version")) {
            line = line.trim();
            String clipped = line.substring(line.indexOf("#", 2) + 1, line.indexOf("version")).trim();
            try {
                return Optional.of(fmt.parse(clipped));
            } catch (ParseException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
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
    public ReleaseMetadata releasemeta(@RequestParam("version") String version, @RequestParam("name") String releaseName, final @RequestParam(value = "status", required = false, defaultValue = "") String status) {
        Criteria c = Criteria.where("name").is(releaseName.trim()).and("version").is(version.trim());
        ReleaseArtifact release = mongoTemplate.findOne(Query.query(c), ReleaseArtifact.class);
        ReleaseMetadata meta = new ReleaseMetadata();
        meta.setReleaseName(releaseName.trim());
        meta.setVersion(version.trim());
        for (MavenCoordinates artifact : release.getMavenArtifacts()) {
            ArtifactCollection artifactCollection = new ArtifactCollection();
            artifactCollection.setMavenCoordinates(artifact);
            List<MavenGitVersionMapping> byMavenCoordinates = mavenGitVersionMappingRepository.findByMavenCoordinates(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
            if (byMavenCoordinates != null && !byMavenCoordinates.isEmpty()) {
                artifactCollection.setStatus(ArtifactCollectionStatusType.COLLECTED);
            } else {
                artifactCollection.setStatus(ArtifactCollectionStatusType.NOT_REGISTERED);
            }
            if (StringUtils.hasText(status) && status.equalsIgnoreCase(artifactCollection.getStatus().toString())) {
                meta.getArtifacts().add(artifactCollection);
            } else if (status.trim().length() == 0) {
                meta.getArtifacts().add(artifactCollection);
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
