package com.kuuhaku.model;

import org.json.JSONObject;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class guildConfig {
    @Id
    private String guildID;
    private String prefix = "!";
    private String msgBoasVindas = "Seja bem-vindo(a) %user%!";
    private String msgAdeus = "Ahh...%user% deixou este servidor!";
    private String canalbv = null;
    private String canalav = null;
    private String cargowarn = null;
    private String cargoslvl = "";

    public guildConfig() {

    }

    public String getGuildId() {
        return guildID;
    }

    public void setGuildId(String id) {
        this.guildID = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getMsgBoasVindas() {
        return msgBoasVindas;
    }

    public void setMsgBoasVindas(String msgBoasVindas) {
        this.msgBoasVindas = msgBoasVindas;
    }


    public String getMsgAdeus() {
        return msgAdeus;
    }

    public void setMsgAdeus(String msgAdeus) {
        this.msgAdeus = msgAdeus;
    }


    public String getCanalbv() {
        return canalbv;
    }

    public void setCanalbv(String canalbv) {
        this.canalbv = canalbv;
    }

    public String getCanalav() {
        return canalav;
    }

    public void setCanalav(String canalav) {
        this.canalav = canalav;
    }

    public String getCargowarn() {
        return cargowarn;
    }

    public void setCargowarn(String cargowarn) {
        this.cargowarn = cargowarn;
    }

    public JSONObject getCargoslvl() {
        return new JSONObject(cargoslvl);
    }

    public void setCargoslvl(JSONObject cargoslvl) {
        this.cargoslvl = cargoslvl.toString();
    }
}
