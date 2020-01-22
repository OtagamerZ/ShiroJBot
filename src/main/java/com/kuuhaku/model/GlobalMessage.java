package com.kuuhaku.model;

import org.json.JSONObject;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class GlobalMessage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	private String userId;
	private String name;
	private String avatar;
	private String content;
	private long time = System.currentTimeMillis();

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public long getTime() {
		return time;
	}

	@Override
	public String toString() {
		JSONObject out = new JSONObject();
		out.put("id", id);
		out.put("userId", userId);
		out.put("name", name);
		out.put("avatar", avatar);
		out.put("content", content);
		out.put("time", time);
		return out.toString();
	}
}
