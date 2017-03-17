package com.sai.pumpkin.domain;

import lombok.Data;

/**
 * Created by saipkri on 18/03/17.
 */
@Data
public class ArtifactCollection {
    private MavenCoordinates mavenCoordinates;
    private ArtifactCollectionStatusType status;
}
