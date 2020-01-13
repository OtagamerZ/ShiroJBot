package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.RelayBlockList;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class BlockCommand extends Command {

	public BlockCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public BlockCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public BlockCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public BlockCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		try {
			if (args.length > 2) {
				if (StringUtils.isNumeric(args[0])) {
					String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
					boolean isMentioned = message.getMentionedUsers().size() > 0;
					Member m = guild.getMember(Main.getJibril().getSelfUser());
					assert m != null;
					switch (args[1]) {
						case "temp":
							RelayBlockList.blockID(isMentioned ? message.getMentionedUsers().get(0).getId() : args[0], reason);
							Main.getRelay().relayMessage(message, (isMentioned ? message.getMentionedUsers().get(0).getAsMention() : "<@" + args[0] + ">") + " bloqueado do chat global.\nRazão: " + reason, m, guild, null);
							break;
						case "perma":
							if (Helper.hasPermission(member, PrivilegeLevel.DEV)) {
								RelayBlockList.permaBlockID(isMentioned ? message.getMentionedUsers().get(0).getId() : args[0], reason);
								Main.getRelay().relayMessage(message, (isMentioned ? message.getMentionedUsers().get(0).getAsMention() : "<@" + args[0] + ">") + " banido permanentemente do chat global.\nRazão: " + reason, m, guild, null);
							} else {
								channel.sendMessage(":x: | Permissões insuficientes.").queue();
							}
							break;
						case "thumb":
							RelayBlockList.blockThumb(isMentioned ? message.getMentionedUsers().get(0).getId() : args[0]);
							Main.getRelay().relayMessage(message, (isMentioned ? message.getMentionedUsers().get(0).getAsMention() : "Avatar de <@" + args[0] + ">") + " foi censurado do chat global.", m, guild, null);
							break;
						default:
							channel.sendMessage(":x: | Tipo inválido, o tipo deve ser thumb, temp ou perma.").queue();
					}
				} else {
					channel.sendMessage(":x: | ID inválido, identificadores possuem apenas dígitos de 0 à 9.").queue();
				}
			} else {
				channel.sendMessage(":x: | Você precisa passar o ID do usuário a ser bloqueado, o tipo de bloqueio (thumb/temp/perma) e a razão para o bloqueio.").queue();
			}
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | ID de usuário incorreto.").queue();
		}
	}
}
