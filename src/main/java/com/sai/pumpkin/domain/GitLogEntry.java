package com.sai.pumpkin.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.List;

/**
 * Created by saipkri on 07/03/17.
 */
@Data
public class GitLogEntry implements Serializable {
    @Id
    private String id;
    private String uuid;
    private String revision;
    private String author;
    private String dateTime;
    private String commitMessage;
    @Transient
    private List<ChangeSetEntry> changes;
    @Transient
    private MavenCoordinates mavenCoordinates;
    private List<String> changeUUIDs;

}
