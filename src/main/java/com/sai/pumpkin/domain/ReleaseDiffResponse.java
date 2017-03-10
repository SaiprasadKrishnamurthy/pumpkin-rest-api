package com.sai.pumpkin.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by saipkri on 08/03/17.
 */
@Data
public class ReleaseDiffResponse implements Serializable {
    private List<GitLogSummaryResponse> diffs = new ArrayList<>();
    private List<MavenGitVersionMapping> newlyAdded = new ArrayList<>();
    private List<MavenGitVersionMapping> removed = new ArrayList<>();
}
