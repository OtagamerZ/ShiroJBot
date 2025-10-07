/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.AutoModType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.GuildFeature;
import com.kuuhaku.model.persistent.converter.ChannelConverter;
import com.kuuhaku.model.persistent.converter.GuildFeatureConverter;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.converter.RoleConverter;
import com.kuuhaku.model.persistent.javatype.ChannelJavaType;
import com.kuuhaku.model.persistent.javatype.RoleJavaType;
import com.kuuhaku.model.records.embed.Embed;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.internal.entities.RoleImpl;
import net.dv8tion.jda.internal.entities.channel.concrete.TextChannelImpl;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "guild_settings", schema = "shiro")
@JavaTypeRegistration(javaType = Role.class, descriptorClass = RoleJavaType.class)
@JavaTypeRegistration(javaType = GuildMessageChannel.class, descriptorClass = ChannelJavaType.class)
public class GuildSettings extends DAO<GuildSettings> {
	@Id
	@Column(name = "gid", nullable = false)
	private String gid;

	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "channel", nullable = false)
	@Convert(converter = ChannelConverter.class)
	@CollectionTable(
			schema = "shiro",
			name = "guild_settings_kawaipon_channel",
			joinColumns = @JoinColumn(name = "gid")
	)
	private Set<TextChannelImpl> kawaiponChannels = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "channel", nullable = false)
	@Convert(converter = ChannelConverter.class)
	@CollectionTable(
			schema = "shiro",
			name = "guild_settings_drop_channel",
			joinColumns = @JoinColumn(name = "gid")
	)
	private Set<TextChannelImpl> dropChannels = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "channel", nullable = false)
	@Convert(converter = ChannelConverter.class)
	@CollectionTable(
			schema = "shiro",
			name = "guild_settings_denied_channel",
			joinColumns = @JoinColumn(name = "gid")
	)
	private Set<TextChannelImpl> deniedChannels = new LinkedHashSet<>();

	@Column(name = "notifications_channel")
	@Convert(converter = ChannelConverter.class)
	private TextChannelImpl notificationsChannel;

	@Column(name = "general_channel")
	@Convert(converter = ChannelConverter.class)
	private TextChannelImpl generalChannel;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "embed", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject embed = new JSONObject();

	@Column(name = "join_role")
	@Convert(converter = RoleConverter.class)
	private RoleImpl joinRole;

	@Column(name = "welcomer")
	@Convert(converter = RoleConverter.class)
	private RoleImpl welcomer;

	@OneToMany(mappedBy = "settings", cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	@OrderBy("level")
	private final List<LevelRole> levelRoles = new ArrayList<>();

	@OneToMany(mappedBy = "settings", cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	private final List<CustomAnswer> customAnswers = new ArrayList<>();

	@OneToMany(mappedBy = "settings", cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	@OrderBy("threshold")
	private final List<AutoRule> autoRules = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "category", nullable = false)
	@CollectionTable(
			schema = "shiro",
			name = "guild_settings_disabled_category",
			joinColumns = @JoinColumn(name = "gid")
	)
	private Set<Category> disabledCategories = new HashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "command", nullable = false)
	@CollectionTable(
			schema = "shiro",
			name = "guild_settings_disabled_command",
			joinColumns = @JoinColumn(name = "gid")
	)
	private Set<String> disabledCommands = new HashSet<>();

	@Column(name = "starboard_threshold", nullable = false)
	private int starboardThreshold = 5;

	@Column(name = "starboard_channel")
	@Convert(converter = ChannelConverter.class)
	private TextChannelImpl starboardChannel;

	@Column(name = "feature_flags", nullable = false)
	@Convert(converter = GuildFeatureConverter.class)
	private EnumSet<GuildFeature> featureFlags = EnumSet.noneOf(GuildFeature.class);

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "aliases", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject aliases = new JSONObject();

	@Enumerated(EnumType.STRING)
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			schema = "shiro",
			name = "automod_entry",
			joinColumns = @JoinColumn(name = "gid", referencedColumnName = "gid")
	)
	@Column(name = "type", nullable = false)
	@MapKeyColumn(name = "id")
	private Map<AutoModType, String> automodEntries = new HashMap<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "channel_locales", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject channelLocales = new JSONObject();

	public GuildSettings() {
	}

	public GuildSettings(String gid) {
		this.gid = gid;
	}

	public String getGid() {
		return gid;
	}

	public Set<TextChannelImpl> getKawaiponChannels() {
		return kawaiponChannels;
	}

	public Set<TextChannelImpl> getDropChannels() {
		return dropChannels;
	}

	public Set<TextChannelImpl> getDeniedChannels() {
		return deniedChannels;
	}

	public TextChannelImpl getNotificationsChannel() {
		return notificationsChannel;
	}

	public void setNotificationsChannel(TextChannelImpl notificationsChannel) {
		this.notificationsChannel = notificationsChannel;
	}

	public TextChannelImpl getGeneralChannel() {
		return generalChannel;
	}

	public void setGeneralChannel(TextChannelImpl generalChannel) {
		this.generalChannel = generalChannel;
	}

	public JSONObject getEmbed() {
		return embed;
	}

	public void setEmbed(Embed embed) {
		this.embed = new JSONObject(embed);
	}

	public Role getJoinRole() {
		return joinRole;
	}

	public void setJoinRole(Role joinRole) {
		this.joinRole = (RoleImpl) joinRole;
	}

	public Role getWelcomer() {
		return welcomer;
	}

	public void setWelcomer(Role welcomer) {
		this.welcomer = (RoleImpl) welcomer;
	}

	public List<LevelRole> getLevelRoles() {
		return levelRoles;
	}

	public List<LevelRole> getRolesForLevel(int level) {
		return getLevelRoles().stream()
				.filter(r -> r.getLevel() == level)
				.toList();
	}

	public List<CustomAnswer> getCustomAnswers() {
		return customAnswers;
	}

	public List<AutoRule> getAutoRules() {
		return autoRules;
	}

	public Set<Category> getDisabledCategories() {
		return disabledCategories;
	}

	public Set<String> getDisabledCommands() {
		return disabledCommands;
	}

	public int getStarboardThreshold() {
		return starboardThreshold;
	}

	public void setStarboardThreshold(int starboardThreshold) {
		this.starboardThreshold = starboardThreshold;
	}

	public TextChannelImpl getStarboardChannel() {
		return starboardChannel;
	}

	public void setStarboardChannel(TextChannelImpl starboardChannel) {
		this.starboardChannel = starboardChannel;
	}

	public boolean isFeatureEnabled(GuildFeature feature) {
		return featureFlags.contains(feature);
	}

	public EnumSet<GuildFeature> getFeatures() {
		return featureFlags;
	}

	public JSONObject getAliases() {
		return aliases;
	}

	public Map<AutoModType, String> getAutoModEntries() {
		return automodEntries;
	}

	public JSONObject getChannelLocales() {
		return channelLocales;
	}
}
