package com.sai.pumpkin.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by saipkri on 18/03/17.
 */
@Data
public class ReleaseMetadata implements Serializable{
    private String releaseName;
    private String version;
    private List<ArtifactCollection> artifacts = new ArrayList<>();
}
