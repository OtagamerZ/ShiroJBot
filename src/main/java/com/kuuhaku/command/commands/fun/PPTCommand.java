package com.kuuhaku.command.commands.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.util.Random;

public class PPTCommand extends Command {
	
	public PPTCommand() {
		super("ppt", new String[] {"rps"}, "<pedra/papel/tesoura>", "A Shiro joga pedra/papel/tesoura com você.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		
		if(args.length < 1) { channel.sendMessage(":x: | Você tem que escolher pedra, papel ou tesoura!").queue(); return; }
		
		int pcOption = Math.abs((new Random()).nextInt()) % 3;
		int win = 2;
		
		switch(args[0].toLowerCase()) {
			case "pedra":
				switch(pcOption) {
					case 1:
						win = 0;
						break;
					case 2:
						win = 1;
						break;
				}
				break;
			case "papel":
				switch(pcOption) {
					case 0:
						win = 1;
						break;
					case 2:
						win = 0;
						break;
				}
				break;
			case "tesoura":
				switch(pcOption) {
					case 0:
						win = 0;
						break;
					case 1:
						win = 1;
						break;
				}
				break;
			default:
			channel.sendMessage(":x: | Você tem que escolher pedra, papel ou tesoura!").queue();
			return;
		}
		
		String pcChoice = "";
		
		switch(pcOption) {
			case 0:
				pcChoice = ":punch: **Pedra**";
				break;
			case 1:
				pcChoice = ":raised_back_of_hand: **Papel**";
				break;
			case 2:
				pcChoice = ":v: **Tesoura**";
				break;
		}
		
		switch(win) {
			case 0:
				channel.sendMessage("Perdeu! Eu escolhi " + pcChoice).queue();
				break;
			case 1:
				channel.sendMessage("Ganhou! Eu escolhi " + pcChoice).queue();
				break;
			case 2:
				channel.sendMessage("Empate! Eu escolhi " + pcChoice).queue();
				break;
		}
		
	}

}
