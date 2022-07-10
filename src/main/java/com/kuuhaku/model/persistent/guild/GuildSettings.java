/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.GuildFeature;
import com.kuuhaku.model.persistent.converter.EmbedConverter;
import com.kuuhaku.model.persistent.converter.GuildFeatureConverter;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "guild_settings")
public class GuildSettings extends DAO<GuildSettings> {
	@Id
	@Column(name = "gid", nullable = false)
	private String gid;

	@Column(name = "anti_raid_threshold", nullable = false)
	private int antiRaidThreshold = 0;

	@ElementCollection
	@Column(name = "kawaipon_channels")
	@Type(type = "com.kuuhaku.model.persistent.descriptor.type.ChannelStringType")
	@CollectionTable(name = "guild_settings_kawaiponChannels", joinColumns = @JoinColumn(name = "gid"))
	private List<TextChannel> kawaiponChannels = new ArrayList<>();

	@ElementCollection
	@Column(name = "drop_channels")
	@Type(type = "com.kuuhaku.model.persistent.descriptor.type.ChannelStringType")
	@CollectionTable(name = "guild_settings_dropChannels", joinColumns = @JoinColumn(name = "gid"))
	private List<TextChannel> dropChannels = new ArrayList<>();

	@ElementCollection
	@Column(name = "denied_channels")
	@Type(type = "com.kuuhaku.model.persistent.descriptor.type.ChannelStringType")
	@CollectionTable(name = "guild_settings_deniedChannels", joinColumns = @JoinColumn(name = "gid"))
	private List<TextChannel> deniedChannels = new ArrayList<>();

	@Column(name = "notificationsChannel")
	@Type(type = "com.kuuhaku.model.persistent.descriptor.type.ChannelStringType")
	private TextChannel notificationsChannel;

	@Convert(converter = EmbedConverter.class)
	@Column(name = "embed", nullable = false)
	private AutoEmbedBuilder embed = new AutoEmbedBuilder();

	@Column(name = "join_role")
	@Type(type = "com.kuuhaku.model.persistent.descriptor.type.RoleStringType")
	private Role joinRole;

	@Column(name = "welcomer")
	@Type(type = "com.kuuhaku.model.persistent.descriptor.type.RoleStringType")
	private Role welcomer;

	@OneToMany(mappedBy = "settings", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private List<ColorRole> colorRoles = new ArrayList<>();

	@OneToMany(mappedBy = "settings", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private List<LevelRole> levelRoles = new ArrayList<>();

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
	@Type(type = "com.kuuhaku.model.persistent.descriptor.type.ChannelStringType")
	private TextChannel starboardChannel;

	@Convert(converter = GuildFeatureConverter.class)
	@Column(name = "feature_flags", nullable = false)
	private EnumSet<GuildFeature> featureFlags = EnumSet.noneOf(GuildFeature.class);

	@Convert(converter = JSONObjectConverter.class)
	@Column(name = "aliases", nullable = false)
	private JSONObject aliases = new JSONObject();

	public GuildSettings() {
	}

	public GuildSettings(String gid) {
		this.gid = gid;
	}

	public String getGid() {
		return gid;
	}

	public int getAntiRaidThreshold() {
		return antiRaidThreshold;
	}

	public void setAntiRaidThreshold(int antiRaidThreshold) {
		this.antiRaidThreshold = antiRaidThreshold;
	}

	public List<TextChannel> getKawaiponChannels() {
		return kawaiponChannels;
	}

	public List<TextChannel> getDropChannels() {
		return dropChannels;
	}

	public List<TextChannel> getDeniedChannels() {
		return deniedChannels;
	}

	public TextChannel getNotificationsChannel() {
		return notificationsChannel;
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

	public List<ColorRole> getColorRoles() {
		return colorRoles;
	}

	public List<LevelRole> getLevelRoles() {
		return levelRoles;
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

	public TextChannel getStarboardChannel() {
		return starboardChannel;
	}

	public void setStarboardChannel(TextChannel starboardChannel) {
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
}
