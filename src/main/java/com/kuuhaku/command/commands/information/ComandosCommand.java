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
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ComandosCommand extends Command {

	public ComandosCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public ComandosCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public ComandosCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public ComandosCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		Map<String, Page> pages = new LinkedHashMap<>();

		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(ShiroInfo.getLocale(I18n.PT).getString("str_command-list-title"));
		eb.setDescription(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_command-list-description"), prefix, Arrays.stream(Category.values()).filter(c -> c.isEnabled(gc, guild)).count(), Main.getCommandManager().getCommands().stream().filter(c -> c.getCategory().isEnabled(gc, guild)).count()));
		for (Category cat : Category.values()) {
			if (cat.isEnabled(gc, guild)) eb.addField(cat.getEmote() + " | " + cat.getName(), Helper.VOID, true);
		}

		eb.addField("<:tips:684039810079522846> | Dicas", Helper.VOID, true);

		eb.setColor(Color.PINK);
		eb.setFooter(Main.getInfo().getFullName(), null);
		eb.setThumbnail(Objects.requireNonNull(Main.getInfo().getAPI().getEmoteById(Helper.HOME)).getImageUrl());

		pages.put(Helper.HOME, new Page(PageType.EMBED, eb.build()));

		if (args.length == 0) {

			for (Category cat : Category.values()) {
				EmbedBuilder ceb = new EmbedBuilder();
				ceb.setTitle(cat.getName());
				ceb.setColor(Color.PINK);
				ceb.setFooter(Main.getInfo().getFullName(), null);
				ceb.setThumbnail(Objects.requireNonNull(Main.getInfo().getAPI().getEmoteById(cat.getEmoteId())).getImageUrl());

				ceb.setDescription("Prefixo: `" + prefix + "`\n"
						+ cat.getCmds().size() + " comandos encontrados nesta categoria!");

				if (!cat.isEnabled(gc, guild))
					continue;
				if (cat.getCmds().size() == 0) {
					ceb.addField(Helper.VOID, cat.getDescription() + "\n*Ainda não existem comandos nesta categoria.*", false);
					continue;
				}

				StringBuilder cmds = new StringBuilder();

				for (Command cmd : cat.getCmds()) {
					cmds.append("`").append(cmd.getName()).append("`  ");
				}

				ceb.addField(Helper.VOID, cat.getDescription() + "\n" + cmds.toString().trim(), false);
				ceb.addField(Helper.VOID, "Para informações sobre um comando em especifico digite `" + prefix + "cmds [comando]`.", false);
				pages.put(cat.getEmoteId(), new Page(PageType.EMBED, ceb.build()));
			}

			EmbedBuilder ceb = new EmbedBuilder();
			ceb.setTitle("Dicas e Curiosidades");
			ceb.setColor(Color.PINK);
			ceb.setFooter(Main.getInfo().getFullName(), null);
			ceb.setThumbnail(Objects.requireNonNull(Objects.requireNonNull(Main.getInfo().getAPI().getEmoteById("684039810079522846")).getImageUrl()));

			ceb.addField(Helper.VOID, "Você pode usar emotes de qualquer servidor que a Shiro participe, basta digitar `e:` antes da mensagem, e `&` antes do nome do emote.", false);
			ceb.addField(Helper.VOID, "Os botões que aparecem em alguns comandos, como estes abaixo, são feitos usando uma biblioteca aberta de paginação escrita pelo meu Nii-chan. Você pode usá-la se quiser!", false);
			ceb.addField(Helper.VOID, "Membros que tenham uma waifu (usando o comando `" + prefix + "marry`) recebem 25% mais experiência quando no mesmo servidor que a/o waifu. Cada divórcio reduz permanentemente esse bônus!", false);
			ceb.addField(Helper.VOID, "Todo começo de mês, os membros do exceed vitorioso recebem 2x mais experiência por uma semana.", false);

			pages.put("684039810079522846", new Page(PageType.EMBED, ceb.build()));

			channel.sendMessage(eb.build()).queue(s -> Pages.categorize(Main.getInfo().getAPI(), s, pages, 60, TimeUnit.SECONDS));
			return;
		}

		eb.clear();
		eb.setColor(Color.PINK);
		eb.setFooter(Main.getInfo().getFullName(), null);
		eb.setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png");

		String cmdName = args[0];

		Command cmd = null;

		for (Command cmmd : Main.getCommandManager().getCommands()) {
			boolean found = false;

			if (cmmd.getName().equalsIgnoreCase(cmdName) && cmmd.getCategory().isEnabled(gc, guild)) found = true;
			else if (cmmd.getName().equalsIgnoreCase(cmdName) && !cmmd.getCategory().isEnabled(gc, guild)) {
				channel.sendMessage(":x: | Módulo desabilitado neste servidor!").queue();
				return;
			}

			for (String alias : cmmd.getAliases()) {
				if (alias.equalsIgnoreCase(cmdName) && cmmd.getCategory().isEnabled(gc, guild)) {
					found = true;
					break;
				} else if (alias.equalsIgnoreCase(cmdName) && !cmmd.getCategory().isEnabled(gc, guild)) {
					channel.sendMessage(":x: | Módulo desabilitado neste servidor!").queue();
					return;
				}
			}

			if (found) {
				cmd = cmmd;
				break;
			}
		}

		if (cmd == null) {
			channel.sendMessage(":x: | Esse comando não foi encontrado!").queue();
			return;
		}

		eb.setTitle(cmd.getName() + (cmd.getUsage() != null ? " " + cmd.getUsage() : ""));

		StringBuilder aliases = new StringBuilder("**Aliases**: ");

		for (String al : cmd.getAliases()) {
			aliases.append("`").append(al).append("`  ");
		}

		eb.setDescription(cmd.getDescription() + "\n"
				+ (cmd.getAliases().length != 0 ? aliases.toString().trim() + "\n" : "")
				+ "**Categoria**: " + cmd.getCategory().getName()
				+ "\n");

		channel.sendMessage(eb.build()).queue();

	}
}
