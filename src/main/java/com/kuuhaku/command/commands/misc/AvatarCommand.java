package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.io.IOException;


public class AvatarCommand extends Command {

	public AvatarCommand() {
		super("avatar", "<@usuário/guild>", "Dá-lhe o seu avatar ou então o avatar da pessoa mencionada. Para pegar o ícone do servidor digite apenas guild no lugar da menção.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		if (message.getMentionedUsers().size() > 1) { channel.sendMessage(":x: | Você só pode mencionar 1 utilizador de cada vez.").queue(); return; }

		EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Helper.getRandomColor());


		if (message.getMentionedUsers().size() == 0) {
			if(args.length > 0){
				if(args[0].trim().equalsIgnoreCase("guild")) {
                    if (guild.getIconUrl() == null) {
                        channel.sendMessage(":x: | O servidor não possui icon.").queue();
                        return;
                    }
					eb.setTitle("Icon do servidor");
					eb.setImage(guild.getIconUrl());
                    try {
                        eb.setColor(Helper.colorThief(guild.getIconUrl()));
                    } catch (IOException ignore) {
                    }
				}
			} else {
                if (author.getAvatarUrl() == null) {
                    channel.sendMessage(":x: | Você não possui avatar").queue();
                    return;
                }
				eb.setTitle("Seu avatar");
                eb.setImage(author.getAvatarUrl());
                try {
                    eb.setColor(Helper.colorThief(author.getAvatarUrl()));
                } catch (IOException ignore) {
                }
			}
		} else if (message.getMentionedUsers().size() == 1) {
			if(message.getMentionedUsers().get(0).getAvatarUrl() == null) { channel.sendMessage(":x: | O utilizador `" + message.getMentionedUsers().get(0).getAsTag() + "` não possui avatar.").queue(); return;}
			if(author.getId().equals(message.getMentionedUsers().get(0).getId())) {
				eb.setTitle("Seu avatar");
                eb.setImage(author.getAvatarUrl());
			} else {
				eb.setTitle("Avatar de: " + message.getMentionedUsers().get(0).getAsTag());
                eb.setImage(message.getMentionedUsers().get(0).getAvatarUrl());
                try {
                    eb.setColor(Helper.colorThief(message.getMentionedUsers().get(0).getAvatarUrl()));
                } catch (IOException ignore) {
                }
			}
		}
		channel.sendMessage(eb.build()).queue();
	}
}
