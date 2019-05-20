package com.kuuhaku.model;

import org.json.JSONObject;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Map;

@Entity
public class guildConfig {
    @Id
    private String guildID;
    private String prefix = "!";
    private String msgBoasVindas = "Seja bem-vindo(a) %user%!";
    private String msgAdeus = "Ahh...%user% deixou este servidor!";
    private String canalbv = null;
    private String canaladeus = null;
    private String canalav = null;
    private String canalsug = null;
    private String cargowarn = null;
    private String cargoslvl = "{}";
    private String lvlNotif = "true";
    private String cargoNew = "{}";
    private boolean anyPlace = false;
    private boolean anyTell = false;

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

    public Map<String, Object> getCargoslvl() { return new JSONObject(cargoslvl).toMap(); }

    public void setCargoslvl(JSONObject cargoslvl) {
        this.cargoslvl = cargoslvl.toString();
    }

    public boolean getLvlNotif() {
        return Boolean.parseBoolean(lvlNotif);
    }

    public void setLvlNotif(boolean lvlNotif) {
        this.lvlNotif = Boolean.toString(lvlNotif);
    }

    public Map<String, Object> getCargoNew() {
        return new JSONObject(cargoNew).toMap();
    }

    public void setCargoNew(JSONObject cargoNew) {
        this.cargoNew = cargoNew.toString();
    }

    public String getCanalsug() {
        return canalsug;
    }

    public void setCanalsug(String canalsug) {
        this.canalsug = canalsug;
    }

    public boolean isAnyPlace() {
        return anyPlace;
    }

    public void setAnyPlace(boolean anyPlace) {
        this.anyPlace = anyPlace;
    }

    public boolean isAnyTell() {
        return anyTell;
    }

    public void setAnyTell(boolean anyTell) {
        this.anyTell = anyTell;
    }

    public String getCanaladeus() {
        return canaladeus;
    }

    public void setCanaladeus(String canaladeus) {
        this.canaladeus = canaladeus;
    }
}
