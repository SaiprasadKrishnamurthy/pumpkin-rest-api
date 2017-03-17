package com.sai.pumpkin.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by saipkri on 18/03/17.
 */
@Data
public class ArtifactCollection implements Serializable {
    private MavenCoordinates mavenCoordinates;
    private ArtifactCollectionStatusType status;
}
