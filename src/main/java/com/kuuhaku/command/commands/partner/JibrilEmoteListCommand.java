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

package com.kuuhaku.command.commands.partner;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
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

public class JibrilEmoteListCommand extends Command {

	public JibrilEmoteListCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public JibrilEmoteListCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public JibrilEmoteListCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public JibrilEmoteListCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		List<Page> pages = new ArrayList<>();
		EmbedBuilder eb = new EmbedBuilder();
		List<List<Emote>> emotes = Helper.chunkify(Main.getJibril().getEmotes().stream().filter(e -> StringUtils.containsIgnoreCase(e.getAsMention(), args.length > 0 ? args[0] : "")).collect(Collectors.toList()), 10);

		for (int i = 0; i < emotes.size(); i++) {
			eb.clear();

			eb.setTitle("<a:SmugDance:598842924725305344> Emotes disponíveis para a Jibril:");
			eb.setColor(Helper.getRandomColor());
			emotes.get(i).forEach(e -> eb.addField("Emote: " + e.getAsMention(), "Menção: " + e.getAsMention().replace("<", "`{").replace(">", "}`").replace(":", "&"), false));
			eb.setAuthor("Para usar estes emotes, simplesmente digite a menção no chat global, ela será convertida automaticamente.");
			eb.setFooter("Página " + (i + 1) + " de " + emotes.size() + ". Total de " + emotes.stream().mapToInt(List::size).sum() + " resultados.", null);

			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(Main.getInfo().getAPI(), s, pages, 60, TimeUnit.SECONDS));
	}
}
