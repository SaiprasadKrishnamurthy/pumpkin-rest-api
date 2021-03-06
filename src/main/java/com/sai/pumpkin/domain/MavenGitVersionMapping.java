package com.sai.pumpkin.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.List;

/**
 * Created by saipkri on 07/03/17.
 */
@Data
public class MavenGitVersionMapping implements Serializable {
    @Id
    public String id;
    private ArtifactConfig artifactConfig;
    private MavenCoordinates mavenCoordinates;
    private String gitRevision;
    private long timestamp;

    public String getGitRevision() {
        return gitRevision.trim().replace("\n","");
    }
}
