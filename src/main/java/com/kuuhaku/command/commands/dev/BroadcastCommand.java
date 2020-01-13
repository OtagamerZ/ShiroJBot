package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.GuildDAO;
import com.kuuhaku.controller.mysql.TagDAO;
import com.kuuhaku.method.Pages;
import com.kuuhaku.model.GuildConfig;
import com.kuuhaku.model.Page;
import com.kuuhaku.model.Tags;
import com.kuuhaku.type.PageType;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class BroadcastCommand extends Command {

	public BroadcastCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public BroadcastCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public BroadcastCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public BroadcastCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | É necessário informar um tipo de broadcast (geral/parceiros).").queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage(":x: | É necessário informar uma mensagem para enviar.").queue();
			return;
		}

		String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
		Map<String, Boolean> result = new HashMap<>();
		StringBuilder sb = new StringBuilder();
		List<Page> pages = new ArrayList<>();
		EmbedBuilder eb = new EmbedBuilder();

		switch (args[0].toLowerCase()) {
			case "geral":
				List<GuildConfig> gcs = GuildDAO.getAllGuilds();
				List<List<GuildConfig>> gcPages = Helper.chunkify(gcs, 10);

				for (List<GuildConfig> gs : gcPages) {
					result.clear();
					eb.clear();
					sb.setLength(0);

					for (GuildConfig gc : gs) {
						try {
							Objects.requireNonNull(Main.getInfo().getGuildByID(gc.getGuildID()).getTextChannelById(gc.getCanalLog())).sendMessage(msg).complete();
							result.put(gc.getName(), true);
						} catch (Exception e) {
							result.put(gc.getName(), false);
						}
					}

					showResult(result, sb, pages, eb);
				}

				channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(Main.getInfo().getAPI(), s, pages, 60, TimeUnit.SECONDS));
				break;
			case "parceiros":
				List<Tags> ps = TagDAO.getAllPartners();
				List<List<Tags>> psPages = Helper.chunkify(ps, 10);

				for (List<Tags> p : psPages) {
					result.clear();
					eb.clear();
					sb.setLength(0);

					for (Tags t : p) {
						User u = Helper.getOr(Main.getInfo().getUserByID(t.getId()), null);

						if (u == null) {
							result.put("Desconhecido (" + t.getId() + ")", false);
						} else {
							try {
								u.openPrivateChannel().complete().sendMessage(msg).complete();
								result.put(u.getAsTag(), true);
							} catch (ErrorResponseException e) {
								result.put(u.getAsTag(), false);
							}
						}
					}

					showResult(result, sb, pages, eb);
				}

				channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(Main.getInfo().getAPI(), s, pages, 60, TimeUnit.SECONDS));
				break;
			default:
				channel.sendMessage(":x: | Tipo desconhecido, os tipos válidos são **geral** ou **parceiros**").queue();
		}
	}

	private void showResult(Map<String, Boolean> result, StringBuilder sb, List<Page> pages, EmbedBuilder eb) {
		sb.append("```diff\n");
		result.forEach((key, value) -> sb.append(value ? "+ " : "- ").append(key).append("\n"));
		sb.append("```");

		eb.setTitle("__**STATUS**__ ");
		eb.setDescription(sb.toString());
		pages.add(new Page(PageType.EMBED, eb.build()));
	}
}
