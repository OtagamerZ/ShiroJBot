package com.kuuhaku.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.OffsetDateTime;

@Entity
public class Log {
	@Id
	private int id;
	private String user;
	private String guild;
	private final OffsetDateTime timestamp = OffsetDateTime.now().minusHours(3);

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

	public OffsetDateTime getTimestamp() {
		return timestamp;
	}
}
