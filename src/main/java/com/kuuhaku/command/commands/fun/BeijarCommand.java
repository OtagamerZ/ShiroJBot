package com.kuuhaku.command.commands.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.ReactionsList;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Random;

public class BeijarCommand extends Command {

	private static final boolean answer = false;

	public BeijarCommand() {
		super("beijar", new String[] {"kiss"}, "<@usuário>", "Beija a pessoa mencionada.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		if (message.getMentionedUsers().size() == 0) { channel.sendMessage(":x: | Você tem que mencionar a pessoa que quer beijar!").queue(); return; }
		if (message.getMentionedUsers().size() > 1) { channel.sendMessage(":x: | Você só pode beijar uma pessoa de cada vez! Você não tem duas bocas...").queue(); return; }

		if (message.getMentionedUsers().get(0).getId().equals(author.getId())) { channel.sendMessage(":x: | Você não pode se beijar a si mesmo... ou será que pode ?").queue(); return; }

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
		} catch (IOException err) { err.printStackTrace(); }
	}

}
