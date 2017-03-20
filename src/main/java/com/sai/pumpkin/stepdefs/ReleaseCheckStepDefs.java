package com.sai.pumpkin.stepdefs;

import com.sai.pumpkin.ApplicationContextProvider;
import com.sai.pumpkin.domain.FailedExpectation;
import com.sai.pumpkin.domain.MavenCoordinates;
import com.sai.pumpkin.domain.ReleaseArtifact;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Created by saipkri on 19/03/17.
 */
public class ReleaseCheckStepDefs {
    private final ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
    private ReleaseArtifact latestRelease;

    @Given("^The latest Release Package$")
    public void aReleasePackage() throws Throwable {
        MongoTemplate mongoTemplate = applicationContext.getBean(MongoTemplate.class);
        List<ReleaseArtifact> all = mongoTemplate.findAll(ReleaseArtifact.class);
        latestRelease = all.get(all.size() - 1);
    }

    @When("^installed$")
    public void installed() throws Throwable {

    }

    @Then("^the version of maven artifact \"([^\"]*)\" should be \"([^\"]*)\"$")
    public void theVersionOfMavenArtifactShouldBe(String gidAid, String version) throws Throwable {
        List<MavenCoordinates> mavenCoordinates = latestRelease.getMavenArtifacts().stream().filter(mc ->
                mc.getGroupId().equals(gidAid.split(":")[0]) && mc.getArtifactId().equals(gidAid.split(":")[1]))
                .collect(toList());
        if (mavenCoordinates.isEmpty()) {
            throw new FailedExpectation(version, String.format("No artifact found for the groupId: %s , artifactId: %s", gidAid.split(":")[0], gidAid.split(":")[1]));
        }
        if (!mavenCoordinates.stream().anyMatch(mc -> mc.getVersion().equals(version))) {
            throw new FailedExpectation(version, mavenCoordinates.stream().map(mc -> mc.getVersion()).collect(joining(",")));
        }
    }

    @And("^the version of maven artifact \"([^\"]*)\" should be atleast \"([^\"]*)\"$")
    public void theVersionOfMavenArtifactShouldBeGreaterThan(String gidAid, String version) throws Throwable {
        List<MavenCoordinates> mavenCoordinates = latestRelease.getMavenArtifacts().stream().filter(mc ->
                mc.getGroupId().equals(gidAid.split(":")[0]) && mc.getArtifactId().equals(gidAid.split(":")[1]))
                .collect(toList());
        if (mavenCoordinates.isEmpty()) {
            throw new FailedExpectation(version, String.format("No artifact found for the groupId: %s , artifactId: %s", gidAid.split(":")[0], gidAid.split(":")[1]));
        }
        if (!mavenCoordinates.stream().anyMatch(mc -> toNumeric(mc.getVersion()) >= toNumeric(version))) {
            throw new FailedExpectation("Greater than: " + version, mavenCoordinates.stream().map(mc -> mc.getVersion()).collect(joining(",")));
        }
    }

    private long toNumeric(String version) {
        version = version.trim().replace("_", "").replace("-", "").replace("SNAPSHOT", "");
        return Long.parseLong(version) * 100000000;
    }
}
