package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.util.Random;




public class AvatarCommand extends Command {

	public AvatarCommand() {
		super("avatar", "[@utilizador]", "Dá-lhe o seu avatar ou então o avatar da pessoa mencionada.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		if (message.getMentionedUsers().size() > 1) { channel.sendMessage(":x: | Você só pode mencionar 1 utilizador de cada vez.").queue(); return; }

		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor((new Random()).nextInt(6331903));


		if (message.getMentionedUsers().size() == 0) {
			if(args.length > 0){
				if(args[0].trim().equalsIgnoreCase("guild")) {
					if(guild.getIconUrl() == null) { channel.sendMessage(":x: | O servidor não possui icon.").queue(); return; }
					eb.setTitle("Icon do servidor");
					eb.setImage(guild.getIconUrl());
				}
			} else {
				if (author.getAvatarUrl() == null) { channel.sendMessage(":x: | Você não possui avatar").queue(); return; }
				eb.setTitle("Seu avatar");
				eb.setImage(author.getAvatarUrl() + "?size=2048");
			}
		} else if (message.getMentionedUsers().size() == 1) {
			if(message.getMentionedUsers().get(0).getAvatarUrl() == null) { channel.sendMessage(":x: | O utilizador `" + message.getMentionedUsers().get(0).getAsTag() + "` não possui avatar.").queue(); return;}
			if(author.getId().equals(message.getMentionedUsers().get(0).getId())) {
				eb.setTitle("Seu avatar");
				eb.setImage(author.getAvatarUrl() + "?size=2048");
			} else {
				eb.setTitle("Avatar de: " + message.getMentionedUsers().get(0).getAsTag());
				eb.setImage(message.getMentionedUsers().get(0).getAvatarUrl() + "?size=2048");
			}
		}
		channel.sendMessage(eb.build()).queue();
	}
}
