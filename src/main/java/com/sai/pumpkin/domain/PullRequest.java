package com.sai.pumpkin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.List;

/**
 * Created by saipkri on 23/03/17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PullRequest {
    @Id
    private String id;
    private int number;
    private String title;
    private long closedDate;
    private String mergedInto;
    private String author;
    private List<String> approverNames;
}
