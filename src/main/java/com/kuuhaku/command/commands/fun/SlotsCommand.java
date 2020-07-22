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

package com.kuuhaku.command.commands.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.SlotsDAO;
import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Slots;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SlotsCommand extends Command {
	private List<String> rolled = new ArrayList<>();

	public SlotsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public SlotsCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public SlotsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public SlotsCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			EmbedBuilder eb = new EmbedBuilder();
			eb.setColor(Helper.getRandomColor());
			eb.setDescription(prizeTable());
			eb.setTitle("Tabela de prêmios");
			eb.setFooter("Use `" + prefix + "slots VALOR` para jogar (valor mínimo: 25 créditos)");

			channel.sendMessage(eb.build()).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0]) || Integer.parseInt(args[0]) < 25) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_slots-invalid-number")).queue();
			return;
		} else if (ShiroInfo.gameInProgress(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		AtomicInteger bet = new AtomicInteger(Integer.parseInt(args[0]));

		if (acc.getBalance() < bet.get()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
			return;
		}

		boolean highbet = bet.get() >= 100;
		Slots slt = SlotsDAO.getSlots();
		acc.removeCredit(bet.get(), this.getClass());
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
				msg = "Eita, parece que você não teve sorte hoje!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(1);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (watermelon >= 3) {
				bet.set(Math.round(bet.get() * 1.5f));
				msg = "E temos três melancias!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(2);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (cherry >= 3) {
				bet.set(bet.get() * 2);
				msg = "Três cerejas no bolo!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(4);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (heart >= 3) {
				bet.set(Math.round(bet.get() * 2.75f));
				msg = "Três corações apaixonados!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(6);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (bell >= 3) {
				bet.set(bet.get() * 4);
				msg = "Toquem os sinos!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(16);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (bar >= 3) {
				bet.set(bet.get() * 9);
				msg = "Chamem a polícia, temos um sortudo!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(45);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (horseshoe >= 3) {
				bet.set(bet.get() * 35);
				msg = "Alguem sequestrou um doente, três ferraduras de ouro!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(65);
					PStateDAO.savePoliticalState(ps);
				}
			} else if (diamond >= 3) {
				bet.set(bet.get() * 50);
				msg = "Assalto ao banco da sorte, temos três diamantes!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(100);
					PStateDAO.savePoliticalState(ps);
				}
			}

			boolean pot = false;
			if (jackpot >= 3) {
				bet.set(slt.jackpot());
				pot = true;
				msg = "Impossível, " + author.getAsMention() + " detonou a loteria, **JACKPOT**!!!";
				win = true;

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(500);
					PStateDAO.savePoliticalState(ps);
				}
			}

			if (win) {
				if (pot)
					msg += "\n\n<a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503>\n\n";
				msg += "\nSeu prêmio é de __**" + bet + " créditos.**__";
				if (pot)
					msg += "\n\n<a:YellowArrowRight:680461983342264360><a:YellowArrowLeft:680461765863145503><a:YellowArrowLeft:680461765863145503><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360><a:YellowArrowRight:680461983342264360>";
			} else {
				bet.set(0);
				msg += "Poxa, parece que você não teve sorte hoje. Volte sempre!";

				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(false);
					PStateDAO.savePoliticalState(ps);
				}
			}

			ShiroInfo.getGameLock().remove(author.getId());
			channel.sendMessage(msg).queue();
			acc.addCredit(bet.get(), this.getClass());
			AccountDAO.saveAccount(acc);
			SlotsDAO.saveSlots(slt);
		};

		ShiroInfo.getGameLock().add(author.getId());
		channel.sendMessage(":white_flower: | **Aposta de " + author.getAsMention() + ": __" + args[0] + "__**").queue(s -> {
			s.editMessage(s.getContentRaw() + "\n\n" + "**Prêmio acumulado: __" + slt.getPot() + "__**\n" + (highbet ? "     ⇩       ⇩      ⇩       ⇩      ⇩" : "              ⇩       ⇩       ⇩") + "\n┌──┬──┬──┬──┬──┐\n" + showSlots(0) + "\n└──┴──┴──┴──┴──┘\n" + (highbet ? "     ⇧       ⇧      ⇧       ⇧      ⇧" : "              ⇧       ⇧       ⇧")).queue();
			for (int i = 1; i < 6; i++) {
				if (i != 5)
					s.editMessage(s.getContentRaw() + "\n\n" + "**Prêmio acumulado: __" + slt.getPot() + "__**\n" + (highbet ? "     ⇩       ⇩      ⇩       ⇩      ⇩" : "              ⇩       ⇩       ⇩") + "\n┌──┬──┬──┬──┬──┐\n" + showSlots(i) + "\n└──┴──┴──┴──┴──┘\n" + (highbet ? "     ⇧       ⇧      ⇧       ⇧      ⇧" : "              ⇧       ⇧       ⇧")).queueAfter(3 + (3 * i), TimeUnit.SECONDS);
				else
					s.editMessage(s.getContentRaw() + "\n\n" + "**Prêmio acumulado: __" + slt.getPot() + "__**\n" + (highbet ? "     ⇩       ⇩      ⇩       ⇩      ⇩" : "              ⇩       ⇩       ⇩") + "\n┌──┬──┬──┬──┬──┐\n" + showSlots(i) + "\n└──┴──┴──┴──┴──┘\n" + (highbet ? "     ⇧       ⇧      ⇧       ⇧      ⇧" : "              ⇧       ⇧       ⇧")).queueAfter(3 + (3 * i), TimeUnit.SECONDS, f -> r.run());
			}
		});
	}

	private String showSlots(int phase) {
		switch (phase) {
			case 0:
				return "│" + Slots.SLOT + " │" + Slots.SLOT + " │" + Slots.SLOT + "│ " + Slots.SLOT + "│ " + Slots.SLOT + "│";
			case 1:
				return "│" + rolled.get(0) + " │" + Slots.SLOT + " │" + Slots.SLOT + "│ " + Slots.SLOT + "│ " + Slots.SLOT + "│";
			case 2:
				return "│" + rolled.get(0) + " │" + rolled.get(1) + " │" + Slots.SLOT + "│ " + Slots.SLOT + "│ " + Slots.SLOT + "│";
			case 3:
				return "│" + rolled.get(0) + " │" + rolled.get(1) + " │" + rolled.get(2) + "│ " + Slots.SLOT + "│ " + Slots.SLOT + "│";
			case 4:
				return "│" + rolled.get(0) + " │" + rolled.get(1) + " │" + rolled.get(2) + "│ " + rolled.get(3) + "│ " + Slots.SLOT + "│";
			case 5:
				return "│" + rolled.get(0) + " │" + rolled.get(1) + " │" + rolled.get(2) + "│ " + rolled.get(3) + "│ " + rolled.get(4) + "│";
			default:
				return "";
		}
	}

	private void rollSlots() {
		rolled.clear();
		for (int i = 0; i < 5; i++) {
			rolled.add(Slots.getSlot());
		}
	}

	private String prizeTable() {
		return Slots.LEMON + Slots.LEMON + Slots.LEMON + " -> x0.8\n" +
				Slots.WATERMELON + Slots.WATERMELON + Slots.WATERMELON + " -> x1.5\n" +
				Slots.CHERRY + Slots.CHERRY + Slots.CHERRY + " -> x2\n" +
				Slots.HEART + Slots.HEART + Slots.HEART + " -> x2.75\n" +
				Slots.BELL + Slots.BELL + Slots.BELL + " -> x4\n" +
				Slots.BAR + Slots.BAR + Slots.BAR + " -> x9\n" +
				Slots.HORSESHOE + Slots.HORSESHOE + Slots.HORSESHOE + " -> x35\n" +
				Slots.DIAMOND + Slots.DIAMOND + Slots.DIAMOND + " -> x50\n" +
				Slots.JACKPOT + Slots.JACKPOT + Slots.JACKPOT + " -> JACKPOT!\n";
	}

	private boolean anyCombination() {
		if (Collections.frequency(rolled, Slots.LEMON) >= 3) return true;
		else if (Collections.frequency(rolled, Slots.WATERMELON) >= 3) return true;
		else if (Collections.frequency(rolled, Slots.CHERRY) >= 3) return true;
		else if (Collections.frequency(rolled, Slots.HEART) >= 3) return true;
		else if (Collections.frequency(rolled, Slots.BELL) >= 3) return true;
		else if (Collections.frequency(rolled, Slots.BAR) >= 3) return true;
		else if (Collections.frequency(rolled, Slots.HORSESHOE) >= 3) return true;
		else if (Collections.frequency(rolled, Slots.DIAMOND) >= 3) return true;
		else return Collections.frequency(rolled, Slots.JACKPOT) >= 3;
	}
}
