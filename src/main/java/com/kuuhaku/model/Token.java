package com.kuuhaku.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Token {
	@Id
	private int id;
	@Column(columnDefinition = "String default \"\"")
	private String token;
	@Column(columnDefinition = "String default \"\"")
	private String holder;
	@Column(columnDefinition = "Integer default 0")
	private int calls;

	public String getToken() {
		return token;
	}

	public String getHolder() {
		return holder;
	}

	public Token addCall() {
		calls++;
		return this;
	}
}
