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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.CreditLoan;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.ExceedMember;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoanCommand implements Executable {

	public LoanCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public LoanCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public LoanCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public LoanCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		}

		ExceedMember ex = ExceedDAO.getExceedMember(author.getId());
		if (args.length < 1) {
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setTitle(":bank: | Empréstimo de créditos");
			eb.setThumbnail("https://image.flaticon.com/icons/png/512/1462/1462438.png");
			eb.setDescription("""
					Está precisando de créditos rápidos? Estão aparecendo muitas cartas que você deseja obter? Talvez seu Kawaigotchi esteja morrendo?
					Não se preocupe, nós podemos resolver!
					     
					Usando este comando você pode contratar um ~~agiota~~ empréstimo de créditos e ter a possibilidade de pagar a dívida mais tarde.
					""");
			eb.addField("Plano Lite: `" + prefix + "emprestimo 1`", "1.000 créditos (juros de " + Helper.round(CreditLoan.LOAN_1.getInterest(ex) * 100 - 100, 1) + "%)", false);
			eb.addField("Plano Colecionador: `" + prefix + "emprestimo 2`", "2.500 créditos (juros de " + Helper.round(CreditLoan.LOAN_2.getInterest(ex) * 100 - 100, 1) + "%)", false);
			eb.addField("Plano Bate-papo: `" + prefix + "emprestimo 3`", "5.000 créditos (juros de " + Helper.round(CreditLoan.LOAN_3.getInterest(ex) * 100 - 100, 1) + "%)", false);
			eb.addField("Plano Animador de Chat: `" + prefix + "emprestimo 4`", "10.000 créditos (juros de " + Helper.round(CreditLoan.LOAN_4.getInterest(ex) * 100 - 100, 1) + "%)", false);
			eb.addField("Plano Ultimate: `" + prefix + "emprestimo 5`", "25.000 créditos (juros de " + Helper.round(CreditLoan.LOAN_5.getInterest(ex) * 100 - 100, 1) + "%)", false);
			eb.setFooter("Não há prazo para debitar a dívida, todo crédito que você ganhar reduzirá a dívida.");

			channel.sendMessage(eb.build()).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
            channel.sendMessage("❌ | O plano precisa ser um valor numérico de 1 à 5.").queue();
            return;
        }

        int loan = Integer.parseInt(args[0]);

        if (loan < 1 || loan > 5) {
            channel.sendMessage("❌ | O plano precisa ser um valor numérico de 1 à 5.").queue();
            return;
        }

        Account acc = AccountDAO.getAccount(author.getId());

        if (acc.getLoan() > 0) {
            channel.sendMessage("❌ | Você ainda não terminou de pagar seu último empréstimo.").queue();
            return;
        }

        CreditLoan cl = CreditLoan.getById(loan);

        String hash = Helper.generateHash(guild, author);
        ShiroInfo.getHashes().add(hash);
        Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Você está prestes a obter __**" + Helper.separate(cl.getLoan()) + " créditos**__ a um juros de __" + Helper.round(cl.getInterest(ex) * 100 - 100, 1) + "%__ (__**" + Helper.separate(Math.round(cl.getLoan() * cl.getInterest(ex))) + " de dívida**__), deseja confirmar?")
                .queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
                            if (!ShiroInfo.getHashes().remove(hash)) return;
                            Main.getInfo().getConfirmationPending().invalidate(author.getId());
                            Account finalAcc = AccountDAO.getAccount(author.getId());
                            cl.sign(finalAcc);

                            s.delete().flatMap(d -> channel.sendMessage("Obrigada por ser mais um cliente do Shiro Empréstimos LTDA! Você não receberá mais créditos até que termine de pagar sua dívida.")).queue();
                        }), true, 1, TimeUnit.MINUTES,
                        u -> u.getId().equals(author.getId()),
                        ms -> {
                            ShiroInfo.getHashes().remove(hash);
                            Main.getInfo().getConfirmationPending().invalidate(author.getId());
                        })
                );
    }

}
