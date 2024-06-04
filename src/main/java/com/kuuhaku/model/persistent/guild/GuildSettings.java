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
import com.kuuhaku.model.common.AutoEmbedBuilder;
import com.kuuhaku.model.enums.AutoModType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.GuildFeature;
import com.kuuhaku.model.persistent.converter.*;
import com.kuuhaku.model.persistent.javatype.ChannelJavaType;
import com.kuuhaku.model.persistent.javatype.RoleJavaType;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.hibernate.type.SqlTypes;

import java.util.*;

import static jakarta.persistence.CascadeType.*;

@Entity
@Table(name = "guild_settings")
@JavaTypeRegistration(javaType = Role.class, descriptorClass = RoleJavaType.class)
@JavaTypeRegistration(javaType = GuildMessageChannel.class, descriptorClass = ChannelJavaType.class)
public class GuildSettings extends DAO<GuildSettings> {
	@Id
	@Column(name = "gid", nullable = false)
	private String gid;

	@ElementCollection
	@Column(name = "kawaipon_channels")
	@CollectionTable(name = "guild_settings_kawaiponChannels", joinColumns = @JoinColumn(name = "gid"))
	private List<GuildMessageChannel> kawaiponChannels = new ArrayList<>();

	@ElementCollection
	@Column(name = "drop_channels")
	@CollectionTable(name = "guild_settings_dropChannels", joinColumns = @JoinColumn(name = "gid"))
	private List<GuildMessageChannel> dropChannels = new ArrayList<>();

	@ElementCollection
	@Column(name = "denied_channels")
	@CollectionTable(name = "guild_settings_deniedChannels", joinColumns = @JoinColumn(name = "gid"))
	private List<GuildMessageChannel> deniedChannels = new ArrayList<>();

	@Column(name = "notifications_channel")
	@Convert(converter = ChannelConverter.class)
	private GuildMessageChannel notificationsChannel;

	@Column(name = "general_channel")
	@Convert(converter = ChannelConverter.class)
	private GuildMessageChannel generalChannel;

	@Column(name = "embed", nullable = false, columnDefinition = "TEXT")
	@Convert(converter = EmbedConverter.class)
	private AutoEmbedBuilder embed = new AutoEmbedBuilder();

	@Column(name = "join_role")
	@Convert(converter = RoleConverter.class)
	private Role joinRole;

	@Column(name = "welcomer")
	@Convert(converter = RoleConverter.class)
	private Role welcomer;

	@OneToMany(mappedBy = "settings", cascade = ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	@OrderBy("level")
	private final List<LevelRole> levelRoles = new ArrayList<>();

	@OneToMany(mappedBy = "settings", cascade = ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private final List<CustomAnswer> customAnswers = new ArrayList<>();

	@OneToMany(mappedBy = "settings", cascade = ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	@OrderBy("threshold")
	private final List<AutoRule> autoRules = new ArrayList<>();

	@ElementCollection
	@Column(name = "disabled_categories")
	@CollectionTable(name = "guild_settings_disabledcategories", joinColumns = @JoinColumn(name = "gid"))
	private Set<Category> disabledCategories = new HashSet<>();

	@ElementCollection
	@Column(name = "disabled_commands")
	@CollectionTable(name = "guild_settings_disabledcommands", joinColumns = @JoinColumn(name = "gid"))
	private Set<String> disabledCommands = new HashSet<>();

	@Column(name = "starboard_threshold", nullable = false)
	private int starboardThreshold = 5;

	@Column(name = "starboard_channel")
	@Convert(converter = ChannelConverter.class)
	private GuildMessageChannel starboardChannel;

	@Column(name = "feature_flags", nullable = false)
	@Convert(converter = GuildFeatureConverter.class)
	private EnumSet<GuildFeature> featureFlags = EnumSet.noneOf(GuildFeature.class);

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "aliases", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject aliases = new JSONObject();

	@Enumerated(EnumType.STRING)
	@ElementCollection
	@CollectionTable(
			name = "automod_entries",
			joinColumns = @JoinColumn(name = "gid", referencedColumnName = "gid")
	)
	@Column(name = "type")
	@MapKeyColumn(name = "id")
	private Map<AutoModType, String> automodEntries = new HashMap<>();

	public GuildSettings() {
	}

	public GuildSettings(String gid) {
		this.gid = gid;
	}

	public String getGid() {
		return gid;
	}

	public List<GuildMessageChannel> getKawaiponChannels() {
		return kawaiponChannels;
	}

	public List<GuildMessageChannel> getDropChannels() {
		return dropChannels;
	}

	public List<GuildMessageChannel> getDeniedChannels() {
		return deniedChannels;
	}

	public GuildMessageChannel getNotificationsChannel() {
		return notificationsChannel;
	}

	public void setNotificationsChannel(GuildMessageChannel notificationsChannel) {
		this.notificationsChannel = notificationsChannel;
	}

	public GuildMessageChannel getGeneralChannel() {
		return generalChannel;
	}

	public void setGeneralChannel(GuildMessageChannel generalChannel) {
		this.generalChannel = generalChannel;
	}

	public AutoEmbedBuilder getEmbed() {
		return embed;
	}

	public void setEmbed(AutoEmbedBuilder embed) {
		this.embed = embed;
	}

	public Role getJoinRole() {
		return joinRole;
	}

	public void setJoinRole(Role joinRole) {
		this.joinRole = joinRole;
	}

	public Role getWelcomer() {
		return welcomer;
	}

	public void setWelcomer(Role welcomer) {
		this.welcomer = welcomer;
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

	public GuildMessageChannel getStarboardChannel() {
		return starboardChannel;
	}

	public void setStarboardChannel(GuildMessageChannel starboardChannel) {
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
}
