package com.sai.pumpkin.rest;

import com.sai.pumpkin.domain.PullRequest;
import com.sai.pumpkin.domain.Team;
import com.sai.pumpkin.repository.PullRequestRepository;
import com.sai.pumpkin.repository.TeamRepository;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by saipkri on 07/03/17.
 */
@RestController
public class PullRequestResource {

    private final PullRequestRepository pullRequestRepository;

    @Inject
    public PullRequestResource(final PullRequestRepository teamRepository) {
        this.pullRequestRepository = teamRepository;
    }

    @ApiOperation("Finds all pull request that is known to this app")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/pull-requests", method = RequestMethod.GET, produces = "application/json")
    public List<PullRequest> pullRequests() {
        return pullRequestRepository.findAll();
    }

}
