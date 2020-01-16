package com.kuuhaku.command.commands.dev;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LeaveCommand extends Command {

	public LeaveCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public LeaveCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public LeaveCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public LeaveCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new EmbedBuilder();

		List<String[]> servers = new ArrayList<>();
		Main.getInfo().getAPI().getGuilds().forEach(g -> servers.add(new String[]{g.getName(), g.getId(), String.valueOf(g.getMembers().stream().filter(m -> !m.getUser().isBot()).count())}));
		List<List<String[]>> svPages = Helper.chunkify(servers, 10);

		List<Page> pages = new ArrayList<>();

		for (int i = 0; i < svPages.size(); i++) {
			eb.clear();

			eb.setTitle("Servidores que eu participo:");
			svPages.get(i).forEach(p -> eb.addField("Nome: " + p[0], "ID: " + p[1] + "\nMembros: " + p[2], false));
			eb.setFooter("Página " + (i + 1) + " de " + svPages.size() + ". Total de " + svPages.stream().mapToInt(List::size).sum() + " resultados.", null);

			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		try {
			Guild guildToLeave = Main.getInfo().getGuildByID(rawCmd.split(" ")[1]);
			guildToLeave.leave().queue();
			channel.sendMessage("Ok, acabei de sair desse servidor!").queue();
		} catch (ArrayIndexOutOfBoundsException e) {
			channel.sendMessage("Escolha o servidor que devo sair!\n").embed((MessageEmbed) pages.get(0).getContent()).queue(m -> Pages.paginate(Main.getInfo().getAPI(), m, pages, 60, TimeUnit.SECONDS));
		} catch (NullPointerException ex) {
			channel.sendMessage(":x: | Servidor não encontrado!\n").embed((MessageEmbed) pages.get(0).getContent()).queue(m -> Pages.paginate(Main.getInfo().getAPI(), m, pages, 60, TimeUnit.SECONDS));
		}
	}

}
