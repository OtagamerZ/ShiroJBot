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
				int page = Integer.parseInt(args[1]);
				List<CustomAnswers> ca = SQLite.getCADump();
				EmbedBuilder eb = new EmbedBuilder();
				ca.removeIf(a -> !a.getGuildID().equals(guild.getId()));

				eb.setTitle("<a:SmugDance:598842924725305344> Emotes disponíveis:");
				eb.setColor(new Color(Helper.rng(255), Helper.rng(255), Helper.rng(255)));
				for (int i = -10 + (10 * page); i < ca.size() && i < (10 * page); i++) {
					Emote e = Main.getInfo().getAPI().getEmotes().get(i);
					eb.addField("Emote " + e.toString(), e.getAsMention() + " " + e.getAsMention() + " " + e.getAsMention(), false);
				}

				channel.sendMessage(eb.build()).queue();
			} catch (ArrayIndexOutOfBoundsException ex) {
				channel.sendMessage(":x: | Você precisa definir uma página.").queue();
			}
		}
	}
}
