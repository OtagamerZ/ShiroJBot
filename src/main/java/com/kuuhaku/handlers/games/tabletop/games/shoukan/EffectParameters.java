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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.Main;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Phase;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class EffectParameters {
	private final Phase phase;
	private final EffectTrigger trigger;
	private final Shoukan shoukan;
	private final int index;
	private final Side side;
	private final Map<Side, Hand> hands;
	private final Map<Side, List<SlotColumn<Drawable, Drawable>>> slots;
	private final Map<Side, LinkedList<Drawable>> graveyard;
	private final Duelists duelists;
	private final TextChannel channel;

	public EffectParameters(Phase phase, EffectTrigger trigger, Shoukan shoukan, int index, Side side, Duelists duelists, TextChannel channel) {
		this.phase = phase;
		this.trigger = trigger;
		this.shoukan = shoukan;
		this.index = index;
		this.side = side;
		this.hands = shoukan.getHands();
		this.slots = shoukan.getArena().getSlots();
		this.graveyard = shoukan.getArena().getGraveyard();
		this.duelists = duelists;
		this.channel = channel;
	}

	public Phase getPhase() {
		return phase;
	}

	public EffectTrigger getTrigger() {
		return trigger;
	}

	public Shoukan getShoukan() {
		return shoukan;
	}

	public int getIndex() {
		return index;
	}

	public Side getSide() {
		return side;
	}

	public Map<Side, Hand> getHands() {
		return hands;
	}

	public Map<Side, List<SlotColumn<Drawable, Drawable>>> getSlots() {
		return slots;
	}

	public Map<Side, LinkedList<Drawable>> getGraveyard() {
		return graveyard;
	}

	public Duelists getDuelists() {
		return duelists;
	}

	public Map<Race, Integer> countTypesInGraveyard(Side side) {
		Map<Race, Integer> types = new HashMap<>();

		for (Race race : Race.values()) {
			types.put(race, (int) graveyard.get(side).stream()
					.filter(d -> d instanceof Champion)
					.map(d -> (Champion) d)
					.filter(c -> c.getRace() == race)
					.count());
		}

		return types;
	}

	public TextChannel getChannel() {
		return channel;
	}

	public void sendWebhookMessage(String message, String gif) {
		Webhook wh = Helper.getOrCreateWebhook(channel, "Shiro", Main.getInfo().getAPI());
		Card c = duelists.getAttacker().getCard();

		WebhookMessageBuilder wmb = new WebhookMessageBuilder()
				.setContent(message)
				.setAvatarUrl("https://api.%s/card?name=%s&anime=%s".formatted(System.getenv("SERVER_URL"), c.getId(), c.getAnime().name()))
				.setUsername(c.getName());

		if (gif != null) {
			InputStream is = this.getClass().getClassLoader().getResourceAsStream("shoukan/gifs/" + gif + ".gif");
			if (is != null) wmb.addFile("effect.gif", is);
		}

		try {
			if (wh == null) return;
			WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();
			wc.send(wmb.build()).get();
		} catch (InterruptedException | ExecutionException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
