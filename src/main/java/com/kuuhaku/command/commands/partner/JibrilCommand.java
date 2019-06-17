package com.kuuhaku.command.commands.partner;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class JibrilCommand extends Command {

	public JibrilCommand() {
		super("jibril", "Chama a Jibril para ser a mensageira em seu servidor.", Category.PARTNER);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (!MySQL.getTagById(author.getId()).isPartner() && !Helper.hasPermission(member, PrivilegeLevel.DEV)) {
			channel.sendMessage(":x: | Este comando é exlusivo para parceiros!").queue();
			return;
		}

		author.openPrivateChannel().queue(c -> c.sendMessage("Olá, obrigado por apoiar meu desenvolvimento!\n\nPara chamar a Jibril para seu servidor, utilize este link:\n" + System.getenv("JIBRIL_LINK")).queue());
	}

}
