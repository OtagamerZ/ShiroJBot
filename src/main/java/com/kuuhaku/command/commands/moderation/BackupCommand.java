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

package com.kuuhaku.command.commands.moderation;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.BackupDAO;
import com.kuuhaku.model.persistent.Backup;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class BackupCommand extends Command {

	public BackupCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public BackupCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public BackupCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public BackupCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}


	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Backup data = BackupDAO.getGuildBackup(guild);

		if (args.length < 1) {
			channel.sendMessage(":x: | Você deve informar o tipo de operação (salvar ou recuperar).").queue();
			return;
		} else if (!Helper.containsAny(args[0], "salvar", "recuperar")) {
			channel.sendMessage(":x: | O primeiro argumento deve ser salvar ou recuperar.").queue();
			return;
		} else if (!guild.getSelfMember().hasPermission(Permission.ADMINISTRATOR)) {
			channel.sendMessage(":x: | Preciso da permissão de administradora para efetuar operações de backup.").queue();
			return;
		}

		if (args[0].equalsIgnoreCase("salvar")) {
			data.setGuild(guild.getId());
			data.saveServerData(guild);
			channel.sendMessage("Backup feito com sucesso, utilize `" + prefix + "backup recuperar` para recuperar para este estado do servidor. (ISSO IRÁ REESCREVER O SERVIDOR, TODAS AS MENSAGENS SERÃO PERDIDAS)").queue();
		} else if (data.getGuild() == null || data.getGuild().isEmpty()) {
			channel.sendMessage(":x: | Nenhum backup foi feito ainda, utilize o comando `" + prefix + "backup salvar` para criar um backup.").queue();
		} else if (data.getLastRestored().toLocalDateTime().plusDays(7).until(LocalDateTime.now(), ChronoUnit.DAYS) < 7) {
			channel.sendMessage(":x: | Você precisa aguardar " + (7 - data.getLastRestored().toLocalDateTime().plusDays(7).until(LocalDateTime.now(), ChronoUnit.DAYS)) + " antes de restaurar o backup de canais novamente.").queue();
		} else {
			channel.sendMessage("**Restaurar um backup irá limpar todas as mensagens do servidor, inclusive aquelas fixadas**\n\nPor favor, confirme esta operação clicando no botão abaixo.").queue(s ->
					Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (mb, ms) -> data.restore(guild)), true, 30, TimeUnit.SECONDS));
		}
	}
}
