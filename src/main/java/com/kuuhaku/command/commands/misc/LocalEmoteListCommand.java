package com.kuuhaku.command.commands.misc;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LocalEmoteListCommand extends Command {

	public LocalEmoteListCommand() {
		super("emotes", "<nome> <página>", "Mostra a lista de emotes disponíveis no servidor em que o comando foi executado.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(":x: | Você precisa definir uma página de emotes.").queue();
		} else if (!StringUtils.isNumeric(args[0])) {
			if (args.length == 1) {
				channel.sendMessage(":x: | Você precisa definir uma página de emotes logo após o nome.").queue();
				return;
			}
			try {
				List<MessageEmbed.Field> f = new ArrayList<>();
				int page = Integer.parseInt(args[1]);
				EmbedBuilder eb = new EmbedBuilder();

				eb.setTitle("<a:SmugDance:598842924725305344> Emotes disponíveis neste servidor:");
				eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));

				guild.getEmotesByName(args[0], true).forEach(e -> f.add(new MessageEmbed.Field("Emote " + e.getAsMention(), "Menção: " + e.getAsMention().replace("<", "`{").replace(">", "}`").replace(":", "&"), false)));
				List<MessageEmbed.Field> subF = f.subList(-10 + (10 * page), 10 * page > f.size() ? f.size() - 1 : 10 * page);
				subF.forEach(eb::addField);
				eb.setAuthor("Para usar estes emotes, utilize o comando \"" + SQLite.getGuildPrefix(guild.getId()) + "say MENÇÃO\"");
				eb.setFooter("Página " + page + ". Mostrando " + (-10 + 10 * page) + " - " + (10 * page > f.size() ? f.size() - 1 : 10 * page) + " resultados.", null);

				channel.sendMessage(eb.build()).queue();
			} catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
				channel.sendMessage(":x: | Página inválida, no total existem `" + guild.getEmotesByName(args[0], true).size() / 10 + "` páginas de emotes.").queue();
			}
		} else {
			try {
				List<MessageEmbed.Field> f = new ArrayList<>();
				int page = Integer.parseInt(args[0]);
				EmbedBuilder eb = new EmbedBuilder();

				eb.setTitle("<a:SmugDance:598842924725305344> Emotes disponíveis neste servidor:");
				eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));

				guild.getEmotes().forEach(e -> f.add(new MessageEmbed.Field("Emote " + e.getAsMention(), "Menção: " + e.getAsMention().replace("<", "`{").replace(">", "}`").replace(":", "&"), false)));
				List<MessageEmbed.Field> subF = f.subList(-10 + (10 * page), 10 * page > f.size() ? f.size() - 1 : 10 * page);
				subF.forEach(eb::addField);
				eb.setAuthor("Para usar estes emotes, utilize o comando \"" + SQLite.getGuildPrefix(guild.getId()) + "say MENÇÃO\"");
				eb.setFooter("Página " + page + ". Mostrando " + (-10 + 10 * page) + " - " + (10 * page > f.size() ? f.size() - 1 : 10 * page) + " resultados.", null);

				channel.sendMessage(eb.build()).queue();
			} catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
				channel.sendMessage(":x: | Página inválida, no total existem `" + guild.getEmotes().size() / 10 + "` páginas de emotes.").queue();
			}
		}
	}
}
