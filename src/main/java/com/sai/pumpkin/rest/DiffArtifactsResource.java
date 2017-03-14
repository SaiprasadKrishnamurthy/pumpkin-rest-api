package com.sai.pumpkin.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.pumpkin.core.MavenGitVersionCollector;
import com.sai.pumpkin.domain.*;
import com.sai.pumpkin.repository.GitLogResponseRepository;
import com.sai.pumpkin.repository.MavenGitVersionMappingRepository;
import com.sai.pumpkin.repository.ReleaseArtifactRepository;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    public DiffArtifactsResource(final GitLogResponseRepository gitLogResponseRepository, final MavenGitVersionMappingRepository mavenGitVersionMappingRepository, final MavenGitVersionCollector mavenGitVersionCollector, ReleaseArtifactRepository releaseArtifactRepository) {
        this.gitLogResponseRepository = gitLogResponseRepository;
        this.mavenGitVersionMappingRepository = mavenGitVersionMappingRepository;
        this.mavenGitVersionCollector = mavenGitVersionCollector;
        this.releaseArtifactRepository = releaseArtifactRepository;
    }

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

        return mavenGitVersionCollector.summarize(c1[0], c1[1], c1[2], c2[0], c2[1], c2[2]);
    }

    @ApiOperation("Gets a diff between release 1 and release 2")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/release-diff", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> releaseDiff(@ApiParam("name:version") @RequestParam("releaseCoordinates1") String releaseCoordinates1,
                                         @ApiParam("name:version") @RequestParam("releaseCoordinates2") String releaseCoordinates2) {
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
        List<MavenCoordinates> removed = artifact1.getMavenArtifacts().stream().filter(old -> !artifact2.getMavenArtifacts().contains(old)).collect(Collectors.toList());
        List<MavenCoordinates> added = artifact2.getMavenArtifacts().stream().filter(nw -> !artifact1.getMavenArtifacts().contains(nw)).collect(Collectors.toList());
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

        for (MavenCoordinates diff : diffs) {
            MavenCoordinates old = artifact1.getMavenArtifacts().stream().filter(mc -> mc.getGroupId().equals(diff.getGroupId()) && mc.getArtifactId().equals(diff.getArtifactId())).findFirst().get();
            MavenCoordinates nw = artifact2.getMavenArtifacts().stream().filter(mc -> mc.getGroupId().equals(diff.getGroupId()) && mc.getArtifactId().equals(diff.getArtifactId())).findFirst().get();
            GitLogSummaryResponse s = mavenGitVersionCollector.summarize(old.getGroupId(), old.getArtifactId(), old.getVersion(), nw.getGroupId(), nw.getArtifactId(), nw.getVersion());
            if (s != null) {
                summaries.add(s);
            } else {
                LOGGER.warn("No Log Summary found for: {}, {}", old, nw);
            }
        }
        ReleaseDiffResponse releaseDiffResponse = new ReleaseDiffResponse();
        releaseDiffResponse.setDiffs(summaries);
        releaseDiffResponse.setNewlyAdded(added.stream().flatMap(mc -> mavenGitVersionMappingRepository.findByMavenCoordinates(mc.getGroupId(), mc.getArtifactId(), mc.getVersion()).stream()).collect(Collectors.toList()));
        releaseDiffResponse.setRemoved(removed.stream().flatMap(mc -> mavenGitVersionMappingRepository.findByMavenCoordinates(mc.getGroupId(), mc.getArtifactId(), mc.getVersion()).stream()).collect(Collectors.toList()));
        return new ResponseEntity<>(releaseDiffResponse, HttpStatus.OK);
    }

    @ApiOperation("Gets a diff between artifact 1 and artifact 2")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/artifact-diff", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> artifactDiff(@ApiParam("groupId:artifactId:version") @RequestParam("mavenCoordinates1") String mavenCoordinates1,
                                          @ApiParam("groupId:artifactId:version") @RequestParam("mavenCoordinates2") String mavenCoordinates2) {
        String[] c1 = mavenCoordinates1.split(":");
        String[] c2 = mavenCoordinates2.split(":");
        if (c1.length < 2 || c2.length < 2) {
            throw new IllegalArgumentException("Maven coordinates must be in the format: 'groupId:artifactId:version'");
        }

        GitLogSummaryResponse diffResponse = mavenGitVersionCollector.summarize(c1[0], c1[1], c1[2], c2[0], c2[1], c2[2]);
        ReleaseDiffResponse releaseDiffResponse = new ReleaseDiffResponse();
        releaseDiffResponse.setDiffs(Arrays.asList(diffResponse));
        return new ResponseEntity<>(releaseDiffResponse, HttpStatus.OK);
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
        List<MavenCoordinates> added = artifact2.getMavenArtifacts().stream().filter(nw -> !artifact1.getMavenArtifacts().contains(nw)).collect(Collectors.toList());
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
                GitLogResponse s = mavenGitVersionCollector.diffLog(old.getGroupId(), old.getArtifactId(), old.getVersion(), nw.getGroupId(), nw.getArtifactId(), nw.getVersion());
                grand.addAll(mavenGitVersionCollector.filterByCommitters(s, committersCsv));
            }
        }
        return new ResponseEntity<>(grand, HttpStatus.OK);
    }

    @ApiOperation("Deletes the diff result collected of artifact 1 and artifact 2")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.DELETE, RequestMethod.GET})
    @RequestMapping(value = "/removediff", method = RequestMethod.DELETE, produces = "application/json")
    @CacheEvict(cacheNames = {"detailedDiffCache", "summaryDiffCache"}, key = "#p0.concat('-').concat(#p1)")
    public ResponseEntity<?> deleteDiff(@ApiParam("groupId:artifactId:version") @RequestParam("mavenCoordinates1") String mavenCoordinates1,
                                        @ApiParam("groupId:artifactId:version") @RequestParam("mavenCoordinates2") String mavenCoordinates2) {
        String[] c1 = mavenCoordinates1.split(":");
        String[] c2 = mavenCoordinates2.split(":");
        if (c1.length < 3 || c2.length < 3) {
            return new ResponseEntity<Object>("Maven coordinates must be in the format: 'groupId:artifactId:version'", HttpStatus.BAD_REQUEST);
        }
        GitLogResponse gitLogResponse = gitLogResponseRepository.findByMavenCoordinates(c1[0], c1[1], c1[2], c2[0], c2[1], c2[2]);
        mavenGitVersionCollector.deleteDiffResultTree(gitLogResponse);
        return new ResponseEntity<>(HttpStatus.OK);
    }


}
