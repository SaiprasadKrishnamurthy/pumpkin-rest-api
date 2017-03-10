package com.sai.pumpkin.repository;

import com.sai.pumpkin.domain.GitLogEntry;
import com.sai.pumpkin.domain.GitLogResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * Created by saipkri on 07/03/17.
 */

public interface GitLogEntryRepository extends MongoRepository<GitLogEntry, String> {
    @Query("{ 'uuid' : ?0 }")
    GitLogEntry findByUUID(String uuid);
}
