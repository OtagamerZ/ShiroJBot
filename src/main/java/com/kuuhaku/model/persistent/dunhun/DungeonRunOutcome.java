package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.dunhun.Node;
import com.kuuhaku.model.common.dunhun.context.NodeContext;
import com.kuuhaku.model.records.id.DungeonRunOutcomeId;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.intellij.lang.annotations.Language;

import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "dungeon_run_outcome", schema = "dunhun")
public class DungeonRunOutcome extends DAO<DungeonRunOutcome> {
	@EmbeddedId
	private DungeonRunOutcomeId id;

	@ManyToOne(optional = false)
	@JoinColumns({
			@JoinColumn(name = "hero_id", referencedColumnName = "hero_id", insertable = false, updatable = false),
			@JoinColumn(name = "dungeon_id", referencedColumnName = "dungeon_id", insertable = false, updatable = false)
	})
	@Fetch(FetchMode.JOIN)
	private DungeonRun parent;

	@Column(name = "floor", nullable = false)
	private int floor;

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	public DungeonRunOutcome() {
	}

	public DungeonRunOutcome(DungeonRun parent, Node node, @Language("Groovy") String effect) {
		this.id = new DungeonRunOutcomeId(parent.getId(), node.getId());
		this.parent = parent;
		this.floor = parent.getFloor();
		this.effect = effect;
	}

	public DungeonRunOutcomeId getId() {
		return id;
	}

	public DungeonRun getParent() {
		return parent;
	}

	public int getFloor() {
		return floor;
	}

	public void apply(Dunhun game, Node node) {
		if (effect == null) return;

		try {
			Utils.exec(id.toString(), effect, Map.of(
					"ctx", new NodeContext(game, node)
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to apply modifier {}", id, e);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		DungeonRunOutcome that = (DungeonRunOutcome) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
