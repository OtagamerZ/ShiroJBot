package com.kuuhaku.model;

import com.kuuhaku.utils.ExceedEnums;

public class Exceed {
	private final ExceedEnums exceed;
	private final int members;
	private final long exp;

	public Exceed(ExceedEnums exceed, int members, long exp) {
		this.exceed = exceed;
		this.members = members;
		this.exp = exp;
	}

	public ExceedEnums getExceed() {
		return exceed;
	}

	public int getMembers() {
		return members;
	}

	public long getExp() {
		return exp;
	}
}
