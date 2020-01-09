/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.BackupDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.Backup;
import com.kuuhaku.model.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

public class BackupCommand extends Command {

	public BackupCommand() {
		super("backup", new String[]{"dados"}, "<salvar/recuperar/permissoes>", "Salva ou recupera um backup do servidor - ISSO IRÁ SOBRESCREVER COMPLETAMENTE O ESTADO ATUAL DO SERVIDOR.", Category.MODERACAO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		Backup data = BackupDAO.getGuildBackup(guild);

		if (args.length < 1) {
			channel.sendMessage(":x: | É necessário definir se a ação é de salvar ou recuperar.").queue();
			return;
		} else if (!Helper.containsAny(args[0], "salvar", "recuperar")) {
			channel.sendMessage(":x: | O primeiro argumento deve ser salvar ou recuperar.").queue();
			return;
		} else if (!guild.getSelfMember().hasPermission(Permission.ADMINISTRATOR)) {
            channel.sendMessage(":x: | Preciso da permissão de administrador para efetuar operações de backup.").queue();
            return;
        }

		if (args[0].equalsIgnoreCase("salvar")) {
            data.setGuild(guild.getId());
            data.saveServerData(guild);
            BackupDAO.saveBackup(data);
            channel.sendMessage("Backup feito com sucesso, utilize `" + prefix + "backup recuperar` para recuperar para este estado do servidor. (ISSO IRÁ REESCREVER O SERVIDOR, TODAS AS MENSAGENS SERÃO PERDIDAS)").queue();
		} else if (data.getGuild() == null || data.getGuild().isEmpty()) {
            channel.sendMessage(":x: | Nenhum backup foi feito ainda, utilize o comando `" + prefix + "backup salvar` para criar um backup.").queue();
        } else if (args[0].equalsIgnoreCase("recuperar")) {
		    data.recoverServerData(guild);
		} else if (args[0].equalsIgnoreCase("permissoes")) {
			data.restorePermissions(guild);
		}
	}
}
