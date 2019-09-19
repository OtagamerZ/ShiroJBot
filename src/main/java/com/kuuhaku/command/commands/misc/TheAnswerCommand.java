package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import javax.persistence.NoResultException;

public class TheAnswerCommand extends Command {

	public TheAnswerCommand() {
		super("arespostaé", new String[]{"theansweris", "responder", "answer"}, "Leu as regras?", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (guild.getId().equals("421495229594730496")) {
			try {
				if (MySQL.getTagById(author.getId()).isReader())
					channel.sendMessage(":x: | Você já descobriu a resposta, não precisa mais usar este comando.").queue();
			} catch (NoResultException e) {
				MySQL.addUserTagsToDB(author.getId());
			} finally {
				message.delete().queue();
				if (!MySQL.getTagById(author.getId()).isReader()) {
					if (String.join(" ", args).replace(".", "").equalsIgnoreCase(System.getenv("SECRET"))) {
						MySQL.giveTagReader(author.getId());
						channel.sendMessage("Obrigado por ler as regras!").queue();
					} else {
						channel.sendMessage(":x: | Resposta errada, leia as regras para achar a resposta.").queue();
					}
				}
			}
		} else channel.sendMessage(":x: | Este comando só pode ser usado no servidor OtagamerZ.").queue();
	}
}
