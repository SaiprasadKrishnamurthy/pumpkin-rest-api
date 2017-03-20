package com.sai.pumpkin.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by saipkri on 19/03/17.
 */
@Data
@AllArgsConstructor
public class FailedExpectation extends RuntimeException {
    private String expected;
    private String actual;

    public String toString() {
        return String.format("Failed expectation: Expected: %s\t Actual: %s", expected, actual);
    }

}
