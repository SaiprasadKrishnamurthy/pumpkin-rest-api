package com.sai.pumpkin.domain;

import lombok.Data;

import java.util.List;

/**
 * Created by saipkri on 10/04/17.
 */
@Data
public class Team {
    private String id;
    private String name;
    private TeamMember lead;
    private List<TeamMember> members;
}
