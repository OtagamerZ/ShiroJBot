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
import com.kuuhaku.interfaces.annotations.GachaType;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.gacha.*;
import com.kuuhaku.model.enums.*;
import com.kuuhaku.model.enums.Currency;
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
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.reflections8.Reflections;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

@Command(
		name = "gacha",
		category = Category.MISC
)
@Signature(allowEmpty = true, value = "<type:word>")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class GachaCommand implements Executable {
	private static final Reflections refl = new Reflections("com.kuuhaku.model.common.gacha");
	private static final Set<Class<?>> gachas = refl.getTypesAnnotatedWith(GachaType.class);

	@Override
	@SuppressWarnings("unchecked")
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();

		if (!args.has("type")) {
			List<Page> pages = new ArrayList<>();

			EmbedBuilder eb = new ColorlessEmbedBuilder();
			for (Class<?> gacha : gachas) {
				GachaType type = gacha.getAnnotation(GachaType.class);
				eb.setTitle(locale.get("gacha/" + type.value()) + " (`" + type.value().toUpperCase() + "` - " + locale.get("currency/" + type.currency(), type.price()) + ")")
						.setDescription(locale.get("gacha/" + type.value() + "_desc"));

				pages.add(new InteractPage(eb.build()));
			}

			pages.sort(Comparator.comparing(p -> ((MessageEmbed) p.getContent()).getTitle()));
			Utils.paginate(pages, event.channel(), event.user());
			return;
		}

		String id = args.getString("type");
		Class<? extends Gacha> chosen = null;
		Set<String> types = new HashSet<>();
		for (Class<?> gacha : gachas) {
			GachaType type = gacha.getAnnotation(GachaType.class);
			if (type.value().equalsIgnoreCase(id)) {
				chosen = (Class<? extends Gacha>) gacha;
				break;
			}

			types.add(type.value().toUpperCase());
		}

		if (chosen == null) {
			Pair<String, Double> sug = Utils.didYouMean(id.toUpperCase(), types);
			event.channel().sendMessage(locale.get("error/unknown_gacha", sug.getFirst())).queue();
			return;
		}

		GachaType type = chosen.getAnnotation(GachaType.class);
		if (!acc.hasEnough(type.price(), type.currency())) {
			event.channel().sendMessage(locale.get("error/insufficient_" + type.currency())).queue();
			return;
		} else if (acc.getKawaipon().getCapacity() < type.prizes()) {
			event.channel().sendMessage(locale.get("error/insufficient_space")).queue();
			return;
		}

		try {
			Gacha gacha = chosen.getConstructor().newInstance();
			Utils.confirm(locale.get("question/gacha", locale.get("gacha/" + type.value()).toLowerCase(), locale.get("currency/" + type.currency(), type.price())), event.channel(),
					w -> {
						List<String> result = gacha.draw();

						BufferedImage bi = new BufferedImage(265 * result.size(), 400, BufferedImage.TYPE_INT_ARGB);
						Graphics2D g2d = bi.createGraphics();
						g2d.setRenderingHints(Constants.HD_HINTS);

						g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 20));

						for (String s : result) {
							drawCard(g2d, locale, acc, s);
						}

						if (type.currency() == Currency.CR) {
							acc.consumeCR(type.price(), "Gacha");
						} else {
							acc.consumeGems(type.price(), "Gacha");
						}

						g2d.dispose();

						event.channel()
								.sendMessage(locale.get("str/gacha_result", event.user().getAsMention()))
								.addFile(IO.getBytes(bi, "png"), "result.png")
								.queue();

						return true;
					}, event.user()
			);
		} catch (PendingConfirmationException e) {
			event.channel().sendMessage(locale.get("error/pending_confirmation")).queue();
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private void drawCard(Graphics2D g2d, I18N locale, Account acc, String card) {
		Kawaipon kp = acc.getKawaipon();
		Deck deck = acc.getCurrentDeck();
		String hPath = deck.getStyling().getFrame().isLegacy() ? "old" : "new";

		Set<CardType> types = Bit.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", card));
		CardType tp = types.stream().findFirst().orElse(CardType.KAWAIPON);

		Card c = DAO.find(Card.class, card);
		try {
			Graph.drawOutlinedString(g2d, c.getName(),
					265 / 2 - g2d.getFontMetrics().stringWidth(c.getName()) / 2, 20,
					6, Color.BLACK
			);

			switch (tp) {
				case KAWAIPON -> {
					KawaiponCard kc = new KawaiponCard(c, Calc.chance(0.1 * Spawn.getRarityMult()));
					kc.setKawaipon(kp);
					kc.save();

					g2d.drawImage(c.drawCard(kc.isChrome()), 5, 20, null);

					new StashedCard(kp, kc).save();
				}
				case EVOGEAR -> {
					Evogear e = DAO.find(Evogear.class, card);

					g2d.drawImage(e.render(locale, deck), 5, 20, null);
					if (e.getTier() == 4) {
						g2d.drawImage(IO.getResourceAsImage("kawaipon/frames/" + hPath + "/hero.png"), 5, 20, null);
					}

					new StashedCard(kp, c, tp).save();
				}
				case FIELD -> {
					Field f = DAO.find(Field.class, card);

					g2d.drawImage(f.render(locale, deck), 5, 20, null);
					g2d.drawImage(IO.getResourceAsImage("kawaipon/frames/" + hPath + "/buffed.png"), 5, 20, null);

					new StashedCard(kp, c, tp).save();
				}
			}
		} finally {
			g2d.translate(265, 0);
		}

	}
}
