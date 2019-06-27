package com.kuuhaku.command.commands.partner;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.persistence.NoResultException;

public class AiCommand extends Command {

	public AiCommand() {
		super("ia", "Cria uma instância da IA da Shiro para seu servidor.", Category.PARTNER);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		try {
			if (!MySQL.getTagById(author.getId()).isPartner() && !Helper.hasPermission(member, PrivilegeLevel.DEV)) {
				channel.sendMessage(":x: | Este comando é exlusivo para parceiros!").queue();
				return;
			}
		} catch (NoResultException e) {
			channel.sendMessage(":x: | Este comando é exlusivo para parceiros!").queue();
			return;
		}

		guildConfig gc = SQLite.getGuildById(guild.getId());

		if (gc.isAiMode()) {
			SQLite.updateGuildIaMode(false, gc);
			channel.sendMessage("Modo IA desligado").queue();
		} else {
			SQLite.updateGuildIaMode(true, gc);
			channel.sendMessage("Modo IA habilitado, para iniciar uma conversa com a Shiro digite `Quero falar com a Shiro`").queue();
		}
	}
}
