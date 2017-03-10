package com.sai.pumpkin.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.List;

/**
 * Created by saipkri on 08/03/17.
 */
@Data
public class ReleaseArtifact implements Serializable {
    @Id
    private String id;
    private String name;
    private String version;
    private List<MavenCoordinates> mavenArtifacts;
}
