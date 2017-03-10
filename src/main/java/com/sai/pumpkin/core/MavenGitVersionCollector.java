package com.sai.pumpkin.core;

import com.sai.pumpkin.domain.*;
import com.sai.pumpkin.repository.ChangeSetEntryRepository;
import com.sai.pumpkin.repository.GitLogEntryRepository;
import com.sai.pumpkin.repository.GitLogResponseRepository;
import com.sai.pumpkin.repository.MavenGitVersionMappingRepository;
import com.sai.pumpkin.utils.GitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.inject.Inject;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by saipkri on 08/03/17.
 */
@Component
public class MavenGitVersionCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenGitVersionCollector.class);

    @Value("${localGitWorkspace}")
    private String localGitWorkspace;

    private final MongoTemplate mongoTemplate;
    private final GitLogEntryRepository gitLogEntryRepository;
    private final ChangeSetEntryRepository changeSetEntryRepository;
    private final GitLogResponseRepository gitLogResponseRepository;
    private final MavenGitVersionMappingRepository mavenGitVersionMappingRepository;


    @Inject
    public MavenGitVersionCollector(final MongoTemplate mongoTemplate, final GitLogEntryRepository gitLogEntryRepository, final ChangeSetEntryRepository changeSetEntryRepository, final GitLogResponseRepository gitLogResponseRepository, MavenGitVersionMappingRepository mavenGitVersionMappingRepository) {
        this.mongoTemplate = mongoTemplate;
        this.gitLogEntryRepository = gitLogEntryRepository;
        this.changeSetEntryRepository = changeSetEntryRepository;
        this.gitLogResponseRepository = gitLogResponseRepository;
        this.mavenGitVersionMappingRepository = mavenGitVersionMappingRepository;
    }

    public void collect(ArtifactConfig config) {
        try {
            Consumer<MavenGitVersionMapping> saveOrUpdateFunction = (mapping) -> {
                Criteria criteria = Criteria.where("mavenCoordinates.groupId").is(mapping.getMavenCoordinates().getGroupId())
                        .and("mavenCoordinates.artifactId").is(mapping.getMavenCoordinates().getArtifactId())
                        .and("mavenCoordinates.version").is(mapping.getMavenCoordinates().getVersion());
                mongoTemplate.remove(Query.query(criteria), MavenGitVersionMapping.class);
                mongoTemplate.save(mapping);
                LOGGER.info("Saved: "+mapping );
            };
            GitUtils.collectFromLog(localGitWorkspace, config, saveOrUpdateFunction);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Cacheable(cacheNames = "detailedDiffCache", key = "#p0.concat('detailedDiffCache').concat(#p1).concat(#p2).concat(#p3).concat(#p4).concat(#p5)")
    public GitLogResponse diffLog(final String g1, final String a1, final String v1, final String g2, final String a2, final String v2) {
        MavenGitVersionMapping m1 = mavenGitVersionMappingRepository.findByMavenCoordinates(g1, a1, v1);
        MavenGitVersionMapping m2 = mavenGitVersionMappingRepository.findByMavenCoordinates(g2, a2, v2);

        StopWatch clock = new StopWatch();
        GitLogResponse gitLogResponse = null;
        try {
            clock.start();
            gitLogResponse = diffLogPreComputed(m1, m2);
            if (gitLogResponse == null) {
                gitLogResponse = GitUtils.gitLogResponse(localGitWorkspace, m1, m2);
                List<String> gitLogEntryUuids = new ArrayList<>();
                List<GitLogEntry> entries = gitLogResponse.getGitLogEntries();

                for (GitLogEntry entry : entries) {
                    entry.setUuid(UUID.randomUUID().toString());
                    gitLogEntryUuids.add(entry.getUuid());
                    List<String> changesUuids = entry.getChanges().stream().map(changeSetEntry -> {
                        String uuid = UUID.randomUUID().toString();
                        changeSetEntry.setUuid(uuid);
                        mongoTemplate.save(changeSetEntry);
                        return uuid;
                    }).collect(Collectors.toList());
                    entry.setChangeUUIDs(changesUuids);
                    mongoTemplate.save(entry);
                }
                gitLogResponse.setGitLogUUIDs(gitLogEntryUuids);
                mongoTemplate.save(gitLogResponse);
                clock.stop();
                LOGGER.info("Time taken by the Difference collector engine to COMPUTE the diff between {} and {} : {} seconds", m1.getMavenCoordinates(), m2.getMavenCoordinates(), clock.getTotalTimeSeconds() + " seconds");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return gitLogResponse;
    }

    public GitLogResponse diffLogPreComputed(final MavenGitVersionMapping m1, final MavenGitVersionMapping m2) {
        StopWatch clock = new StopWatch();
        GitLogResponse gitLogResponse = null;

        try {
            clock.start();
            gitLogResponse = gitLogResponseRepository.findByMavenCoordinates(m1.getMavenCoordinates().getGroupId(), m1.getMavenCoordinates().getArtifactId(), m1.getMavenCoordinates().getVersion(),
                    m2.getMavenCoordinates().getGroupId(), m2.getMavenCoordinates().getArtifactId(), m2.getMavenCoordinates().getVersion());
            if (gitLogResponse == null) {
                return null;
            }
            gitLogResponse.setGitLogEntries(new ArrayList<>());

            for (String entry : gitLogResponse.getGitLogUUIDs()) {
                GitLogEntry gitLogEntry = gitLogEntryRepository.findByUUID(entry);
                gitLogEntry.setChanges(new ArrayList<>());
                gitLogResponse.getGitLogEntries().add(gitLogEntry);

                for (String changesetEntry : gitLogEntry.getChangeUUIDs()) {
                    ChangeSetEntry changeSetEntry = changeSetEntryRepository.findByUUID(changesetEntry);
                    gitLogEntry.getChanges().add(changeSetEntry);
                }
            }
            clock.stop();
            LOGGER.info("Time taken by the Difference collector engine to GET the diff between {} and {} : {} seconds", m1.getMavenCoordinates(), m2.getMavenCoordinates(), clock.getTotalTimeSeconds() + " seconds");
            return gitLogResponse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteDiffResultTree(final GitLogResponse gitLogResponse) {
        gitLogResponse.getGitLogUUIDs().stream().forEach(gitLogUUID -> {
            GitLogEntry gitLogEntry = gitLogEntryRepository.findByUUID(gitLogUUID);
            gitLogEntry.getChangeUUIDs().stream().forEach(changeSetUUID -> {
                ChangeSetEntry changeSetEntry = changeSetEntryRepository.findByUUID(changeSetUUID);
                mongoTemplate.remove(changeSetEntry);
            });
            mongoTemplate.remove(gitLogEntry);
        });
        mongoTemplate.remove(gitLogResponse);
    }

    @Cacheable(cacheNames = "summaryDiffCache", key = "#p0.concat('summaryDiffCache').concat(#p1).concat(#p2).concat(#p3).concat(#p4).concat(#p5)")
    public GitLogSummaryResponse summarize(final String g1, final String a1, final String v1, final String g2, final String a2, final String v2) {
        MavenGitVersionMapping m1 = mavenGitVersionMappingRepository.findByMavenCoordinates(g1, a1, v1);
        MavenGitVersionMapping m2 = mavenGitVersionMappingRepository.findByMavenCoordinates(g2, a2, v2);

        StopWatch clock = new StopWatch();
        clock.start();
        GitLogResponse gitLogResponse = diffLog(g1, a1, v1, g2, a2, v2);
        GitLogSummaryResponse summaryResponse = new GitLogSummaryResponse();
        summaryResponse.setFrom(m1);
        summaryResponse.setTo(m2);

        gitLogResponse.getGitLogEntries().forEach(gle -> {
            String author = gle.getAuthor().trim();

            gle.getChanges().forEach(cse -> {
                summaryResponse.getAuthorsToChangeSet().compute(author, (k, v) -> {
                    if (v == null) {
                        return new HashSet<>();
                    } else {
                        v.add(cse);
                        return v;
                    }
                });
            });
        });
        clock.stop();
        LOGGER.info("Time taken by the Difference collector engine to COMPUTE the diff SUMMARY between {} and {} : {} seconds", m1.getMavenCoordinates(), m2.getMavenCoordinates(), clock.getTotalTimeSeconds() + " seconds");
        return summaryResponse;
    }

    public ViewSourceResponse source(ArtifactConfig artifactConfig, String filePath, String gitRevision) {
        try {

            String localRepo = localGitWorkspace + File.separator + artifactConfig.getRepoName() + File.separator;
            String source = GitUtils.gitShowFile(localRepo, filePath, gitRevision);
            return new ViewSourceResponse(filePath, gitRevision, source);
        } catch (Exception ex) {
            LOGGER.error("Error while viewing source", ex);
            throw new RuntimeException(ex);
        }
    }

    public Map<String, String> diff(ArtifactConfig artifactConfig, String filePath, String from, String to) {
        try {
            String localRepo = localGitWorkspace + File.separator + artifactConfig.getRepoName() + File.separator;
            String html = GitUtils.diff(localRepo.trim(), filePath.trim(), from.trim(), to.trim());
            Map<String, String> resp = new HashMap<>();
            resp.put("diff", html);
            return resp;
        } catch (Exception ex) {
            LOGGER.error("Error while viewing source", ex);
            throw new RuntimeException(ex);
        }
    }

    public List<GitLogEntry> filterByCommitters(GitLogResponse s, String committersCsv) {
        List<GitLogEntry> gitLogEntries = new ArrayList<>();
        for (GitLogEntry e : s.getGitLogEntries()) {
            committersCsv = committersCsv.toLowerCase().trim().replace(" ", "");
            String author = e.getAuthor().toLowerCase().trim().replace(" ", "").replace("\t", "").replaceAll("\\P{Alnum}", "");
            if (committersCsv.contains(author)) {
                e.setMavenCoordinates(s.getTo());
                gitLogEntries.add(e);
            }
        }
        return gitLogEntries;
    }

    public Map<String, String> bulkDiff(ArtifactConfig artifactConfig, String gitSha) {
        try {
            String localRepo = localGitWorkspace + File.separator + artifactConfig.getRepoName() + File.separator;
            String html = GitUtils.bulkDiff(localRepo.trim(), gitSha);
            Map<String, String> resp = new HashMap<>();
            resp.put("diff", html);
            return resp;
        } catch (Exception ex) {
            LOGGER.error("Error while viewing source", ex);
            throw new RuntimeException(ex);
        }
    }
}
