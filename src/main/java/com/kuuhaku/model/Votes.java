package com.kuuhaku.model;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Votes {
	@Id
	private int id;
	private String guildID;
	private String votedUser;
	private String votedUserID;
	private String user;
	private String userID;
	private int vote;

	public void addArgs(Guild guild, User user, User target, boolean positive) {
		this.guildID = guild.getId();
		this.votedUser = target.getAsTag();
		this.votedUserID = target.getId();
		this.user = user.getAsTag();
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
		return user;
	}

	public String getUserID() {
		return userID;
	}

	public int getVote() {
		return vote;
	}
}
