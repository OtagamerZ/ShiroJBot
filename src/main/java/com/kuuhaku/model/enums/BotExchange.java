/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model.enums;

import com.kuuhaku.utils.Helper;
import org.intellij.lang.annotations.Language;

import java.util.Arrays;
import java.util.Map;

public enum BotExchange {
	LORITTA(
			"297153970613387264",
			1.25f,
			"<:lori_rica:593979718919913474> \\*\\*\\|\\*\\* (?:<@(?<from>\\d+)>) Você está prestes a transferir (?<value>[\\d,]+) sonhos para <@572413282653306901>",
			"(?::money_with_wings:|\uD83D\uDCB8) \\| (?:<@(?<from>\\d+)>) Transação realizada com sucesso! <@572413282653306901> recebeu (?<value>[\\d,]+) Sonhos!",
			"sonho",
			"✅"
	);

	private final String id;
	private final float rate;
	@Language("RegExp")
	private final String trigger;
	@Language("RegExp")
	private final String confirmation;
	private final String currency;
	private final String reactionEmote;

	BotExchange(String id, float rate, @Language("RegExp") String trigger, @Language("RegExp") String confirmation, String currency, String reactionEmote) {
		this.id = id;
		this.rate = rate;
		this.trigger = trigger;
		this.confirmation = confirmation;
		this.currency = currency;
		this.reactionEmote = reactionEmote;
	}

	public String getId() {
		return id;
	}

	public float getRate() {
		return rate;
	}

	public String getTrigger() {
		return trigger;
	}

	public String getConfirmation() {
		return confirmation;
	}

	public String getCurrency() {
		return currency;
	}

	public String getReactionEmote() {
		return reactionEmote;
	}

	public static boolean isBotAdded(String id) {
		return Arrays.stream(values()).anyMatch(b -> b.id.equals(id));
	}

	public static BotExchange getById(String id) {
		return Arrays.stream(values()).filter(b -> b.id.equals(id)).findFirst().orElseThrow();
	}

	public boolean matchTrigger(String s) {
		return Helper.regex(s, trigger).find();
	}

	public Map<String, String> getTriggerValues(String s) {
		return Helper.extractNamedGroups(s, trigger);
	}

	public boolean matchConfirmation(String s) {
		return Helper.regex(s, confirmation).find();
	}

	public Map<String, String> getConfirmationValues(String s) {
		return Helper.extractNamedGroups(s, confirmation);
	}
}
