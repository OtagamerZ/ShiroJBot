package com.kuuhaku.command.commands.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class IDCommand extends Command {

	public IDCommand() {
		super("id", "<(guild)> <nome>", "Pesquisa o ID dos usuários com o nome informado", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length > 0) {
			try {
				String arg = String.join(" ", args);
				String sv = Helper.containsAll(arg, "(", ")") ? arg.substring(arg.indexOf("("), arg.indexOf(")") + 1) : "";
				String ex = Helper.containsAll(arg, "[", "]") ? arg.substring(arg.indexOf("["), arg.indexOf("]") + 1) : "";
				String name = arg.replace(sv, "").replace(ex, "").trim();
				List<User> us = Main.getInfo().getAPI().getUsersByName(name, true);
				String ids = us.stream().map(u -> u.getAsTag() + " -> " + u.getId() + "\n").collect(Collectors.joining());
				EmbedBuilder eb = new EmbedBuilder();

				eb.setTitle("IDs dos usuários encontrados");
				eb.setDescription(ids);
				eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));

				channel.sendMessage(eb.build()).queue();
			} catch (InsufficientPermissionException ex) {
				channel.sendMessage(":x: | Não consigo mandar embeds aqui.").queue();
			} catch (Exception e) {
				channel.sendMessage(":x: | Nenhum usuário encontrado.").queue();
			}
		}
	}

}
