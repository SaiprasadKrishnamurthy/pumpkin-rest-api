package com.sai.pumpkin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.List;

/**
 * Created by saipkri on 23/03/17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PullRequest implements Serializable {
    @Id
    private String id;
    private int number;
    private String title;
    private long closedDate;
    private String mergedInto;
    private String author;
    private List<String> approverNames;
    // https://bitbucket-eng-sjc1.cisco.com/bitbucket/projects/CVGPI/repos/wireless/pull-requests/5900 ==> links.self
    private String url;
}
