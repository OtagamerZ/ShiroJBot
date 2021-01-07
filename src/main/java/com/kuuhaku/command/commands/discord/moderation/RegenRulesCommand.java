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

package com.kuuhaku.command.commands.discord.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;

public class RegenRulesCommand extends Command {

	public RegenRulesCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public RegenRulesCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public RegenRulesCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public RegenRulesCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		message.delete().complete();
		try {
			String[] rules = new String[gc.getRules().size()];
			for (int i = 0; i < gc.getRules().size(); i++) {
				String[] rule = gc.getRules().get(i).split(";");
				rules[i] = "**%s - %s**\n%s".formatted(i + 1, rule[0], rule[1]);
			}

			String text = String.join("\n\n", rules);
			if (text.length() > 2000) {
				channel.sendMessage("❌ | Mensagem das regras ultrapassa 2000 caractéres.").queue();
				return;
			}

			if (guild.getId().equals(ShiroInfo.getSupportServerID())) {
				channel.sendFile(Helper.getImage("https://i.imgur.com/JQ3LvGK.png"), "title.png").complete();
				channel.sendFile(Helper.getImage("https://i.imgur.com/9dfpeel.png"), "welcome.png").complete();
				channel.sendMessage("Seja bem-vindo(a) ao meu servidor oficial de suporte, qualquer duvida que tenha sobre como me utilizar será esclarecida por um de nossos membros, fique à vontade e lembre-se de sempre relatar quando achar algo suspeito.").complete();
			} else {
				if (guild.getBannerUrl() != null)
					channel.sendFile(Helper.getImage(guild.getBannerUrl()), "title.png").complete();
				channel.sendFile(Helper.getImage("https://i.imgur.com/9dfpeel.png"), "welcome.png").complete();
				channel.sendMessage("Seja bem-vindo(a) ao servidor " + guild.getName() + ", fique à vontade e lembre-se de sempre relatar quando achar algo suspeito.").complete();
			}

			channel.sendFile(Helper.getImage("https://i.imgur.com/aCYUW1G.png"), "rules.png").complete();

			if (guild.getId().equals(ShiroInfo.getSupportServerID()))
				channel.sendMessage("""
						%s
						
						Divirta-se e, caso tenha lido as regras você pode utilizar o comando `s!arespostaé RESPOSTA` para completar a seguinte frase e ganhar um emblema único:
						Infratores serão `_______ __ ______ ___ ____ ____`
						      
						É proibido compartilhar qual é a resposta, se não não teria graça!
						""".formatted(text)).complete();
			else
				channel.sendMessage(text + "\n\nCaso precise de ajuda, ou queira ajudar no meu desenvolvimento, venha para nosso servidor de suporte: https://discord.gg/9sgkzna").complete();

			if (Helper.getSponsors().length() > 0) {
				channel.sendFile(Helper.getImage("https://i.imgur.com/U9lTSWD.png"), "sponsors.png").complete();
				channel.sendMessage(Helper.getSponsors()).complete();
			}
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
