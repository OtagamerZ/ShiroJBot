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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.handlers.games.tabletop.games.hitotsu.Hitotsu;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Command(
        name = "hitotsu",
        aliases = {"uno"},
        usage = "req_bet-mentions",
        category = Category.FUN
)
@Requires({
        Permission.MESSAGE_MANAGE,
        Permission.MESSAGE_ADD_REACTION,
        Permission.MESSAGE_ATTACH_FILES
})
public class HitotsuCommand implements Executable {

    @Override
    public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
        if (message.getMentionedUsers().isEmpty()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().get(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		}

        Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

        if (kp.getCards().size() < 25) {
            channel.sendMessage("❌ | É necessário ter ao menos 25 cartas para poder jogar Hitotsu.").queue();
            return;
        }

        for (User u : message.getMentionedUsers()) {
            Kawaipon k = KawaiponDAO.getKawaipon(u.getId());
			if (k.getCards().size() < 25) {
				channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_not-enough-cards-mention"), u.getAsMention())).queue();
				return;
			} else if (Main.getInfo().getConfirmationPending().get(u.getId()) != null) {
				channel.sendMessage("❌ | " + u.getAsMention() + " possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
				return;
			} else if (Main.getInfo().gameInProgress(u.getId())) {
				channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_mention-in-game"), u.getAsMention())).queue();
				return;
			} else if (u.getId().equals(author.getId())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-play-with-yourself")).queue();
				return;
			}
        }

        Account acc = AccountDAO.getAccount(author.getId());

        int bet = 0;
        if (args.length > 1 && StringUtils.isNumeric(args[0])) {
            bet = Integer.parseInt(args[0]);
            if (bet < 0) {
                channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-credit-amount")).queue();
                return;
            } else if (acc.getBalance() < bet) {
                channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
                return;
            }

            for (User u : message.getMentionedUsers()) {
                Account a = AccountDAO.getAccount(u.getId());
                if (a.getBalance() < bet) {
                    channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-mention"), u.getAsMention())).queue();
                    return;
                }
            }
        }

        String id = author.getId() + "." + message.getMentionedUsers().stream().map(User::getId).map(s -> s + ".").collect(Collectors.joining()) + guild.getId();

        if (Main.getInfo().gameInProgress(author.getId())) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
            return;
        }

        List<User> players = new ArrayList<>() {{
            add(author);
            addAll(message.getMentionedUsers());
        }};

        Set<String> accepted = new HashSet<>() {{
            add(author.getId());
        }};

        Game t = new Hitotsu(Main.getShiroShards(), channel, bet, players.toArray(User[]::new));
        String msg;
        if (players.size() > 2)
            msg = message.getMentionedUsers().stream().map(User::getAsMention).map(s -> s + ", ").collect(Collectors.joining()) + " vocês foram desafiados a uma partida de Hitotsu, desejam aceitar?" + (bet != 0 ? " (aposta: " + Helper.separate(bet) + " créditos)" : "");
        else
            msg = message.getMentionedUsers().get(0).getAsMention() + " você foi desafiado a uma partida de Hitotsu, deseja aceitar?" + (bet != 0 ? " (aposta: " + Helper.separate(bet) + " créditos)" : "");

        long millis = System.currentTimeMillis();
        for (User player : players) {
            String hash = Helper.generateHash(guild, player, millis);
            ShiroInfo.getHashes().add(hash);
            Main.getInfo().getConfirmationPending().put(player.getId(), true);
        }
        channel.sendMessage(msg).queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
                    if (players.contains(mb.getUser())) {
                        if (Main.getInfo().gameInProgress(mb.getId())) {
                            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
                            return;
                        } else if (Main.getInfo().gameInProgress(author.getId())) {
                            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-in-game")).queue();
                            return;
                        }

                        if (ShiroInfo.getHashes().remove(Helper.generateHash(guild, mb.getUser(), millis))) {
                            if (!accepted.contains(mb.getId())) {
                                channel.sendMessage(mb.getAsMention() + " aceitou a partida.").queue();
                                accepted.add(mb.getId());
                            }

                            if (accepted.size() == players.size()) {
								Main.getInfo().getConfirmationPending().remove(author.getId());
                                //Main.getInfo().getGames().put(id, t);
                                s.delete().queue(null, Helper::doNothing);
                                t.start();
                            }
                        }
                    }
                }), true, 1, TimeUnit.MINUTES,
                u -> Helper.equalsAny(u.getId(), players.stream().map(User::getId).toArray(String[]::new)),
                ms -> {
                    for (User player : players) {
                        String hash = Helper.generateHash(guild, player, millis);
						ShiroInfo.getHashes().remove(hash);
						Main.getInfo().getConfirmationPending().remove(player.getId());
                    }
                })
        );
    }
}
