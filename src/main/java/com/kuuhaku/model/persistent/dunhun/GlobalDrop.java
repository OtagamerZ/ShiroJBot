package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.ItemUseException;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	@Language("Groovy")
	@Column(name = "can_use", columnDefinition = "TEXT")
	private String canUse;

	@Column(name = "sorting")
	private Integer sorting;

	public String getId() {
		return id;
	}

	public UserItem getItem() {
		return item;
	}

	public int getMinLevel() {
		return minLevel;
	}

	public void apply(I18N locale, MessageChannel channel, Account acc, Gear gear) {
		if (effect == null) return;

		try {
			Utils.exec(id, effect, Map.of(
					"locale", locale,
					"channel", channel,
					"acc", acc,
					"gear", gear
			));
		} catch (Exception e) {
			if (e instanceof ItemUseException) throw e;

			Constants.LOGGER.warn("Failed to apply crafting item {}", id, e);
		}
	}

	public boolean canUse(Gear gear) {
		if (effect == null) return false;
		else if (canUse == null) return true;

		try {
			Object out = Utils.exec(id, canUse, Map.of("gear", gear));
			if (out instanceof Boolean b) {
				return b;
			}
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to check usability of {}", id, e);
		}

		return false;
	}

	public Integer getSorting() {
		return sorting;
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

	public static GlobalDrop getRandomCraft() {
		List<Object[]> drops = DAO.queryAllUnmapped("""
				SELECT item_id
				     , weight
				FROM global_drop
				WHERE weight > 0
				  AND effect IS NOT NULL
				"""
		);
		if (drops.isEmpty()) return null;

		RandomList<String> rl = new RandomList<>();
		for (Object[] a : drops) {
			rl.add((String) a[0], ((Number) a[1]).intValue());
		}

		return DAO.find(GlobalDrop.class, rl.get());
	}
}
