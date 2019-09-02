package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LocalEmoteListCommand extends Command {

	public LocalEmoteListCommand() {
		super("emotes", "<nome> <página>", "Mostra a lista de emotes disponíveis no servidor em que o comando foi executado.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		List<MessageEmbed> pages = new ArrayList<>();
		List<MessageEmbed.Field> f = new ArrayList<>();

		EmbedBuilder eb = new EmbedBuilder();

		guild.getEmotes().stream().filter(e -> StringUtils.containsIgnoreCase(e.getAsMention(), args[0])).collect(Collectors.toList()).forEach(e -> f.add(new MessageEmbed.Field("Emote " + e.getAsMention(), "Menção: " + e.getAsMention().replace("<", "`{").replace(">", "}`").replace(":", "&"), false)));

		for (int i = 0; i < Math.ceil(f.size() / 10f); i++) {
			eb.clear();
			List<MessageEmbed.Field> subF = f.subList(-10 + (10 * (i + 1)), 10 * (i + 1) > f.size() ? f.size() : 10 * (i + 1));
			subF.forEach(eb::addField);

			eb.setTitle("<a:SmugDance:598842924725305344> Emotes disponíveis para a Jibril:");
			eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));
			eb.setAuthor("Para usar estes emotes, utilize o comando \"" + SQLite.getGuildPrefix(guild.getId()) + "say MENÇÃO\"");
			eb.setFooter("Página " + (i + 1) + ". Mostrando " + (-10 + 10 * (i + 1)) + " - " + (10 * (i + 1) > f.size() ? f.size() : 10 * (i + 1)) + " resultados.", null);

			pages.add(eb.build());
		}

		channel.sendMessage(pages.get(0)).queue(s -> Helper.paginate(s, pages));
	}
}
