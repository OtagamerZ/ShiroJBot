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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.PendingBindingDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.PendingBinding;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NonNls;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BindCommand extends Command {

    public BindCommand(String name, String description, Category category, boolean requiresMM) {
        super(name, description, category, requiresMM);
    }

    public BindCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
        super(name, aliases, description, category, requiresMM);
    }

    public BindCommand(String name, String usage, String description, Category category, boolean requiresMM) {
        super(name, usage, description, category, requiresMM);
    }

    public BindCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
        super(name, aliases, usage, description, category, requiresMM);
    }

    @Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());

		if (!acc.getTwitchId().isBlank()) {
			channel.sendMessage("❌ | Você já vinculou esta conta a um perfil da Twitch.").queue();
			return;
		}

		try {
			String code = Hex.encodeHexString(MessageDigest.getInstance("SHA-1").digest(author.getId().getBytes(StandardCharsets.UTF_8)));
			PendingBinding pb = PendingBindingDAO.getPendingBinding(code);

            if (pb != null) {
                channel.sendMessage("Código e instruções reenviados nas mensagens privadas.").queue();
            } else {
                channel.sendMessage("Código e instruções enviados nas mensagens privadas.").queue();
            }

            author.openPrivateChannel().queue(c ->
                            c.sendMessage("Use este código no comando `s!vincular` no chat do canal `kuuhaku_otgmz` para vincular esta conta ao seu perfil da Twitch:\n\n`" + (pb == null ? code : pb.getHash()) + "`")
                                    .queue(s -> {
                                        if (pb == null) {
                                            PendingBinding p = new PendingBinding(code, author.getId());
                                            PendingBindingDAO.savePendingBinding(p);
                                        }
                                    }, err -> channel.sendMessage("❌ | Seu canal de mensagens privadas está bloqueado para mim.").queue()),
                    Helper::doNothing);
        } catch (NoSuchAlgorithmException e) {
            Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
        }
    }
}
