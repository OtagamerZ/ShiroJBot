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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.LotteryDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Lottery;
import com.kuuhaku.model.persistent.LotteryValue;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LotteryCommand extends Command {

    public LotteryCommand(String name, String description, Category category, boolean requiresMM) {
        super(name, description, category, requiresMM);
    }

    public LotteryCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
        super(name, aliases, description, category, requiresMM);
    }

    public LotteryCommand(String name, String usage, String description, Category category, boolean requiresMM) {
        super(name, usage, description, category, requiresMM);
    }

    public LotteryCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
        super(name, aliases, usage, description, category, requiresMM);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
        if (args.length < 1) {
            channel.sendMessage("O prêmio atual é __**" + LotteryDAO.getLotteryValue().getValue() + " créditos**__.").queue();
            return;
        } else if (args[0].split(",").length != 6 || args[0].length() != 17) {
            channel.sendMessage("❌ | Você precisa informar 6 dezenas separadas por vírgula.").queue();
            return;
        } else if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
            channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
            return;
        }

        Account acc = AccountDAO.getAccount(author.getId());

        for (String dozen : args[0].split(",")) {
            if (!StringUtils.isNumeric(dozen) || !Helper.between(Integer.parseInt(dozen), 0, 31)) {
                channel.sendMessage("❌ | As dezenas devem ser valores numéricos de 00 a 30.").queue();
                return;
            } else if (args[0].split(dozen).length > 2) {
                channel.sendMessage("❌ | Você não pode repetir dezenas.").queue();
                return;
            }
        }

        long cost = (long) (1000 * Math.pow(5, LotteryDAO.getLotteriesByUser(author.getId()).size()));
        if (acc.getTotalBalance() < cost) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
            return;
        }

        String hash = Helper.generateHash(guild, author);
        ShiroInfo.getHashes().add(hash);
        Main.getInfo().getConfirmationPending().put(author.getId(), true);
        channel.sendMessage("Você está prestes a comprar um bilhete de loteria com as dezenas `" + args[0].replace(",", " ") + "` por " + cost + " créditos, deseja confirmar?")
                .queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
                            if (!ShiroInfo.getHashes().remove(hash)) return;
                            Main.getInfo().getConfirmationPending().invalidate(author.getId());
                            acc.consumeCredit(cost, this.getClass());
                            AccountDAO.saveAccount(acc);
                            LotteryDAO.saveLottery(new Lottery(author.getId(), args[0]));

                            LotteryValue lv = LotteryDAO.getLotteryValue();
                            lv.addValue(cost);
                            LotteryDAO.saveLotteryValue(lv);

                            s.delete().flatMap(d -> channel.sendMessage("✅ | Bilhete comprado com sucesso!")).queue();
                        }), true, 1, TimeUnit.MINUTES,
                        u -> u.getId().equals(author.getId()),
                        ms -> {
                            ShiroInfo.getHashes().remove(hash);
                            Main.getInfo().getConfirmationPending().invalidate(author.getId());
                        })
                );
    }
}
