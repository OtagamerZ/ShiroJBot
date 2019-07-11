package com.kuuhaku.command.commands.misc;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EmoteListCommand extends Command {

	public EmoteListCommand() {
		super("emotes", "<pagina/nome>", "Mostra a list de emotes disponíveis para uso através da Shiro/Jibril.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(":x: | Você precisa definir uma página de emotes.").queue();
		} else if (!StringUtils.isNumeric(args[0])) {

		} else {
			try {
				List<MessageEmbed.Field> f = new ArrayList<>();
				int page = Integer.parseInt(args[0]);
				EmbedBuilder eb = new EmbedBuilder();

				eb.setTitle("<a:SmugDance:598842924725305344> Emotes disponíveis:");
				eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));

				Main.getInfo().getAPI().getEmotes().forEach(e -> f.add(new MessageEmbed.Field("Emote " + e.getAsMention(), "Menção: \\" + e.getAsMention(), false)));
				for (int i = -10 + (10 * page); i < f.size() && i < (10 * page); i++) {
					eb.addField(f.get(i));
				}
				eb.setFooter("Página " + page + ". Mostrando " + (-10 + 10 * page) + " - " + (page * 10) + " resultados.", null);

				channel.sendMessage(eb.build()).queue();
			} catch (ArrayIndexOutOfBoundsException ex) {
				channel.sendMessage(":x: | Página inválida, no total existem `" + Main.getInfo().getAPI().getEmotes().size() + "` páginas de emotes.").queue();
			}
		}
	}
}
