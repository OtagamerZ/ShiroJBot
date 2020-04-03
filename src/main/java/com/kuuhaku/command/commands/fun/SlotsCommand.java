/*
 * This file is part of Shiro J Bot.
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
import com.kuuhaku.controller.postgresql.SlotsDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Slots;
import com.kuuhaku.utils.Helper;
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
			channel.sendMessage(":x: | A aposta deve ser um valor numérico maior ou igual a 25.").queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		AtomicInteger bet = new AtomicInteger(Integer.parseInt(args[0]));

		if (acc.getBalance() < bet.get()) {
			channel.sendMessage(":x: | Você não tem créditos suficientes.").queue();
			return;
		}

		boolean highbet = bet.get() >= 100;
		Slots slt = SlotsDAO.getSlots();
		acc.removeCredit(bet.get());
		slt.addToPot(bet.get());

		rolled.clear();

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
			} else if (watermelon >= 3) {
				bet.set(Math.round(bet.get() * 1.5f));
				msg = "E temos três melancias!";
				win = true;
			} else if (cherry >= 3) {
				bet.set(bet.get() * 2);
				msg = "Três cerejas no bolo!";
				win = true;
			} else if (heart >= 3) {
				bet.set(Math.round(bet.get() * 2.75f));
				msg = "Três corações apaixonados!";
				win = true;
			} else if (bell >= 3) {
				bet.set(bet.get() * 4);
				msg = "Toquem os sinos!";
				win = true;
			} else if (bar >= 3) {
				bet.updateAndGet(v -> v * 9);
				msg = "Chamem a polícia, temos um sortudo!";
				win = true;
			} else if (horseshoe >= 3) {
				bet.updateAndGet(v -> v * 35);
				msg = "Alguem sequestrou um doente, três ferraduras de ouro!";
				win = true;
			} else if (diamond >= 3) {
				bet.updateAndGet(v -> v * 50);
				msg = "Assalto ao banco da sorte, temos três diamantes!";
				win = true;
			}

			boolean pot = false;
			if (jackpot >= 3) {
				bet.set(slt.jackpot());
				pot = true;
				msg = "Impossível! " + guild.getPublicRole().getAsMention() + " " + author.getAsMention() + " detonou a loteria, **JACKPOT**!!!";
				win = true;
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
			}

			channel.sendMessage(msg).queue();
			acc.addCredit(bet.get());
			AccountDAO.saveAccount(acc);
			SlotsDAO.saveSlots(slt);
		};

		channel.sendMessage(":white_flower: | **Aposta de " + author.getAsMention() + ": __" + args[0] + "__**").queue(s -> {
			s.editMessage(s.getContentRaw() + "\n\n" + "**Prêmio acumulado: __" + slt.getPot() + "__**\n" + (highbet ? "     ⇩       ⇩      ⇩       ⇩      ⇩" : "              ⇩       ⇩       ⇩") + "\n┌──┬──┬──┬──┬──┐\n" + rollSlot(0) + "\n└──┴──┴──┴──┴──┘\n" + (highbet ? "     ⇧       ⇧      ⇧       ⇧      ⇧" : "              ⇧       ⇧       ⇧")).queue();
			for (int i = 1; i < 6; i++) {
				if (i != 5)
					s.editMessage(s.getContentRaw() + "\n\n" + "**Prêmio acumulado: __" + slt.getPot() + "__**\n" + (highbet ? "     ⇩       ⇩      ⇩       ⇩      ⇩" : "              ⇩       ⇩       ⇩") + "\n┌──┬──┬──┬──┬──┐\n" + rollSlot(i) + "\n└──┴──┴──┴──┴──┘\n" + (highbet ? "     ⇧       ⇧      ⇧       ⇧      ⇧" : "              ⇧       ⇧       ⇧")).queueAfter(2 + (2 * i), TimeUnit.SECONDS);
				else
					s.editMessage(s.getContentRaw() + "\n\n" + "**Prêmio acumulado: __" + slt.getPot() + "__**\n" + (highbet ? "     ⇩       ⇩      ⇩       ⇩      ⇩" : "              ⇩       ⇩       ⇩") + "\n┌──┬──┬──┬──┬──┐\n" + rollSlot(i) + "\n└──┴──┴──┴──┴──┘\n" + (highbet ? "     ⇧       ⇧      ⇧       ⇧      ⇧" : "              ⇧       ⇧       ⇧")).queueAfter(2 + (2 * i), TimeUnit.SECONDS, f -> r.run());
			}
		});
	}

	private String rollSlot(int phase) {
		if (phase > 0) rolled.add(Slots.getSlot());
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
}
