package com.kuuhaku.command.commands.dev;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.util.Arrays;

public class BroadcastCommand extends Command {

	public BroadcastCommand() {
		super("broadcast", new String[]{"bc", "avisar"}, "<tipo> <mensagem>", "Envia um aviso a todos os donos de servidor que possuem a Shiro, ou a todos o parceiros.", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | É necessário informar um tipo de broadcast (geral/parceiros).").queue();
		} else if (args.length < 2) {
			channel.sendMessage(":x: | É necessário informar uma mensagem para enviar.").queue();
		}

		String msg = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
		switch (args[1]) {
			case "geral":
				channel.sendMessage(msg).queue();
				break;
			case "parceiros":
				channel.sendMessage(msg).queue();
				break;
			default: channel.sendMessage(":x: | Tipo desconhecido, os tipos válidos são **geral** ou **parceiros**").queue();
		}
	}
}
