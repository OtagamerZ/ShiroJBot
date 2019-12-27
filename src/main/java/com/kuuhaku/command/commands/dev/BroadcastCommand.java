package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL.TagDAO;
import com.kuuhaku.controller.SQLite.BackupDAO;
import com.kuuhaku.model.Tags;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BroadcastCommand extends Command {

	public BroadcastCommand() {
		super("broadcast", new String[]{"bc", "avisar"}, "<tipo> <mensagem>", "Envia um aviso a todos os donos de servidor que possuem a Shiro, ou a todos o parceiros.", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | É necessário informar um tipo de broadcast (geral/parceiros).").queue();
		} else if (args.length < 2) {
			channel.sendMessage(":x: | É necessário informar uma mensagem para enviar.").queue();
		}

		String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
		switch (args[0].toLowerCase()) {
			case "geral":
				List<guildConfig> gcs = BackupDAO.getGuildDump();
				for (guildConfig gc : gcs) {
					if (gc.getCanalLog().equals("") || gc.getCanalLog() == null) {
						return;
					}

					Objects.requireNonNull(Main.getInfo().getGuildByID(gc.getGuildID()).getTextChannelById(gc.getCanalLog())).sendMessage(msg).queue();
				}
				break;
			case "parceiros":
				List<Tags> ps = TagDAO.getAllTags();
				for (Tags t : ps) {
					if (t.isPartner()) {
						try {
							Main.getInfo().getUserByID(t.getId()).openPrivateChannel().queue(c -> c.sendMessage(msg).queue());
						} catch (Exception ignore) {
						}
					}
				}
				break;
			default: channel.sendMessage(":x: | Tipo desconhecido, os tipos válidos são **geral** ou **parceiros**").queue();
		}
	}
}
