package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;

public class Impact {

    public JsonElement          author;
    public Map<String, Integer> impacts = new HashMap<String, Integer>();
    public List<JsonElement>    commits = new ArrayList<JsonElement>();

    /**
     * @return the author
     */
    public JsonElement getAuthor() {
        return author;
    }

    /**
     * @param author
     *        the author to set
     */
    public void setAuthor(JsonElement author) {
        this.author = author;
    }

    /**
     * @return the impacts
     */
    public Map<String, Integer> getImpacts() {
        return impacts;
    }

    /**
     * @param impacts
     *        the impacts to set
     */
    public void setImpacts(Map<String, Integer> impacts) {
        this.impacts = impacts;
    }

    /**
     * @return the commits
     */
    public List<JsonElement> getCommits() {
        return commits;
    }

    /**
     * @param commits
     *        the commits to set
     */
    public void setCommits(List<JsonElement> commits) {
        this.commits = commits;
    }

}
