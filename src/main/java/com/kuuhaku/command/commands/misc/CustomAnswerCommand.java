package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.util.ArrayList;
import java.util.List;

public class CustomAnswerCommand extends Command {

	public CustomAnswerCommand() {
		super("fale", "<gatilho>;<resposta>", "Configura uma resposta para o gatilho (frase) especificado.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (!Helper.hasPermission(member, PrivilegeLevel.MOD) && !SQLite.getGuildById(guild.getId()).isAnyTell()) {
			channel.sendMessage(":x: | Este servidor não está configurado para permitir respostas customizadas da comunidade.").queue();
			return;
		} else if (args.length == 0) {
			channel.sendMessage(":x: | Você precisa definir um gatilho e uma mensagem.").queue();
			return;
		} else if (args[0].equals("lista")) {
			try {
				int page = Integer.parseInt(args[1]);
				List<CustomAnswers> ca = SQLite.getCADump();
				List<String> answers = new ArrayList<>();
				ca.removeIf(a -> !a.getGuildID().equals(guild.getId()));
				for (int i = -10 + (10 * page); i < ca.size() && i <= (10 * page); i++) {
					answers.add("`(" + ca.get(i).getId() + ")` " + ca.get(i).getGatilho() + " **->** " + ca.get(i).getAnswer() + "");
				}

				channel.sendMessage("__**Respostas deste servidor:**__\n\n" + (answers.size() == 0 ? "`Nenhuma`" : answers.toString().replace("[", "").replace("]", "").replace(", ", "\n"))).queue();
				return;
			} catch (NumberFormatException e) {
				channel.sendMessage(":x: | Número inválido.").queue();
				return;
			} catch (ArrayIndexOutOfBoundsException ex) {
				channel.sendMessage(":x: | Você precisa definir uma página.").queue();
				return;
			}
		}

		String txt = String.join(" ", args);

		if (txt.contains(";")) {
			if (txt.split(";")[1].length() <= 200) {
				SQLite.addCAtoDB(guild, txt.split(";")[0], txt.split(";")[1]);
				channel.sendMessage("Agora quando alguém disser `" + txt.split(";")[0] + "` irei responder `" + txt.split(";")[1] + "`.").queue();
			} else {
				channel.sendMessage(":x: | Woah, essa resposta é muito longa, não consigo decorar isso tudo!").queue();
			}
		} else {
			channel.sendMessage(":x: | O gatilho e a resposta devem estar separados por ponto e virgula (`;`).").queue();
		}
	}
}
