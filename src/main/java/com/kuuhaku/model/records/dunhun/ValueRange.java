package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public record ValueRange(int min, int max) {
	private static final Pattern PATTERN = Pattern.compile("^(?<min>-?\\d+)(?:-(?<max>-?\\d+))?$");

	public static ValueRange parse(String expr) {
		JSONObject values = Utils.extractNamedGroups(expr, PATTERN);
		if (values.isEmpty()) throw new RuntimeException("Failed to parse expression '" + expr + "'");

		return new ValueRange(
				values.getInt("min"),
				values.getInt("max", values.getInt("min"))
		);
	}

	public int withRoll(double roll) {
		return (int) (min + (max - min) * roll);
	}

	public ValueRange multiply(double mult) {
		return multiply(mult, mult);
	}

	public ValueRange multiply(double minMult, double maxMult) {
		return new ValueRange((int) (min * minMult), (int) (max * maxMult));
	}

	@Override
	public @NotNull String toString() {
		return min() + " - " + max();
	}
}
