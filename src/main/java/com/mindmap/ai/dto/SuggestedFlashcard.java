package com.mindmap.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One flashcard suggestion from the AI. Fields are editable before acceptance.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuggestedFlashcard {

    @JsonProperty("question") private String question;
    @JsonProperty("answer")   private String answer;

    // Transient — user decision in the dialog
    private boolean accepted = true;

    public SuggestedFlashcard() { }
    public SuggestedFlashcard(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() { return question; }
    public void setQuestion(String q) { this.question = q; }

    public String getAnswer() { return answer; }
    public void setAnswer(String a) { this.answer = a; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean a) { this.accepted = a; }
}