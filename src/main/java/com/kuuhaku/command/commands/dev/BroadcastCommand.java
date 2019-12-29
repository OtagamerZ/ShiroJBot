package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL.GuildDAO;
import com.kuuhaku.controller.MySQL.TagDAO;
import com.kuuhaku.model.Tags;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BroadcastCommand extends Command {

	public BroadcastCommand() {
		super("broadcast", new String[]{"bc", "avisar"}, "<tipo> <mensagem>", "Envia um aviso a todos os donos de servidor que possuem a Shiro, ou a todos o parceiros.", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | É necessário informar um tipo de broadcast (geral/parceiros).").queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage(":x: | É necessário informar uma mensagem para enviar.").queue();
			return;
		}

		String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
		Map<Object, Boolean> result = new HashMap<>();
		StringBuilder sb = new StringBuilder();

		switch (args[0].toLowerCase()) {
			case "geral":
				List<guildConfig> gcs = GuildDAO.getAllGuilds();

				for (guildConfig gc : gcs) {
					try {
						//Objects.requireNonNull(Main.getInfo().getGuildByID(gc.getGuildID()).getTextChannelById(gc.getCanalLog())).sendMessage(msg).queue();
						result.put(gc.getName(), true);
					} catch (Exception e) {
						result.put(gc.getName(), false);
					}
				}

				sb.append("```diff\n");
				result.forEach((key, value) -> sb.append(value ? "+ " : "- ").append(key).append("\n"));
				sb.append("```");

				channel.sendMessage("__**STATUS**__ " + sb.toString()).queue();
				break;
			case "parceiros":
				List<Tags> ps = TagDAO.getAllPartners();

				for (Tags t : ps) {
					User u = Helper.getOr(Main.getInfo().getUserByID(t.getId()), null);

					if (u == null) {
						result.put("Desconhecido (" + t.getId() + ")", false);
					} else {
						try {
							u.openPrivateChannel().queue(c -> {
								try {
									//c.sendMessage(msg).queue();
									result.put(u, true);
								} catch (Exception e) {
									result.put(u, false);
								}
							});
						} catch (Exception e) {
							result.put(u, false);
						}
					}
					System.out.println(Arrays.toString(result.entrySet().toArray()));
				}

				sb.append("```diff\n");
				result.forEach((key, value) -> sb.append(value ? "+ " : "- ").append(((User) key).getAsTag()).append("\n"));
				sb.append("```");

				channel.sendMessage("__**STATUS**__ " + sb.toString()).queue();
				break;
			default:
				channel.sendMessage(":x: | Tipo desconhecido, os tipos válidos são **geral** ou **parceiros**").queue();
		}
	}
}
