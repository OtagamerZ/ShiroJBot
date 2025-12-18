/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.interfaces.annotations.GachaType;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.gacha.Gacha;
import com.kuuhaku.model.enums.*;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.*;
import com.kuuhaku.util.IO;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

@Command(
		name = "gacha",
		category = Category.MISC
)
@Syntax(allowEmpty = true, value = "<type:word:r>")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class GachaCommand implements Executable {
	@Override
	@SuppressWarnings("unchecked")
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();

		if (!args.has("type")) {
			List<Page> pages = new ArrayList<>();

			EmbedBuilder eb = new ColorlessEmbedBuilder();
			for (Class<?> gacha : Gacha.getGachas()) {
				GachaType type = gacha.getAnnotation(GachaType.class);
				if (type == null) continue;

				String price;
				if (type.currency() == Currency.ITEM) {
					UserItem item = DAO.find(UserItem.class, type.itemCostId());
					price = type.price() + " " + item.getName(locale);
				} else {
					price = locale.get("currency/" + type.currency(), type.price());
				}

				eb.setTitle(locale.get("gacha/" + type.value()) + " (`" + type.value().toUpperCase() + "` - " + price + ")")
						.setDescription(locale.get("gacha/" + type.value() + "_desc"))
						.setFooter(acc.getBalanceFooter(locale));

				pages.add(InteractPage.of(eb.build()));
			}

			pages.sort(Comparator.comparing(p -> Utils.getOr(((MessageEmbed) p.getContent()).getTitle(), "")));
			Utils.paginate(pages, event.channel(), event.user());
			return;
		}

		String id = args.getString("type");
		Class<? extends Gacha> chosen = null;
		Set<String> types = new HashSet<>();
		for (Class<?> gacha : Gacha.getGachas()) {
			GachaType type = gacha.getAnnotation(GachaType.class);
			if (type == null) continue;

			if (type.value().equalsIgnoreCase(id)) {
				chosen = (Class<? extends Gacha>) gacha;
				break;
			}

			types.add(type.value().toUpperCase());
		}

		if (chosen == null) {
			String sug = Utils.didYouMean(id.toUpperCase(), types);
			if (sug == null) {
				event.channel().sendMessage(locale.get("error/unknown_gacha_none")).queue();
			} else {
				event.channel().sendMessage(locale.get("error/unknown_gacha", sug)).queue();
			}
			return;
		}

		GachaType type = chosen.getAnnotation(GachaType.class);
		assert type != null;

		if (!acc.hasEnough(type.price(), type.currency(), type.itemCostId())) {
			event.channel().sendMessage(locale.get("error/insufficient_" + type.currency())).queue();
			return;
		} else if (acc.getKawaipon().getCapacity() < type.prizes()) {
			event.channel().sendMessage(locale.get("error/insufficient_space")).queue();
			return;
		}

		try {
			Gacha gacha = chosen.getConstructor(User.class).newInstance(event.user());
			String price;
			if (type.currency() == Currency.ITEM) {
				UserItem item = DAO.find(UserItem.class, type.itemCostId());
				price = type.price() + " " + item.getName(locale);
			} else {
				price = locale.get("currency/" + type.currency(), type.price());
			}

			Utils.confirm(locale.get("question/gacha", locale.get("gacha/" + type.value()).toLowerCase(), price), event.channel(),
					w -> {
						if (acc.hasChanged()) {
							event.channel().sendMessage(locale.get("error/account_state_changed")).queue();
							return true;
						}

						List<String> result = gacha.draw(acc);
						if (result.isEmpty()) {
							event.channel().sendMessage(locale.get("error/empty_result")).queue();
							return true;
						}

						BufferedImage bi = new BufferedImage(265 * result.size(), 400, BufferedImage.TYPE_INT_ARGB);
						Graphics2D g2d = bi.createGraphics();
						g2d.setRenderingHints(Constants.HD_HINTS);

						g2d.setFont(Fonts.OPEN_SANS.deriveBold(20));

						for (String s : result) {
							drawCard(locale, g2d, acc, type, s);
						}

						switch (type.currency()) {
							case CR -> acc.consumeCR(type.price(), "Gacha");
							case GEM -> acc.consumeGems(type.price(), "Gacha");
							case ITEM -> acc.consumeItem(type.itemCostId(), type.price());
						}

						g2d.dispose();

						event.channel()
								.sendMessage(locale.get("str/gacha_result", event.user().getAsMention()))
								.addFiles(FileUpload.fromData(IO.getBytes(bi, "png"), "result.png"))
								.queue();

						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException |
				 NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private void drawCard(I18N locale, Graphics2D g2d, Account acc, GachaType type, String id) {
		Kawaipon kp = acc.getKawaipon();
		Deck deck = acc.getDeck();
		String hPath = deck.getFrame().isLegacy() ? "old" : "new";

		CardType tp = Bit32.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", id)).stream()
				.findFirst()
				.orElse(CardType.KAWAIPON);

		Card card = DAO.find(Card.class, id);
		try {
			Graph.drawOutlinedString(g2d, card.getName(),
					265 / 2 - g2d.getFontMetrics().stringWidth(card.getName()) / 2, 20,
					6, Color.BLACK
			);

			switch (tp) {
				case KAWAIPON, SENSHI -> {
					StashedCard sc = new StashedCard(card, Calc.chance(0.1 * Spawn.getRarityMult()));
					sc.setKawaipon(kp);
					sc.save();

					g2d.drawImage(sc.render(), 5, 20, null);
				}
				case EVOGEAR -> {
					Evogear e = card.asEvogear();

					g2d.drawImage(e.render(locale, deck), 5, 20, null);
					if (e.getTier() == 4) {
						g2d.drawImage(IO.getResourceAsImage("shoukan/frames/state/" + hPath + "/hero.png"), 5, 20, null);
					}

					new StashedCard(kp, e).save();
				}
				case FIELD -> {
					Field f = card.asField();

					g2d.drawImage(f.render(locale, deck), 5, 20, null);
					g2d.drawImage(IO.getResourceAsImage("shoukan/frames/state/" + hPath + "/buffed.png"), 5, 20, null);

					new StashedCard(kp, f).save();
				}
			}
		} finally {
			g2d.translate(265, 0);
		}
	}
}
