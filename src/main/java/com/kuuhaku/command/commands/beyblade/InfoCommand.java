package com.kuuhaku.command.commands.beyblade;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.Beyblade;
import com.kuuhaku.model.Special;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InfoCommand extends Command {

	public InfoCommand() {
		super("binfo", new String[]{"bbeyblade", "bb"}, "Mostra dados sobre sua Beyblade.", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (MySQL.getBeybladeById(author.getId()) == null) {
			channel.sendMessage(":x: | Você não possui uma Beyblade.").queue();
			return;
		}

		channel.sendMessage("<a:Loading:598500653215645697> Analizando...").queue(m -> {
			EmbedBuilder eb = new EmbedBuilder();
			Beyblade bb = Objects.requireNonNull(MySQL.getBeybladeById(author.getId()));
			if (args.length > 0) {
				switch (args[0].toLowerCase()) {
					case "tigre":
					case "tiger":
						eb.setTitle("Alinhamento TIGRE");
						eb.setThumbnail("https://i.imgur.com/fLZPBP8.png");
						eb.setColor(Color.decode("#ffb300"));
						for (Special s : new ArrayList<Special>() {{
							add(Special.getSpecial(11));
							add(Special.getSpecial(12));
						}}) {
							eb.addField(s.getName(), s.getDescription(), false);
						}
						m.delete().queue();
						channel.sendMessage(eb.build()).queue();
						return;
					case "dragão":
					case "dragon":
						eb.setTitle("Alinhamento DRAGÃO");
						eb.setThumbnail("https://i.imgur.com/g2L0cfy.png");
						eb.setColor(Color.RED);
						for (Special s : new ArrayList<Special>() {{
							add(Special.getSpecial(21));
							add(Special.getSpecial(22));
						}}) {
							eb.addField(s.getName(), s.getDescription(), false);
						}
						m.delete().queue();
						channel.sendMessage(eb.build()).queue();
						return;
					case "urso":
					case "bear":
						eb.setTitle("Alinhamento URSO");
						eb.setThumbnail("https://i.imgur.com/MG789l8.png");
						eb.setColor(Color.decode("#00ffb3"));
						for (Special s : new ArrayList<Special>() {{
							add(Special.getSpecial(31));
						}}) {
							eb.addField(s.getName(), s.getDescription(), false);
						}
						m.delete().queue();
						channel.sendMessage(eb.build()).queue();
						return;
				}
			}

			if (args.length == 1 && (args[0].equals("alinhamento") || args[0].equals("casa"))) {
				if (bb.getS() == null) {
					m.editMessage(":x: | Você não possui um alinhamento.").queue();
					return;
				}
				eb.setAuthor("Alinhamento de " + bb.getName() + ":");
				switch (bb.getS().getType()) {
					case "TIGER":
						eb.setThumbnail("https://i.imgur.com/fLZPBP8.png");
						eb.setColor(Color.decode("#ffb300"));
						break;
					case "DRAGON":
						eb.setThumbnail("https://i.imgur.com/g2L0cfy.png");
						eb.setColor(Color.RED);
						break;
					case "BEAR":
						eb.setThumbnail("https://i.imgur.com/MG789l8.png");
						eb.setColor(Color.decode("#00ffb3"));
						break;
				}
				eb.setColor(Color.decode(bb.getColor()));
				eb.addField(bb.getS().getName(), bb.getS().getDescription(), false);
				eb.addField("Dificuldade:", bb.getS().getDiff() + "/100 (" + (
						bb.getS().getDiff() <= 25 ? "fácil" : (
								bb.getS().getDiff() > 25 && bb.getS().getDiff() <= 50 ? "médio" : (
										bb.getS().getDiff() > 50 && bb.getS().getDiff() <= 75 ? "difícil" : "praticamente impossível"
								))
				) + ")", true);

				m.delete().queue();
				channel.sendMessage(eb.build()).queue();
				return;
			}

			eb.setAuthor("Beyblade de " + message.getAuthor().getName(), message.getAuthor().getAvatarUrl());
			eb.setTitle(bb.getName());
			eb.setColor(Color.decode(bb.getColor()));
			if (bb.getS() == null) eb.setThumbnail("https://www.beybladetr.com/img/BeybladeLogolar/BeyIcon.png");
			else if (bb.getS().getType().equals("TIGER")) eb.setThumbnail("https://i.imgur.com/fLZPBP8.png");
			else if (bb.getS().getType().equals("DRAGON")) eb.setThumbnail("https://i.imgur.com/g2L0cfy.png");
			else if (bb.getS().getType().equals("BEAR")) eb.setThumbnail("https://i.imgur.com/MG789l8.png");
			eb.addField("Velocidade:", Float.toString(bb.getSpeed()), true);
			eb.addField("Força:", Float.toString(bb.getStrength()), true);
			eb.addField("Estabilidade:", Float.toString(bb.getStability()), true);
			eb.addField("Especial:", bb.getS() != null ? "(" + bb.getS().getType() + ") " + bb.getS().getName() : "Não obtido", true);
			eb.addField("Vitórias/Derrotas:", bb.getWins() + "/" + bb.getLoses(), true);
			eb.addField(":diamond_shape_with_a_dot_inside: Pontos de combate:", bb.getPoints() + " pontos", true);

			m.delete().queue();
			channel.sendMessage(eb.build()).queue();
		});
	}
}
