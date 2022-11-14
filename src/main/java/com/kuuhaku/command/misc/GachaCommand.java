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

import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.PendingConfirmationException;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.gacha.*;
import com.kuuhaku.model.enums.*;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.*;
import com.kuuhaku.util.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Command(
		name = "gacha",
		category = Category.MISC
)
@Signature("<type:word>[basic,premium,summoner,daily]")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class GachaCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();

		if (!args.has("type")) {
			List<Page> pages = new ArrayList<>();

			EmbedBuilder eb = new ColorlessEmbedBuilder();
			for (String type : new String[]{"basic", "premium", "summoner", "daily"}) {
				Gacha<String> gacha = switch (type) {
					case "premium" -> new PremiumGacha();
					case "summoner" -> new SummonersGacha();
					case "daily" -> new DailyGacha();
					default -> new BasicGacha();
				};

				eb.setTitle(locale.get("gacha/" + type) + " (`" + type.toUpperCase() + "` - " + locale.get("currency/" + gacha.getCurrency(), gacha.getPrice()) + ")")
						.setDescription(locale.get("gacha/" + type + "_desc"));

				pages.add(new InteractPage(eb.build()));
			}

			Utils.paginate(pages, event.channel(), event.user());
			return;
		}

		String type = args.getString("type");
		Gacha<String> gacha = switch (type) {
			case "premium" -> new PremiumGacha();
			case "summoner" -> new SummonersGacha();
			case "daily" -> new DailyGacha();
			default -> new BasicGacha();
		};

		if (!acc.hasEnough(gacha.getPrice(), gacha.getCurrency())) {
			event.channel().sendMessage(locale.get("error/insufficient_" + gacha.getCurrency())).queue();
			return;
		} else if (acc.getKawaipon().getCapacity() < gacha.getPrizeCount()) {
			event.channel().sendMessage(locale.get("error/insufficient_space")).queue();
			return;
		}

		try {
			Utils.confirm(locale.get("question/gacha", locale.get("gacha/" + type).toLowerCase(), locale.get("currency/" + gacha.getCurrency(), gacha.getPrice())), event.channel(),
					w -> {
						List<String> result = gacha.draw();

						BufferedImage bi = new BufferedImage(265 * result.size(), 400, BufferedImage.TYPE_INT_ARGB);
						Graphics2D g2d = bi.createGraphics();
						g2d.setRenderingHints(Constants.HD_HINTS);

						g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 20));

						List<StashedCard> acts = new ArrayList<>();
						for (String s : result) {
							acts.add(drawCard(g2d, locale, acc, s, type));
						}

						for (StashedCard act : acts) {
							act.save();
						}

						if (gacha.getCurrency() == Currency.CR) {
							acc.consumeCR(gacha.getPrice(), "Gacha");
						} else {
							acc.consumeGems(gacha.getPrice(), "Gacha");
						}

						g2d.dispose();

						event.channel()
								.sendMessage(locale.get("str/gacha_result", event.user().getAsMention()))
								.addFile(IO.getBytes(bi, "webp"), "result.webp")
								.queue();

						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		}
	}

	private StashedCard drawCard(Graphics2D g2d, I18N locale, Account acc, String card, String type) {
		Kawaipon kp = acc.getKawaipon();
		Deck deck = acc.getCurrentDeck();
		String hPath = deck.getStyling().getFrame().isLegacy() ? "old" : "new";

		CardType tp;
		if (type.equalsIgnoreCase("basic")) {
			tp = CardType.KAWAIPON;
		} else {
			Set<CardType> types = Bit.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", card));
			tp = types.stream().findFirst().orElse(CardType.KAWAIPON);
		}

		Card c = DAO.find(Card.class, card);
		try {
			Graph.drawOutlinedString(g2d, c.getName(),
					265 / 2 - g2d.getFontMetrics().stringWidth(c.getName()) / 2, 20,
					6, Color.BLACK
			);

			switch (tp) {
				case KAWAIPON -> {
					KawaiponCard kc = new KawaiponCard(c, Calc.chance(0.1 * (1 - Spawn.getRarityMult())));

					g2d.drawImage(c.drawCard(kc.isChrome()), 5, 20, null);

					kc.setKawaipon(kp);
					return new StashedCard(kp, kc);
				}
				case EVOGEAR -> {
					Evogear e = DAO.find(Evogear.class, card);

					g2d.drawImage(e.render(locale, deck), 5, 20, null);
					if (e.getTier() == 4) {
						g2d.drawImage(IO.getResourceAsImage("kawaipon/frames/" + hPath + "/hero.png"), 5, 20, null);
					}

					return new StashedCard(kp, c, tp);
				}
				case FIELD -> {
					Field f = DAO.find(Field.class, card);

					g2d.drawImage(f.render(locale, deck), 5, 20, null);
					g2d.drawImage(IO.getResourceAsImage("kawaipon/frames/" + hPath + "/buffed.png"), 5, 20, null);

					return new StashedCard(kp, c, tp);
				}
			}
		} finally {
			g2d.translate(265, 0);
		}

		return null;
	}
}