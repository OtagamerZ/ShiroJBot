package com.kuuhaku.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class PermaBlock {
	@Id
	private String id;

	public PermaBlock() {

	}

	public void block(String id) {
		this.id = id;
	}
}
