package com.mindmap.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SuggestedQuizQuestion {

    @JsonProperty("type")        private String type;           // MCQ | TRUE_FALSE | SHORT
    @JsonProperty("question")    private String question;
    @JsonProperty("options")     private List<String> options = new ArrayList<>();
    @JsonProperty("correct")     private String correct;
    @JsonProperty("explanation") private String explanation;

    private boolean accepted = true;

    public String getType() { return type; }
    public void setType(String t) { this.type = t; }

    public String getQuestion() { return question; }
    public void setQuestion(String q) { this.question = q; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> o) { this.options = o == null ? new ArrayList<>() : o; }

    public String getCorrect() { return correct; }
    public void setCorrect(String c) { this.correct = c; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String e) { this.explanation = e; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean a) { this.accepted = a; }
}