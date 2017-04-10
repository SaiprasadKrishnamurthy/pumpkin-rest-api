package com.sai.pumpkin.rest;

import com.sai.pumpkin.domain.Team;
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
public class TeamResource {

    private final TeamRepository teamRepository;

    @Inject
    public TeamResource(final TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @ApiOperation("Finds all teams")
    @CrossOrigin(methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.GET})
    @RequestMapping(value = "/teams", method = RequestMethod.GET, produces = "application/json")
    public List<Team> teams() {
        return teamRepository.findAll();
    }

}
