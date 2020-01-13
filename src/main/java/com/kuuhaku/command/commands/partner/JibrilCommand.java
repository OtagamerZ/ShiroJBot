package com.kuuhaku.command.commands.partner;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

public class JibrilCommand extends Command {

	public JibrilCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public JibrilCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public JibrilCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public JibrilCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
        Helper.nonPartnerAlert(author, member, channel, "Para chamar a Jibril para seu servidor, utilize este link:\n", "JIBRIL_LINK");
    }

}
