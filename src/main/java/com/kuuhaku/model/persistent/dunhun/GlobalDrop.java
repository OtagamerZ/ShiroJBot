package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.common.dunhun.context.ActorContext;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.intellij.lang.annotations.Language;

import java.util.List;
import java.util.Map;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "max_rarity")
	private RarityClass maxRarity;

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	public String getId() {
		return id;
	}

	public UserItem getItem() {
		return item;
	}

	public int getMinLevel() {
		return minLevel;
	}

	public RarityClass getMaxRarity() {
		return maxRarity;
	}

	public void apply(Account acc, Gear gear) {
		if (maxRarity == null || effect == null) return;

		try {
			Utils.exec(id, effect, Map.of(
					"acc", acc,
					"gear", gear
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to apply crafting item {}", id, e);
		}
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
