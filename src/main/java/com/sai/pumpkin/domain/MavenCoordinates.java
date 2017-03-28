package com.sai.pumpkin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by saipkri on 07/03/17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"version", "builtTimestamp"})
public class MavenCoordinates implements Serializable {
    private String groupId;
    private String artifactId;
    private String version;
    private long builtTimestamp = -1;

    public String shortString() {
        return groupId + ":" + artifactId;
    }

    @Override
    public String toString() {
        return "[" + groupId + ":" + artifactId + ":" + version + "]";
    }
}
