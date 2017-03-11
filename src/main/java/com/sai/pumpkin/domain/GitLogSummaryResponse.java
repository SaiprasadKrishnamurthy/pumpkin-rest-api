package com.sai.pumpkin.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by saipkri on 07/03/17.
 */
@Data
public class GitLogSummaryResponse implements Serializable {
    @Id
    private String id;
    private MavenGitVersionMapping from;
    private MavenGitVersionMapping to;
    private long noOfFilesChanged;
    private long noOfLinesInserted;
    private long noOfLinesDeleted;
    private Map<String, Set<ChangeSetEntry>> authorsToChangeSet = new HashMap<>();
}
