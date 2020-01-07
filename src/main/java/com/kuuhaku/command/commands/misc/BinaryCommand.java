package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;

public class BinaryCommand extends Command {

	public BinaryCommand() {
		super("bin", "<texto>", "Transforma uma frase ou texto em binário", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		String text = String.join(" ", args);
		try {
			byte[] bytes = text.getBytes();
			StringBuilder binary = new StringBuilder();
			for (byte b : bytes)
			{
				int val = b;
				for (int i = 0; i < 8; i++)
				{
					binary.append((val & 128) == 0 ? 0 : 1);
					val <<= 1;
				}
				binary.append(' ');
			}
			channel.sendMessage(":1234: `" + binary.toString() + "`").queue();
		} catch (IllegalStateException e) {
			channel.sendMessage(":x: | Mensagem muito grande (Max: 2000 | Lembre-se que cada caractére vale por 8, incluindo espaços).").queue();
		}
	}
}
