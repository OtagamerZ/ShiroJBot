package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public record SkillValue(int min, int max, boolean withAdded) {
	private static final Pattern PATTERN = Pattern.compile("^(?<min>-?\\d+)(?:-(?<max>-?\\d+))?(?<added>:DMG)?$");

	public static SkillValue parse(String expr) {
		JSONObject values = Utils.extractNamedGroups(expr, PATTERN);
		if (values.isEmpty()) throw new RuntimeException("Failed to parse expression '" + expr + "'");

		return new SkillValue(
				values.getInt("min"),
				values.getInt("max", values.getInt("min")),
				values.has("added")
		);
	}

	public int withLevel(int level) {
		return (int) (min + (max - min) * Calc.clamp(level, 0, 100) / 100.0);
	}

	public int valueFor(Skill skill, Actor<?> source) {
		int level;
		double mult;
		if (source instanceof Hero h) {
			level = h.getStats().getLevel();
			mult = h.getSenshi().getPower() + h.getAttributes().wis() * 0.05;
		} else {
			level = source.getGame().getAreaLevel();

			mult = switch (source.getRarityClass()) {
				case MAGIC -> 1.25;
				case RARE -> 2;
				default -> 1;
			} * (source.getSenshi().getPower() + level * 0.025);
		}

		int added = 0;
		if (withAdded) {
			if (skill.getStats().isSpell()) {
				added = (int) source.getModifiers().getSpellDamage().get();
			} else {
				added = source.getSenshi().getDmg();
			}
		}

		return (int) ((withLevel(level) + added * skill.getStats().getEfficiency()) * mult);
	}

	@Override
	public @NotNull String toString() {
		return min() + " - " + max();
	}
}
