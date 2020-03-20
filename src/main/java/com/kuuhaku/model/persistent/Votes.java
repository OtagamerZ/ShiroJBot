/*
 * This file is part of Shiro J Bot.
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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.*;

@Entity
@Table(name = "votes")
public class Votes {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String guildID = "";

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String votedUser = "";

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String votedUserID = "";

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String usr = "";

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String userID = "";

	@Column(columnDefinition = "INT DEFAULT 0")
	private int vote = 0;

	public void addArgs(Guild guild, User user, User target, boolean positive) {
		this.guildID = guild.getId();
		this.votedUser = target.getAsTag();
		this.votedUserID = target.getId();
		this.usr = user.getAsTag();
		this.userID = user.getId();
		this.vote = positive ? 1 : -1;
	}

	public String getGuildID() {
		return guildID;
	}

	public String getVotedUser() {
		return votedUser;
	}

	public String getVotedUserID() {
		return votedUserID;
	}

	public String getUser() {
		return usr;
	}

	public String getUserID() {
		return userID;
	}

	public int getVote() {
		return vote;
	}
}
