package com.kuuhaku.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class PermaBlock {
	@Id
	private String id;

	PermaBlock() {

	}

	void block(String id) {
		this.id = id;
	}
}
