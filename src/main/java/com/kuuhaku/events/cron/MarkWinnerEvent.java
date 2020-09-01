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

package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.LotteryDAO;
import com.kuuhaku.controller.sqlite.KGotchiDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Lottery;
import com.kuuhaku.model.persistent.LotteryValue;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class MarkWinnerEvent implements Job {
	public static JobDetail markWinner;

	@Override
	public void execute(JobExecutionContext context) {
		ExceedDAO.markWinner(ExceedDAO.findWinner());
		Helper.logger(this.getClass()).info("Vencedor mensal: " + ExceedDAO.getWinner());

		String ex = ExceedDAO.getWinner();
		ExceedDAO.getExceedMembers(ExceedEnums.getByName(ex)).forEach(em -> {
					User u = Main.getInfo().getUserByID(em.getId());
					if (u != null) u.openPrivateChannel().queue(c -> {
						try {
							c.sendMessage("O seu Exceed foi campeão neste mês, parabéns!\n" +
									"Todos da " + ex + " ganharão experiência em dobro durante 1 semana.").queue();
						} catch (Exception ignore) {
						}
					});
				}
		);

		ExceedDAO.unblock();

		List<Kawaigotchi> kgs = KGotchiDAO.getAllKawaigotchi();

		kgs.forEach(k -> {
			if (k.getDiedAt().plusMonths(1).isBefore(LocalDateTime.now()) || k.getOffSince().plusMonths(1).isBefore(LocalDateTime.now()))
				KGotchiDAO.deleteKawaigotchi(k);
			try {
				k.update(Main.getInfo().getMemberByID(k.getUserId()));
			} catch (NullPointerException ignore) {
			}
		});

		String[] dozens = new String[6];
		for (int i = 0; i < 6; i++) {
			dozens[i] = StringUtils.leftPad(String.valueOf(Helper.rng(60, false)), 2, "0");
		}
		String result = String.join(",", dozens);
		List<Lottery> winners = LotteryDAO.getLotteries();

		winners.removeIf(l ->
				!Arrays.stream(l.getDozens().split(",")).allMatch(result::contains)
		);

		LotteryValue value = LotteryDAO.getLotteryValue();

		TextChannel chn = Main.getInfo().getGuildByID(ShiroInfo.getSupportServerID()).getTextChannelById(ShiroInfo.getAnnouncementChannelID());

		assert chn != null;
		if (winners.size() == 0) {
			chn.sendMessage(
					"As dezenas sorteadas foram `" + String.join(" ", dozens) + "`.\n" +
							"Como não houveram vencedores, o prêmio de " + value.getValue() + " créditos será acumulado para o próximo mês!"
			).queue();
		} else if (winners.size() == 1) {
			chn.sendMessage(
					"As dezenas sorteadas foram `" + String.join(" ", dozens) + "`.\n" +
							"O vencedor de " + value.getValue() + " créditos foi " + Main.getInfo().getUserByID(winners.get(0).getUid()).getName() + ", parabéns!"
			).queue();
		} else {
			chn.sendMessage(
					"As dezenas sorteadas foram `" + String.join(" ", dozens) + "`.\n" +
							"Os " + winners.size() + " vencedores dividirão em partes iguais o prêmio de " + value.getValue() + " créditos, parabéns!!"
			).queue();
		}

		winners.forEach(l -> {
			Account acc = AccountDAO.getAccount(l.getUid());
			acc.addCredit(value.getValue() / winners.size(), this.getClass());
			AccountDAO.saveAccount(acc);

			Main.getInfo().getUserByID(l.getUid()).openPrivateChannel().queue(c -> {
				c.sendMessage("Você ganhou " + (value.getValue() / winners.size()) + " créditos na loteria, parabéns!").queue(null, Helper::doNothing);
			}, Helper::doNothing);
		});

		LotteryDAO.closeLotteries();
	}
}
