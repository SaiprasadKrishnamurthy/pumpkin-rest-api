package com.sai.pumpkin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by saipkri on 09/03/17.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ViewSourceResponse {
    private String fileName;
    private String gitRevision;
    private String source;
}
