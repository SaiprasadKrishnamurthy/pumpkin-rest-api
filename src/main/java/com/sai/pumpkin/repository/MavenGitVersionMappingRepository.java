package com.sai.pumpkin.repository;

import com.sai.pumpkin.domain.MavenGitVersionMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * Created by saipkri on 07/03/17.
 */

public interface MavenGitVersionMappingRepository extends MongoRepository<MavenGitVersionMapping, String> {

    @Query("{ 'artifactConfig.name' : ?0 }")
    List<MavenGitVersionMapping> findByArtifactConfigName(String artifactConfigName);

    @Query("{$and: [{'mavenCoordinates.groupId' : ?0} , {'mavenCoordinates.artifactId' : ?1} , {'mavenCoordinates.version' : ?2}]}")
    List<MavenGitVersionMapping> findByMavenCoordinates(String groupId, String artifactId, String version);

    @Query("{$and: [{'mavenCoordinates.groupId' : ?0} , {'mavenCoordinates.artifactId' : ?1} , {'mavenCoordinates.version' : ?2}, {'timestamp': ?3}]}")
    List<MavenGitVersionMapping> findByMavenCoordinates(String groupId, String artifactId, String version, long timestamp);

    @Query("{$and: [{'mavenCoordinates.groupId' : ?0} , {'mavenCoordinates.artifactId' : ?1}]}")
    List<MavenGitVersionMapping> findByMavenCoordinates(String groupId, String artifactId);

    @Query("{$and: [{'mavenCoordinates.artifactId' : {'$regex': '?0'}}]}")
    List<MavenGitVersionMapping> findByMavenCoordinates(String artifactId);

    @Query("{$and: [{'timestamp' : {'$gte': ?0}}]}")
    List<MavenGitVersionMapping> findGreaterThanTimestamp(long timestamp);

    @Query("{$and: [{'timestamp' : {'$lt': ?0}}]}")
    List<MavenGitVersionMapping> findLesserThanTimestamp(long timestamp);

    @Query("{$and: [{'gitRevision' : '?0'}, {'artifactConfig.name' : '?1'}]}")
    MavenGitVersionMapping findByGitRevision(String rev, String artifactConfigName);
}
