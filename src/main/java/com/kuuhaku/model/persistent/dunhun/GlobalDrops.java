package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.user.UserItem;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Objects;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "global_drops", schema = "dunhun")
public class GlobalDrops extends DAO<GlobalDrops> {
	@Id
	@Column(name = "item_id", nullable = false)
	private String id;

	@OneToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "item_id")
	@Fetch(FetchMode.JOIN)
	@MapsId("id")
	private UserItem item;

	@Column(name = "weight", nullable = false)
	private int weight;

	@Column(name = "min_level", nullable = false)
	private int minLevel;

	public String getId() {
		return id;
	}

	public UserItem getItem() {
		return item;
	}

	public int getMinLevel() {
		return minLevel;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		GlobalDrops that = (GlobalDrops) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
