package com.sai.pumpkin.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by saipkri on 26/03/17.
 */
@Data
public class CollectionJob implements Serializable{
    private String id;
    private String configName;
    private long startTime;
    private long endTime;
    private long totalTime;
}
