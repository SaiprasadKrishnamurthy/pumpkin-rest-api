package com.sai.pumpkin.repository;

import com.sai.pumpkin.domain.ChangeSetEntry;
import com.sai.pumpkin.domain.GitLogEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * Created by saipkri on 07/03/17.
 */

public interface ChangeSetEntryRepository extends MongoRepository<ChangeSetEntry, String> {
    @Query("{ 'uuid' : ?0 }")
    ChangeSetEntry findByUUID(String uuid);
}
