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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.interfaces.annotations.WhenNull;
import com.kuuhaku.model.persistent.id.ProfileId;
import com.kuuhaku.utils.Utils;
import net.dv8tion.jda.api.entities.Member;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "profile")
public class Profile extends DAO implements Blacklistable {
	@EmbeddedId
	private ProfileId id;

	@Column(name = "xp", nullable = false)
	private long xp;

	@OneToMany(mappedBy = "profile", orphanRemoval = true)
	private List<VoiceData> voiceData = new ArrayList<>();

	@ElementCollection
	@Column(name = "warn")
	@CollectionTable(name = "profile_warns")
	private List<String> warns = new ArrayList<>();

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "uid")
	@Fetch(FetchMode.JOIN)
	@MapsId("uid")
	private Account account;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumns({
			@PrimaryKeyJoinColumn(name = "uid"),
			@PrimaryKeyJoinColumn(name = "gid")
	})
	@Fetch(FetchMode.JOIN)
	private ProfileSettings settings;

	public Profile() {
	}

	public Profile(Member member) {
		this(new ProfileId(member.getId(), member.getGuild().getId()));
	}

	@WhenNull
	public Profile(ProfileId id) {
		this.id = id;
		this.account = DAO.find(Account.class, id.getUid());
		this.settings = new ProfileSettings(id);
	}

	public ProfileId getId() {
		return id;
	}

	public long getXp() {
		return xp;
	}

	public void addXp(long value) {
		xp += value;
	}

	public List<VoiceData> getVoiceData() {
		return voiceData;
	}

	public List<String> getWarns() {
		return warns;
	}

	public Account getAccount() {
		return account;
	}

	public ProfileSettings getSettings() {
		return Utils.getOr(settings, new ProfileSettings(id));
	}

	@Override
	public boolean isBlacklisted() {
		return account.isBlacklisted();
	}
}
