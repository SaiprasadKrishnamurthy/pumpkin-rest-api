package com.sai.pumpkin.repository;

import com.sai.pumpkin.domain.ArtifactConfig;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;

/**
 * Created by saipkri on 07/03/17.
 */
@Repository
public class ArtifactConfigRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Inject
    public ArtifactConfigRepositoryCustom(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void save(ArtifactConfig artifactConfig) {
        Criteria criteria = Criteria.where("repoUrl").is(artifactConfig.getRepoUrl()).and("branch").is(artifactConfig.getBranch()).and("pomPath").is(artifactConfig.getPomPath());
        mongoTemplate.remove(Query.query(criteria), ArtifactConfig.class);
        mongoTemplate.save(artifactConfig);
    }
}
