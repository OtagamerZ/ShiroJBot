/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.enums.SupportTier;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Lottery;
import com.kuuhaku.model.persistent.LotteryValue;
import com.kuuhaku.utils.ShiroInfo;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.entities.TextChannel;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MonthlyEvent implements Job {

	@Override
	public void execute(JobExecutionContext context) {
		ClanDAO.resetScores();

		List<String> ns = List.of(
				"00", "01", "02", "03", "04", "05",
				"06", "07", "08", "09", "10", "11",
				"12", "13", "14", "15", "16", "17",
				"18", "19", "20", "21", "22", "23",
				"24", "25", "26", "27", "28", "29",
				"30"
		);
		List<String> dozens = CollectionHelper.getRandomN(ns, 6, 1);
		dozens.sort(Comparator.comparingInt(Integer::parseInt));
		String result = String.join(",", dozens);
		List<Lottery> winners = Lottery.queryAll(Lottery.class, "SELECT l FROM Lottery l WHERE l.valid = TRUE");

		winners.removeIf(l ->
				!Arrays.stream(l.getDozens().split(",")).allMatch(result::contains)
		);

		LotteryValue value = LotteryValue.find(LotteryValue.class, 0);

		TextChannel chn = Main.getGuildByID(ShiroInfo.SUPPORT_SERVER_ID).getTextChannelById(ShiroInfo.ANNOUNCEMENT_CHANNEL_ID);

		String msg;

		assert chn != null;
		if (winners.isEmpty())
			msg = """
					As dezenas sorteadas foram `%s`.
					Como não houveram vencedores, o prêmio de %s CR será acumulado para o próximo mês!
					""".formatted(String.join(" ", dozens), StringHelper.separate(value.getValue()));
		else if (winners.size() == 1)
			msg = """
					As dezenas sorteadas foram `%s`.
					O vencedor de %s CR foi %s, parabéns!
					""".formatted(String.join(" ", dozens), StringHelper.separate(value.getValue()), Main.getUserByID(winners.get(0).getUid()).getName());
		else
			msg = """
					As dezenas sorteadas foram `%s`.
					Os %s vencedores dividirão em partes iguais o prêmio de %s CR, parabéns!!
					""".formatted(String.join(" ", dozens), winners.size(), StringHelper.separate(value.getValue()));

		chn.sendMessage(msg).queue();
		MiscHelper.broadcast(msg, null);

		for (Lottery l : winners) {
			Account acc = Account.find(Account.class, l.getUid());
			acc.addCredit(value.getValue() / winners.size(), MonthlyEvent.class);
			acc.save();

			Main.getUserByID(l.getUid()).openPrivateChannel().queue(c -> c.sendMessage("Você ganhou " + StringHelper.separate(value.getValue() / winners.size()) + " CR na loteria, parabéns!").queue(null, MiscHelper::doNothing), MiscHelper::doNothing);
		}

		Lottery.apply("UPDATE Lottery l SET l.valid = FALSE");

		for (Map.Entry<String, SupportTier> entry : ShiroInfo.getSupports().entrySet()) {
			Account acc = Account.find(Account.class, entry.getKey());
			acc.addCredit(entry.getValue().getSalary(), MonthlyEvent.class);
			acc.save();
		}
	}
}
