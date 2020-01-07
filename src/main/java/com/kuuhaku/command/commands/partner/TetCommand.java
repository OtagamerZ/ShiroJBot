package com.kuuhaku.command.commands.partner;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

public class TetCommand extends Command {

	public TetCommand() {
		super("tet", "Chama o Tet para ser a mensageira em seu servidor.", Category.PARTNER);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Helper.nonPartnerAlert(author, member, channel, "Para chamar o Tet para seu servidor, utilize este link:\n", "TET_LINK");
	}
}
