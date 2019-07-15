/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.client.managers.EmoteManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PollCommand extends Command {

	public PollCommand() {
		super("enquete", new String[]{"poll"}, "<pergunta>", "Inicia uma enquete no canal atual ou no configurado pelos moderadores", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | Você precisa digitar uma pergunta para a enquete.").queue();
			return;
		} else if (String.join(" ", args).length() < 10) {
			channel.sendMessage(":x: | Pergunta muito curta, tente complementá-la mais!").queue();
			return;
		} else if (String.join(" ", args).length() > 2000) {
			channel.sendMessage(":x: | Pergunta muito longa, tente simplificá-la mais!").queue();
			return;
		}

		guildConfig gc = SQLite.getGuildById(guild.getId());

		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(":notepad_spiral: Enquete criada por " + member.getEffectiveName());
		eb.setThumbnail("https://www.kalkoken.org/apps/easypoll/resources/poll-logo.png");
		eb.setDescription(String.join(" ", args));
		eb.setFooter("Clique nas reações abaixo para votar", null);
		eb.setColor(Color.decode("#2195f2"));

		if (gc.getCanalSUG() == null || gc.getCanalSUG().isEmpty()) {
			SQLite.updateGuildCanalSUG("", gc);
			channel.sendMessage(eb.build()).queue(m -> {
				m.addReaction("\uD83D\uDC4D").queue();
				m.addReaction("\uD83D\uDC4E").queue();
				String msgID = m.getId();
				final Runnable awaitPollEnd = () -> {
					final int[] pos = {0};
					final int[] neg = {0};
					channel.getMessageById(msgID).queue(msg -> {
						pos[0] = (int) msg.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals("\uD83D\uDC4D")).count() - 1;
						neg[0] = (int) msg.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals("\uD83D\uDC4E")).count() - 1;
					});
					boolean NOVOTE = false;

					if (pos[0] == 0 && neg[0] == 0) {
						pos[0] = 1;
						neg[0] = 1;
						NOVOTE = true;
					}

					eb.setAuthor("A enquete feita por " + member.getEffectiveName() + " foi encerrada!");
					eb.setTitle("Enquete: ("+ (NOVOTE ? "nenhum voto" : (pos[0] + neg[0]) + " votos") +")");
					eb.addField("Aprovação: ", NOVOTE ? "0.0%" : Helper.round((((float) pos[0] * 100f) / ((float) pos[0] + (float) neg[0])), 1) + "%", true);
					eb.addField("Reprovação: ", NOVOTE ? "0.0%" : Helper.round((((float) neg[0] * 100f) / ((float) pos[0] + (float) neg[0])), 1) + "%", true);

					m.editMessage(eb.build()).submit();
					author.openPrivateChannel().queue(c -> c.sendMessage(eb.setAuthor("Sua enquete foi encerrada!").build()).submit());
					m.clearReactions().complete();
				};
				Main.getInfo().getScheduler().schedule(awaitPollEnd, gc.getPollTime(), TimeUnit.SECONDS);
			});
		} else {
			try {

				guild.getTextChannelById(gc.getCanalSUG()).sendMessage(eb.build()).queue(m -> {
					m.addReaction("\uD83D\uDC4D").queue();
					m.addReaction("\uD83D\uDC4E").queue();
					String msgID = m.getId();
					final Runnable awaitPollEnd = () -> {
						final int[] pos = {(int) m.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals("\uD83D\uDC4D")).count() - 1};
						final int[] neg = {(int) m.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals("\uD83D\uDC4E")).count() - 1};
						channel.getMessageById(msgID).queue(msg -> {
							pos[0] = (int) msg.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals("\uD83D\uDC4D")).count() - 1;
							neg[0] = (int) msg.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals("\uD83D\uDC4E")).count() - 1;
						});
						boolean NOVOTE = false;

						if (pos[0] == 0 && neg[0] == 0) {
							pos[0] = 1;
							neg[0] = 1;
							NOVOTE = true;
						}

						eb.setAuthor("A enquete feita por " + member.getEffectiveName() + " foi encerrada!");
						eb.setTitle("Enquete: ("+ (NOVOTE ? "nenhum voto" : (pos[0] + neg[0]) + " votos") +")");
						eb.addField("Aprovação: ", NOVOTE ? "0.0%" : Helper.round((((float) pos[0] * 100f) / ((float) pos[0] + (float) neg[0])), 1) + "%", true);
						eb.addField("Reprovação: ", NOVOTE ? "0.0%" : Helper.round((((float) neg[0] * 100f) / ((float) pos[0] + (float) neg[0])), 1) + "%", true);

						m.editMessage(eb.build()).submit();
						author.openPrivateChannel().queue(c -> c.sendMessage(eb.setAuthor("Sua enquete foi encerrada!").build()).submit());
						m.clearReactions().complete();
					};
					Main.getInfo().getScheduler().schedule(awaitPollEnd, gc.getPollTime(), TimeUnit.SECONDS);
				});
			} catch (Exception e) {
				SQLite.updateGuildCanalSUG("", gc);
				channel.sendMessage(eb.build()).queue(m -> {
					m.addReaction("\uD83D\uDC4D").queue();
					m.addReaction("\uD83D\uDC4E").queue();
					String msgID = m.getId();
					final Runnable awaitPollEnd = () -> {
						final int[] pos = {(int) m.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals("\uD83D\uDC4D")).count() - 1};
						final int[] neg = {(int) m.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals("\uD83D\uDC4E")).count() - 1};
						channel.getMessageById(msgID).queue(msg -> {
							pos[0] = (int) msg.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals("\uD83D\uDC4D")).count() - 1;
							neg[0] = (int) msg.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals("\uD83D\uDC4E")).count() - 1;
						});
						boolean NOVOTE = false;
						System.out.println(pos[0] + " - " + neg[0]);

						if (pos[0] == 0 && neg[0] == 0) {
							pos[0] = 1;
							neg[0] = 1;
							NOVOTE = true;
						}

						eb.setAuthor("A enquete feita por " + member.getEffectiveName() + " foi encerrada!");
						eb.setTitle("Enquete: ("+ (NOVOTE ? "nenhum voto" : (pos[0] + neg[0]) + " votos") +")");
						eb.addField("Aprovação: ", NOVOTE ? "0.0%" : Helper.round((((float) pos[0] * 100f) / ((float) pos[0] + (float) neg[0])), 1) + "%", true);
						eb.addField("Reprovação: ", NOVOTE ? "0.0%" : Helper.round((((float) neg[0] * 100f) / ((float) pos[0] + (float) neg[0])), 1) + "%", true);

						m.editMessage(eb.build()).submit();
						author.openPrivateChannel().queue(c -> c.sendMessage(eb.setAuthor("Sua enquete foi encerrada!").build()).submit());
						m.clearReactions().complete();
					};
					Main.getInfo().getScheduler().schedule(awaitPollEnd, gc.getPollTime(), TimeUnit.SECONDS);
				});
			}
		}

		channel.sendMessage("Enquete criada com sucesso, ela encerrará automaticamente em " + gc.getPollTime() + " segundos.").queue();
	}
}
