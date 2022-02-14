package com.kuuhaku.model.persistent;

import javax.persistence.*;

@Entity
@Table(name = "checkitem")
public class Checkitem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String description;

	@Column(columnDefinition = "BOOLEAN")
	private Boolean status;

	public Checkitem() {
	}

	public Checkitem(String description) {
		this.description = description;
	}

	public int getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}
}
