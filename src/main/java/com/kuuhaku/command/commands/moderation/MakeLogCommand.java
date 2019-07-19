package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;

public class MakeLogCommand extends Command {

	public MakeLogCommand() {
		super("log", new String[]{"makelog"}, "Cria um canal de log para as ações da Shiro.", Category.MODERACAO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		try {
			guildConfig gc = SQLite.getGuildById(guild.getId());

			if (gc.getLogChannel() != null && !gc.getLogChannel().isEmpty()) {
				try {
					guild.getTextChannelById(gc.getLogChannel()).delete().queue();
				} catch (Exception ignore) {
				}
				guild.getController().createTextChannel("shiro-log").queue(c -> {
					gc.setLogChannel(c.getId());
					channel.sendMessage("Canal de log criado com sucesso em " + ((TextChannel) c).getAsMention()).queue();
				});
			}

			SQLite.updateGuildChannels(gc);
		} catch (InsufficientPermissionException e) {
			channel.sendMessage(":x: | Não tenho permissões sufficientes para criar um canal.").queue();
		}
	}
}
