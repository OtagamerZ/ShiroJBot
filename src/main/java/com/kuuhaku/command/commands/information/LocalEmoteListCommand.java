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

package com.kuuhaku.command.commands.information;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LocalEmoteListCommand extends Command {

	public LocalEmoteListCommand() {
		super("emotes", "<nome>", "Mostra a lista de emotes disponíveis no servidor em que o comando foi executado.", Category.INFO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		List<Page> pages = new ArrayList<>();
		List<MessageEmbed.Field> f = new ArrayList<>();

		EmbedBuilder eb = new EmbedBuilder();

		guild.getEmotes().stream().filter(e -> StringUtils.containsIgnoreCase(e.getAsMention(), args.length > 0 ? args[0] : "")).collect(Collectors.toList()).forEach(e -> f.add(new MessageEmbed.Field("Emote " + e.getAsMention(), "Menção: " + e.getAsMention().replace("<", "`{").replace(">", "}`").replace(":", "&"), false)));

		try {
			for (int i = 0; i < Math.ceil(f.size() / 10f); i++) {
				eb.clear();
				List<MessageEmbed.Field> subF = f.subList(-10 + (10 * (i + 1)), Math.min(10 * (i + 1), f.size()));
				subF.forEach(eb::addField);

				eb.setTitle("<a:SmugDance:598842924725305344> Emotes disponíveis neste servidor:");
				Helper.finishEmbed(guild, pages, f, eb, i);
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(Main.getInfo().getAPI(), s, pages, 60, TimeUnit.SECONDS));
		} catch (IndexOutOfBoundsException e) {
			channel.sendMessage(":x: | Nenhum emote encontrado com esse nome.").queue();
		}
	}
}
