/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model;

import com.kuuhaku.Main;
import org.json.JSONObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Map;

@Entity
public class guildConfig {
    @Id
    private String guildID;
    private String name;
    private String prefix = Main.getInfo().getDefaultPrefix();
    private String msgBoasVindas = "Seja bem-vindo(a) ao %guild%, %user%!";
    private String msgAdeus = "Ahh...%user% deixou este servidor!";
    private String canalbv = null;
    private String canaladeus = null;
    private String canalav = null;
    private String canalsug = null;
    private String canallvl = null;
    private String canalrelay = null;
    private String cargowarn = null;
    private String cargoslvl = "{}";
    private String lvlNotif = "true";
    private String cargoNew = "{}";
    private boolean anyTell = false;
    @Column(columnDefinition = "boolean default false")
    private boolean aiMode = false;
    private boolean markForDelete;

    public guildConfig() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getCanalBV() {
        return canalbv;
    }

    public void setCanalBV(String canalbv) {
        this.canalbv = canalbv;
    }

    public String getMsgBoasVindas() {
        return msgBoasVindas;
    }

    public void setMsgBoasVindas(String msgBoasVindas) {
        this.msgBoasVindas = msgBoasVindas;
    }

    public String getCanalAdeus() {
        return canaladeus;
    }

    public void setCanalAdeus(String canaladeus) {
        this.canaladeus = canaladeus;
    }

    public String getMsgAdeus() {
        return msgAdeus;
    }

    public void setMsgAdeus(String msgAdeus) {
        this.msgAdeus = msgAdeus;
    }

    public String getCanalAV() {
        return canalav;
    }

    public void setCanalAV(String canalav) {
        this.canalav = canalav;
    }

    public String getCargoWarn() {
        return cargowarn;
    }

    public void setCargoWarn(String cargowarn) {
        this.cargowarn = cargowarn;
    }

    public String getCanalSUG() {
        return canalsug;
    }

    public void setCanalSUG(String canalsug) {
        this.canalsug = canalsug;
    }

    public String getCanalLvl() {
        return canallvl;
    }

    public void setCanalLvl(String canallvl) {
        this.canallvl = canallvl;
    }

    public Map<String, Object> getCargoslvl() {
        return new JSONObject(cargoslvl).toMap();
    }

    public void setCargosLvl(JSONObject cargoslvl) {
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

    public boolean isAnyTell() {
        return anyTell;
    }

    public void setAnyTell(boolean anyTell) {
        this.anyTell = anyTell;
    }

    public boolean isMarkForDelete() {
        return markForDelete;
    }

    public void setMarkForDelete(boolean markForDelete) {
        this.markForDelete = markForDelete;
    }

    public String getCanalRelay() {
        return canalrelay;
    }

    public void setCanalRelay(String canalrelay) {
        this.canalrelay = canalrelay;
    }

    public String getGuildID() {
        return guildID;
    }

    public void setGuildID(String guildID) {
        this.guildID = guildID;
    }

    public boolean isAiMode() {
        return aiMode;
    }

    public void setAiMode(boolean aiMode) {
        this.aiMode = aiMode;
    }
}
