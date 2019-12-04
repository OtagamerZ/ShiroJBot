package com.kuuhaku.model;

import com.kuuhaku.utils.Helper;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
public class Log {
	@Id
	private int id;
	private String user;
	private String guild;
	private String command;
	private final String timestamp = OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3")).format(Helper.dateformat);

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}

	public Log setUser(String user) {
		this.user = user;
		return this;
	}

	public String getGuild() {
		return guild;
	}

	public Log setGuild(String guild) {
		this.guild = guild;
		return this;
	}

	public String getCommand() {
		return command;
	}

	public Log setCommand(String command) {
		this.command = command;
		return this;
	}

	public String getTimestamp() {
		return timestamp;
	}
}
