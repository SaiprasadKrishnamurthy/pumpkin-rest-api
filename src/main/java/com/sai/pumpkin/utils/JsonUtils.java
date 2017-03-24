package com.sai.pumpkin.utils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sai.pumpkin.domain.PullRequest;

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
                prs.add(new PullRequest(null, id, title, closedDate, onTopOfSha, author, approverNames));
            } catch (Exception ignored) {
            }
        }
        return prs;
    }
}
