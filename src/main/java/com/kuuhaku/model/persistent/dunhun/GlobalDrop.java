package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.persistent.user.UserItem;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.List;
import java.util.Objects;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "global_drop", schema = "dunhun")
public class GlobalDrop extends DAO<GlobalDrop> {
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
		GlobalDrop that = (GlobalDrop) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	public static GlobalDrop getRandom(Dunhun game) {
		List<Object[]> drops = DAO.queryAllUnmapped("""
				SELECT item_id
				     , weight
				FROM global_drop
				WHERE weight > 0
				  AND min_level <= ?1
				""", game.getAreaLevel()
		);
		if (drops.isEmpty()) return null;

		RandomList<String> rl = new RandomList<>(game.getNodeRng());
		for (Object[] a : drops) {
			rl.add((String) a[0], ((Number) a[1]).intValue());
		}

		return DAO.find(GlobalDrop.class, rl.get());
	}
}
