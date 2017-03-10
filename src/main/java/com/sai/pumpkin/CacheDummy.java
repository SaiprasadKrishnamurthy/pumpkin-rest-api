package com.sai.pumpkin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.pumpkin.domain.MavenCoordinates;
import com.sai.pumpkin.domain.ReleaseArtifact;

import java.util.Arrays;

/**
 * Created by saipkri on 08/03/17.
 */
public class CacheDummy {
    public static void mains(String[] args) throws Exception {
        ReleaseArtifact r1 = new ReleaseArtifact();
        r1.setName("Prime Infrastructure");
        r1.setVersion("2.0");
        r1.setMavenArtifacts(Arrays.asList(new MavenCoordinates("com.cisco.wnbu", "wcs_second", "3.2.0.0.113")));
        System.out.println(new ObjectMapper().writeValueAsString(r1));


    }
}
