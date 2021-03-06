package com.sai.pumpkin.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.pumpkin.domain.ReleaseExpectation;
import com.sai.pumpkin.stepdefs.ReleaseCheckStepDefs;
import cucumber.api.cli.Main;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.stream.Collectors.joining;

/**
 * Created by saipkri on 19/03/17.
 */
public class CucumberUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CucumberUtils.class);

    public static String runFeature(String testName, String featureFileContents) throws Exception {
        LOGGER.info("Test name: {}, \n {}", testName, featureFileContents);
        String outHtmlDir = System.getProperty("java.io.tmpdir") + File.separator + "pumpkin";
        LOGGER.info("Dir: {}", outHtmlDir);
        FileUtils.forceMkdir(new File(outHtmlDir));
        FileUtils.write(new File(outHtmlDir + File.separator + testName + ".feature"), featureFileContents);
        LOGGER.info("{}", Files.list(Paths.get(new File(outHtmlDir).getAbsolutePath())).map(Path::toString).collect(joining("\n")));
        Main.run(new String[]{outHtmlDir, "-p", "html:" + outHtmlDir, "-g", ReleaseCheckStepDefs.class.getPackage().getName()}, Thread.currentThread().getContextClassLoader());
        LOGGER.info("{}", Files.list(Paths.get(new File(outHtmlDir).getAbsolutePath())).map(Path::toString).collect(joining("\n")));
        String templateContents = IOUtils.toString(CucumberUtils.class.getClassLoader().getResourceAsStream("test_report_template.html"));
        String reportJscontents = IOUtils.toString(new FileInputStream(outHtmlDir + File.separator + "report.js"));
        templateContents = templateContents.replace("REPORT_JS", reportJscontents);
        return templateContents;
    }

    public static void mains(String[] args) throws Exception {
        String testName = "aaa";
        String contents = IOUtils.toString(CucumberUtils.class.getClassLoader().getResourceAsStream("release_check_feature.feature"));
        System.out.println(runFeature(testName, contents));

        ReleaseExpectation r = new ReleaseExpectation();
        r.setFeatureText(contents);
        r.setName(testName);
        r.setFailureSlackWebhook("http://slack.com/webhook");
        System.out.println(new ObjectMapper().writeValueAsString(r));
    }
}
