package com.sai.pumpkin.repository;

import com.sai.pumpkin.domain.PullRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * Created by saipkri on 07/03/17.
 */

public interface PullRequestRepository extends MongoRepository<PullRequest, String> {
    @Query("{$and: [{'mergedInto' : ?0}]}")
    List<PullRequest> findPullRequestsMergedIntoCommit(String mergedInto);
}
