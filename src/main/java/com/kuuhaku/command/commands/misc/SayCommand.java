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

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

public class SayCommand extends Command {

	public SayCommand() {
		super("say", new String[] {"diga", "repetir"}, "<mensagem>", "Repete a mensagem, também converterá menções de emotes.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {

		if(args.length == 0) { channel.sendMessage(":x: | Você precisa definir uma mensagem.").queue(); return; }

		channel.sendMessage(Helper.makeEmoteFromMention(args)).queue();
		if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) message.delete().queue();
	}

}
