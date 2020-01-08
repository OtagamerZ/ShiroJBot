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
import com.kuuhaku.command.Category;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	private String canalsug = null;
	@Column(columnDefinition = "int default 60")
	private int pollTime = 60;
	private String canallvl = null;
	private String canalrelay = null;
	private String cargowarn = null;
	@Column(columnDefinition = "int default 60")
	private int warnTime = 60;
	@Column(columnDefinition = "text")
    private String cargoslvl = "{}";
	private String lvlNotif = "true";
	private String cargoNew = "{}";
	private boolean anyTell = false;
	private String noLinkChannels = "";
	@Column(length = 191)
	private String canalLog = "";
	private String noSpamChannels = "";
	@Column(columnDefinition = "int default 5")
	private int noSpamAmount = 5;
	@Column(columnDefinition = "boolean default false")
	private boolean hardAntispam;
	@Column(columnDefinition = "boolean default false")
	private boolean antiRaid = false;
	@Column(columnDefinition = "boolean default false")
	private boolean liteMode = false;
	@Column(columnDefinition = "boolean default false")
	private boolean allowImg = false;
	private String disabledModules = "";
	private String buttonConfigs = "";
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
		try {
			return new JSONObject(cargoslvl).toMap();
		} catch (Exception e) {
			return null;
		}
	}

	public void setCargosLvl(JSONObject cargoslvl) {
		this.cargoslvl = cargoslvl.toString();
	}

	public boolean isLvlNotif() {
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

	public boolean isNotAnyTell() {
		return !anyTell;
	}

	public void setAnyTell(boolean anyTell) {
		this.anyTell = anyTell;
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

	public ArrayList<String> getNoLinkChannels() {
		return getChannels(noLinkChannels);
	}

	@NotNull
	private ArrayList<String> getChannels(String noLinkChannels) {
		try {
			ArrayList<String> l = new ArrayList<>(Arrays.asList(noLinkChannels.replace("[", "").replace("]", "").replace(" ", "").replace("\n", "").split(",")));
			l.removeIf(String::isEmpty);
			return l;
		} catch (NullPointerException e) {
			return new ArrayList<>();
		}
	}

	public void addNoLinkChannel(TextChannel ch) {
		List<String> ph = new ArrayList<>(getNoLinkChannels());
		ph.add(ch.getId());
		noLinkChannels = ph.toString();
	}

	public void removeNoLinkChannel(TextChannel ch) {
		List<String> ph = new ArrayList<>(getNoLinkChannels());
		ph.removeIf(s -> s.equals(ch.getId()));
		noLinkChannels = ph.toString();
	}

	public ArrayList<String> getNoSpamChannels() {
		return getChannels(noSpamChannels);
	}

	public void addNoSpamChannel(TextChannel ch) {
		List<String> ph = new ArrayList<>(getNoSpamChannels());
		ph.add(ch.getId());
		noSpamChannels = ph.toString();
	}

	public void removeNoSpamChannel(TextChannel ch) {
		List<String> ph = new ArrayList<>(getNoSpamChannels());
		ph.removeIf(s -> s.equals(ch.getId()));
		noSpamChannels = ph.toString();
	}

	public boolean isHardAntispam() {
		return hardAntispam;
	}

	public void setHardAntispam(boolean hardAntispam) {
		this.hardAntispam = hardAntispam;
	}

	public int getNoSpamAmount() {
		return noSpamAmount;
	}

	public void setNoSpamAmount(int noSpamAmount) {
		this.noSpamAmount = noSpamAmount;
	}

	public int getPollTime() {
		return pollTime;
	}

	public void setPollTime(int pollTime) {
		this.pollTime = pollTime;
	}

	public int getWarnTime() {
		return warnTime;
	}

	public void setWarnTime(int warnTime) {
		this.warnTime = warnTime;
	}

	public boolean isAntiRaid() {
		return antiRaid;
	}

	public void setAntiRaid(boolean antiRaid) {
		this.antiRaid = antiRaid;
	}

	public String getCanalLog() {
		return canalLog;
	}

	public void setCanalLog(String id) {
		this.canalLog = id;
	}

	public boolean isLiteMode() {
		return liteMode;
	}

	public void setLiteMode(boolean liteMode) {
		this.liteMode = liteMode;
	}

	public boolean isAllowImg() {
		return allowImg;
	}

	public void setAllowImg(boolean allowImg) {
		this.allowImg = allowImg;
	}

	public boolean isMarkForDelete() {
		return markForDelete;
	}

	public List<Category> getDisabledModules() {
		List<Category> cats = new ArrayList<>();
		if (Helper.getOr(disabledModules, null) == null) return cats;
		String[] dmods = disabledModules.split(",");
		for (String mod : dmods) {
			try {
				cats.add(Category.getByName(mod));
			} catch (IndexOutOfBoundsException e) {
				return cats;
			}
		}
		return cats;
	}

	public void setDisabledModules(List<Category> disabledModules) {
		this.disabledModules = Arrays.toString(disabledModules.toArray()).replace("[", "").replace("]", "").replace(" ", "");
	}

	public JSONArray getButtonConfigs() {
		if (buttonConfigs == null || buttonConfigs.isEmpty()) return new JSONArray();
		else return new JSONArray(buttonConfigs);
	}

	public void setButtonConfigs(JSONArray buttonConfigs) {
		this.buttonConfigs = buttonConfigs.toString();
	}
}
