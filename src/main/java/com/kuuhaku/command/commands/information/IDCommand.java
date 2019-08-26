package com.kuuhaku.command.commands.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class IDCommand extends Command {

	public IDCommand() {
		super("id", "Pesquisa o ID dos usuários com o nome informado", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length > 0) {
			try {
				String arg = String.join(" ", args);
				System.out.println(arg);
				String sv = Helper.containsAll(arg, "(", ")") ? arg.substring(arg.indexOf("("), arg.indexOf(")") + 1) : "";
				System.out.println(sv);
				String ex = Helper.containsAll(arg, "[", "]") ? arg.substring(arg.indexOf("["), arg.indexOf("]") + 1) : "";
				System.out.println(ex);
				String name = arg.replace(sv, "").replace(ex, "").trim();
				System.out.println(name);
				List<User> us = Main.getInfo().getAPI().getUsersByName(name, false);
				System.out.println(us);
				try {
					if (!sv.isEmpty())
						us.removeIf(
								u -> u.getMutualGuilds().stream().map(Guild::getName).collect(Collectors.toList()).contains(sv)
						);
				} catch (Exception ignore) {
				}
				try {
					if (!ex.isEmpty())
						us.removeIf(u ->
								MySQL.getMembers().stream().noneMatch(m -> m.getExceed().equals(ex))
						);
				} catch (Exception ignore) {
				}
				String ids = us.stream()
						.map(u -> u.getAsTag() + " -> " + u.getId() + "\n").collect(Collectors.joining());
				EmbedBuilder eb = new EmbedBuilder();

				eb.setTitle("IDs dos usuários encontrados");
				eb.setDescription(ids);
				eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));

				channel.sendMessage(eb.build()).queue();
			} catch (Exception e) {
				channel.sendMessage(":x: | Nenhum usuário encontrado.").queue();
			}
		}
	}

}
