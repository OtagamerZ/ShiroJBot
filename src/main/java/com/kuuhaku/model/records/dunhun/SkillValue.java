package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.model.common.dunhun.Actor;
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
		double mult = 1;
		if (skill.getStats().isSpell()) {
			mult = source.getSenshi().getPower();
		}

		int added = 0;
		if (withAdded && skill.getStats().getEfficiency() > 0) {
			double eff = skill.getStats().getEfficiency();
			if (skill.getStats().isSpell()) {
				added = (int) (source.getModifiers().getSpellDamage().get() * eff);
			} else {
				added = (int) (source.getSenshi().getDmg() * eff);
			}
		}

		return (int) ((withLevel(source.getLevel()) + added) * mult);
	}

	@Override
	public @NotNull String toString() {
		return min() + " - " + max();
	}
}
