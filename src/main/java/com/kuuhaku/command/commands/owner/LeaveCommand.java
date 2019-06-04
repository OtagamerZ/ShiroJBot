package com.kuuhaku.command.commands.owner;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.util.ArrayList;
import java.util.List;

public class LeaveCommand extends Command {

	public LeaveCommand() {
		super("leave", new String[]{"sair"}, "<ID do servidor>", "Sai do servidor cujo o ID foi dado.", Category.OWNER);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		List<String> servers = new ArrayList<>();
		Main.getInfo().getAPI().getGuilds().forEach(g -> servers.add("(" + g.getId() + ") " + g.getName()));
		String serverList = servers.toString().replace("[", "```").replace("]", "```").replace(",", "\n");
		try {
			Guild guildToLeave = Main.getInfo().getGuildByID(rawCmd.split(" ")[1]);
			guildToLeave.leave().queue();
			channel.sendMessage("Ok, acabei de sair desse servidor, Nii-chan!").queue();
		} catch (ArrayIndexOutOfBoundsException e) {
			channel.sendMessage("Você esqueceu de me dizer o ID do servidor, Nii-chan!\n" + serverList).queue();
		} catch (NullPointerException ex) {
			channel.sendMessage("Servidor não encontrado!\n" + serverList).queue();
		}

	}

}
