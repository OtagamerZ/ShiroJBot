package com.kuuhaku.command.commands.dev;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InviteCommand extends Command {

	public InviteCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public InviteCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public InviteCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public InviteCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new EmbedBuilder();

		List<String[]> servers = new ArrayList<>();
		Main.getInfo().getAPI().getGuilds().stream().filter(s -> s.getSelfMember().hasPermission(Permission.CREATE_INSTANT_INVITE) && s.getDefaultChannel() != null).forEach(g -> servers.add(new String[]{g.getName(), g.getId(), String.valueOf(g.getMembers().stream().filter(m -> !m.getUser().isBot()).count())}));
		List<List<String[]>> svPages = Helper.chunkify(servers, 10);

		List<Page> pages = new ArrayList<>();

		for (int i = 0; i < svPages.size(); i++) {
			eb.clear();

			eb.setTitle("Servidores que eu posso criar um convite:");
			svPages.get(i).forEach(p -> eb.addField("Nome: " + p[0], "ID: " + p[1] + "\nMembros: " + p[2], false));
			eb.setFooter("Página " + (i + 1) + " de " + svPages.size() + ". Total de " + svPages.stream().mapToInt(List::size).sum() + " resultados.", null);

			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		try {
			Guild guildToInvite = Main.getInfo().getGuildByID(rawCmd.split(" ")[1]);
			assert guildToInvite.getDefaultChannel() != null;
			String invite = guildToInvite.getDefaultChannel().createInvite().setMaxUses(1).setMaxAge((long) 30, TimeUnit.SECONDS).complete().getUrl();
			channel.sendMessage("Aqui está!\n" + invite).queue();
		} catch (ArrayIndexOutOfBoundsException e) {
			channel.sendMessage("Escolha o servidor que devo criar um convite!\n").embed((MessageEmbed) pages.get(0).getContent()).queue(m -> Pages.paginate(Main.getInfo().getAPI(), m, pages, 60, TimeUnit.SECONDS));
		} catch (NullPointerException ex) {
			channel.sendMessage(":x: | Servidor não encontrado!\n").embed((MessageEmbed) pages.get(0).getContent()).queue(m -> Pages.paginate(Main.getInfo().getAPI(), m, pages, 60, TimeUnit.SECONDS));
		}
	}
}
