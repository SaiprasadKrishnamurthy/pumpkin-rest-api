package com.sai.pumpkin.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private long timestamp;
    private String commitMessage;

    @Transient
    private final SimpleDateFormat IN_DATE_FMT_ALT_2 = new SimpleDateFormat("EEE MMM dd HH:mm:ss YYYY Z");

    @Transient
    private List<ChangeSetEntry> changes;
    @Transient
    private MavenCoordinates mavenCoordinates;
    private List<String> changeUUIDs;

    public void setDateTime(final String dateTime) {
        this.dateTime = dateTime;
        try {
            this.timestamp = IN_DATE_FMT_ALT_2.parse(this.dateTime).getTime();
        } catch (ParseException ignore) {
            ignore.printStackTrace();
        }
    }

}
