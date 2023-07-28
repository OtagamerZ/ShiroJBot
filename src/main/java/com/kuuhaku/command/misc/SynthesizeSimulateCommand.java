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
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Calc;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import org.apache.commons.collections4.bag.HashBag;

import java.util.stream.DoubleStream;

@Command(
		name = "synth",
		path = "simulate",
		category = Category.MISC
)
@Signature("<mult:number:r>")
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class SynthesizeSimulateCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		double mult = args.getDouble("mult");
		if (mult <= 0) {
			event.channel().sendMessage(locale.get("error/invalid_value_low", 0)).queue();
			return;
		}

		RandomList<Byte> pool = new RandomList<>(2 * mult);
		for (byte i = 0; i < 4; i++) {
			pool.add(i, DAO.queryNative(Integer.class, "SELECT get_weight('EVOGEAR', ?1)", i + 1));
		}

		double[][] odds = new double[4][5];

		HashBag<Byte> bag = new HashBag<>();
		for (int i = 0; i < 5; i++) {
			bag.clear();

			for (int j = 0; j < 300_000; j++) {
				bag.add(pool.get());
			}

			for (byte b = 0; b < 4; b++) {
				odds[b][i] = (double) bag.getCount(b) / bag.size();
			}
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/simulated_odds"));

		for (int i = 0; i < odds.length; i++) {
			double[] results = odds[i];
			double avg = DoubleStream.of(results).average().orElseThrow();
			double var = DoubleStream.of(results).max().orElseThrow();

			eb.appendDescription("**%s:** %s%% `± %s%%`\n".formatted(
					locale.get("str/tier", i + 1),
					Calc.round(avg * 100, 2),
					Calc.round((var - avg) * 100, 2)
			));
		}

		event.channel().sendMessageEmbeds(eb.build()).queue();
	}
}
