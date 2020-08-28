/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.utils;

import org.intellij.lang.annotations.Language;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public enum BotExchange {
	LORITTA(
			"297153970613387264",
			1,
			"(<:lori_rica:593979718919913474> \\*\\*\\|\\*\\* )<(@|@!)\\d+>( Você está prestes a transferir )\\d+( sonhos para )<(@|@!)\\d+>(!)",
			"(\uD83D\uDCB8 \\*\\*\\|\\*\\* )<(@|@!)\\d+>( Transação realizada com sucesso! )<(@|@!)\\d+>( recebeu \\*\\*)",
			"sonho",
			"✅"
	);

	private final String id;
	private final float rate;
	private final String trigger;
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

	public Predicate<String> matchTrigger() {
		return Pattern.compile(trigger).asMatchPredicate();
	}

	public Predicate<String> matchConfirmation() {
		return Pattern.compile(confirmation).asMatchPredicate();
	}
}
