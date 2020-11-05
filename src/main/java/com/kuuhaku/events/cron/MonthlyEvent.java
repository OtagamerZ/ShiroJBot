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
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.controller.sqlite.KGotchiDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

public class MonthlyEvent implements Job {
	public static JobDetail markWinner;

	@Override
	public void execute(JobExecutionContext context) {
		call();
	}

	public static void call() {
		if (ExceedDAO.verifyMonth()) {
			ExceedDAO.markWinner(ExceedDAO.findWinner());
			Helper.logger(MonthlyEvent.class).info("Vencedor mensal: " + ExceedDAO.getWinner());

			String ex = ExceedDAO.getWinner();
			ExceedDAO.getExceedMembers(ExceedEnum.getByName(ex)).forEach(em -> {
						User u = Main.getInfo().getUserByID(em.getId());
						if (u != null) u.openPrivateChannel().queue(c -> {
							try {
								c.sendMessage("""
										O seu Exceed foi campeão neste mês, parabéns!
										Todos da %s ganharão experiência em dobro durante 1 semana além de isenção de taxas.
										""".formatted(ex)).queue(null, Helper::doNothing);
							} catch (Exception ignore) {
							}
						});
					}
			);

			ExceedDAO.getExceedMembers().forEach(em -> {
				if (Main.getInfo().getUserByID(em.getId()) == null) ExceedDAO.removeMember(em);
			});

			ExceedDAO.unblock();
		}

		List<Kawaigotchi> kgs = KGotchiDAO.getAllKawaigotchi();

		kgs.forEach(k -> {
			try {
				if (k.getDiedAt().plusMonths(1).isBefore(LocalDateTime.now()) || k.getOffSince().plusMonths(1).isBefore(LocalDateTime.now()))
					com.kuuhaku.controller.postgresql.KGotchiDAO.deleteKawaigotchi(k);
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
			chn.sendMessage("""
					As dezenas sorteadas foram `%s`.
					Como não houveram vencedores, o prêmio de %d créditos será acumulado para o próximo mês!
					""".formatted(String.join(" ", dozens), value.getValue())
			).queue();
		} else if (winners.size() == 1) {
			chn.sendMessage("""
					As dezenas sorteadas foram `%s`.
					O vencedor de %d créditos foi %s, parabéns!
					""".formatted(String.join(" ", dozens), value.getValue(), Main.getInfo().getUserByID(winners.get(0).getUid()).getName())
			).queue();
		} else {
			chn.sendMessage("""
					As dezenas sorteadas foram `%s`.
					Os %d vencedores dividirão em partes iguais o prêmio de %d créditos, parabéns!!
					""".formatted(String.join(" ", dozens), winners.size(), value.getValue())
			).queue();
		}

		winners.forEach(l -> {
			Account acc = AccountDAO.getAccount(l.getUid());
			acc.addCredit(value.getValue() / winners.size(), MonthlyEvent.class);
			AccountDAO.saveAccount(acc);

			Main.getInfo().getUserByID(l.getUid()).openPrivateChannel().queue(c -> {
				c.sendMessage("Você ganhou " + (value.getValue() / winners.size()) + " créditos na loteria, parabéns!").queue(null, Helper::doNothing);
			}, Helper::doNothing);
		});

		LotteryDAO.closeLotteries();
		DynamicParameterDAO.setParam("last_upd_month", String.valueOf(OffsetDateTime.now().atZoneSameInstant(ZoneId.of("GMT-3")).getMonthValue()));

		List<Blacklist> blacklist = BlacklistDAO.getBlacklist();
		for (Blacklist bl : blacklist) {
			if (bl.canClear()) {
				Account acc = AccountDAO.getAccount(bl.getId());
				Kawaipon kp = KawaiponDAO.getKawaipon(bl.getId());
				Kawaigotchi kg = KGotchiDAO.getKawaigotchi(bl.getId());
				ExceedMember em = ExceedDAO.getExceedMember(bl.getId());
				Tags t = TagDAO.getTagById(bl.getId());

				AccountDAO.removeAccount(acc);
				MemberDAO.clearMember(bl.getId());
				KawaiponDAO.removeKawaipon(kp);
				if (kg != null)
					com.kuuhaku.controller.postgresql.KGotchiDAO.deleteKawaigotchi(kg);
				ExceedDAO.removeMember(em);
				TagDAO.clearTags(t);
				WaifuDAO.voidCouple(bl.getId());
				TokenDAO.voidToken(bl.getId());
			}
		}
	}
}
