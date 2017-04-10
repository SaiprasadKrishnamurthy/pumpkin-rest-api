package com.sai.pumpkin.rest;

import com.sai.pumpkin.domain.MavenGitVersionMapping;
import com.sai.pumpkin.domain.MavenGitVersionMappingProto;
import com.sai.pumpkin.repository.MavenGitVersionMappingRepository;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class ExportResource {

    private final MavenGitVersionMappingRepository mavenGitVersionMappingRepository;

    @Inject
    public ExportResource(final MavenGitVersionMappingRepository mavenGitVersionMappingRepository) {
        this.mavenGitVersionMappingRepository = mavenGitVersionMappingRepository;
    }

    @ApiOperation("Exports the entire database into a protobuf")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/export", method = RequestMethod.GET, produces = "application/x-protobuf")
    public MavenGitVersionMappingProto.MavenGitVersionMappings export() {
        List<MavenGitVersionMapping> mavenGitVersionMappings = mavenGitVersionMappingRepository.findAll();

        MavenGitVersionMappingProto.MavenGitVersionMappings out = MavenGitVersionMappingProto.MavenGitVersionMappings.newBuilder().addAllMavenGitVersionMapping(mavenGitVersionMappings.parallelStream().map(m -> {
            MavenGitVersionMappingProto.MavenGitVersionMapping.Builder builder = MavenGitVersionMappingProto.MavenGitVersionMapping.newBuilder();
            builder.setGitRevision(m.getGitRevision())
                    .setId(m.getId())
                    .setTimestamp(m.getTimestamp())
                    .setArtifactConfig(MavenGitVersionMappingProto.ArtifactConfig.newBuilder().setBranch(m.getArtifactConfig().getBranch()).setName(m.getArtifactConfig().getName()).setPomPath(m.getArtifactConfig().getPomPath()).setRepoName(m.getArtifactConfig().getRepoName()).setRepoUrl(m.getArtifactConfig().getRepoUrl()))
                    .setMavenCoordinates(MavenGitVersionMappingProto.MavenCoordinates.newBuilder().setArtifactId(m.getMavenCoordinates().getGroupId()).setGroupId(m.getMavenCoordinates().getGroupId()).setVersion(m.getMavenCoordinates().getVersion()).setBuiltTimestamp(m.getMavenCoordinates().getBuiltTimestamp()));
            return builder.build();
        }).collect(Collectors.toList())).build();
        System.out.println(out);
        return out;
    }
}
