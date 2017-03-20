package com.sai.pumpkin.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

/**
 * Created by saipkri on 07/03/17.
 */
@Data
public class ReleaseExpectation implements Serializable {
    @Id
    public String id;
    private String name;
    private String featureText;
    private String failureSlackWebhook;
}
