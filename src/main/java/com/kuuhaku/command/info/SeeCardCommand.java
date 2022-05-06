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

package com.kuuhaku.command.info;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Kawaipon;
import com.kuuhaku.model.persistent.user.KawaiponCard;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.utils.IO;
import com.kuuhaku.utils.ImageFilters;
import com.kuuhaku.utils.Utils;
import com.kuuhaku.utils.XStringBuilder;
import com.kuuhaku.utils.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

@Command(
		name = "see",
		category = Category.INFO
)
@Signature("<card:word:r> <foil:word>[n,c,s]")
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ATTACH_FILES
})
public class SeeCardCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Kawaipon kp = DAO.find(Kawaipon.class, event.user().getId());

		Card card = DAO.find(Card.class, args.getString("card").toUpperCase(Locale.ROOT));
		if (card == null) {
			List<String> names = DAO.queryAllNative(String.class, "SELECT id FROM card");

			Pair<String, Double> sug = Utils.didYouMean(args.getString("card").toUpperCase(Locale.ROOT), names);
			event.channel().sendMessage(locale.get("error/unknown_card", sug.getFirst())).queue();
			return;
		}

		boolean foil = args.getString("foil", "n").equalsIgnoreCase("c");
		KawaiponCard kc = kp.getCard(card, foil);

		BufferedImage bi;
		if (kc == null) {
			bi = ImageFilters.silhouette(card.drawCard(foil));
		} else {
			bi = card.drawCard(foil);
		}

		int stored = DAO.queryNative(Integer.class, "SELECT COUNT(1) FROM stashed_card WHERE kawaipon_uid = ?1 AND card_id = ?2",
				event.user().getId(),
				card.getId()
		);
		EmbedBuilder eb = new EmbedBuilder()
				.setAuthor(locale.get("str/in_stash", stored))
				.setTitle(card.getName() + " (" + card.getAnime() + ")")
				.setImage("attachment://card.png");

		if (kc != null) {
			XStringBuilder sb = new XStringBuilder();
			sb.appendNewLine(locale.get("str/quality", kc.getQuality()));
			sb.appendNewLine(locale.get("str/suggested_price", kc.getSuggestedPrice()));

			eb.addField(locale.get("str/information"), sb.toString(), true);
		}

		Senshi senshi = DAO.query(Senshi.class, "SELECT s FROM Senshi s WHERE s.card = ?1", card);
		if (senshi != null) {
			eb.addField(locale.get("str/shoukan_enabled"), locale.get("icon/success") + " " + locale.get("str/yes"), true);
		} else {
			eb.addField(locale.get("str/shoukan_enabled"), locale.get("icon/error") + " " + locale.get("str/no"), true);
		}

		event.channel().sendMessageEmbeds(eb.build())
				.addFile(IO.getBytes(bi, "png"), "card.png")
				.queue();
	}
}
