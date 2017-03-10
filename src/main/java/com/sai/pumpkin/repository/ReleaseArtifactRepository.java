package com.sai.pumpkin.repository;

import com.sai.pumpkin.domain.ReleaseArtifact;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * Created by saipkri on 07/03/17.
 */

public interface ReleaseArtifactRepository extends MongoRepository<ReleaseArtifact, String> {
    @Query("{$and: [{'name' : ?0} , {'version' : ?1}]}")
    ReleaseArtifact findRelease(String name, String version);
}
