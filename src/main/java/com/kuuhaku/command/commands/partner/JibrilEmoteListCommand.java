package com.kuuhaku.command.commands.partner;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.method.Pages;
import com.kuuhaku.model.Page;
import com.kuuhaku.type.PageType;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class JibrilEmoteListCommand extends Command {

	public JibrilEmoteListCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public JibrilEmoteListCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public JibrilEmoteListCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public JibrilEmoteListCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		List<Page> pages = new ArrayList<>();
		List<MessageEmbed.Field> f = new ArrayList<>();

		EmbedBuilder eb = new EmbedBuilder();

		Main.getJibril().getEmotes().stream().filter(e -> StringUtils.containsIgnoreCase(e.getAsMention(), args.length > 0 ? args[0] : "")).collect(Collectors.toList()).forEach(e -> f.add(new MessageEmbed.Field("Emote " + e.getAsMention(), "Menção: " + e.getAsMention().replace("<", "`{").replace(">", "}`").replace(":", "&"), false)));

		for (int i = 0; i < Math.ceil(f.size() / 10f); i++) {
			eb.clear();
			List<MessageEmbed.Field> subF = f.subList(-10 + (10 * (i + 1)), Math.min(10 * (i + 1), f.size()));
			subF.forEach(eb::addField);

			eb.setTitle("<a:SmugDance:598842924725305344> Emotes disponíveis para a Jibril:");
			eb.setColor(Helper.getRandomColor());
			eb.setAuthor("Para usar estes emotes, simplesmente digite a menção no chat global, ela será convertida automaticamente.");
			eb.setFooter("Página " + (i + 1) + ". Mostrando " + (-10 + 10 * (i + 1)) + " - " + (Math.min(10 * (i + 1), f.size())) + " resultados.", null);

			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(Main.getInfo().getAPI(), s, pages, 60, TimeUnit.SECONDS));
	}
}
