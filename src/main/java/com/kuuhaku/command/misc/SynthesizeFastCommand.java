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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Field;
import com.kuuhaku.model.persistent.user.*;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.*;

@Command(
		name = "synth",
		path = "fast",
		category = Category.MISC
)
@Syntax("<cards:text:r>")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class SynthesizeFastCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = data.profile().getAccount().getKawaipon();
		String[] ids = args.getString("cards").toUpperCase().split(" +");

		List<StashedCard> cards = new ArrayList<>();
		List<StashedCard> stash = data.profile().getAccount().getKawaipon().getNotInUse();

		for (String id : ids) {
			Card c = DAO.find(Card.class, id);
			if (c == null) {
				String sug = Utils.didYouMean(id, "SELECT id AS value FROM v_card_names");
				if (sug == null) {
					event.channel().sendMessage(locale.get("error/unknown_card_none")).queue();
				} else {
					event.channel().sendMessage(locale.get("error/unknown_card", sug)).queue();
				}
				return;
			}

			StashedCard mat = stash.stream()
					.filter(sc -> sc.getCard().getId().equalsIgnoreCase(id) && !cards.contains(sc))
					.findFirst()
					.orElse(null);

			if (mat == null) {
				event.channel().sendMessage(locale.get("error/not_owned")).queue();
				return;
			}

			cards.add(mat);
		}

		if (cards.size() < 3) {
			event.channel().sendMessage(locale.get("error/invalid_synth_material")).queue();
			return;
		}

		double mult = SynthesizeCommand.getMult(cards);
		int field = (int) Math.round(
				cards.stream()
						.mapToDouble(sc -> {
							if (sc.getType() == CardType.FIELD || sc.isChrome()) {
								return 100 / 5d;
							}

							return 0;
						}).sum()
		);

		Account acc = data.profile().getAccount();

		double totalQ = 1;
		int chromas = 0;
		Set<Integer> delSc = new HashSet<>();
		Set<Rarity> rarities = EnumSet.noneOf(Rarity.class);
		for (StashedCard sc : cards) {
			if (sc.isChrome()) {
				chromas++;
			}

			if (sc.getType() == CardType.KAWAIPON) {
				rarities.add(sc.getCard().getRarity());
				totalQ += sc.getQuality();
			}

			delSc.add(sc.getId());
		}

		DAO.applyNative(StashedCard.class, "DELETE FROM stashed_card WHERE id IN ?1", delSc);

		if (rarities.size() >= 5) {
			UserItem item = DAO.find(UserItem.class, "CHROMATIC_ESSENCE");
			if (item != null) {
				int gained = 1 + (int) (totalQ / 10);
				acc.addItem(item, gained);
				event.channel().sendMessage(locale.get("str/received_item", gained, item.getName(locale))).queue();
			}
		}

		if (Calc.chance(field)) {
			Field f = Utils.getRandomEntry(DAO.queryAll(Field.class, "SELECT f FROM Field f WHERE f.effectOnly = FALSE"));
			StashedCard sc = new StashedCard(kp, f);
			if (Calc.chance(0.05 * chromas)) {
				sc.setChrome(true);
			}
			sc.save();

			if (sc.isChrome()) {
				kp.getAccount().setDynValue("chrome_field", true);
			}

			event.channel().sendMessage(locale.get("success/synth", f))
					.addFiles(FileUpload.fromData(IO.getBytes(f.render(locale, kp.getAccount().getDeck()), "png"), "synth.png"))
					.queue();
		} else {
			Evogear e = SynthesizeCommand.rollSynthesis(event.user(), mult, false);
			StashedCard sc = new StashedCard(kp, e);
			if (Calc.chance(0.05 * chromas)) {
				sc.setChrome(true);
			}
			sc.save();

			if (sc.isChrome()) {
				DynamicProperty prop = kp.getAccount().getDynamicProperty("highest_chrome");
				prop.setValue(Math.max(NumberUtils.toInt(prop.getValue()), e.getTier()));
				prop.save();
			}

			event.channel().sendMessage(locale.get("success/synth", e + " (" + StringUtils.repeat("★", e.getTier()) + ")"))
					.addFiles(FileUpload.fromData(IO.getBytes(e.render(locale, kp.getAccount().getDeck()), "png"), "synth.png"))
					.queue();
		}
	}
}
