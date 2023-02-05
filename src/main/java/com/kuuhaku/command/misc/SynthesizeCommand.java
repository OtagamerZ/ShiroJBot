/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.misc;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Spawn;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import jakarta.persistence.NoResultException;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Command(
		name = "synth",
		category = Category.MISC
)
@Signature("<card:word:r> <card:word:r> <card:word:r> <confirm:word>[y]")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class SynthesizeCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		List<StashedCard> cards = new ArrayList<>();
		CompletableFuture<Void> setup = new CompletableFuture<>();
		for (Object entry : args.getJSONArray("card")) {
			if (entry instanceof String card) {
				Card c = DAO.find(Card.class, card.toUpperCase());
				if (c == null) {
					List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card WHERE rarity NOT IN ('ULTIMATE', 'NONE')");

					Pair<String, Double> sug = Utils.didYouMean(card.toUpperCase(), names);
					event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
					return;
				}

				AtomicBoolean failed = new AtomicBoolean();
				CompletableFuture<Void> select = new CompletableFuture<>();
				List<StashedCard> stash = data.profile().getAccount().getKawaipon().getNotInUse();
				stash.removeIf(cards::contains);
				Utils.selectOption(args.containsKey("confirm"), locale, event.channel(), stash, c, event.user())
						.thenAccept(sc -> {
							if (sc == null) {
								event.channel().sendMessage(locale.get("error/invalid_value")).queue();
								failed.set(true);
								return;
							} else if (cards.contains(sc)) {
								event.channel().sendMessage(locale.get("error/twice_added")).queue();
								failed.set(true);
								return;
							}

							cards.add(sc);
							select.complete(null);
						})
						.exceptionally(t -> {
							if (!(t.getCause() instanceof NoResultException)) {
								Constants.LOGGER.error(t, t);
							}

							event.channel().sendMessage(locale.get("error/not_owned")).queue();
							failed.set(true);
							select.complete(null);
							return null;
						});

				try {
					select.get();
					if (failed.get()) {
						return;
					}

					setup.complete(null);
				} catch (InterruptedException | ExecutionException ignore) {
				}
			}
		}

		try {
			setup.get();
		} catch (InterruptedException | ExecutionException ignore) {
		}

		if (cards.size() != 3) {
			event.channel().sendMessage(locale.get("error/invalid_synth_material")).queue();
			return;
		}

		try {
			double mult = getMult(cards);
			int field = (int) Math.round(
					cards.stream()
							.mapToDouble(sc -> {
								if (sc.getKawaiponCard() != null && sc.getKawaiponCard().isChrome()) {
									return 100 / 3d;
								}

								return 0;
							}).sum()
			);

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setDescription(locale.get("str/synthesis_info", Utils.roundToString(mult, 2), field));

			Utils.confirm(locale.get("question/synth"), eb.build(), event.channel(), w -> {
						Kawaipon kp = data.profile().getAccount().getKawaipon();

						for (StashedCard sc : cards) {
							if (sc.getType() == CardType.KAWAIPON) {
								KawaiponCard kc = sc.getKawaiponCard(false);
								if (kc != null) {
									kc.delete();
								}
							}

							sc.delete();
						}

						if (Calc.chance(field)) {
							Field f = Utils.getRandomEntry(DAO.queryAll(Field.class, "SELECT f FROM Field f WHERE f.effect = FALSE"));
							new StashedCard(kp, f.getCard(), CardType.FIELD).save();

							event.channel().sendMessage(locale.get("success/synth", f))
									.addFile(IO.getBytes(f.render(locale, kp.getAccount().getCurrentDeck()), "png"), "synth.png")
									.queue();
						} else {
							Evogear e = rollSynthesis(mult);
							new StashedCard(kp, e.getCard(), CardType.EVOGEAR).save();

							event.channel().sendMessage(locale.get("success/synth", e + " (" + StringUtils.repeat("★", e.getTier()) + ")"))
									.addFile(IO.getBytes(e.render(locale, kp.getAccount().getCurrentDeck()), "png"), "synth.png")
									.queue();
						}

						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}

	public static Evogear rollSynthesis(List<StashedCard> cards) {
		return rollSynthesis(getMult(cards));
	}

	public static Evogear rollSynthesis(double mult) {
		RandomList<Evogear> pool = new RandomList<>(1 / (3 / mult));
		List<Evogear> evos = DAO.findAll(Evogear.class);
		for (Evogear evo : evos) {
			if (evo.getTier() <= 0) continue;

			pool.add(evo, 5 - evo.getTier());
		}

		return pool.get();
	}

	private static double getMult(List<StashedCard> cards) {
		double inc = 1;
		double more = 1 * Spawn.getRarityMult();

		for (StashedCard sc : cards) {
			switch (sc.getType()) {
				case KAWAIPON -> {
					KawaiponCard kc = sc.getKawaiponCard();
					int rarity = sc.getCard().getRarity().getIndex();

					if (kc != null) {
						if (kc.isChrome()) {
							more *= 1 + rarity * kc.getQuality() / 200;
						} else {
							inc += rarity * kc.getQuality() / 200;
						}
					}
				}
				case EVOGEAR -> {
					Evogear ev = DAO.find(Evogear.class, sc.getCard().getId());
					inc += ev.getTier() / 6d;
				}
				case FIELD -> more *= 1.25;
			}
		}

		return 1 * inc * more;
	}
}