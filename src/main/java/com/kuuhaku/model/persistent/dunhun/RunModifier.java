package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.common.dunhun.EffectBase;
import com.kuuhaku.model.common.dunhun.Floor;
import com.kuuhaku.model.common.dunhun.Node;
import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.localized.LocalizedRunModifier;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.intellij.lang.annotations.Language;

import java.util.*;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "run_modifier", schema = "dunhun")
public class RunModifier extends DAO<RunModifier> {
	@Id
	@Column(name = "id")
	private String id;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedRunModifier> infos = new HashSet<>();

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	@Column(name = "weight", nullable = false)
	private int weight;

	@Column(name = "min_floor", nullable = false)
	private int minFloor;

	private transient EffectBase effectCache;
	private transient String familyCache;

	public String getId() {
		return id;
	}

	public LocalizedRunModifier getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny()
				.orElseGet(() -> new LocalizedRunModifier(locale, id, id + ":" + locale));
	}

	public int getWeight() {
		return weight;
	}

	public int getMinFloor() {
		return minFloor;
	}

	public String getModFamily() {
		if (familyCache != null) return familyCache;

		return familyCache = Utils.regex(id, "_[IVX]+$").replaceAll("");
	}

	public EffectBase toEffect(Dunhun game) {
		if (effect == null) return null;
		else if (effectCache != null) return effectCache;

		try {
			Object out = Utils.exec(id, effect, Map.of(
					"ctx", new EffectContext<>(game, this)
			));
			if (out instanceof EffectBase e) {
				return effectCache = e;
			}
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to apply modifier {}", id, e);
		}

		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		RunModifier that = (RunModifier) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	public static RunModifier getRandom(Node node) {
		return Utils.withUnsafeRng(rng -> {
			rng.setSeed(node.getSeed());
			return getRandom(rng, node.getSublevel().getFloor());
		});
	}

	public static RunModifier getRandom(Floor floor) {
		return Utils.withUnsafeRng(rng -> {
			rng.setSeed(floor.getSeed());
			return getRandom(rng, floor);
		});
	}

	private static RunModifier getRandom(Random rng, Floor floor) {
		JSONArray modifiers = new JSONArray();
		for (RunModifier m : floor.getModifiers()) {
			modifiers.add(m.getId());
		}

		List<Object[]> mods = DAO.queryAllUnmapped("""
				SELECT id
				     , weight
				FROM run_modifier
				WHERE weight > 0
				  AND min_floor <= ?1
				  AND NOT has(get_affix_family(cast(?2 AS JSONB)), get_affix_family(id))
				""", floor.getFloor(), modifiers.toString());
		if (mods.isEmpty()) return null;

		RandomList<String> rl = new RandomList<>(rng);
		for (Object[] a : mods) {
			rl.add((String) a[0], ((Number) a[1]).intValue());
		}

		if (rl.entries().isEmpty()) return null;
		return DAO.find(RunModifier.class, rl.get());
	}
}
