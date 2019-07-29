package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class SayCommand extends Command {
	
	public SayCommand() {
		super("say", new String[] {"diga", "repetir"}, "<mensagem>", "Repete a mensagem, também converterá menções de emotes.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		
		if(args.length == 0) { channel.sendMessage(":x: | Você precisa definir uma mensagem.").queue(); return; }

		if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) message.delete().queue();
		channel.sendMessage(Helper.makeEmoteFromMention(args)).queue();
	}

}
