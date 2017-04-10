package com.sai.pumpkin.repository;

import com.sai.pumpkin.domain.ArtifactConfig;
import com.sai.pumpkin.domain.Team;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by saipkri on 07/03/17.
 */

public interface TeamRepository extends MongoRepository<Team, String> {

}
