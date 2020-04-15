/*
 * This file is part of Shiro J Bot.
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PollCommand extends Command {

	public PollCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public PollCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public PollCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public PollCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_poll-no-question")).queue();
			return;
		} else if (String.join(" ", args).length() < 10) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_poll-too-short")).queue();
			return;
		} else if (String.join(" ", args).length() > 2000) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_poll-too-long")).queue();
			return;
		}

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(":notepad_spiral: Enquete criada por " + member.getEffectiveName());
		eb.setThumbnail("https://www.kalkoken.org/apps/easypoll/resources/poll-logo.png");
		eb.setDescription(String.join(" ", args));
		eb.setFooter("Clique nas reações abaixo para votar", null);
		eb.setColor(Color.decode("#2195f2"));

		if (gc.getCanalSUG() == null || gc.getCanalSUG().isEmpty()) {
			gc.setCanalSUG(null);
			GuildDAO.updateGuildSettings(gc);
			channel.sendMessage(eb.build()).queue(m -> {
				m.addReaction("\uD83D\uDC4D").queue();
				m.addReaction("\uD83D\uDC4E").queue();
				m.addReaction("❌").queue();
				Main.getInfo().getPolls().put(m.getId(), new Integer[]{0, 0});
				Main.getInfo().getScheduler().schedule(() -> showResult(m, member, eb), gc.getPollTime(), TimeUnit.SECONDS);
			});
		} else {
			try {
				Objects.requireNonNull(guild.getTextChannelById(gc.getCanalSUG())).sendMessage(eb.build()).queue(m -> {
					m.addReaction("\uD83D\uDC4D").queue();
					m.addReaction("\uD83D\uDC4E").queue();
					m.addReaction("❌").queue();
					Main.getInfo().getPolls().put(m.getId(), new Integer[]{0, 0});
					Main.getInfo().getScheduler().schedule(() -> showResult(m, member, eb), gc.getPollTime(), TimeUnit.SECONDS);
				});
			} catch (Exception e) {
				if (gc.getCanalSUG() == null || gc.getCanalSUG().isEmpty())
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_send-embed")).queue();
				else
					channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_send-embed-in-channel"), Objects.requireNonNull(guild.getTextChannelById(gc.getCanalSUG())).getAsMention())).queue();
				return;
			}
		}

		channel.sendMessage("Enquete criada com sucesso, ela encerrará automaticamente em " + gc.getPollTime() + " segundos.").queue();
	}

	private static void showResult(Message msg, Member member, EmbedBuilder eb) {
		int pos = Main.getInfo().getPolls().get(msg.getId())[0];
		int neg = Main.getInfo().getPolls().get(msg.getId())[1];
		Main.getInfo().getPolls().remove(msg.getId());
		boolean NOVOTE = false;

		if (pos == 0 && neg == 0) {
			pos = 1;
			neg = 1;
			NOVOTE = true;
		}

		eb.setAuthor("A enquete feita por " + member.getEffectiveName() + " foi encerrada!");
		eb.setTitle("Enquete: (" + (NOVOTE ? "nenhum voto" : (pos + neg) + " votos") + ")");
		eb.addField("Aprovação: ", NOVOTE ? "0.0%" : Helper.round((((float) pos * 100f) / ((float) pos + (float) neg)), 1) + "%", true);
		eb.addField("Reprovação: ", NOVOTE ? "0.0%" : Helper.round((((float) neg * 100f) / ((float) pos + (float) neg)), 1) + "%", true);

		msg.editMessage(eb.build()).queue();
		member.getUser().openPrivateChannel().queue(c -> c.sendMessage(eb.setAuthor("Sua enquete foi encerrada!").build()).queue());
		msg.clearReactions().queue();
	}
}
