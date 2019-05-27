package com.kuuhaku.command.commands.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.ReactionsList;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Random;

public class AbracarCommand extends Command {

	private static final boolean answer = false;

	public AbracarCommand() {
		super("abraçar", new String[] {"abracar", "vemca"}, "<@usuário>", "Abraça a pessoa mencionada.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		if (message.getMentionedUsers().size() == 0) { channel.sendMessage(":x: | Você tem que mencionar a pessoa que quer abraçar!").queue(); return; }
		if (message.getMentionedUsers().size() > 1) { channel.sendMessage(":x: | Você só pode abraçar uma pessoa de cada vez!").queue(); return; }

		if (message.getMentionedUsers().get(0).getId().equals(author.getId())) { channel.sendMessage(":x: | Você não pode se abraçar a si mesmo...").queue(); return; }

		try {
			URL url = new URL(ReactionsList.hug()[new Random().nextInt(ReactionsList.hug().length)]);
			HttpURLConnection con = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0");

			String msg = "";
			switch (new Random().nextInt(3)) {
				case 0:
					msg = "Awnn, que abraço fofo! :blush:";
					break;
				case 1:
					msg = "Já shippo os dois ein!";
					break;
				case 2:
					msg = "Ai sim ein, tem que ir pra cima mesmo!";
					break;
			}

			if (message.getMentionedUsers().get(0).getId().equals(Main.getInfo().getSelfUser().getId())) {
				if (message.getAuthor().getId().equals("350836145921327115")) {
					switch (new Random().nextInt(3)) {
						case 0:
							msg = ("Arigatou, Nii-chan!");
							break;
						case 1:
							msg = ("N-N-Não precisava, Nii-chan!");
							break;
						case 2:
							msg = ("N-N-Nii-chan no baka!");
							break;
					}
					channel.sendMessage(msg).addFile(con.getInputStream(), "hug.gif").queue();
				} else {
					switch (new Random().nextInt(3)) {
						case 0:
							msg = ("Moshi moshi, FBI-sama?");
							break;
						case 1:
							msg = ("B-B-Baka!");
							break;
						case 2:
							msg = ("Paraaa, to ocupada jogando agora!");
							break;
					}
					channel.sendMessage(msg).addFile(con.getInputStream(), "hug.gif").queue();
				}
			} else {
				if (!answer) { msg = (author.getAsMention() + " abraçou " + message.getMentionedUsers().get(0).getAsMention() + msg); }
				else {msg = ( message.getMentionedUsers().get(0).getAsMention() + " retribuiu o abraço " + author.getAsMention() + " - " + msg); }
				channel.sendMessage(msg).addFile(con.getInputStream(), "hug.gif").queue();
			}
		} catch (Exception err) { err.printStackTrace(); }
	}

}
