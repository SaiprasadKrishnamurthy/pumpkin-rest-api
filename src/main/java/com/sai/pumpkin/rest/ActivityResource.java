package com.sai.pumpkin.rest;

import com.sai.pumpkin.domain.MavenGitVersionMapping;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.TreeMap;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class ActivityResource {

    private final MongoTemplate mongoTemplate;

    public ActivityResource(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @ApiOperation("Finds all activities (based on number of commits) since the specified timestamp.")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/activity", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Long> activity(@RequestParam("sinceTimestamp") long sinceTimestamp) {
        MatchOperation matchStage = Aggregation.match(new Criteria("timestamp").gte(sinceTimestamp));
        GroupOperation groupOperation = group("artifactConfig.name").count().as("count");
        Aggregation aggregation = newAggregation(
                matchStage, groupOperation);
        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, MavenGitVersionMapping.class, Map.class);
        Map<String, Long> res = new TreeMap<>();
        results.forEach(m -> res.put(m.get("_id").toString(), Long.parseLong(m.get("count").toString())));
        return res;
    }

    @ApiOperation("Finds all activities (based on number of commits) since the specified timestamp.")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/activity", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Long> committerActivity(@RequestParam("sinceTimestamp") long sinceTimestamp) {
        MatchOperation matchStage = Aggregation.match(new Criteria("timestamp").gte(sinceTimestamp));
        GroupOperation groupOperation = group("artifactConfig.name").count().as("count");
        Aggregation aggregation = newAggregation(
                matchStage, groupOperation);
        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, MavenGitVersionMapping.class, Map.class);
        Map<String, Long> res = new TreeMap<>();
        results.forEach(m -> res.put(m.get("_id").toString(), Long.parseLong(m.get("count").toString())));
        return res;
    }
}
