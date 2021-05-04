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

package com.kuuhaku.model.persistent.guild;

import com.kuuhaku.Main;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.json.JSONObject;

import javax.persistence.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "guildconfig")
@OptimisticLocking(type = OptimisticLockType.VERSION)
public class GuildConfig {
	@Id
	@Column(columnDefinition = "VARCHAR(191)")
	private String guildId;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String name = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT 's!'")
	private String prefix = ShiroInfo.getDefaultPrefix();

	//ROLES
	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String muteRole = "";

	//CHANNELS
	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String welcomeChannel = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String byeChannel = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String suggestionChannel = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String levelChannel = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String relayChannel = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String logChannel = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String alertChannel = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String kawaiponChannel = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String dropChannel = "";

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String generalChannel = "";

	//TEXTS
	@Column(columnDefinition = "TEXT")
	private String welcomeMessage = "Seja bem-vindo(a) ao %guild%, %user%!";

	@Column(columnDefinition = "TEXT")
	private String byeMessage = "Ahh...%user% deixou este servidor!";

	@Column(columnDefinition = "TEXT")
	private String buttonConfigs = "{}";

	@Column(columnDefinition = "TEXT")
	private String embedTemplate = "{}";

	@Column(columnDefinition = "TEXT")
	private String generalTopic = "";

	//NUMBERS
	@Column(columnDefinition = "INT NOT NULL DEFAULT 60")
	private int pollTime = 60;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 60")
	private int muteTime = 60;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 5")
	private int noSpamAmount = 5;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 5")
	private int antiRaidTime = 10;

	//SWITCHES
	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
	private boolean levelNotif = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean anyTell = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean hardAntispam = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean antiRaid = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean liteMode = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean nqnMode = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean cardSpawn = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean dropSpawn = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean autoExceedRoles = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean smallCards = false;

	//COLLECTIONS
	//ROLES
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "guildconfig_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<LevelRole> levelRoles = new HashSet<>();

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "guildconfig_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<ColorRole> colorRoles = new HashSet<>();

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "guildconfig_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<PaidRole> paidRoles = new HashSet<>();

	//CHANNELS
	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> noLinkChannels = new HashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> noSpamChannels = new HashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> noCommandChannels = new HashSet<>();

	//CONFIGS
	@ElementCollection(fetch = FetchType.EAGER)
	private List<String> rules = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> disabledCommands = new HashSet<>();

	public GuildConfig(String guildId, String name) {
		this.guildId = guildId;
		this.name = name;
	}

	public GuildConfig(String guildId) {
		this.guildId = guildId;
	}

	public GuildConfig() {
	}

	public String getGuildId() {
		return guildId;
	}

