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
		message.delete().queue(s -> {
			try {
				if (guild.getId().equals(ShiroInfo.getSupportServerID()))
					channel.sendFile(Helper.getImage("https://i.imgur.com/JQ3LvGK.png"), "title.png").queue();
				channel.sendFile(Helper.getImage("https://i.imgur.com/9dfpeel.png"), "welcome.png").queue();
				channel.sendMessage(guild.getId().equals(ShiroInfo.getSupportServerID()) ? "Seja bem-vindo(a) ao meu servidor oficial de suporte, qualquer duvida que tenha sobre como me utilizar será esclarecida por um de nossos membros, fique à vontade e lembre-se de sempre relatar quando achar algo suspeito" : "Seja bem-vindo(a) ao servidor " + guild.getName() + ", fique à vontade e lembre-se de sempre relatar quando achar algo suspeito").queue();
				channel.sendFile(Helper.getImage("https://i.imgur.com/aCYUW1G.png"), "rules.png").queue();
				channel.sendMessage("**1 - É proibido qualquer ato de SPAM, flood ou bullying**\n" +
						"Por razões obvias, tais coisas poluem o servidor e prejudicam a comunidade e é considerado um ato grave para medidas de punição.\n" +
						"Infratores serão punidos de acordo com seus atos.\n\n" +
						"**2 - Proibido postar qualquer conteúdo pornográfico ou racista**\n" +
						"Ninguem gosta de racismo, e este servidor não é um lugar para coisas NSFW, então mantenha-os no seu HD. (Inclui avatares)\n\n" +
						"**3 - Atenha-se ao tópico**\n" +
						"Existem vários canais para diversos assuntos, não há necessidade de entupir tudo em um canal só.\n\n" +
						"**4 - Não peça cargos, xp ou coisas do tipo**\n" +
						"Eu sou a única que gerencia isso, e não adianta insistir!\n" +
						"Pronto pessoal, isso é tudo!\n" +
						(guild.getId().equals(ShiroInfo.getSupportServerID()) ? "\nDivirta-se e, caso tenha lido as regras você pode utilizar o comando `s!arespostaé RESPOSTA` para completar a seguinte frase e ganhar um emblema único:\n" +
								"Infratores serão `_______ __ ______ ___ ____ ____`\n\n" +
								"É proibido compartilhar qual é a resposta, se não não teria graça!" : "Caso precise de ajuda, ou queira ajudar no meu desenvolvimento, venha para nosso servidor de suporte: https://discord.gg/9sgkzna")).queue();
				if (Helper.getSponsors().length() > 0) {
					channel.sendFile(Helper.getImage("https://i.imgur.com/U9lTSWD.png"), "partners.png").queue();
					channel.sendMessage(Helper.getSponsors()).queue();
				}
			} catch (IOException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
