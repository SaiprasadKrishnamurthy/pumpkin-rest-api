package com.sai.pumpkin.rest;

import com.sai.pumpkin.core.MavenGitVersionCollector;
import com.sai.pumpkin.domain.*;
import com.sai.pumpkin.repository.GitLogResponseRepository;
import com.sai.pumpkin.repository.MavenGitVersionMappingRepository;
import com.sai.pumpkin.repository.PullRequestRepository;
import com.sai.pumpkin.repository.ReleaseArtifactRepository;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class DiffArtifactsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiffArtifactsResource.class);

    private final GitLogResponseRepository gitLogResponseRepository;
    private final MavenGitVersionMappingRepository mavenGitVersionMappingRepository;
    private final MavenGitVersionCollector mavenGitVersionCollector;
    private final ReleaseArtifactRepository releaseArtifactRepository;
    private final PullRequestRepository pullRequestRepository;
    private final MongoTemplate mongoTemplate;

    @Inject
    public DiffArtifactsResource(final GitLogResponseRepository gitLogResponseRepository, final MavenGitVersionMappingRepository mavenGitVersionMappingRepository, final MavenGitVersionCollector mavenGitVersionCollector, ReleaseArtifactRepository releaseArtifactRepository, final PullRequestRepository pullRequestRepository, MongoTemplate mongoTemplate) {
        this.gitLogResponseRepository = gitLogResponseRepository;
        this.mavenGitVersionMappingRepository = mavenGitVersionMappingRepository;
        this.mavenGitVersionCollector = mavenGitVersionCollector;
        this.releaseArtifactRepository = releaseArtifactRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Cacheable(cacheNames = "summaryDiffCache", key = "#p0.concat('summaryDiffCache').concat(#p1)")
    @ApiOperation("Gets a diff between artifact 1 and artifact 2")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/summarydiff", method = RequestMethod.GET, produces = "application/json")
    public GitLogSummaryResponse summarydiff(@ApiParam("groupId:artifactId:version") @RequestParam("mavenCoordinates1") String mavenCoordinates1,
                                             @ApiParam("groupId:artifactId:version") @RequestParam("mavenCoordinates2") String mavenCoordinates2) {
        String[] c1 = mavenCoordinates1.split(":");
        String[] c2 = mavenCoordinates2.split(":");
        if (c1.length < 3 || c2.length < 3) {
            throw new IllegalArgumentException("Maven coordinates must be in the format: 'groupId:artifactId:version'");
        }
        return mavenGitVersionCollector.summarize(c1[0], c1[1], c1[2], "", c2[0], c2[1], c2[2], "", 0L);
    }

    @Cacheable(cacheNames = "releaseDiffCache", key = "#p0.concat('releaseDiffCache').concat(#p1)")
    @ApiOperation("Gets a diff between release 1 and release 2")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/release-diff", method = RequestMethod.GET, produces = "application/json")
    public ReleaseDiffResponse releaseDiff(@ApiParam("name:version") @RequestParam("releaseCoordinates1") String releaseCoordinates1,
                                           @ApiParam("name:version") @RequestParam("releaseCoordinates2") String releaseCoordinates2) {
        String[] c1 = releaseCoordinates1.split(":");
        String[] c2 = releaseCoordinates2.split(":");
        if (c1.length < 2 || c2.length < 2) {
            throw new IllegalArgumentException("Release coordinates must be in the format: 'releaseName:version'");
        }

        ReleaseArtifact artifact1 = releaseArtifactRepository.findRelease(c1[0].trim(), c1[1].trim());
        ReleaseArtifact artifact2 = releaseArtifactRepository.findRelease(c2[0].trim(), c2[1].trim());

        if (artifact1 == null || artifact2 == null) {
            throw new IllegalArgumentException("Not found");

        }
        List<MavenCoordinates> removed = artifact1.getMavenArtifacts().stream().filter(old -> !artifact2.getMavenArtifacts().contains(old)).collect(toList());
        List<MavenCoordinates> added = artifact2.getMavenArtifacts().stream().filter(nw -> !artifact1.getMavenArtifacts().contains(nw)).collect(toList());
        List<GitLogSummaryResponse> summaries = new ArrayList<>();

        List<MavenCoordinates> diffs = new ArrayList<>();
        List<MavenCoordinates> bigger = artifact1.getMavenArtifacts();
        List<MavenCoordinates> smaller = artifact2.getMavenArtifacts();

        if (bigger.size() < smaller.size()) {
            List<MavenCoordinates> temp = bigger;
            bigger = smaller;
            smaller = temp;
        }
        for (MavenCoordinates m : bigger) {
            Optional<MavenCoordinates> _m = smaller.stream().filter(s -> s.equals(m)).findFirst();
            if (_m.isPresent() && !_m.get().getVersion().equals(m.getVersion())) {
                diffs.add(m);
            } else if (_m.isPresent() && _m.get().getVersion().equals(m.getVersion()) && _m.get().getVersion().contains("SNAPSHOT")) {
                // 2 snapshots same version.
                diffs.add(m);
            }
        }

        for (MavenCoordinates diff : diffs) {
            MavenCoordinates old = artifact1.getMavenArtifacts().stream().filter(mc -> mc.getGroupId().equals(diff.getGroupId()) && mc.getArtifactId().equals(diff.getArtifactId())).findFirst().get();
            MavenCoordinates nw = artifact2.getMavenArtifacts().stream().filter(mc -> mc.getGroupId().equals(diff.getGroupId()) && mc.getArtifactId().equals(diff.getArtifactId())).findFirst().get();

            // Get the from and to.
            MavenGitVersionMapping[] fromAndTo = mavenGitVersionCollector.fromAndTo(old, nw);

            if (fromAndTo.length == 2) {
                GitLogSummaryResponse s = mavenGitVersionCollector.summarize(old.getGroupId(), old.getArtifactId(), old.getVersion(), fromAndTo[0].getTimestamp() + "", nw.getGroupId(), nw.getArtifactId(), nw.getVersion(), fromAndTo[1].getTimestamp() + "", 0L);
                if (s != null) {
                    summaries.add(s);
                }
            } else {
                LOGGER.warn("No Log Summary found for: {}, {}", old, nw);
            }
        }
        ReleaseDiffResponse releaseDiffResponse = new ReleaseDiffResponse();
        releaseDiffResponse.setDiffs(summaries);
        releaseDiffResponse.setNewlyAdded(added.stream().flatMap(mc -> mavenGitVersionMappingRepository.findByMavenCoordinates(mc.getGroupId(), mc.getArtifactId(), mc.getVersion()).stream()).collect(toList()));
        releaseDiffResponse.setRemoved(removed.stream().flatMap(mc -> mavenGitVersionMappingRepository.findByMavenCoordinates(mc.getGroupId(), mc.getArtifactId(), mc.getVersion()).stream()).collect(toList()));
        return releaseDiffResponse;
    }

    @ApiOperation("Gets a diff between snapshot 1 and snapshot 2")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/release-diff", method = RequestMethod.GET, produces = "application/json")
    public ReleaseDiffResponse snapshotDiff(@ApiParam("name:version") @RequestParam("releaseCoordinates1") String releaseCoordinates1,
                                            @ApiParam("name:version") @RequestParam("releaseCoordinates2") String releaseCoordinates2,
                                            @ApiParam("snapshotGoBackUpToMinutes") @RequestParam(name = "snapshotGoBackUpToMinutes", required = false, defaultValue = "120") int snapshotGoBackUpToMinutes) {
        String[] c1 = releaseCoordinates1.split(":");
        String[] c2 = releaseCoordinates2.split(":");
        if (c1.length < 2 || c2.length < 2) {
            throw new IllegalArgumentException("Release coordinates must be in the format: 'releaseName:version'");
        }

        ReleaseArtifact artifact1 = releaseArtifactRepository.findRelease(c1[0].trim(), c1[1].trim());
        ReleaseArtifact artifact2 = releaseArtifactRepository.findRelease(c2[0].trim(), c2[1].trim());

        if (artifact1 == null || artifact2 == null) {
            throw new IllegalArgumentException("Not found");

        }
        List<MavenCoordinates> removed = artifact1.getMavenArtifacts().stream().filter(old -> !artifact2.getMavenArtifacts().contains(old)).collect(toList());
        List<MavenCoordinates> added = artifact2.getMavenArtifacts().stream().filter(nw -> !artifact1.getMavenArtifacts().contains(nw)).collect(toList());
        List<GitLogSummaryResponse> summaries = new ArrayList<>();

        List<MavenCoordinates> diffs = new ArrayList<>();
        List<MavenCoordinates> bigger = artifact1.getMavenArtifacts();
        List<MavenCoordinates> smaller = artifact2.getMavenArtifacts();

        if (bigger.size() < smaller.size()) {
            List<MavenCoordinates> temp = bigger;
            bigger = smaller;
            smaller = temp;
        }
        for (MavenCoordinates m : bigger) {
            Optional<MavenCoordinates> _m = smaller.stream().filter(s -> s.equals(m)).findFirst();
            if (_m.isPresent() && !_m.get().getVersion().equals(m.getVersion())) {
                diffs.add(m);
            } else if (_m.isPresent() && _m.get().getVersion().equals(m.getVersion()) && _m.get().getVersion().contains("SNAPSHOT")) {
                // 2 snapshots same version.
                diffs.add(m);
            }
        }

        for (MavenCoordinates diff : diffs) {
            MavenCoordinates old = artifact1.getMavenArtifacts().stream().filter(mc -> mc.getGroupId().equals(diff.getGroupId()) && mc.getArtifactId().equals(diff.getArtifactId())).findFirst().get();
            MavenCoordinates nw = artifact2.getMavenArtifacts().stream().filter(mc -> mc.getGroupId().equals(diff.getGroupId()) && mc.getArtifactId().equals(diff.getArtifactId())).findFirst().get();

            // Get the from and to.
            MavenGitVersionMapping[] fromAndTo = mavenGitVersionCollector.fromAndTo(old, nw);

            if (fromAndTo.length == 2) {
                GitLogSummaryResponse s = mavenGitVersionCollector.summarize(old.getGroupId(), old.getArtifactId(), old.getVersion(), fromAndTo[0].getTimestamp() + "", nw.getGroupId(), nw.getArtifactId(), nw.getVersion(), fromAndTo[1].getTimestamp() + "", (System.currentTimeMillis() - (snapshotGoBackUpToMinutes * 60 * 1000)));
                if (s != null) {
                    summaries.add(s);
                }
            } else {
                LOGGER.warn("No Log Summary found for: {}, {}", old, nw);
            }
        }
        ReleaseDiffResponse releaseDiffResponse = new ReleaseDiffResponse();
        releaseDiffResponse.setDiffs(summaries);
        releaseDiffResponse.setNewlyAdded(added.stream().flatMap(mc -> mavenGitVersionMappingRepository.findByMavenCoordinates(mc.getGroupId(), mc.getArtifactId(), mc.getVersion()).stream()).collect(toList()));
        releaseDiffResponse.setRemoved(removed.stream().flatMap(mc -> mavenGitVersionMappingRepository.findByMavenCoordinates(mc.getGroupId(), mc.getArtifactId(), mc.getVersion()).stream()).collect(toList()));
        return releaseDiffResponse;
    }

    @ApiOperation("Gets a diff between artifact 1 and artifact 2")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/artifact-diff", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> artifactDiff(@ApiParam("groupId:artifactId:version:timestamp") @RequestParam("mavenCoordinates1") String mavenCoordinates1,
                                          @ApiParam("groupId:artifactId:version:timestamp") @RequestParam("mavenCoordinates2") String mavenCoordinates2) {
        String[] c1 = mavenCoordinates1.split(":");
        String[] c2 = mavenCoordinates2.split(":");
        if (c1.length < 2 || c2.length < 2) {
            throw new IllegalArgumentException("Maven coordinates must be in the format: 'groupId:artifactId:version:timestamp'");
        }

        GitLogSummaryResponse diffResponse = mavenGitVersionCollector.summarize(c1[0], c1[1], c1[2], c1[3], c2[0], c2[1], c2[2], c2[3], 0L);
        ReleaseDiffResponse releaseDiffResponse = new ReleaseDiffResponse();
        releaseDiffResponse.setDiffs(Arrays.asList(diffResponse));
        return new ResponseEntity<>(releaseDiffResponse, HttpStatus.OK);
    }

    @ApiOperation("Gets detailed commits since a specified timestamp")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/changes", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> artifactDiff(@ApiParam("fromTimestamp") @RequestParam(value = "fromTimestamp", required = false) Long fromTimestamp,
                                          @RequestParam(value = "untilTimestamp", required = false) Long untilTimestamp,
                                          @ApiParam("relativeTime") @RequestParam(value = "relativeTime", required = false) Long relativeTime,
                                          @ApiParam("relativeTimeUnit") @RequestParam(value = "relativeTimeUnit", required = false) TimeUnit relativeTimeUnit) throws Exception {
        LOGGER.info("Range timestamp: {} - {} ", fromTimestamp, untilTimestamp);
        LOGGER.info("Relative timestamp: {} - {} ", relativeTime, relativeTimeUnit);

        if ((fromTimestamp != null && untilTimestamp != null) && (relativeTime != null)) {
            return new ResponseEntity<>("You can either specify a time range or a relative time with a unit. Not both.", HttpStatus.BAD_REQUEST);
        }
        if ((fromTimestamp != null && untilTimestamp != null) && (relativeTimeUnit != null)) {
            return new ResponseEntity<>("You can either specify a time range or a relative time with a unit. Not both.", HttpStatus.BAD_REQUEST);
        }
        if (relativeTime != null && relativeTimeUnit != null) {
            relativeTime = System.currentTimeMillis() - relativeTimeUnit.toMillis(relativeTime);
            fromTimestamp = relativeTime;
            untilTimestamp = System.currentTimeMillis();
        }
        Query query = Query.query(Criteria.where("timestamp").gt(fromTimestamp).lte(untilTimestamp));
        List<MavenGitVersionMapping> after = mongoTemplate.find(query, MavenGitVersionMapping.class);

        LOGGER.info("Retrieved entries within the time range: {}", after);

        List<GitLogResponse> responses = new ArrayList<>();

        final Map<String, List<MavenGitVersionMapping>> afterMap = new HashMap<>();

        Collections.sort(after, (a, b) -> Long.valueOf(a.getTimestamp()).compareTo(b.getTimestamp()));

        after.forEach(m -> {
            LOGGER.info("Git Revision: {} ", m.getGitRevision());
            afterMap.compute(m.getMavenCoordinates().shortString(), (k, v) -> {
                if (v == null) {
                    List<MavenGitVersionMapping> mapping = new ArrayList<>();
                    mapping.add(m);
                    return mapping;
                } else {
                    v.add(m);
                    return v;
                }
            });
        });

        for (Map.Entry<String, List<MavenGitVersionMapping>> afterEntry : afterMap.entrySet()) {
            if (!afterEntry.getValue().isEmpty()) {
                MavenGitVersionMapping nw = afterEntry.getValue().get(afterEntry.getValue().size() - 1);
                // Get the last version just lesser than the timestamp.
                query = Query.query(Criteria.where("timestamp").lt(fromTimestamp).and("artifactConfig.name").is(nw.getArtifactConfig().getName())).with(new Sort(Sort.Direction.DESC, "timestamp")).limit(1);
                List<MavenGitVersionMapping> startingRev = mongoTemplate.find(query, MavenGitVersionMapping.class);
                LOGGER.info("Start Revision: {}", startingRev);
                MavenGitVersionMapping old = startingRev.get(0);

                if (old != null && nw != null) {
                    GitLogResponse s = mavenGitVersionCollector.diffLog(old.getMavenCoordinates().getGroupId(), old.getMavenCoordinates().getArtifactId(), old.getMavenCoordinates().getVersion(), old.getTimestamp() + "", nw.getMavenCoordinates().getGroupId(), nw.getMavenCoordinates().getArtifactId(), nw.getMavenCoordinates().getVersion(), nw.getTimestamp() + "");
                    List<PullRequest> prs = pullRequestRepository.findPullRequestsMergedIntoCommit(old.getGitRevision());
                    List<PullRequest> prs1 = afterEntry.getValue().stream()
                            .flatMap(m ->
                                    pullRequestRepository.findPullRequestsMergedIntoCommit(m.getGitRevision()).stream())
                            .collect(toList());

                    if (prs != null) {
                        s.getPullRequests().addAll(prs);
                    }
                    if (prs1 != null) {
                        s.getPullRequests().addAll(prs1);
                    }
                    if (!s.getGitLogEntries().isEmpty()) {
                        responses.add(s);
                    }
                }
            }
        }
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    @ApiOperation("Gets a detailed commits between release 1 and release 2 filtered by a csv of committers")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/detailedcommits", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> detailedcommits(@ApiParam("releaseCoordinates1") @RequestParam("releaseCoordinates1") String releaseCoordinates1,
                                             @ApiParam("releaseCoordinates2") @RequestParam("releaseCoordinates2") String releaseCoordinates2,
                                             @ApiParam("committersCsv") @RequestParam("committersCsv") String committersCsv) {
        String[] c1 = releaseCoordinates1.split(":");
        String[] c2 = releaseCoordinates2.split(":");
        if (c1.length < 2 || c2.length < 2) {
            throw new IllegalArgumentException("Release coordinates must be in the format: 'releaseName:version'");
        }

        ReleaseArtifact artifact1 = releaseArtifactRepository.findRelease(c1[0].trim(), c1[1].trim());
        ReleaseArtifact artifact2 = releaseArtifactRepository.findRelease(c2[0].trim(), c2[1].trim());

        if (artifact1 == null || artifact2 == null) {
            return new ResponseEntity<>("No release found for the given coordinates.", HttpStatus.NOT_FOUND);

        }
        List<MavenCoordinates> added = artifact2.getMavenArtifacts().stream().filter(nw -> !artifact1.getMavenArtifacts().contains(nw)).collect(toList());
        List<GitLogSummaryResponse> summaries = new ArrayList<>();

        List<MavenCoordinates> diffs = new ArrayList<>();
        List<MavenCoordinates> bigger = artifact1.getMavenArtifacts();
        List<MavenCoordinates> smaller = artifact2.getMavenArtifacts();

        if (bigger.size() < smaller.size()) {
            List<MavenCoordinates> temp = bigger;
            bigger = smaller;
            smaller = temp;
        }
        for (MavenCoordinates m : bigger) {
            Optional<MavenCoordinates> _m = smaller.stream().filter(s -> s.equals(m)).findFirst();
            if (_m.isPresent() && !_m.get().getVersion().equals(m.getVersion())) {
                diffs.add(m);
            }
        }

        diffs.addAll(added);
        List<GitLogEntry> grand = new ArrayList<>();
        for (MavenCoordinates diff : diffs) {
            Optional<MavenCoordinates> first = artifact1.getMavenArtifacts().stream().filter(mc -> mc.getGroupId().equals(diff.getGroupId()) && mc.getArtifactId().equals(diff.getArtifactId())).findFirst();
            Optional<MavenCoordinates> first1 = artifact2.getMavenArtifacts().stream().filter(mc -> mc.getGroupId().equals(diff.getGroupId()) && mc.getArtifactId().equals(diff.getArtifactId())).findFirst();
            if (first.isPresent() && first1.isPresent()) {
                MavenCoordinates old = first.get();
                MavenCoordinates nw = first1.get();
                GitLogResponse s = mavenGitVersionCollector.diffLog(old.getGroupId(), old.getArtifactId(), old.getVersion(), "", nw.getGroupId(), nw.getArtifactId(), nw.getVersion(), "");
                if (s != null) {
                    grand.addAll(mavenGitVersionCollector.filterByCommitters(s, committersCsv));
                }
            }
        }
        return new ResponseEntity<>(grand, HttpStatus.OK);
    }

    @ApiOperation("Refreshes all the cache")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.DELETE, RequestMethod.GET})
    @RequestMapping(value = "/cache-refresh", method = RequestMethod.GET, produces = "application/json")
    @CacheEvict(cacheNames = {"detailedDiffCache", "summaryDiffCache", "releaseMetaCache", "releaseDiffCache"}, allEntries = true)
    public ResponseEntity<?> refreshCache() {
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
