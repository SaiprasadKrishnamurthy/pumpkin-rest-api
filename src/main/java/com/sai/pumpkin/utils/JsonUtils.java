package com.sai.pumpkin.utils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sai.pumpkin.domain.PullRequest;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Created by saipkri on 23/03/17.
 */
public final class JsonUtils {

    public static List<PullRequest> pullRequest(final String json) throws Exception {
        DocumentContext jsonContext = JsonPath.parse(json);
        List<PullRequest> prs = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            try {
                String jsonPath = "$.values[" + i + "]";
                Integer id = jsonContext.read(jsonPath + ".id");
                String title = jsonContext.read(jsonPath + ".title");
                long closedDate = jsonContext.read(jsonPath + ".closedDate");
                String onTopOfSha = jsonContext.read(jsonPath + ".toRef.latestCommit");
                String author = jsonContext.read(jsonPath + ".author.user.displayName");
                String approverPath = jsonPath + ".reviewers[?(@.approved==true)])";
                List<Map> approvers = jsonContext.read(approverPath);
                List<String> approverNames = approvers.stream().map(m -> ((Map) m.get("user")).get("displayName").toString()).collect(toList());
                //TODO pull req url.
                String pullRequestUrl = jsonPath + ".links.self[0].href";
                String prUrl = jsonContext.read(pullRequestUrl);

                prs.add(new PullRequest(null, id, title, closedDate, onTopOfSha, author, approverNames, prUrl));
            } catch (Exception ignored) {
            }
        }
        return prs;
    }

    public static void main(String[] args) throws Exception {
        String json = IOUtils.toString(new FileInputStream("/Users/saipkri/pumpkin/pumpkin-rest-api/a.json"));
        System.out.println(pullRequest(json));
    }
}
