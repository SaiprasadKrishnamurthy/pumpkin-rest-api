package com.sai.pumpkin.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;

/**
 * Created by saipkri on 20/03/17.
 */
@Data
public class ReleaseExpectationResult {
    @Id
    private String id;
    private String testName;
    private long executionDateTime;
    private String htmlReport;
    private boolean failure;
}
