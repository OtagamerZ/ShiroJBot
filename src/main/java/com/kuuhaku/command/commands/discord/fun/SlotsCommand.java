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
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.PStateDAO;
import com.kuuhaku.controller.postgresql.SlotsDAO;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.Slot;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Slots;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.kuuhaku.model.enums.Slot.*;

@Command(
		name = "roleta",
		aliases = {"slots"},
		usage = "req_bet",
		category = Category.FUN
)
@Requires({Permission.MESSAGE_EXT_EMOJI})
public class SlotsCommand implements Executable {
	private final List<Slot> rolled = new ArrayList<>();

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			EmbedBuilder eb = new ColorlessEmbedBuilder();
			eb.setDescription(prizeTable());
			eb.setTitle("Tabela de prêmios");
			eb.setFooter("Use `" + prefix + "slots VALOR` para jogar (Valor mínimo: 1000 créditos)");

			channel.sendMessage(eb.build()).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0]) || Integer.parseInt(args[0]) < 1000) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_slots-invalid-number")).queue();
			return;
		} else if (Main.getInfo().gameInProgress(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		AtomicLong bet = new AtomicLong(Long.parseLong(args[0]));

		if (acc.getTotalBalance() < bet.get()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
			return;
		}

		Slots slt = SlotsDAO.getSlots();
		acc.consumeCredit(bet.get(), this.getClass());
		AccountDAO.saveAccount(acc);
		slt.addToPot(bet.get());

		rollSlots();

		Runnable r = () -> {
			Slot combo = Slot.getCombo(rolled);
			if (combo != null) {
				Account facc = AccountDAO.getAccount(author.getId());
				Slots slts = SlotsDAO.getSlots();

				if (combo == JACKPOT) {
					long prize = slt.jackpot();

					facc.addCredit(prize, this.getClass());

					channel.sendMessage(
							"""
									%s
									         
									<a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503>
									Seu prêmio é de __**%s créditos**__!         
									<a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503>
									""".formatted(combo.getMessage(), Helper.separate(prize))
					).queue();
				} else {
					long prize = Math.round(bet.get() * combo.getMultiplier());

					facc.addCredit(prize, this.getClass());
					slts.addToPot(Math.round(bet.get() * 0.75));

					channel.sendMessage("""
							%s
							Seu prêmio é de __**%s créditos**__!
							""".formatted(combo.getMessage(), Helper.separate(prize))).queue();
				}

				SlotsDAO.saveSlots(slts);
				AccountDAO.saveAccount(facc);

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(combo.getInfluence());
					PStateDAO.savePoliticalState(ps);
				}
			} else {
				channel.sendMessage("Poxa, parece que você não teve sorte hoje. Volte sempre!").queue();

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = com.kuuhaku.controller.postgresql.PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(false);
					PStateDAO.savePoliticalState(ps);
				}
			}
		};

		final String top = "<:corner_down_right:747882840451973170><:horizontal_top:747882840351572020><:cross_down:747882840477138994><:horizontal_top:747882840351572020><:cross_down:747882840477138994><:horizontal_top:747882840351572020><:corner_down_left:747882840380932286>";
		final String bottom = "<:corner_up_right:747882840439652522><:horizontal_bottom:747882840565350430><:cross_up:747882840489853000><:horizontal_bottom:747882840565350430><:cross_up:747882840489853000><:horizontal_bottom:747882840565350430><:corner_up_left:747882840326406246>";

		channel.sendMessage(":white_flower: | **Aposta de " + author.getAsMention() + ": __" + Helper.separate(args[0]) + "__**").queue(s -> {
			s.editMessage("""
					%s
					     
					**Prêmio acumulado: __%s__**
					%s
					%s
					%s
					""".formatted(s.getContentRaw(), Helper.separate(slt.getPot()), top, showSlots(0), bottom))
					.queue(null, Helper::doNothing);
			for (int i = 1; i < 4; i++) {
				if (i != 3)
					s.editMessage("""
							%s
							     
							**Prêmio acumulado: __%s__**
							%s
							%s
							%s
							""".formatted(s.getContentRaw(), Helper.separate(slt.getPot()), top, showSlots(i), bottom))
							.queueAfter(3 + (3 * i), TimeUnit.SECONDS, null, Helper::doNothing);
				else
					s.editMessage("""
							%s
							     
							**Prêmio acumulado: __%s__**
							%s
							%s
							%s
							""".formatted(s.getContentRaw(), Helper.separate(slt.getPot()), top, showSlots(i), bottom))
							.queueAfter(3 + (3 * i), TimeUnit.SECONDS, f -> r.run(), Helper::doNothing);
			}
		});
	}

	private String showSlots(int phase) {
		return switch (phase) {
			case 0 -> "<:vertical_right:747882840569544714>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical_left:747882840414486571>";
			case 1 -> "<:vertical_right:747882840569544714>" + rolled.get(0) + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical_left:747882840414486571>";
			case 2 -> "<:vertical_right:747882840569544714>" + rolled.get(0) + "<:vertical:747883406632943669>" + rolled.get(1) + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical_left:747882840414486571>";
			case 3 -> "<:vertical_right:747882840569544714>" + rolled.get(0) + "<:vertical:747883406632943669>" + rolled.get(1) + "<:vertical:747883406632943669>" + rolled.get(2) + "<:vertical_left:747882840414486571>";
			default -> "";
		};
	}

	private void rollSlots() {
		rolled.clear();
		rolled.addAll(Helper.getRandomN(List.of(Slots.getSlots()), 3));
	}

	private String prizeTable() {
		return """
				%s%s%s -> x%s
				%s%s%s -> x%s
				%s%s%s -> x%s
				%s%s%s -> x%s
				%s%s%s -> x%s
				%s%s%s -> x%s
				%s%s%s -> x%s
				%s%s%s -> x%s
				%s%s%s -> JACKPOT!
				"""
				.formatted(
						LEMON, LEMON, LEMON, LEMON.getMultiplier(),
						WATERMELON, WATERMELON, WATERMELON, WATERMELON.getMultiplier(),
						CHERRY, CHERRY, CHERRY, CHERRY.getMultiplier(),
						HEART, HEART, HEART, HEART.getMultiplier(),
						BELL, BELL, BELL, BELL.getMultiplier(),
						BAR, BAR, BAR, BAR.getMultiplier(),
						HORSESHOE, HORSESHOE, HORSESHOE, HORSESHOE.getMultiplier(),
						DIAMOND, DIAMOND, DIAMOND, DIAMOND.getMultiplier(),
						JACKPOT, JACKPOT, JACKPOT
				);
	}
}
