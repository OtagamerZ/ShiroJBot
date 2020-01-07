package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

public class PermissionCommand extends Command {

	public PermissionCommand() {
		super("permissões", new String[]{"perms", "permisions"}, "Mostra quais permissões a Shiro/Jibril possuem.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage(Helper.getRequiredPerms(message.getTextChannel())).queue();
	}
}
