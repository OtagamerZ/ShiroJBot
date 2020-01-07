package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

public class SayCommand extends Command {
	
	public SayCommand() {
		super("say", new String[] {"diga", "repetir"}, "<mensagem>", "Repete a mensagem, também converterá menções de emotes.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		
		if(args.length == 0) { channel.sendMessage(":x: | Você precisa definir uma mensagem.").queue(); return; }

		channel.sendMessage(Helper.makeEmoteFromMention(args)).queue();
		if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) message.delete().queue();
	}

}
