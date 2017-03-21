package com.sai.pumpkin.rest;

import com.sai.pumpkin.domain.ReleaseExpectation;
import com.sai.pumpkin.domain.ReleaseExpectationResult;
import com.sai.pumpkin.utils.CucumberUtils;
import com.sai.pumpkin.utils.SlackUtils;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class ReleaseExpectationChecksResource {

    private final MongoTemplate mongoTemplate;

    @Inject
    public ReleaseExpectationChecksResource(final MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @ApiOperation("runs a specific the test by name")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/testtemplate", method = RequestMethod.GET, produces = "text/plain")
    public ResponseEntity<?> testTemplate() throws Exception {
        return new ResponseEntity<>(IOUtils.toString(ReleaseExpectationChecksResource.class.getClassLoader().getResourceAsStream("test_report_template.html")), HttpStatus.OK);
    }

    @ApiOperation("runs a specific the test by name")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/testresult", method = RequestMethod.GET, produces = "text/html")
    public ResponseEntity<?> runTestByName(@RequestParam("testName") String testName) throws Exception {
        Criteria criteria = Criteria.where("name").is(testName.trim());
        ReleaseExpectation releaseExpectation = mongoTemplate.findOne(Query.query(criteria), ReleaseExpectation.class);
        if (releaseExpectation == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        String html = CucumberUtils.runFeature(testName.trim(), releaseExpectation.getFeatureText());
        return new ResponseEntity<>(html, HttpStatus.OK);
    }

    @ApiOperation("runs all tests")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/testexec", method = RequestMethod.GET, produces = "text/html")
    public ResponseEntity<?> runAll() throws Exception {
        mongoTemplate.findAll(ReleaseExpectation.class).forEach(re -> {
            try {
                String html = (String) runTestByName(re.getName()).getBody();
                if (html != null) {
                    Criteria criteria = Criteria.where("testName").is(re.getName().trim());
                    mongoTemplate.remove(Query.query(criteria), ReleaseExpectationResult.class);
                    SlackUtils.notify(html.contains("FailedExpectation"), re);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @ApiOperation("save the test")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/test", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<?> save(@RequestBody ReleaseExpectation releaseExpectation) throws Exception {
        Criteria criteria = Criteria.where("name").is(releaseExpectation.getName());
        mongoTemplate.remove(Query.query(criteria), ReleaseExpectation.class);
        mongoTemplate.save(releaseExpectation);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @ApiOperation("gets all the tests")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/tests", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> allTests() throws Exception {
        return new ResponseEntity<>(mongoTemplate.findAll(ReleaseExpectation.class), HttpStatus.OK);
    }
}
