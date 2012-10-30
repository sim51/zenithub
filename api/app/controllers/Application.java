package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Impact;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Controller;
import play.mvc.Util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Application extends Controller {

    private final static String  gihubUrl   = "https://api.github.com/";

    private final static Integer PER_PAGE   = 100;
    private final static String  NB_COMMITS = "commits";
    private final static String  ADDITIONS  = "additions";
    private final static String  DELETIONS  = "deletions";

    public static void impact(String owner, String repo) {

        String ghUser = Play.configuration.getProperty("github.username");
        String ghPwd = Play.configuration.getProperty("github.password");
        String url = gihubUrl + "repos/" + owner + "/" + repo + "/commits?per_page=" + PER_PAGE;

        // look up in cache first
        WS.HttpResponse res = (HttpResponse) Cache.get(url);
        if (Cache.get(url) == null) {
            // Do github async call for commits
            Promise<HttpResponse> futurResponse = WS.url(url).authenticate(ghUser, ghPwd).getAsync();
            res = await(futurResponse);
        }

        // List of all async response
        List<Promise<HttpResponse>> futurCommits = new ArrayList<Promise<HttpResponse>>();

        // parse the response
        if (res.success()) {
            JsonArray commits = res.getJson().getAsJsonArray();
            for (int i = 0; i < commits.size(); i++) {

                JsonObject commit = commits.get(i).getAsJsonObject();

                // Do github async call for commit
                String commitUrl = commit.get("url").getAsString();
                futurCommits.add(WS.url(commitUrl).authenticate(ghUser, ghPwd).getAsync());
            }
            F.Promise<List<WS.HttpResponse>> promises = F.Promise.waitAll(futurCommits);
            List<WS.HttpResponse> httpResponses = await(promises);

            // key : login | value : its position into response arraylist
            Map<String, Integer> authorStack = new HashMap<String, Integer>();
            List<Impact> response = new ArrayList<Impact>();

            for (int j = 0; j < httpResponses.size(); j++) {
                HttpResponse commitResponse = httpResponses.get(j);
                if (commitResponse.success()) {
                    JsonObject object = commitResponse.getJson().getAsJsonObject();
                    if (!object.get("author").isJsonNull()) {
                        JsonElement author = object.get("author");
                        Logger.debug("Parsing commit " + object.get("url").getAsString() + " of "
                                + author.getAsJsonObject().get("login").getAsString());

                        // is user already passed ?
                        String login = author.getAsJsonObject().get("login").getAsString();
                        if (!authorStack.containsKey(login)) {
                            Impact impact = new Impact();
                            impact.author = author;
                            response.add(impact);
                            authorStack.put(login, response.size() - 1);
                        }

                        Impact impact = response.get(authorStack.get(login));

                        // retrive response commit data
                        Integer deletions = object.get("stats").getAsJsonObject().get("deletions").getAsInt();
                        Integer additions = object.get("stats").getAsJsonObject().get("additions").getAsInt();
                        JsonObject commit = object.get("commit").getAsJsonObject();

                        // adding commit
                        impact.commits.add(commit);

                        // Additions
                        if (impact.impacts.containsKey(ADDITIONS)) {
                            impact.impacts.put(ADDITIONS, impact.impacts.get(ADDITIONS) + additions);
                        }
                        else {
                            impact.impacts.put(ADDITIONS, additions);
                        }

                        // Deletions
                        if (impact.impacts.containsKey(DELETIONS)) {
                            impact.impacts.put(DELETIONS, impact.impacts.get(DELETIONS) + deletions);
                        }
                        else {
                            impact.impacts.put(DELETIONS, deletions);
                        }
                    }
                }
            }

            // last loop to do nb commit
            for (int j = 0; j < response.size(); j++) {
                Impact impact = response.get(j);
                impact.impacts.put(NB_COMMITS, impact.commits.size());
            }
            renderJSON(response);
        }
        else {
            error();
        }
    }

    @Util
    public static void renderJSON(Object object) {
        if (request.params._contains("callback")) {
            Gson gson = new Gson();
            String out = gson.toJson(object);
            renderText(request.params.get("callback") + "(" + out + ")");
        }
        else {
            Controller.renderJSON(object);
        }
    }

}
