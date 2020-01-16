package com.kuuhaku.command.commands.dev;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.LogDAO;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UsageCommand extends Command {

	public UsageCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public UsageCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public UsageCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public UsageCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new EmbedBuilder();

		List<LogDAO.UsageView> usos = LogDAO.getUses();
		List<List<LogDAO.UsageView>> uPages = Helper.chunkify(usos, 10);

		List<Page> pages = new ArrayList<>();

		for (int i = 0; i < uPages.size(); i++) {
			eb.clear();

			eb.setTitle("Quantidade de comandos usados por servidor:");
			uPages.get(i).forEach(p -> eb.addField("Servidor: " + p.getGuild(), "Usos: " + p.getUses(), false));
			eb.setFooter("Página " + (i + 1) + " de " + uPages.size() + ". Total de " + uPages.stream().mapToInt(List::size).sum() + " resultados.", null);

			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(Main.getInfo().getAPI(), s, pages, 60, TimeUnit.SECONDS));
	}
}
