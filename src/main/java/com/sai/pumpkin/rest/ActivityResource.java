package com.sai.pumpkin.rest;

import com.sai.pumpkin.domain.CollectionJob;
import com.sai.pumpkin.domain.GitLogEntry;
import com.sai.pumpkin.domain.MavenGitVersionMapping;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

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
    @RequestMapping(value = "/commitsTrend", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Integer> commitsTrend(@RequestParam("sinceTimestamp") long sinceTimestamp) {

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy");
        Map<String, Integer> histogram = new LinkedHashMap<>();
        long from = DateUtils.round(new Date(sinceTimestamp), Calendar.DAY_OF_MONTH).getTime();
        long to = from + (1000 * 60 * 60 * 24);

        for (int i = 0; i < 20; i++) {
            Query q = Query.query(Criteria.where("timestamp").gte(from).lt(to));
            List<MavenGitVersionMapping> results = mongoTemplate.find(q, MavenGitVersionMapping.class);
            if (results != null) {
                histogram.put(sdf.format(new Date(from)), results.size());
            } else {
                histogram.put(sdf.format(new Date(from)), 0);
            }
            from = to;
            to = from + (1000 * 60 * 60 * 24);
        }
        return histogram;
    }

    @ApiOperation("Finds all activities (based on number of commits by a committer) since the specified timestamp.")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/committer-activities", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Long> committerActivities(@RequestParam("sinceTimestamp") long sinceTimestamp) {
        MatchOperation matchStage = Aggregation.match(new Criteria("timestamp").gte(sinceTimestamp));
        GroupOperation groupOperation = group("author").count().as("count");
        Aggregation aggregation = newAggregation(
                matchStage, groupOperation);
        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, GitLogEntry.class, Map.class);
        Map<String, Long> res = new TreeMap<>();
        results.forEach(m -> res.put(m.get("_id").toString(), Long.parseLong(m.get("count").toString())));
        return res;
    }

    @ApiOperation("Finds the number of commits per repository")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/commit-counts-per-repo", method = RequestMethod.GET, produces = "application/json")
    public Map<String, Long> commitCountPerRepo(@RequestParam("sinceTimestamp") long sinceTimestamp) {
        MatchOperation matchStage = Aggregation.match(new Criteria("timestamp").gte(sinceTimestamp));
        GroupOperation groupOperation = group("artifactConfig.repoName").count().as("count");
        Aggregation aggregation = newAggregation(
                matchStage, groupOperation);
        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, MavenGitVersionMapping.class, Map.class);
        Map<String, Long> res = new TreeMap<>();
        results.forEach(m -> res.put(m.get("_id").toString(), Long.parseLong(m.get("count").toString())));
        return res;
    }

    @ApiOperation("Finds all activities (based on number of commits by a committer) since the specified timestamp.")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/collection-job-stats", method = RequestMethod.GET, produces = "application/json")
    public List<CollectionJob> collectionJobStats() {
        Query q = Query.query(Criteria.where("startTime").gt(0L)).with(new Sort(Sort.Direction.DESC, "startTime")).limit(700);
        return mongoTemplate.find(q, CollectionJob.class);
    }
}
