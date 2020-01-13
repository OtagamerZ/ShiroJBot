package com.kuuhaku.command.commands.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.method.Pages;
import com.kuuhaku.model.GuildConfig;
import com.kuuhaku.model.Page;
import com.kuuhaku.type.PageType;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ComandosCommand extends Command {

	public ComandosCommand() {
		super("comandos", new String[]{"cmds", "cmd", "comando", "ajuda", "help"}, "[comando]", "Fornece uma lista de todos os comandos disponiveis no bot.", Category.INFO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("**Lista de Comandos**");
		eb.setDescription("Clique nas categorias abaixo para ver os comandos de cada uma.\n\n" +
				"Prefixo: `" + prefix + "`\n"
				+ Arrays.stream(Category.values()).filter(c -> c.isEnabled(gc, guild)).count() + " categorias encontradas!" + "\n"
				+ Main.getCommandManager().getCommands().stream().filter(c -> c.getCategory().isEnabled(gc, guild)).count() + " comandos encontrados!");
		for (Category cat : Category.values()) {
			if (cat.isEnabled(gc, guild)) eb.addField(cat.getEMOTE() + " | " + cat.getName(), Helper.VOID, true);
		}
		eb.setColor(Color.PINK);
		eb.setFooter(Main.getInfo().getFullName(), null);
		eb.setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png");


		if (args.length == 0) {
			Map<String, Page> pages = new HashMap<>();

			for (Category cat : Category.values()) {
				EmbedBuilder ceb = new EmbedBuilder();
				ceb.setColor(Color.PINK);
				ceb.setFooter(Main.getInfo().getFullName(), null);
				ceb.setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png");

				ceb.setDescription("Prefixo: `" + prefix + "`\n"
						+ cat.getCmds().size() + " comandos encontrados nesta categoria!");

				if (!cat.isEnabled(gc, guild))
					continue;
				if (cat.getCmds().size() == 0) {
					ceb.addField(cat.getName(), cat.getDescription() + "\n*Ainda não existem comandos nesta categoria.*", false);
					continue;
				}

				StringBuilder cmds = new StringBuilder();

				for (Command cmd : cat.getCmds()) {
					cmds.append("`").append(cmd.getName()).append("`  ");
				}

				ceb.addField(cat.getName(), cat.getDescription() + "\n" + cmds.toString().trim(), false);
				ceb.addField(Helper.VOID, "Para informações sobre um comando em especifico digite `" + prefix + "cmds [comando]`.", false);
				pages.put(cat.getEMOTE(), new Page(PageType.EMBED, ceb.build()));
			}

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

			if (!cmmd.getCategory().isEnabled(gc, guild)) {
				channel.sendMessage(":x: | Módulo desabilitado neste servidor!").queue();
				continue;
			}
			else if (cmmd.getName().equalsIgnoreCase(cmdName)) found = true;

			for (String alias : cmmd.getAliases()) {
				if (alias.equalsIgnoreCase(cmdName)) {
					found = true;
					break;
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
