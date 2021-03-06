package com.sai.pumpkin.repository;

import com.sai.pumpkin.domain.GitLogResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * Created by saipkri on 07/03/17.
 */

public interface GitLogResponseRepository extends MongoRepository<GitLogResponse, String> {
    @Query("{$and: [{'from.groupId' : ?0} , {'from.artifactId' : ?1} , {'from.version' : ?2}, {'to.groupId' : ?3} , {'to.artifactId' : ?4} , {'to.version' : ?5}]}")
    GitLogResponse findByMavenCoordinates(String groupId1, String artifactId1, String version1, String groupId2, String artifactId2, String version2);

    @Query("{$and: [{'from.groupId' : ?0} , {'from.artifactId' : ?1} , {'from.version' : ?2}, {'from.builtTimestamp' : ?3}, {'to.groupId' : ?4} , {'to.artifactId' : ?5} , {'to.version' : ?6}, {'to.builtTimestamp' : ?7}]}")
    GitLogResponse findByMavenCoordinates(String groupId1, String artifactId1, String version1, long timestamp1, String groupId2, String artifactId2, String version2, long timestamp2);
}
