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

package com.kuuhaku.command.commands.discord.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.common.DailyQuest;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Map;

public class CatchKawaiponCommand implements Executable {

	public CatchKawaiponCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public CatchKawaiponCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public CatchKawaiponCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public CatchKawaiponCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		TextChannel chn = gc.getCanalKawaipon().isBlank() ? null : guild.getTextChannelById(gc.getCanalKawaipon());
		KawaiponCard kc = Main.getInfo().getCurrentCard().getIfPresent(guild.getId());

		if (chn != null && !channel.getId().equals(chn.getId())) {
			channel.sendMessage("❌ | O spawn de Kawaipons está configurado no canal " + chn.getAsMention() + ", você não pode coletá-los aqui.").queue();
			return;
		} else if (kc == null) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-card")).queue();
			return;
		}

		int cost = kc.getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE * (kc.isFoil() ? 2 : 1);
		if (acc.getTotalBalance() < cost) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
			return;
		} else if (kp.getCards().contains(kc)) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_card-owned")).queue();
			return;
		}

		Main.getInfo().getCurrentCard().invalidate(guild.getId());
		kp.addCard(kc);
		acc.consumeCredit(cost, this.getClass());

		if (!acc.hasCompletedQuests()) {
			Map<DailyTask, Integer> pg = acc.getDailyProgress();
			pg.compute(DailyTask.CARD_TASK, (k, v) -> Helper.getOr(v, 0) + 1);
			pg.compute(DailyTask.ANIME_TASK, (k, v) -> {
				DailyQuest dq = DailyQuest.getQuest(author.getId());
				if (kc.getCard().getAnime() == dq.getChosenAnime())
					return Helper.getOr(v, 0) + 1;
				else
					return v;
			});
			acc.setDailyProgress(pg);
		}

		KawaiponDAO.saveKawaipon(kp);
		AccountDAO.saveAccount(acc);

		channel.sendMessage("✅ | " + author.getAsMention() + " adquiriu a carta `" + kc.getName() + "` com sucesso!").queue();
	}
}
