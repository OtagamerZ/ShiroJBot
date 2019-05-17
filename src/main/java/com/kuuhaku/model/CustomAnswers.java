package com.kuuhaku.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class CustomAnswers {
    @Id
    @GeneratedValue
    private int id;
    private String guildID;
    private String trigger;
    private String answer;

    public CustomAnswers() {

    }

    public void setGuildID(String guildID) {
        this.guildID = guildID;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public int getId() {
        return id;
    }

    public String getGuildID() {
        return guildID;
    }
}
