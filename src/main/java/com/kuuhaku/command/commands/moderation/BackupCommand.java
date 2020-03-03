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

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.BackupDAO;
import com.kuuhaku.model.persistent.Backup;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.time.LocalDateTime;

public class BackupCommand extends Command {

	public BackupCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public BackupCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public BackupCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public BackupCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Backup data = BackupDAO.getGuildBackup(guild);

		if (args.length < 1 || (args[0].equalsIgnoreCase("recuperar") && args.length < 2)) {
			channel.sendMessage(":x: | É necessário definir se a ação é de salvar ou recuperar e definir o que devo recuperar (canais ou cargos).").queue();
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
			BackupDAO.saveBackup(data);
			channel.sendMessage("Backup feito com sucesso, utilize `" + prefix + "backup recuperar` para recuperar para este estado do servidor. (ISSO IRÁ REESCREVER O SERVIDOR, TODAS AS MENSAGENS SERÃO PERDIDAS)").queue();
		} else if (data.getGuild() == null || data.getGuild().isEmpty()) {
			channel.sendMessage(":x: | Nenhum backup foi feito ainda, utilize o comando `" + prefix + "backup salvar` para criar um backup.").queue();
		} else if (data.getLastRestored().toLocalDateTime().plusDays(7).compareTo(LocalDateTime.now()) > 0) {
			channel.sendMessage(":x: | Você precisa aguardar 1 semana antes de restaurar o backup de canais novamente.").queue();
		} else {
			data.restore(guild);
		}
	}
}
