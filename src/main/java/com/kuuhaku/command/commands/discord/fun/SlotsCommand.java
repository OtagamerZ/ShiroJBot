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
import com.kuuhaku.controller.postgresql.SlotsDAO;
import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Slots;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Command(
		name = "roleta",
		aliases = {"slots"},
		usage = "req_bet",
		category = Category.FUN
)
@Requires({Permission.MESSAGE_EXT_EMOJI})
public class SlotsCommand implements Executable {
	private List<String> rolled = new ArrayList<>();

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			EmbedBuilder eb = new ColorlessEmbedBuilder();
			eb.setDescription(prizeTable());
			eb.setTitle("Tabela de prêmios");
			eb.setFooter("Use `" + prefix + "slots VALOR` para jogar (Valor mínimo: 250 créditos. Para utilizar as 5 casas da roleta, aposte 1000 créditos ou mais)");

			channel.sendMessage(eb.build()).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0]) || Integer.parseInt(args[0]) < 250) {
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

		long initialBet = bet.get();
		boolean highbet = bet.get() >= 1000;
		Slots slt = SlotsDAO.getSlots();
		acc.consumeCredit(bet.get(), this.getClass());
		AccountDAO.saveAccount(acc);
		slt.addToPot(bet.get());

		rollSlots();

		Runnable r = () -> {
			if (!highbet) rolled = rolled.subList(1, rolled.size() - 1);

			int lemon = Collections.frequency(rolled, Slots.LEMON);
			int watermelon = Collections.frequency(rolled, Slots.WATERMELON);
			int cherry = Collections.frequency(rolled, Slots.CHERRY);
			int heart = Collections.frequency(rolled, Slots.HEART);
			int bell = Collections.frequency(rolled, Slots.BELL);
			int bar = Collections.frequency(rolled, Slots.BAR);
			int horseshoe = Collections.frequency(rolled, Slots.HORSESHOE);
			int diamond = Collections.frequency(rolled, Slots.DIAMOND);
			int jackpot = Collections.frequency(rolled, Slots.JACKPOT);

			String msg = "";

			boolean win = false;
			if (lemon >= 3) {
				bet.set(Math.round(bet.get() * 0.8f));
				msg = "Eita, parece que você está azedo hoje!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(1);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (watermelon >= 3) {
				bet.set(Math.round(bet.get() * 1.5f));
				msg = "E temos três melancias!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(2);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (cherry >= 3) {
				bet.set(bet.get() * 2);
				msg = "Três cerejas no bolo!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(4);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (heart >= 3) {
				bet.set(Math.round(bet.get() * 2.75f));
				msg = "Três corações apaixonados!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(6);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (bell >= 3) {
				bet.set(bet.get() * 4);
				msg = "Toquem os sinos!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(16);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (bar >= 3) {
				bet.set(bet.get() * 7);
				msg = "Chamem a polícia, temos um sortudo!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(45);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (horseshoe >= 3) {
				bet.set(bet.get() * 12);
				msg = "Alguem sequestrou um duende, três ferraduras de ouro!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(65);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (diamond >= 3) {
				bet.set(bet.get() * 20);
				msg = "Assalto ao banco da sorte, temos três diamantes!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(80);
					PStateDAO.savePoliticalState(ps);
				}
			}

			boolean pot = false;
			if (jackpot >= 3) {
				bet.set(slt.jackpot());
				pot = true;
				msg = "Impossível, " + author.getAsMention() + " detonou a loteria. **JACKPOT**!!!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(150);
					PStateDAO.savePoliticalState(ps);
				}
			}

			if (win) {
				if (pot)
					msg += "\n\n<a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503>\n";
				msg += "\nSeu prêmio é de __**" + Helper.separate(bet.longValue()) + " créditos.**__";
				if (pot)
					msg += "\n\n<a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360>";
			} else {
				bet.set(0);
				msg += "Poxa, parece que você não teve sorte hoje. Volte sempre!";

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(false);
					PStateDAO.savePoliticalState(ps);
				}
			}

			channel.sendMessage(msg).queue();
			Account facc = AccountDAO.getAccount(author.getId());
			facc.addCredit(bet.get(), this.getClass());
			AccountDAO.saveAccount(facc);
			Slots slts = SlotsDAO.getSlots();
			slts.addToPot(initialBet);
			if (jackpot >= 3) slts.jackpot();
			SlotsDAO.saveSlots(slts);
		};

		final String lowHeader = "<:blank:747876900860461118><:column_disabled_down:747875416567447592><:blank:747876900860461118><:column_enabled_down:747874903570514043><:blank:747876900860461118><:column_enabled_down:747874903570514043><:blank:747876900860461118><:column_enabled_down:747874903570514043><:blank:747876900860461118><:column_disabled_down:747875416567447592><:blank:747876900860461118>";
		final String highHeader = "<:blank:747876900860461118><:column_enabled_down:747874903570514043><:blank:747876900860461118><:column_enabled_down:747874903570514043><:blank:747876900860461118><:column_enabled_down:747874903570514043><:blank:747876900860461118><:column_enabled_down:747874903570514043><:blank:747876900860461118><:column_enabled_down:747874903570514043><:blank:747876900860461118>";
		final String top = "<:corner_down_right:747882840451973170><:horizontal_top:747882840351572020><:cross_down:747882840477138994><:horizontal_top:747882840351572020><:cross_down:747882840477138994><:horizontal_top:747882840351572020><:cross_down:747882840477138994><:horizontal_top:747882840351572020><:cross_down:747882840477138994><:horizontal_top:747882840351572020><:corner_down_left:747882840380932286>";
		final String bottom = "<:corner_up_right:747882840439652522><:horizontal_bottom:747882840565350430><:cross_up:747882840489853000><:horizontal_bottom:747882840565350430><:cross_up:747882840489853000><:horizontal_bottom:747882840565350430><:cross_up:747882840489853000><:horizontal_bottom:747882840565350430><:cross_up:747882840489853000><:horizontal_bottom:747882840565350430><:corner_up_left:747882840326406246>";

		channel.sendMessage(":white_flower: | **Aposta de " + author.getAsMention() + ": __" + Helper.separate(args[0]) + "__**").queue(s -> {
			s.editMessage(s.getContentRaw() + "\n\n" + "**Prêmio acumulado: __" + Helper.separate(slt.getPot()) + "__**\n" + (highbet ? highHeader : lowHeader) + "\n" + top + "\n" + showSlots(0) + "\n" + bottom + "\n").queue(null, Helper::doNothing);
			for (int i = 1; i < 6; i++) {
				if (i != 5)
					s.editMessage(s.getContentRaw() + "\n\n" + "**Prêmio acumulado: __" + Helper.separate(slt.getPot()) + "__**\n" + (highbet ? highHeader : lowHeader) + "\n" + top + "\n" + showSlots(i) + "\n" + bottom + "\n").queueAfter(3 + (3 * i), TimeUnit.SECONDS, null, Helper::doNothing);
				else
					s.editMessage(s.getContentRaw() + "\n\n" + "**Prêmio acumulado: __" + Helper.separate(slt.getPot()) + "__**\n" + (highbet ? highHeader : lowHeader) + "\n" + top + "\n" + showSlots(i) + "\n" + bottom + "\n").queueAfter(3 + (3 * i), TimeUnit.SECONDS, f -> r.run(), Helper::doNothing);
			}
		});
	}

	private String showSlots(int phase) {
		return switch (phase) {
			case 0 -> "<:vertical_right:747882840569544714>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical_left:747882840414486571>";
			case 1 -> "<:vertical_right:747882840569544714>" + rolled.get(0) + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical_left:747882840414486571>";
			case 2 -> "<:vertical_right:747882840569544714>" + rolled.get(0) + "<:vertical:747883406632943669>" + rolled.get(1) + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical_left:747882840414486571>";
			case 3 -> "<:vertical_right:747882840569544714>" + rolled.get(0) + "<:vertical:747883406632943669>" + rolled.get(1) + "<:vertical:747883406632943669>" + rolled.get(2) + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical_left:747882840414486571>";
			case 4 -> "<:vertical_right:747882840569544714>" + rolled.get(0) + "<:vertical:747883406632943669>" + rolled.get(1) + "<:vertical:747883406632943669>" + rolled.get(2) + "<:vertical:747883406632943669>" + rolled.get(3) + "<:vertical:747883406632943669>" + Slots.SLOT + "<:vertical_left:747882840414486571>";
			case 5 -> "<:vertical_right:747882840569544714>" + rolled.get(0) + "<:vertical:747883406632943669>" + rolled.get(1) + "<:vertical:747883406632943669>" + rolled.get(2) + "<:vertical:747883406632943669>" + rolled.get(3) + "<:vertical:747883406632943669>" + rolled.get(4) + "<:vertical_left:747882840414486571>";
			default -> "";
		};
	}

	private void rollSlots() {
		rolled.clear();
		for (int i = 0; i < 5; i++) {
			rolled.add(Slots.getSlot());
		}
	}

	private String prizeTable() {
		return """
				%s%s%s -> x0.8
				%s%s%s -> x1.5
				%s%s%s -> x2
				%s%s%s -> x2.75
				%s%s%s -> x4
				%s%s%s -> x7
				%s%s%s -> x12
				%s%s%s -> x20
				%s%s%s -> JACKPOT!
				"""
				.formatted(
						Slots.LEMON, Slots.LEMON, Slots.LEMON,
						Slots.WATERMELON, Slots.WATERMELON, Slots.WATERMELON,
						Slots.CHERRY, Slots.CHERRY, Slots.CHERRY,
						Slots.HEART, Slots.HEART, Slots.HEART,
						Slots.BELL, Slots.BELL, Slots.BELL,
						Slots.BAR, Slots.BAR, Slots.BAR,
						Slots.HORSESHOE, Slots.HORSESHOE, Slots.HORSESHOE,
						Slots.DIAMOND, Slots.DIAMOND, Slots.DIAMOND,
						Slots.JACKPOT, Slots.JACKPOT, Slots.JACKPOT
				);
	}
}
