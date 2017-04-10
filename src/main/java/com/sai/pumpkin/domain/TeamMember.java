package com.sai.pumpkin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by saipkri on 10/04/17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamMember {
    private String commitName;
    private String fullName;
    private String id;
    private double locationLat = 12.903288;
    private double locationLong = 80.218175;
}
