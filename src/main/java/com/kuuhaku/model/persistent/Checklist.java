package com.kuuhaku.model.persistent;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "checklist")
public class Checklist {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String uid;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String name;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(nullable = false, name = "checklist_id")
	private List<Checkitem> items = new ArrayList<>();

	public Checklist() {
	}

	public Checklist(String uid, String name) {
		this.uid = uid;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getUid() {
		return uid;
	}

	public String getName() {
		return name;
	}

	public List<Checkitem> getItems() {
		return items;
	}
}
