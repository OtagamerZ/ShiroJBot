package com.rdx.command.commands.fun;

import com.rdx.Main;
import com.rdx.command.Category;
import com.rdx.command.Command;
import com.rdx.model.ReactionsList;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Random;

public class BeijarCommand extends Command {

	public static final boolean answer = false;

	public BeijarCommand() {
		super("beijar", new String[] {"kiss"}, "<@usuário>", "Beija a pessoa mencionada.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		if (message.getMentionedUsers().size() == 1) {
			if(!message.getMentionedUsers().get(0).getId().equals(author.getId())) {
				try {
					URL url = new URL(ReactionsList.kiss()[new Random().nextInt(ReactionsList.kiss().length)]);
					HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
					con.setRequestProperty("User-Agent", "Mozilla/5.0");

					String msg = "";

					if (message.getMentionedUsers().get(0).getId().equals(Main.getInfo().getSelfUser().getId())) {
						if (author.getId().equals("321665807988031495")) {
							switch (new Random().nextInt(3)) {
								case 0:
									msg = ("E-Ei!");
									break;
								case 1:
									msg = ("N-Nii-chan?!");
									break;
								case 2:
									msg = ("P-Pera!");
									break;
							}
							channel.sendMessage(msg).addFile(con.getInputStream(), "kiss.gif").queue();
						} else {
							channel.sendMessage(msg).addFile(con.getInputStream(), "kiss.gif").queue();

							switch (new Random().nextInt(3)) {
								case 0:
									msg = "Não tão rápido!";
									break;
								case 1:
									msg = "Só que não!";
									break;
								case 2:
									msg = "Hoje não!";
									break;
							}
							msg = (author.getAsMention() + " esquivou-se - " + msg);
							channel.sendMessage(msg).addFile(con.getInputStream(), "nope.gif").queue();
						}
					} else {
						if (!answer) { msg = (author.getAsMention() + " beijou " + message.getMentionedUsers().get(0).getAsMention() + msg); }
						else { msg = (message.getMentionedUsers().get(0).getAsMention() + " também deu um beijo em " + author.getAsMention() + " - " + msg); }
						channel.sendMessage(msg).addFile(con.getInputStream(), "kiss.gif").queue();
					}
				} catch (IOException err) {
					err.printStackTrace();
				}
			} else {
				channel.sendMessage(":x: | Você não pode se beijar a si mesmo...").queue();
			}
		} else if (message.getMentionedUsers().size() > 1) {
			channel.sendMessage(":x: | Você só pode beijar uma pessoa de cada vez! Você não tem duas bocas...").queue();
		} else {
			channel.sendMessage(":x: | Você tem que mencionar a pessoa que quer beijar!").queue();
		}
		
	}

}
