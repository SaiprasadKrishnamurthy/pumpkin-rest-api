package com.sai.pumpkin.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by saipkri on 07/03/17.
 */
@Data
public class DiffArtifactsRequest implements Serializable {
    private MavenCoordinates from;
    private MavenCoordinates to;
}
