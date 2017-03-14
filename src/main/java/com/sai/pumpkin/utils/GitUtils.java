package com.sai.pumpkin.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.pumpkin.domain.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Created by saipkri on 07/03/17.
 */
public class GitUtils {
    private static final String WORKSPACE = System.getProperty("user.home") + File.separator + "pumpkin_ws";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitUtils.class);

    public static void mains(String[] args) throws Exception {
        String repoName = "wireless";
        String repoPath = "ssh://git@bitbucket-eng-sjc1.cisco.com:7999/cvgpi/wireless.git";
        String pomPath = "rfm/pom.xml";
        String branch = "PI_MAUI";

        ArtifactConfig artifactConfig = new ArtifactConfig();
        artifactConfig.setName("RFM");
        artifactConfig.setRepoName("wireless");
        artifactConfig.setRepoUrl("ssh://git@bitbucket-eng-sjc1.cisco.com:7999/cvgpi/wireless.git");
        artifactConfig.setPomPath("rfm/pom.xml");
        artifactConfig.setBranch("PI_MAUI");

        System.out.println(new ObjectMapper().writeValueAsString(artifactConfig));

    }

    public static void collectFromLog(final String localGitWorkspace, final ArtifactConfig artifactConfig, final Consumer<MavenGitVersionMapping> consumer) throws Exception {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss Z");
        String localRepo = localGitWorkspace + File.separator + artifactConfig.getRepoName() + File.separator;
        new File(localRepo).mkdirs();
        gitClone(artifactConfig.getRepoUrl(), localRepo);
        LOGGER.info(" Before commit sha ");
        List<String> revisions = gitLogCommitSHAs(localRepo, artifactConfig.getPomPath(), artifactConfig.getBranch());
        LOGGER.info(" After commit sha " + revisions);

        for (String rev : revisions) {
            try {
                String tokens[] = rev.split("\\|");
                rev = tokens[0].trim();
                long commitDateTime = fmt.parse(tokens[1].trim()).getTime();

                String pom = gitShowFile(localRepo, artifactConfig.getPomPath(), rev);
                LOGGER.info("\t Pom retrieved successfully");

                String[] gav = PomUtils.gidAidVersionArray(pom);
                LOGGER.info("{} --> {} ", Arrays.deepToString(gav), rev);
//                if (!gav[2].contains("SNAPSHOT")) {
                MavenCoordinates mavenCoordinates = new MavenCoordinates(gav[0], gav[1], gav[2], commitDateTime);
                MavenGitVersionMapping mavenGitVersionMapping = new MavenGitVersionMapping();
                mavenGitVersionMapping.setArtifactConfig(artifactConfig);
                mavenGitVersionMapping.setGitRevision(rev);
                mavenGitVersionMapping.setMavenCoordinates(mavenCoordinates);
                mavenGitVersionMapping.setTimestamp(commitDateTime);
                consumer.accept(mavenGitVersionMapping);
//                }
            } catch (Exception ignore) {
                LOGGER.error("Error during collection for" + artifactConfig + " Git bersion: " + rev, ignore);
            }
        }
    }

    public static GitLogResponse gitLogResponse(final String localGitWorkspace, final MavenGitVersionMapping artifact1, final MavenGitVersionMapping artifact2) throws Exception {
        String localRepo = localGitWorkspace + File.separator + artifact1.getArtifactConfig().getRepoName() + File.separator;
        GitLogResponse resp = new GitLogResponse();
        GitLogEntry curr = null;
        List<GitLogEntry> entries = new ArrayList<>();
        List<ChangeSetEntry> currEntries = null;
        String output = new ProcessExecutor().command("git", "--git-dir=" + localRepo + File.separator + ".git", "log", "--name-status", "--pretty=format:\"%h###%an###%ad###%s\"", artifact1.getGitRevision() + ".." + artifact2.getGitRevision())
                .readOutput(true)
                .execute()
                .outputString();
        StringTokenizer tokenizer = new StringTokenizer(output, "\n");
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if (line.contains("###") && line.indexOf("###") != line.lastIndexOf("###")) {
                String[] tokens = line.split("###");
                if (tokens.length == 4) {
                    String revision = tokens[0];
                    String author = tokens[1].replace(" ", "").replace("\\.", "");
                    String dateTime = tokens[2];
                    String message = tokens[3];
                    curr = new GitLogEntry();
                    curr.setRevision(revision);
                    curr.setAuthor(author);
                    curr.setDateTime(dateTime);
                    curr.setCommitMessage(message);
                    currEntries = new ArrayList<>();
                    curr.setChanges(currEntries);
                    entries.add(curr);
                }
            } else if ((line.startsWith("A") || line.startsWith("M") || line.startsWith("D")) && currEntries != null) {
                String tokens[] = line.split("\t");
                List<String> filtered = Stream.of(tokens).filter(s -> StringUtils.isNotBlank(s.trim())).collect(toList());
                ChangeSetEntry entry = new ChangeSetEntry();
                entry.setChangeType(filtered.get(0));
                entry.setFilePath(filtered.get(1));
                if (filtered.get(1).startsWith(artifact1.getArtifactConfig().moduleDir() + File.separator)) {
                    currEntries.add(entry);
                }
            }
        }

        // Filter here.
        List<GitLogEntry> filtered = entries.stream().filter(gitLogEntry -> gitLogEntry.getChanges()
                .stream()
                .filter(cse -> cse.getFilePath().startsWith(artifact1.getArtifactConfig().moduleDir() + File.separator)).count() > 0)
                .collect(Collectors.toList());

        resp.setFrom(artifact1.getMavenCoordinates());
        resp.setTo(artifact2.getMavenCoordinates());
        resp.setGitLogEntries(filtered);
        return resp;

    }

    public static String gitShowFile(String localRepo, String filePath, String revision) throws Exception {
        return new ProcessExecutor().command("git", "--git-dir=" + localRepo + File.separator + ".git", "show", revision + ":" + filePath)
                .readOutput(true)
                .execute()
                .outputString();
    }

    private static List<String> gitLogCommitSHAs(String localRepo, String filePath, String branch) throws Exception {
        String baseDir = filePath.substring(0, filePath.lastIndexOf("/"));
        return Arrays.asList(new ProcessExecutor().command("git", "--git-dir=" + localRepo + File.separator + ".git", "log", "--date=iso", "--reverse", "--format=format:%H|%ad", branch, "--follow", "--", baseDir)
                .readOutput(true)
                .execute()
                .outputString()
                .split("\n"));
    }


    private static void gitClone(String repoPath, String localRepo) throws IOException, InterruptedException, TimeoutException {
        int exit = new ProcessExecutor().command("git", "clone", repoPath, localRepo)
                .redirectOutput(new LogOutputStream() {
                    @Override
                    protected void processLine(String line) {
                        System.out.println(line);
                    }
                })
                .execute()
                .getExitValue();
        if (exit > 0) {
            new ProcessExecutor().command("git", "--git-dir=" + localRepo + File.separator + ".git", "fetch")
                    .redirectOutput(new LogOutputStream() {
                        @Override
                        protected void processLine(String line) {
                            System.out.println(line);
                        }
                    })
                    .execute()
                    .getExitValue();
        }
    }

    public static String diff(String localRepo, String filePath, String from, String to) throws Exception {
        return new ProcessExecutor()
                .directory(new File(localRepo))
                .command("diff2html", "-o", "stdout", "-s", "line", "-f", "html", "-i", "command", "-o", "preview", "--", "-M", from + ".." + to, filePath)
                .readOutput(true)
                .execute()
                .outputString();
    }

    public static String bulkDiff(String localRepo, String gitSha) throws Exception {
        return new ProcessExecutor()
                .directory(new File(localRepo))
                .command("diff2html", "-o", "stdout", "-s", "line", "-f", "html", "-i", "command", "-o", "preview", "--", "-M", gitSha + "^", gitSha)
                .readOutput(true)
                .execute()
                .outputString();
    }

    public static String linesStat(String localRepo, String gitRevision, String gitRevision1) throws Exception {
        return new ProcessExecutor().command("git", "--git-dir=" + localRepo + File.separator + ".git", "diff", "--shortstat", gitRevision, gitRevision1)
                .readOutput(true)
                .execute()
                .outputString();
    }
}
