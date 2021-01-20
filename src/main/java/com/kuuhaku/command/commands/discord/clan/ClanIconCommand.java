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

package com.kuuhaku.command.commands.discord.clan;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.enums.ClanTier;
import com.kuuhaku.model.persistent.Clan;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ClanIconCommand extends Command {

	public ClanIconCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ClanIconCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ClanIconCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ClanIconCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());

		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		} else if (c.getMembers().get(author.getId()).ordinal() > 1) {
			channel.sendMessage("❌ | Apenas o líder e o sub-líder podem alterar o emblema do clã.").queue();
			return;
		} else if (c.getTier().ordinal() < ClanTier.GUILD.ordinal()) {
			channel.sendMessage("❌ | Seu clã ainda não desbloqueou o emblema.").queue();
			return;
		} else if (message.getAttachments().size() == 0) {
			channel.sendMessage("❌ | Você precisa enviar uma imagem com dimensões 225x350.").queue();
			return;
		}

		try {
			Message.Attachment a = message.getAttachments().get(0);
			if (!a.isImage()) {
				channel.sendMessage("❌ | Você precisa enviar uma imagem com dimensões 225x350.").queue();
				return;
			}

			BufferedImage bi = ImageIO.read(a.retrieveInputStream().get());
			if (bi.getWidth() != 225 || bi.getHeight() != 350) {
				channel.sendMessage("❌ | As dimensões da imagem devem ser exatamente 225x350.").queue();
				return;
			}

			c.setIcon(bi);
			ClanDAO.saveClan(c);

			channel.sendMessage("✅ | Emblema alterado com sucesso.").queue();
		} catch (InterruptedException | ExecutionException | IOException e) {
			channel.sendMessage("❌ | Imagem inválida.").queue();
		}
	}
}
