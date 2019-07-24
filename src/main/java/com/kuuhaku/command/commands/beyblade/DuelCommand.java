package com.kuuhaku.command.commands.beyblade;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.DuelData;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class DuelCommand extends Command {

	public DuelCommand() {
		super("bduel", new String[]{"bduelar", "desafiar"}, "<@usuário>", "Desafia um usuário para um duelo de Beyblades.", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (MySQL.getBeybladeById(author.getId()) == null) {
			channel.sendMessage(":x: | Você não possui uma Beyblade.").queue();
			return;
		} else if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(":x: | Você precisa mencionar um usuário.").queue();
			return;
		} else if (MySQL.getBeybladeById(message.getMentionedUsers().get(0).getId()) == null) {
			channel.sendMessage(":x: | Este usuário não possui uma Beyblade.").queue();
			return;
		} else if (message.getMentionedUsers().get(0) == author && !author.getId().equals("282546525836541963")) {
			channel.sendMessage(":x: | Você não pode duelar contra você mesmo.").queue();
			return;
		}

		channel.sendMessage("<a:Loading:598500653215645697> Coletando dados...").queue(m -> {
			if (message.getMentionedUsers().size() > 0) {
				if (MySQL.getBeybladeById(message.getMentionedUsers().get(0).getId()) != null) {
					DuelData dd = new DuelData(message.getAuthor(), message.getMentionedUsers().get(0));
					if (ShiroInfo.duels.containsValue(dd))
						m.editMessage("Você já possui um duelo pendente!").queue();
					else
						m.editMessage(message.getMentionedMembers().get(0).getAsMention() + ", você foi desafiado a um duelo de Beyblades por " + message.getAuthor().getAsMention() + ". Se deseja aceitar, clique no botão abaixo:").queue(ms -> {
									ms.addReaction("\u2694").queue();
									ShiroInfo.duels.put(ms.getId(), dd);
								});
				} else {
					m.editMessage("Este usuário ainda não possui uma Beyblade.").queue();
				}
			}
		});
	}
}
