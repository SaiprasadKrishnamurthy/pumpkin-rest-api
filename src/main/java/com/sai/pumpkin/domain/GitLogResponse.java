package com.sai.pumpkin.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by saipkri on 07/03/17.
 */
@Data
public class GitLogResponse implements Serializable {
    @Id
    private String id;
    private MavenCoordinates from;
    private MavenCoordinates to;
    @Transient
    private List<GitLogEntry> gitLogEntries = new ArrayList<>();
    private List<String> gitLogUUIDs = new ArrayList<>();

    public List<GitLogEntry> getGitLogEntries() {
        if (gitLogEntries == null) {
            return new ArrayList<>();
        } else {
            return gitLogEntries;
        }
    }

    public List<String> getGitLogUUIDs() {
        return gitLogUUIDs == null ? new ArrayList<>() : gitLogUUIDs;
    }

}
