package com.sai.pumpkin.rest;

import com.sai.pumpkin.domain.ArtifactConfig;
import com.sai.pumpkin.repository.ArtifactConfigRepository;
import com.sai.pumpkin.repository.ArtifactConfigRepositoryCustom;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class ArtifactConfigResource {

    private final ArtifactConfigRepository artifactConfigRepository;
    private final ArtifactConfigRepositoryCustom artifactConfigRepositoryCustom;

    @Inject
    public ArtifactConfigResource(final ArtifactConfigRepository artifactConfigRepository, final ArtifactConfigRepositoryCustom artifactConfigRepositoryCustom) {
        this.artifactConfigRepository = artifactConfigRepository;
        this.artifactConfigRepositoryCustom = artifactConfigRepositoryCustom;
    }

    @ApiOperation("Finds all configs")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/configs", method = RequestMethod.GET, produces = "application/json")
    public List<ArtifactConfig> configs() {
        return artifactConfigRepository.findAll();
    }

    @ApiOperation("Saves config")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/configs", method = RequestMethod.PUT, produces = "application/json")
    public void save(@RequestBody ArtifactConfig artifactConfig) {
        artifactConfigRepositoryCustom.save(artifactConfig);
    }

    @ApiOperation("Saves config")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/all-configs", method = RequestMethod.PUT, produces = "application/json")
    public void saveAll(@RequestBody List<ArtifactConfig> artifactConfigs) {
        artifactConfigs.forEach(artifactConfigRepository::save);
    }
}