	public void setGuildId(String id) {
		this.guildId = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public Role getMuteRole() {
		return Main.getInfo().getGuildByID(guildId).getRoleById(Helper.getOr(muteRole, "1"));
	}

	public void setMuteRole(String muteRole) {
		this.muteRole = Helper.getOr(muteRole, "");
	}

	public TextChannel getWelcomeChannel() {
		return Main.getInfo().getGuildByID(guildId).getTextChannelById(Helper.getOr(welcomeChannel, "1"));
	}

	public void setWelcomeChannel(String welcomeChannel) {
		this.welcomeChannel = Helper.getOr(welcomeChannel, "");
	}

	public TextChannel getByeChannel() {
		return Main.getInfo().getGuildByID(guildId).getTextChannelById(Helper.getOr(byeChannel, "1"));
	}

	public void setByeChannel(String byeChannel) {
		this.byeChannel = Helper.getOr(byeChannel, "");
	}

	public TextChannel getSuggestionChannel() {
		return Main.getInfo().getGuildByID(guildId).getTextChannelById(Helper.getOr(suggestionChannel, "1"));
	}

	public void setSuggestionChannel(String suggestionChannel) {
		this.suggestionChannel = Helper.getOr(suggestionChannel, "");
	}

	public TextChannel getLevelChannel() {
		return Main.getInfo().getGuildByID(guildId).getTextChannelById(Helper.getOr(levelChannel, "1"));
	}

	public void setLevelChannel(String levelChannel) {
		this.levelChannel = Helper.getOr(levelChannel, "");
	}

	public TextChannel getRelayChannel() {
		return Main.getInfo().getGuildByID(guildId).getTextChannelById(Helper.getOr(relayChannel, "1"));
	}

	public void setRelayChannel(String relayChannel) {
		this.relayChannel = Helper.getOr(relayChannel, "");
	}

	public TextChannel getLogChannel() {
		return Main.getInfo().getGuildByID(guildId).getTextChannelById(Helper.getOr(logChannel, "1"));
	}

	public void setLogChannel(String logChannel) {
		this.logChannel = Helper.getOr(logChannel, "");
	}

	public TextChannel getAlertChannel() {
		return Main.getInfo().getGuildByID(guildId).getTextChannelById(Helper.getOr(alertChannel, "1"));
	}

	public void setAlertChannel(String alertChannel) {
		this.alertChannel = Helper.getOr(alertChannel, "");
	}

	public TextChannel getKawaiponChannel() {
		return Main.getInfo().getGuildByID(guildId).getTextChannelById(Helper.getOr(kawaiponChannel, "1"));
	}

	public void setKawaiponChannel(String kawaiponChannel) {
		this.kawaiponChannel = Helper.getOr(kawaiponChannel, "");
	}

	public TextChannel getDropChannel() {
		return Main.getInfo().getGuildByID(guildId).getTextChannelById(Helper.getOr(dropChannel, "1"));
	}

	public void setDropChannel(String dropChannel) {
		this.dropChannel = Helper.getOr(dropChannel, "");
	}

	public TextChannel getGeneralChannel() {
		return Main.getInfo().getGuildByID(guildId).getTextChannelById(Helper.getOr(generalChannel, "1"));
	}

	public void setGeneralChannel(String generalChannel) {
		this.generalChannel = Helper.getOr(generalChannel, "");
	}

	public String getWelcomeMessage() {
		return welcomeMessage;
	}

	public void setWelcomeMessage(String welcomeMessage) {
		this.welcomeMessage = Helper.getOr(welcomeMessage, "Seja bem-vindo(a) ao %guild%, %user%!");
	}

	public String getByeMessage() {
		return byeMessage;
	}

	public void setByeMessage(String byeMessage) {
		this.byeMessage = Helper.getOr(byeMessage, "Ahh...%user% deixou este servidor!");
	}

	public JSONObject getButtonConfigs() {
		return new JSONObject(buttonConfigs);
	}

	public void setButtonConfigs(JSONObject buttonConfigs) {
		this.buttonConfigs = buttonConfigs.toString();
	}

	public JSONObject getEmbedTemplate() {
		return new JSONObject(embedTemplate);
	}

	public void setEmbedTemplate(JSONObject embedTemplate) {
		this.embedTemplate = embedTemplate.toString();
	}

	public String getEmbedTemplateRaw() {
		return embedTemplate;
	}

	public String getGeneralTopic() {
		return generalTopic;
	}

	public void setGeneralTopic(String generalTopic) {
		this.generalTopic = Helper.getOr(generalTopic, "Contagem de membros em %count% e subindo!");
	}

	public int getPollTime() {
		return pollTime;
	}

	public void setPollTime(int pollTime) {
		this.pollTime = pollTime;
	}

	public int getMuteTime() {
		return muteTime;
	}

	public void setMuteTime(int muteTime) {
		this.muteTime = muteTime;
	}

	public int getNoSpamAmount() {
		return noSpamAmount;
	}

	public void setNoSpamAmount(int noSpamAmount) {
		this.noSpamAmount = noSpamAmount;
	}

	public int getAntiRaidTime() {
		return antiRaidTime;
	}

	public void setAntiRaidTime(int antiRaidTime) {
		this.antiRaidTime = antiRaidTime;
	}

	public boolean isLevelNotif() {
		return levelNotif;
	}

	public void setLevelNotif(boolean levelNotif) {
		this.levelNotif = levelNotif;
	}

	public void toggleLevelNotif() {
		this.levelNotif = !levelNotif;
	}

	public boolean isAnyTell() {
		return anyTell;
	}

	public void setAnyTell(boolean anyTell) {
		this.anyTell = anyTell;
	}

	public void toggleAnyTell() {
		this.anyTell = !anyTell;
	}

	public boolean isHardAntispam() {
		return hardAntispam;
	}

	public void setHardAntispam(boolean hardAntispam) {
		this.hardAntispam = hardAntispam;
	}

	public void toggleHardAntispam() {
		this.hardAntispam = !hardAntispam;
	}

	public boolean isAntiRaid() {
		return antiRaid;
	}

	public void setAntiRaid(boolean antiRaid) {
		this.antiRaid = antiRaid;
	}

	public void toggleAntiRaid() {
		this.antiRaid = !antiRaid;
	}

	public boolean isLiteMode() {
		return liteMode;
	}

	public void setLiteMode(boolean liteMode) {
		this.liteMode = liteMode;
	}

	public void toggleLiteMode() {
		this.liteMode = !liteMode;
	}

	public boolean isNQNMode() {
		return nqnMode;
	}

	public void setNQNMode(boolean nqnMode) {
		this.nqnMode = nqnMode;
	}

	public void toggleNQNMode() {
		this.nqnMode = !nqnMode;
	}

	public boolean isCardSpawn() {
		return cardSpawn;
	}

	public void setCardSpawn(boolean kawaiponEnabled) {
		this.cardSpawn = kawaiponEnabled;
	}

	public void toggleCardSpawn() {
		this.cardSpawn = !cardSpawn;
	}

	public boolean isDropSpawn() {
		return dropSpawn;
	}

	public void setDropSpawn(boolean dropEnabled) {
		this.dropSpawn = dropEnabled;
	}

	public void toggleDropSpawn() {
		this.dropSpawn = !dropSpawn;
	}

	public boolean isAutoExceedRoles() {
		return autoExceedRoles;
	}

	public void setAutoExceedRoles(boolean autoExceedRoles) {
		this.autoExceedRoles = autoExceedRoles;
	}

	public void toggleAutoExceedRoles() {
		this.autoExceedRoles = !autoExceedRoles;
	}

	public boolean isSmallCards() {
		return smallCards;
	}

	public void setSmallCards(boolean smallCards) {
		this.smallCards = smallCards;
	}

	public void toggleSmallCards() {
		this.smallCards = !smallCards;
	}

	public Set<LevelRole> getLevelRoles() {
		return levelRoles;
	}

	public void addLevelRole(String id, int level) {
		LevelRole lr = new LevelRole(id, level);

		levelRoles.remove(lr);
		levelRoles.add(lr);
	}

	public void removeLevelRole(String id) {
		levelRoles.removeIf(lr -> lr.getId().equals(id));
	}

	public void setLevelRoles(Set<LevelRole> levelRoles) {
		this.levelRoles = levelRoles;
	}

	public Set<ColorRole> getColorRoles() {
		return colorRoles;
	}

	public void addColorRole(String id, Color color, String name) {
		colorRoles.add(new ColorRole(id, color, name));
	}

	public void addColorRole(String id, String color, String name) {
		ColorRole cr = new ColorRole(id, color, name);

		colorRoles.remove(cr);
		colorRoles.add(cr);
	}

	public void removeColorRole(String name) {
		colorRoles.removeIf(cr -> cr.getName().equals(name));
	}

	public void setColorRoles(Set<ColorRole> levelRoles) {
		this.colorRoles = levelRoles;
	}

	public Set<PaidRole> getPaidRoles() {
		return paidRoles;
	}

	public void addPaidRole(String id, int price, long expiration) {
		PaidRole pr = new PaidRole(id, price, expiration);

		paidRoles.remove(pr);
		paidRoles.add(pr);
	}

	public void removePaidRole(String id) {
		paidRoles.removeIf(pr -> pr.getId().equals(id));
	}

	public void setPaidRoles(Set<PaidRole> levelRoles) {
		this.paidRoles = levelRoles;
	}

	public Set<String> getNoLinkChannels() {
		return noLinkChannels;
	}

	public void addNoLinkChannel(String channel) {
		noLinkChannels.add(channel);
	}

	public void removeNoLinkChannel(String channel) {
		noLinkChannels.remove(channel);
	}

	public void setNoLinkChannels(Set<String> noLinkChannels) {
		this.noLinkChannels = noLinkChannels;
	}

	public Set<String> getNoSpamChannels() {
		return noSpamChannels;
	}

	public void addNoSpamChannel(String channel) {
		noSpamChannels.add(channel);
	}

	public void removeNoSpamChannel(String channel) {
		noSpamChannels.remove(channel);
	}

	public void setNoSpamChannels(Set<String> noSpamChannels) {
		this.noSpamChannels = noSpamChannels;
	}

	public Set<String> getNoCommandChannels() {
		return noCommandChannels;
	}

	public void addNoCommandChannel(String channel) {
		noCommandChannels.add(channel);
	}

	public void removeNoCommandChannel(String channel) {
		noCommandChannels.remove(channel);
	}

	public void setNoCommandChannels(Set<String> noCommandChannels) {
		this.noCommandChannels = noCommandChannels;
	}

	public List<String> getRules() {
		return rules;
	}

	public void addRule(String rule) {
		rules.add(rule);
	}

	public void removeRule(int index) {
		rules.remove(index);
	}

	public void moveRule(int from, int to) {
		rules.add(to, rules.remove(from));
	}

	public void setRules(List<String> rules) {
		this.rules = rules;
	}

	public Set<String> getDisabledCommands() {
		return disabledCommands;
	}

	public void disableCommand(Class<?> klass) {
		disabledCommands.add(klass.getName());
	}

	public void enableCommand(Class<?> klass) {
		disabledCommands.remove(klass.getName());
	}

	public void setDisabledCommands(Set<Class<?>> classes) {
		disabledCommands = classes.stream().map(Class::getName).collect(Collectors.toSet());
	}
}
