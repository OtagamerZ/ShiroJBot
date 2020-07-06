/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model.persistent;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "guildconfig")
public class GuildConfig {
	@Id
	@Column(columnDefinition = "VARCHAR(191)")
	private String guildID;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String name = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT 's!'")
	private String prefix = Main.getInfo().getDefaultPrefix();

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String cargoWarn = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String cargoVip = "";

	//CHANNELS
	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String canalBv = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String canalAdeus = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String canalSug = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String canalLvl = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String canalrelay = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String canalLog = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String canalAvisos = "";

	//TEXTS
	@Column(columnDefinition = "TEXT")
	private String msgBoasVindas = "Seja bem-vindo(a) ao %guild%, %user%!";

	@Column(columnDefinition = "TEXT")
	private String msgAdeus = "Ahh...%user% deixou este servidor!";

	@Column(columnDefinition = "TEXT")
	private String cargoslvl = "";

	@Column(columnDefinition = "TEXT")
	private String disabledModules = "";

	@Column(columnDefinition = "TEXT")
	private String buttonConfigs = "";

	@Column(columnDefinition = "TEXT")
	private String colorRoles = "";

	@Column(columnDefinition = "TEXT")
	private String ambientSounds = "";

	//NUMBERS
	@Column(columnDefinition = "INT NOT NULL DEFAULT 60")
	private int pollTime = 60;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 60")
	private int warnTime = 60;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 5")
	private int noSpamAmount = 5;

	//CHANNELS
	@Column(columnDefinition = "TEXT")
	private String noLinkChannels = "";

	@Column(columnDefinition = "TEXT")
	private String noSpamChannels = "";

	@Column(columnDefinition = "TEXT")
	private String canalKawaipon = "";

	@Column(columnDefinition = "TEXT")
	private String canalDrop = "";

	//SWITCHES
	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
	private boolean lvlNotif = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean anyTell = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean hardAntispam = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean antiRaid = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean liteMode = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean allowImg = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
	private boolean mmPermissionLock = true;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean markForDelete = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean kawaiponEnabled = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean dropEnabled = false;

	public GuildConfig() {
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
		return canalBv;
	}

	public void setCanalBV(String canalbv) {
		if (canalbv == null || canalbv.equals("-1")) this.canalBv = "";
		else this.canalBv = canalbv;
	}

	public String getMsgBoasVindas() {
		return msgBoasVindas;
	}

	public void setMsgBoasVindas(String msgBoasVindas) {
		if (msgBoasVindas.isBlank()) this.msgBoasVindas = "Seja bem-vindo(a) ao %guild%, %user%!";
		else this.msgBoasVindas = msgBoasVindas;
	}

	public String getCanalAdeus() {
		return canalAdeus;
	}

	public void setCanalAdeus(String canaladeus) {
		if (canaladeus == null || canaladeus.equals("-1")) this.canalAdeus = "";
		else this.canalAdeus = canaladeus;
	}

	public String getMsgAdeus() {
		return msgAdeus;
	}

	public void setMsgAdeus(String msgAdeus) {
		if (msgAdeus.isBlank()) this.msgAdeus = "Ahh...%user% deixou este servidor!";
		else this.msgAdeus = msgAdeus;
	}

	public String getCargoWarn() {
		return cargoWarn;
	}

	public void setCargoWarn(String cargoWarn) {
		if (cargoWarn == null || cargoWarn.equals("-1")) this.cargoWarn = "";
		else this.cargoWarn = cargoWarn;
	}

	public String getCargoVip() {
		return cargoVip;
	}

	public void setCargoVip(String cargoVip) {
		if (cargoVip == null || cargoVip.equals("-1")) this.cargoVip = "";
		else this.cargoVip = cargoVip;
	}

	public String getCanalSUG() {
		return canalSug;
	}

	public void setCanalSUG(String canalsug) {
		if (canalsug == null || canalsug.equals("-1")) this.canalSug = "";
		else this.canalSug = canalsug;
	}

	public String getCanalLvl() {
		return canalLvl;
	}

	public void setCanalLvl(String canallvl) {
		if (canallvl == null || canallvl.equals("-1")) this.canalLvl = "";
		else this.canalLvl = canallvl;
	}

	public String getCanalKawaipon() {
		return canalKawaipon;
	}

	public void setCanalKawaipon(String canalKawaipon) {
		if (canalKawaipon == null || canalKawaipon.equals("-1")) this.canalKawaipon = "";
		else this.canalKawaipon = canalKawaipon;
	}

	public String getCanalDrop() {
		return canalDrop;
	}

	public void setCanalDrop(String canalDrop) {
		if (canalDrop == null || canalDrop.equals("-1")) this.canalDrop = "";
		else this.canalDrop = canalDrop;
	}

	public String getCanalAvisos() {
		return canalAvisos;
	}

	public void setCanalAvisos(String canalAvisos) {
		if (canalAvisos == null || canalAvisos.equals("-1")) this.canalAvisos = "";
		else this.canalAvisos = canalAvisos;
	}

	public Map<String, Object> getCargoslvl() {
		try {
			return new JSONObject(cargoslvl).toMap();
		} catch (Exception e) {
			return new JSONObject().toMap();
		}
	}

	public void setCargosLvl(JSONObject cargoslvl) {
		this.cargoslvl = cargoslvl.toString();
	}

	public boolean isLvlNotif() {
		return lvlNotif;
	}

	public void setLvlNotif(boolean lvlNotif) {
		this.lvlNotif = lvlNotif;
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
		if (canalrelay == null || canalrelay.equals("-1")) this.canalrelay = "";
		else this.canalrelay = canalrelay;
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

	public void setCanalLog(String canalLog) {
		if (canalLog == null || canalLog.equals("-1")) this.canalLog = "";
		else this.canalLog = canalLog;
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

	public void toggleKawaipon() {
		kawaiponEnabled = !kawaiponEnabled;
	}

	public void toggleDrop() {
		dropEnabled = !dropEnabled;
	}

	public boolean isKawaiponEnabled() {
		return kawaiponEnabled;
	}

	public boolean isDropEnabled() {
		return dropEnabled;
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

	public JSONObject getButtonConfigs() {
		if (buttonConfigs == null || buttonConfigs.isEmpty()) return new JSONObject();
		else return new JSONObject(buttonConfigs);
	}

	public void setButtonConfigs(JSONObject buttonConfigs) {
		this.buttonConfigs = buttonConfigs.toString();
	}

	public JSONObject getColorRoles() {
		return new JSONObject(Helper.getOr(colorRoles, "{}"));
	}

	public void addColorRole(String name, String color, Role role) {
		JSONObject jo = getColorRoles();
		JSONObject r = new JSONObject();

		r.put("color", color);
		r.put("role", role.getId());

		jo.put(name, r);
		this.colorRoles = jo.toString();
	}

	public void removeColorRole(String name) {
		JSONObject jo = getColorRoles();
		jo.remove(name);
		this.colorRoles = jo.toString();
	}

	public JSONObject getAmbientSounds() {
		return new JSONObject(Helper.getOr(ambientSounds, "{}"));
	}

	public void addAmbientSound(String name, String link) {
		JSONObject jo = getAmbientSounds();

		jo.put(name, link);
		this.ambientSounds = jo.toString();
	}

	public void removeAmbientSound(String name) {
		JSONObject jo = getAmbientSounds();
		jo.remove(name);
		this.ambientSounds = jo.toString();
	}

	public void switchServerMMLock() {
		this.mmPermissionLock = !this.mmPermissionLock;
	}

	public boolean isServerMMLocked() {
		return mmPermissionLock;
	}
}
