package com.kuuhaku.handlers.games.tabletop.games.shoukan.records;

import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.ArcadeMode;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MathHelper;
import com.kuuhaku.utils.json.JSONArray;
import com.kuuhaku.utils.json.JSONObject;
import com.kuuhaku.utils.XStringBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public record Rules(
		int mana,
		int baseHp,
		int maxCards,
		int baseManaPerTurn,
		boolean noDamage,
		boolean noEquip,
		boolean noSpell,
		boolean noField,
		boolean debug,
		ArcadeMode arcade,
		JSONArray test,
		boolean official
) {
	public Rules() {
		this(0, 5000, 5, 5, false, false, false, false, false, ArcadeMode.NONE, null, true);
	}

	public Rules(boolean official) {
		this(0, 5000, 5, 5, false, false, false, false, false, ArcadeMode.NONE, null, official);
	}

	public Rules(int mana, int baseHp, int maxCards, int baseManaPerTurn, boolean noDamage, boolean noEquip, boolean noSpell, boolean noField, boolean debug, ArcadeMode arcade, JSONArray test) {
		this(mana, baseHp, maxCards, baseManaPerTurn, noDamage, noEquip, noSpell, noField, debug, arcade, test, true);
	}

	public Rules(JSONObject json) {
		this(json, false);
	}

	public Rules(JSONObject json, boolean official) {
		this(
				MathHelper.clamp(json.getInt("mana"), 0, 20),
				MathHelper.clamp(json.getInt("hp", 5000), 500, 9999),
				MathHelper.clamp(json.getInt("cartasmax", 5), 1, 10),
				MathHelper.clamp(json.getInt("manapt", 5), 1, 20),
				json.getBoolean("semdano"),
				json.getBoolean("semequip"),
				json.getBoolean("semmagia"),
				json.getBoolean("semcampo"),
				json.getBoolean("debug"),
				ArcadeMode.get(json.getString("arcade")),
				json.getJSONArray("test"),
				official
		);
	}

	@Override
	public ArcadeMode arcade() {
		return CollectionHelper.getOr(arcade, ArcadeMode.NONE);
	}

	@Override
	public JSONArray test() {
		return CollectionHelper.getOr(test, new JSONArray());
	}

	@Override
	public String toString() {
		XStringBuilder sb = new XStringBuilder();
		Rules def = new Rules();

		if (mana != def.mana)
			sb.appendNewLine("**Mana inicial:** " + mana);
		if (baseHp != def.baseHp)
			sb.appendNewLine("**HP inicial:** " + baseHp);
		if (maxCards != def.maxCards)
			sb.appendNewLine("**Máximo de cartas:** " + maxCards);
		if (baseManaPerTurn != def.baseManaPerTurn)
			sb.appendNewLine("**Mana por turno:** " + baseManaPerTurn);
		if (noDamage != def.noDamage)
			sb.appendNewLine("**Sem dano de combate**");
		if (noEquip != def.noEquip)
			sb.appendNewLine("**Sem equipamentos**");
		if (noSpell != def.noSpell)
			sb.appendNewLine("**Sem magias**");
		if (noField != def.noField)
			sb.appendNewLine("**Sem campo**");
		if (debug != def.debug)
			sb.appendNewLine("**Debug** ");
		if (arcade != def.arcade)
			sb.appendNewLine("**Arcade:** " + StringUtils.capitalize(arcade.name().toLowerCase(Locale.ROOT)));
		if (!CollectionHelper.getOr(test, new JSONArray()).isEmpty())
			sb.appendNewLine("**Cartas iniciais:** " + test.stream()
					.map(s -> Objects.requireNonNull(CardDAO.getCard(String.valueOf(s))).getName())
					.collect(Collectors.collectingAndThen(Collectors.toList(), CollectionHelper.properlyJoin())));
		if (official != def.official)
			sb.appendNewLine("**Oficial**");

		return sb.toString();
	}
}
