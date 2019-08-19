package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.RelayBlockList;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class BlockCommand extends Command {

	public BlockCommand() {
		super("block", new String[]{"bloquear"}, "Bloqueia alguém de usar o chat global.", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		try {
			if (args.length > 2) {
				if (StringUtils.isNumeric(args[0])) {
					String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
					boolean isMentioned = message.getMentionedUsers().size() > 0;
					switch (args[1]) {
						case "temp":
							RelayBlockList.blockID(isMentioned ? message.getMentionedUsers().get(0).getId() : args[0], reason);
							Main.getRelay().relayMessage(message, (isMentioned ? message.getMentionedUsers().get(0).getAsMention() : "<@" + args[0] + ">") + " bloqueado do chat global.\nRazão: " + reason, guild.getSelfMember(), guild, null);
							break;
						case "perma":
							RelayBlockList.permaBlockID(isMentioned ? message.getMentionedUsers().get(0).getId() : args[0]);
							Main.getRelay().relayMessage(message, (isMentioned ? message.getMentionedUsers().get(0).getAsMention() : "<@" + args[0] + ">") + " banido permanentemente do chat global.\nRazão: " + reason, guild.getSelfMember(), guild, null);
							break;
						case "thumb":
							RelayBlockList.blockThumb(isMentioned ? message.getMentionedUsers().get(0).getId() : args[0]);
							Main.getRelay().relayMessage(message, (isMentioned ? message.getMentionedUsers().get(0).getAsMention() : "Avatar de <@" + args[0] + ">") + " foi censurado do chat global.", guild.getSelfMember(), guild, null);
							break;
						default:
							channel.sendMessage(":x: | Tipo inválido, o tipo deve ser temp ou perma.").queue();
					}
				} else {
					channel.sendMessage(":x: | ID inválido, identificadores possuem apenas dígitos de 0 à 9.").queue();
				}
			} else {
				channel.sendMessage(":x: | Você precisa passar o ID do usuário a ser bloqueado, o tipo de bloqueio (temp/perma) e a razão para o bloqueio.").queue();
			}
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | ID de usuário incorreto.").queue();
		}
	}
}
