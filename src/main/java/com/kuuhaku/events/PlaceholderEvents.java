/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.command.commands.PreparedCommand;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.jodah.expiringmap.ExpiringMap;
import org.intellij.lang.annotations.Language;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class PlaceholderEvents extends ListenerAdapter {
	private final ExpiringMap<String, Boolean> ratelimit = ExpiringMap.builder()
			.expiration(30, TimeUnit.MINUTES)
			.build();

	@Override
	public void onGuildMessageUpdate(@Nonnull GuildMessageUpdateEvent event) {
		if (event.getAuthor().isBot()) return;
		Message msg = Main.getInfo().retrieveCachedMessage(event.getGuild(), event.getMessageId());
		onGuildMessageReceived(new GuildMessageReceivedEvent(event.getJDA(), event.getResponseNumber(), event.getMessage()));

		if (msg != null)
			Helper.logToChannel(event.getAuthor(), false, null, "Uma mensagem foi editada no canal " + event.getChannel().getAsMention() + ":```diff\n- " + msg.getContentRaw() + "\n+ " + event.getMessage().getContentRaw() + "```", msg.getGuild());
	}

	@Override
	public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
		User author = event.getAuthor();
		Message message = event.getMessage();
		TextChannel channel = message.getTextChannel();
		Guild guild = message.getGuild();
		String rawMessage = message.getContentRaw().replaceAll(" +", " ");
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		String prefix = gc.getPrefix().toLowerCase(Locale.ROOT);

		if (!channel.canTalk()) return;

		String commandName = "";
		if (rawMessage.toLowerCase(Locale.ROOT).startsWith(prefix)) {
			String rawMsgNoPrefix = rawMessage.substring(prefix.length()).trim();
			commandName = rawMsgNoPrefix.split(" ")[0].trim();
		}

		if (author.isBot()) return;

		PreparedCommand command = Main.getCommandManager().getCommand(commandName);
		if (command != null && !ratelimit.containsKey(guild.getId())) {
			channel.sendMessage(getMigrationMessage()).queue();
			ratelimit.put(guild.getId(), true);
		}
	}

	@Override
	public void onSlashCommand(@Nonnull SlashCommandEvent evt) {
		evt.deferReply().queue(hook -> {
			if (!evt.isFromGuild()) {
				hook.sendMessage("❌ | Meus comandos não funcionam em canais privados.").queue();
				return;
			}

			hook.editOriginal(getMigrationMessage()).queue();
		});
	}

	@Language("Markdown")
	private static String getMigrationMessage() {
		return """
				## Migração em andamento
				A versão 3 da Shiro não está mais disponível, estamos migrando o código e os dados para uma versão mais otimizada e
				melhorada.
								
				### Quais mudanças devo esperar?
				Na nova versão, o foco será mais nas cartas e em seu jogo principal, o Shoukan. Por esse motivo, algumas funcionalidades
				de moderação e interação préviamente disponíveis não estarão mais inclusos, porém, o seu progresso da versão 3 será
				migrado para a nova versão incluindo:
								
				- Cartas na coleção
				- Cartas no armazém
				- Nível e XP
				- Imagem de perfil
				- Mensagem de boas-vindas/adeus (exceto embed)
				- Prefixo
				- Cargos de nível
								
				Caso tenha dúvidas, por favor entrar em contato com a [equipe de suporte](https://discord.gg/9sgkzna).
				""";
	}

	public Map<String, CopyOnWriteArrayList<SimpleMessageListener>> getHandler() {
		return Map.of();
	}

	public void addHandler(Guild guild, SimpleMessageListener sml) {

	}
}
