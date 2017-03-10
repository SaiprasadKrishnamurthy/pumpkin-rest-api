package com.sai.pumpkin.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.pumpkin.core.MavenGitVersionCollector;
import com.sai.pumpkin.domain.ArtifactConfig;
import com.sai.pumpkin.domain.ViewSourceResponse;
import com.sai.pumpkin.repository.GitLogResponseRepository;
import com.sai.pumpkin.repository.MavenGitVersionMappingRepository;
import com.sai.pumpkin.repository.ReleaseArtifactRepository;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Map;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class SourceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceResource.class);

    private final GitLogResponseRepository gitLogResponseRepository;
    private final MavenGitVersionMappingRepository mavenGitVersionMappingRepository;
    private final MavenGitVersionCollector mavenGitVersionCollector;
    private final ReleaseArtifactRepository releaseArtifactRepository;
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    public SourceResource(final GitLogResponseRepository gitLogResponseRepository, final MavenGitVersionMappingRepository mavenGitVersionMappingRepository, final MavenGitVersionCollector mavenGitVersionCollector, ReleaseArtifactRepository releaseArtifactRepository) {
        this.gitLogResponseRepository = gitLogResponseRepository;
        this.mavenGitVersionMappingRepository = mavenGitVersionMappingRepository;
        this.mavenGitVersionCollector = mavenGitVersionCollector;
        this.releaseArtifactRepository = releaseArtifactRepository;
    }

    @ApiOperation("Gets the source of a file for a specific version coordinates")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/source", method = RequestMethod.GET, produces = "application/json")
    public ViewSourceResponse source(@ApiParam("filePath") @RequestParam("filePath") String filePath,
                                     @ApiParam("groupId") @RequestParam("groupId") String groupId,
                                     @ApiParam("artifactId") @RequestParam("artifactId") String artifactId,
                                     @ApiParam("gitRevision") @RequestParam("gitRevision") String gitRevision
    ) {

        ArtifactConfig artifactConfig = mavenGitVersionMappingRepository.findByMavenCoordinates(groupId, artifactId).get(0).getArtifactConfig();
        return mavenGitVersionCollector.source(artifactConfig, filePath, gitRevision);
    }

    @ApiOperation("Gets the diff source between 2 versions of a file")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/diffSource", method = RequestMethod.GET, produces = "application/json")
    public Map<String, String> diff(@ApiParam("filePath") @RequestParam("filePath") String filePath,
                                    @ApiParam("groupId") @RequestParam("groupId") String groupId,
                                    @ApiParam("artifactId") @RequestParam("artifactId") String artifactId,
                                    @ApiParam("gitRevisionFrom") @RequestParam("gitRevisionFrom") String from,
                                    @ApiParam("gitRevisionTo") @RequestParam("gitRevisionTo") String to
    ) {

        ArtifactConfig artifactConfig = mavenGitVersionMappingRepository.findByMavenCoordinates(groupId, artifactId).get(0).getArtifactConfig();
        return mavenGitVersionCollector.diff(artifactConfig, filePath, from, to);
    }

    @ApiOperation("Gets the diff source between a git revision and it's previous")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/bulkDiff", method = RequestMethod.GET, produces = "application/json")
    public Map<String, String> bulkDiff(
                                    @ApiParam("groupId") @RequestParam("groupId") String groupId,
                                    @ApiParam("artifactId") @RequestParam("artifactId") String artifactId,
                                    @ApiParam("gitRevision") @RequestParam("gitRevision") String gitRevision) {

        ArtifactConfig artifactConfig = mavenGitVersionMappingRepository.findByMavenCoordinates(groupId, artifactId).get(0).getArtifactConfig();
        return mavenGitVersionCollector.bulkDiff(artifactConfig, gitRevision.replace("\"",""));
    }
}
