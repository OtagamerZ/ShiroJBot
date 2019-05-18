package com.kuuhaku.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class CustomAnswers {
    @Id
    private Long id = System.currentTimeMillis();
    private String guildID;
    private String gatilho;
    private String answer;

    public CustomAnswers() {

    }

    public void setGuildID(String guildID) {
        this.guildID = guildID;
    }

    public String getGatilho() {
        return gatilho;
    }

    public void setGatilho(String gatilho) {
        this.gatilho = gatilho;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Long getId() {
        return id;
    }

    public String getGuildID() {
        return guildID;
    }
}
