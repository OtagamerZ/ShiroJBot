/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.events;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.BlacklistDAO;
import com.kuuhaku.controller.postgresql.RelayDAO;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.controller.sqlite.CustomAnswerDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.BotExchange;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.PrivilegeLevel;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ShiroEvents extends ListenerAdapter {
	private final Map<String, CopyOnWriteArrayList<SimpleMessageListener>> toHandle = new ConcurrentHashMap<>();

	@Override
	public void onGuildUpdateName(GuildUpdateNameEvent event) {
		GuildConfig gc = GuildDAO.getGuildById(event.getGuild().getId());
		gc.setName(event.getNewName());
		GuildDAO.updateGuildSettings(gc);
	}

	@Override
	public void onGuildUpdateOwner(@Nonnull GuildUpdateOwnerEvent event) {
		assert event.getOldOwner() != null;
		assert event.getNewOwner() != null;

		if (TagDAO.getTagById(event.getOldOwner().getId()).isBeta()) {
			TagDAO.removeTagBeta(event.getOldOwner().getId());

			try {
				TagDAO.getTagById(event.getNewOwner().getId());
			} catch (NoResultException e) {
				TagDAO.addUserTagsToDB(event.getNewOwner().getId());
			} finally {
				TagDAO.giveTagBeta(event.getNewOwner().getId());
			}
		}
	}

	@Override
	public void onGuildMessageUpdate(@NotNull GuildMessageUpdateEvent event) {
		if (event.getAuthor().isBot()) return;
		Message msg = Main.getInfo().retrieveCachedMessage(event.getGuild(), event.getMessageId());
		onGuildMessageReceived(new GuildMessageReceivedEvent(event.getJDA(), event.getResponseNumber(), event.getMessage()));

		if (msg != null)
			Helper.logToChannel(event.getAuthor(), false, null, "Uma mensagem foi editada no canal " + event.getChannel().getAsMention() + ":```diff\n- " + msg.getContentRaw() + "\n+ " + event.getMessage().getContentRaw() + "```", msg.getGuild());
	}

	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
		try {
			User author = event.getAuthor();
			Member member = event.getMember();
			Message message = event.getMessage();
			MessageChannel channel = message.getChannel();
			Guild guild = message.getGuild();
			String rawMessage = StringUtils.normalizeSpace(message.getContentRaw());

			String prefix = "";
			if (!Main.getInfo().isDev()) {
				try {
					prefix = GuildDAO.getGuildById(guild.getId()).getPrefix().toLowerCase();
				} catch (NoResultException | NullPointerException ignore) {
				}
			} else prefix = Main.getInfo().getDefaultPrefix().toLowerCase();

			if (rawMessage.startsWith(";") && ShiroInfo.getDevelopers().contains(author.getId()) && rawMessage.length() > 1) {
				try {
					if (rawMessage.replace(";", "").length() == 0) {
						channel.sendFile(message.getAttachments().get(0).downloadToFile().get()).queue();
					} else {
						MessageAction send = channel.sendMessage(Helper.makeEmoteFromMention(rawMessage.substring(1).split(" ")));
						for (Message.Attachment a : message.getAttachments()) {
							try {
								//noinspection ResultOfMethodCallIgnored
								send.addFile(a.downloadToFile().get());
							} catch (InterruptedException | ExecutionException ignore) {
							}
						}
						send.queue();
						message.delete().queue();
					}
				} catch (InsufficientPermissionException | ExecutionException | InterruptedException ignore) {
				}
				return;
			}

			if (author.isBot() && !Main.getSelfUser().getId().equals(author.getId())) {
				handleExchange(author, message);
				return;
			} else if (member == null) return;

			boolean blacklisted = BlacklistDAO.isBlacklisted(author);

			if (!blacklisted) try {
				MemberDAO.getMemberById(author.getId() + guild.getId());
			} catch (NoResultException e) {
				MemberDAO.addMemberToDB(member);
			}

			/*try {
				MutedMember mm = com.kuuhaku.controller.postgresql.MemberDAO.getMutedMemberById(author.getId());
				if (mm != null && mm.isMuted()) {
					message.delete().complete();
					return;
				}
			} catch (InsufficientPermissionException | ErrorResponseException ignore) {
			}*/

			GuildConfig gc = GuildDAO.getGuildById(guild.getId());
			if (gc.getNoSpamChannels().contains(channel.getId()) && Main.getSelfUser().getId().equals(author.getId())) {
				if (message.getReactions().size() >= gc.getNoSpamAmount()) {
					message.delete()
							.flatMap(s -> channel.sendMessage(":warning: | Opa, sem spam meu amigo!"))
							.queue(msg -> {
								msg.delete().queueAfter(20, TimeUnit.SECONDS, null, Helper::doNothing);
								Helper.logToChannel(member.getUser(), false, null, "Um membro estava spammando no canal " + ((TextChannel) channel).getAsMention(), guild, msg.getContentRaw());
							}, Helper::doNothing);
				} else if (gc.isHardAntispam()) {
					channel.getHistory().retrievePast(20).queue(h -> {
						h.removeIf(m -> ChronoUnit.MILLIS.between(m.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) > 5000 || m.getAuthor() != author);

						countSpam(member, channel, guild, h);
					});
				} else {
					channel.getHistory().retrievePast(20).queue(h -> {
						h.removeIf(m -> ChronoUnit.MILLIS.between(m.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) > 5000 || m.getAuthor() != author && StringUtils.containsIgnoreCase(m.getContentRaw(), rawMessage));

						countSpam(member, channel, guild, h);
					});
				}
			}

			if (Helper.isPureMention(rawMessage) && Helper.isPinging(message, Main.getSelfUser().getId())) {
				channel.sendMessage("Quer saber como pode usar meus comandos? Digite `" + prefix + "ajuda` para ver todos eles ordenados por categoria!").queue(null, Helper::doNothing);
				return;
			}

			if (!author.isBot()) Main.getInfo().cache(guild, message);

			String rawMsgNoPrefix = rawMessage;
			String commandName = "";
			if (rawMessage.toLowerCase().startsWith(prefix)) {
				rawMsgNoPrefix = rawMessage.substring(prefix.length()).trim();
				commandName = rawMsgNoPrefix.split(" ")[0].trim();
			}

			try {
				CustomAnswer ca = CustomAnswerDAO.getCAByTrigger(rawMessage, guild.getId());
				if (ca != null && !ca.isMarkForDelete() && !Main.getSelfUser().getId().equals(author.getId()))
					Helper.typeMessage(channel, Objects.requireNonNull(ca).getAnswer().replace("%user%", author.getAsMention()).replace("%guild%", guild.getName()).replace("%count%", String.valueOf(guild.getMemberCount())));
			} catch (NoResultException | NullPointerException ignore) {
			}

			String[] args = rawMsgNoPrefix.split(" ");
			String argsAsText = rawMsgNoPrefix.replaceFirst(commandName, "").trim();
			boolean hasArgs = (args.length > 1);
			if (hasArgs) {
				args = Arrays.copyOfRange(args, 1, args.length);
				args = ArrayUtils.removeAllOccurences(args, "");
			} else {
				args = new String[0];
			}

			boolean found = false;
			if (!guild.getSelfMember().hasPermission(Permission.MESSAGE_WRITE)) {
				return;
			}

			Command command = Main.getCommandManager().getCommand(commandName);
			if (command != null) {
				found = command.getCategory().isEnabled(gc, guild, author);

				if (found) {
					if (Helper.showMMError(author, channel, guild, rawMessage, command)) return;

					if (command.getCategory() == Category.NSFW && !((TextChannel) channel).isNSFW()) {
						try {
							channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_nsfw-in-non-nsfw-channel")).queue();
						} catch (InsufficientPermissionException ignore) {
						}
						return;
					} else if (!Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
						try {
							channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_not-enough-permission")).queue();
						} catch (InsufficientPermissionException ignore) {
						}
						return;
					} else if (blacklisted) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-blacklisted")).queue();
						return;
					} else if (Main.getInfo().getRatelimit().getIfPresent(author.getId()) != null) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-ratelimited")).queue();
						Main.getInfo().getRatelimit().put(author.getId(), true);
						return;
					}

					if (!TagDAO.getTagById(author.getId()).isBeta() && !Helper.hasPermission(member, PrivilegeLevel.SUPPORT))
						Main.getInfo().getRatelimit().put(author.getId(), true);

					command.execute(author, member, commandName, argsAsText, args, message, channel, guild, prefix);
					Helper.spawnAd(channel);
				}
			}

			if (!found && !author.isBot() && !blacklisted) {
				if (toHandle.containsKey(guild.getId())) {
					List<SimpleMessageListener> evts = getHandler().get(guild.getId());
					for (SimpleMessageListener evt : evts) {
						evt.onGuildMessageReceived(event);
					}
					evts.removeIf(SimpleMessageListener::isClosed);
				}

				Account acc = AccountDAO.getAccount(author.getId());
				if (!acc.getTwitchId().isBlank() && channel.getId().equals(ShiroInfo.getTwitchChannelID()) && Main.getInfo().isLive()) {
					Main.getTwitch().getChat().sendMessage("kuuhaku_otgmz", author.getName() + " disse: " + Helper.stripEmotesAndMentions(rawMessage));
				}

				if (!ShiroInfo.getStaff().contains(author.getId()) && Helper.isPinging(message, ShiroInfo.getNiiChan())) {
					channel.sendMessage("✅ | Você comprou um \"Pingue o Sora por **1.000 de dívida**\" com sucesso!").queue();
					acc.addLoan(1000);
				}

				if (!TagDAO.getTagById(guild.getOwnerId()).isToxic()) {
					if (gc.isKawaiponEnabled()) Helper.spawnKawaipon(gc, (TextChannel) channel);
					if (gc.isDropEnabled()) Helper.spawnDrop(gc, (TextChannel) channel);

					Calendar c = Calendar.getInstance();
					if (c.get(Calendar.MONTH) == Calendar.DECEMBER && gc.isDropEnabled())
						Helper.spawnPadoru(gc, (TextChannel) channel);
				}

				MessageChannel lvlChannel = null;
				try {
					lvlChannel = guild.getTextChannelById(gc.getCanalLvl());
				} catch (Exception ignore) {
				}
				try {
					Map<String, Object> rawLvls = gc.getCargoslvl().entrySet()
							.stream()
							.filter(e -> {
								com.kuuhaku.model.persistent.Member mb = MemberDAO.getMemberById(author.getId() + guild.getId());
								return mb != null && mb.getLevel() >= Integer.parseInt(e.getKey());
							})
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
					Map<Integer, Role> sortedLvls = new TreeMap<>();
					for (Map.Entry<String, Object> entry : rawLvls.entrySet()) {
						sortedLvls.put(
								Integer.parseInt(entry.getKey()),
								guild.getRoleById((String) entry.getValue())
						);
					}
					MessageChannel finalLvlChannel = lvlChannel;
					sortedLvls.keySet().stream().max(Integer::compare).ifPresent(i -> {
						if (gc.isLvlNotif() && !member.getRoles().contains(sortedLvls.get(i))) {
							try {
								if (!guild.getSelfMember().getRoles().get(0).canInteract(sortedLvls.get(i))) return;
								guild.addRoleToMember(member, sortedLvls.get(i)).queue(s -> {
									String content = author.getAsMention() + " ganhou o cargo **`" + sortedLvls.get(i).getName() + "`**! :tada:";
									if (finalLvlChannel != null) {
										finalLvlChannel.getHistory().retrievePast(5).queue(m -> {
											if (m.stream().noneMatch(c -> c.getContentRaw().equals(content))) {
												finalLvlChannel.sendMessage(content).queue(null, Helper::doNothing);
											}
										}, Helper::doNothing);
									} else {
										channel.getHistory().retrievePast(5).queue(m -> {
											if (m.stream().noneMatch(c -> c.getContentRaw().equals(content))) {
												channel.sendMessage(content).queue(null, Helper::doNothing);
											}
										}, Helper::doNothing);
									}
								}, Helper::doNothing);
							} catch (IllegalArgumentException e) {
								Map<String, Object> cl = gc.getCargoslvl();
								cl.remove(String.valueOf(i));
								GuildDAO.updateGuildSettings(gc);
							} catch (ErrorResponseException ignore) {
							}
						}
						rawLvls.remove(String.valueOf(i));
						List<Role> list = new ArrayList<>();
						for (Map.Entry<String, Object> entry : rawLvls.entrySet()) {
							String k = entry.getKey();
							Object v = entry.getValue();
							Role r = guild.getRoleById((String) v);

							if (r != null) list.add(r);
							else {
								Map<String, Object> cl = gc.getCargoslvl();
								cl.remove(String.valueOf(i));
								GuildDAO.updateGuildSettings(gc);
							}
						}
						guild.modifyMemberRoles(member, null, list).queue(null, Helper::doNothing);
					});
				} catch (HierarchyException | InsufficientPermissionException ignore) {
				}

				try {
					if (gc.getNoLinkChannels().contains(channel.getId()) && Helper.findURL(rawMessage)) {
						message.delete().reason("Mensagem possui um URL").queue();
						channel.sendMessage(member.getAsMention() + ", é proibido postar links neste canal!").queue();
						Helper.logToChannel(author, false, null, "Detectei um link no canal " + ((TextChannel) channel).getAsMention(), guild, rawMessage);
					}

					com.kuuhaku.model.persistent.Member m = MemberDAO.getMemberById(member.getId() + member.getGuild().getId());
					if (m == null) {
						MemberDAO.addMemberToDB(member);
						m = MemberDAO.getMemberById(member.getId() + member.getGuild().getId());
					} else if (m.getMid() == null) {
						m.setMid(author.getId());
						m.setSid(guild.getId());
					}

					boolean lvlUp = m.addXp(guild);
					try {
						if (lvlUp && gc.isLvlNotif()) {
							Objects.requireNonNullElse(lvlChannel, channel).sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GG WP! :tada:").queue();
						}
					} catch (InsufficientPermissionException e) {
						channel.sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GG WP! :tada:").queue();
					}

					MemberDAO.updateMemberConfigs(m);
				} catch (ErrorResponseException | NullPointerException e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}

				if (gc.isNQNMode() && Helper.hasEmote(rawMessage))
					try {
						com.kuuhaku.model.persistent.Member m = MemberDAO.getMemberById(author.getId() + guild.getId());

						Webhook wh = Helper.getOrCreateWebhook((TextChannel) channel, "Shiro", Main.getShiroShards());
						Map<String, Runnable> s = Helper.sendEmotifiedString(guild, rawMessage);

						WebhookMessageBuilder wmb = new WebhookMessageBuilder();
						wmb.setContent(String.valueOf(s.keySet().toArray()[0]));
						if (m.getPseudoAvatar() == null || m.getPseudoAvatar().isBlank())
							wmb.setAvatarUrl(author.getAvatarUrl());
						else try {
							wmb.setAvatarUrl(m.getPseudoAvatar());
						} catch (RuntimeException e) {
							m.setPseudoAvatar("");
							MemberDAO.updateMemberConfigs(m);
						}
						if (m.getPseudoName() == null || m.getPseudoName().isBlank()) wmb.setUsername(author.getName());
						else try {
							wmb.setUsername(m.getPseudoName());
						} catch (RuntimeException e) {
							m.setPseudoName("");
							MemberDAO.updateMemberConfigs(m);
						}

						assert wh != null;
						WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();
						message.delete().queue(d -> {
							try {
								wc.send(wmb.build()).thenAccept(rm -> s.get(String.valueOf(s.keySet().toArray()[0])).run()).get();
							} catch (InterruptedException | ExecutionException e) {
								Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
							}
						}, Helper::doNothing);
					} catch (IndexOutOfBoundsException | InsufficientPermissionException | ErrorResponseException | NullPointerException | InterruptedException | ExecutionException ignore) {
					}
			}
		} catch (InsufficientPermissionException | ErrorResponseException ignore) {
		}
	}

	@Override
	public void onRoleDelete(@Nonnull RoleDeleteEvent event) {
		GuildConfig gc = GuildDAO.getGuildById(event.getGuild().getId());
		Map<String, Object> jo = gc.getCargoslvl();

		jo.entrySet().removeIf(e -> String.valueOf(e.getValue()).equals(event.getRole().getId()));

		GuildDAO.updateGuildSettings(gc);
	}

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		try {
			Helper.logger(this.getClass()).info("Estou pronta!");
		} catch (Exception e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar bot: " + e);
		}
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		GuildDAO.addGuildToDB(event.getGuild());
		try {
			Helper.sendPM(Objects.requireNonNull(event.getGuild().getOwner()).getUser(), "Obrigada por me adicionar ao seu servidor, utilize `s!ajuda` em um dos canais do servidor para ver meus comandos!\n\nDúvidas? Pergunte-me diretamente e um de meus desenvolvedores responderá assim que possível!");
		} catch (Exception err) {
			TextChannel dch = event.getGuild().getDefaultChannel();
			if (dch != null) {
				if (dch.canTalk()) {
					dch.sendMessage("Obrigada por me adicionar ao seu servidor, utilize `s!ajuda` para ver meus comandos!\n\nDúvidas? Pergunte-me diretamente e um de meus desenvolvedores responderá assim que possível!").queue();
				}
			}
		}

		for (String d : ShiroInfo.getDevelopers()) {
			Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
				String msg = "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".";
				c.sendMessage(msg).queue();
			});
		}
		Helper.logger(this.getClass()).info("Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		for (String d : ShiroInfo.getDevelopers()) {
			Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
				GuildConfig gc = GuildDAO.getGuildById(event.getGuild().getId());
				gc.setMarkForDelete(true);
				GuildDAO.updateGuildSettings(gc);
				String msg = "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".";
				c.sendMessage(msg).queue();
			});
		}
		Helper.logger(this.getClass()).info("Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		Guild guild = event.getGuild();
		Member member = event.getMember();
		User author = event.getUser();
		try {
			if (BlacklistDAO.isBlacklisted(author)) return;
			GuildConfig gc = GuildDAO.getGuildById(guild.getId());

			if (gc.isAntiRaid() && ChronoUnit.MINUTES.between(author.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) < 10) {
				Helper.logToChannel(author, false, null, "Um usuário foi expulso automaticamente por ter uma conta muito recente.\n`(data de criação: " + author.getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm:ss")) + "h)`", guild);
				guild.kick(member).queue();
				return;
			}

			MemberDAO.addMemberToDB(member);

			if (!gc.getMsgBoasVindas().equals("")) {
				URL url = new URL(Objects.requireNonNull(author.getAvatarUrl()));
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				BufferedImage image = ImageIO.read(con.getInputStream());

				JSONObject template = gc.getEmbedTemplate();
				EmbedBuilder eb;
				if (!template.isEmpty()) {
					if (template.has("color")) eb = new EmbedBuilder();
					else eb = new ColorlessEmbedBuilder();

					eb.setTitle(
							switch (Helper.rng(5, true)) {
								case 0 -> "Opa, parece que temos um novo membro?";
								case 1 -> "Mais um membro para nosso lindo servidor!";
								case 2 -> "Um novo jogador entrou na partida, pressione start 2P!";
								case 3 -> "Agora podemos iniciar a teamfight, um novo membro veio nos ajudar!";
								case 4 -> "Bem-vindo ao nosso servidor, puxe uma cadeira e fique à vontade!";
								default -> "";
							}
					);

					if (template.has("color")) eb.setColor(Color.decode(template.getString("color")));
					if (template.has("thumbnail")) eb.setThumbnail(template.getString("thumbnail"));
					if (template.has("image")) eb.setImage(template.getString("image"));

					eb.setDescription(Helper.replaceTags(gc.getMsgBoasVindas(), author, guild));

					if (template.has("fields")) {
						for (Object j : template.getJSONArray("fields")) {
							try {
								JSONObject jo = (JSONObject) j;
								eb.addField(jo.getString("name"), jo.getString("value"), true);
							} catch (Exception ignore) {
							}
						}
					}

					if (template.has("footer")) eb.setFooter(template.getString("footer"), null);
				} else {
					eb = new EmbedBuilder()
							.setTitle(
									switch (Helper.rng(5, true)) {
										case 0 -> "Opa, parece que temos um novo membro?";
										case 1 -> "Mais um membro para nosso lindo servidor!";
										case 2 -> "Um novo jogador entrou na partida, pressione start 2P!";
										case 3 -> "Agora podemos iniciar a teamfight, um novo membro veio nos ajudar!";
										case 4 -> "Bem-vindo ao nosso servidor, puxe uma cadeira e fique à vontade!";
										default -> "";
									}
							)
							.setAuthor(author.getAsTag(), author.getAvatarUrl(), author.getAvatarUrl())
							.setColor(Helper.colorThief(image))
							.setDescription(Helper.replaceTags(gc.getMsgBoasVindas(), author, guild))
							.setThumbnail(author.getAvatarUrl())
							.setFooter("ID do usuário: " + author.getId(), guild.getIconUrl());
				}

				Objects.requireNonNull(guild.getTextChannelById(gc.getCanalBV())).sendMessage(author.getAsMention()).embed(eb.build()).queue();
				Helper.logToChannel(author, false, null, "Um usuário entrou no servidor", guild);
			} else if (gc.isAntiRaid() && ChronoUnit.MINUTES.between(author.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) < 10) {
				Helper.logToChannel(author, false, null, "Um usuário foi bloqueado de entrar no servidor", guild);
				guild.kick(member).queue();
			}
		} catch (Exception ignore) {
		}
	}

	@Override
	public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event) {
		Guild guild = event.getGuild();
		User author = event.getUser();
		try {
			GuildConfig gc = GuildDAO.getGuildById(guild.getId());

			/*com.kuuhaku.model.persistent.Member m = MemberDAO.getMemberById(event.getMember().getId() + event.getGuild().getId());
			m.setMarkForDelete(true);
			MemberDAO.updateMemberConfigs(m);*/

			if (!gc.getMsgAdeus().equals("")) {
				URL url = new URL(Objects.requireNonNull(author.getAvatarUrl()));
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				BufferedImage image = ImageIO.read(con.getInputStream());

				JSONObject template = gc.getEmbedTemplate();
				EmbedBuilder eb;
				if (!template.isEmpty()) {
					if (template.has("color")) eb = new EmbedBuilder();
					else eb = new ColorlessEmbedBuilder();

					eb.setTitle(
							switch (Helper.rng(5, true)) {
								case 0 -> "Nãããoo...um membro deixou este servidor!";
								case 1 -> "O quê? Temos um membro a menos neste servidor!";
								case 2 -> "Alguém saiu do servidor, deve ter acabado a pilha, só pode!";
								case 3 -> "Bem, alguém não está mais neste servidor, que pena!";
								case 4 -> "Saíram do servidor bem no meio de uma teamfight, da pra acreditar?";
								default -> "";
							}
					);

					if (template.has("color")) eb.setColor(Color.decode(template.getString("color")));
					if (template.has("thumbnail")) eb.setThumbnail(template.getString("thumbnail"));
					if (template.has("image")) eb.setImage(template.getString("image"));

					eb.setDescription(Helper.replaceTags(gc.getMsgAdeus(), author, guild));

					if (template.has("fields")) {
						for (Object j : template.getJSONArray("fields")) {
							try {
								JSONObject jo = (JSONObject) j;
								eb.addField(jo.getString("name"), jo.getString("value"), true);
							} catch (Exception ignore) {
							}
						}
					}

					if (template.has("footer")) eb.setFooter(template.getString("footer"), null);
				} else {
					eb = new EmbedBuilder()
							.setTitle(
									switch (Helper.rng(5, true)) {
										case 0 -> "Nãããoo...um membro deixou este servidor!";
										case 1 -> "O quê? Temos um membro a menos neste servidor!";
										case 2 -> "Alguém saiu do servidor, deve ter acabado a pilha, só pode!";
										case 3 -> "Bem, alguém não está mais neste servidor, que pena!";
										case 4 -> "Saíram do servidor bem no meio de uma teamfight, da pra acreditar?";
										default -> "";
									}
							)
							.setAuthor(author.getAsTag(), author.getAvatarUrl(), author.getAvatarUrl())
							.setColor(Helper.colorThief(image))
							.setDescription(Helper.replaceTags(gc.getMsgAdeus(), author, guild))
							.setThumbnail(author.getAvatarUrl())
							.setFooter("ID do usuário: " + author.getId(), guild.getIconUrl());
				}

				Objects.requireNonNull(guild.getTextChannelById(gc.getCanalAdeus())).sendMessage(eb.build()).queue();
				Helper.logToChannel(author, false, null, "Um usuário saiu do servidor", guild);
			}
		} catch (Exception ignore) {
		}
	}

	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		List<String> staffIds = ShiroInfo.getStaff();
		if (staffIds.contains(event.getAuthor().getId())) {
			String msg = event.getMessage().getContentRaw();
			String[] args = msg.split(" ");

			try {
				switch (args[0].toLowerCase()) {
					case "send", "s" -> {
						if (args.length < 2) return;
						String msgNoArgs = msg.replaceFirst(args[0] + " " + args[1], "").trim();

						try {
							User u = Main.getInfo().getUserByID(args[1]);
							if (u == null) {
								event.getChannel().sendMessage("❌ | Não existe nenhum usuário com esse ID.").queue();
								return;
							} else if (msgNoArgs.length() == 0) {
								event.getChannel().sendMessage("❌ | Você não pode enviar uma mensagem vazia.").queue();
								return;
							}

							u.openPrivateChannel().queue(c ->
									c.sendMessage(event.getAuthor().getName() + " respondeu:\n>>> " + msgNoArgs).queue());
							for (String d : staffIds) {
								if (!d.equals(event.getAuthor().getId())) {
									Main.getInfo().getUserByID(d).openPrivateChannel()
											.flatMap(c -> c.sendMessage(event.getAuthor().getName() + " respondeu o usuário " + u.getName() + ":\n>>> " + msgNoArgs))
											.queue();
								}
							}
							event.getChannel().sendMessage("✅ | Mensagem enviada com sucesso!").queue();
						} catch (NumberFormatException e) {
							event.getChannel().sendMessage("❌ | ID inválido.").queue();
						}
					}
					case "block", "b" -> {
						if (args.length < 2) return;
						String msgNoArgs = msg.replaceFirst(args[0] + " " + args[1], "").trim();

						try {
							User us = Main.getInfo().getUserByID(args[1]);
							if (us == null) {
								event.getChannel().sendMessage("❌ | Não existe nenhum usuário com esse ID.").queue();
								return;
							} else if (msgNoArgs.length() == 0) {
								event.getChannel().sendMessage("❌ | Você precisa especificar uma razão.").queue();
								return;
							}

							EmbedBuilder eb = new EmbedBuilder()
									.setTitle("Você foi bloqueado dos canais de comunicação da Shiro")
									.setDescription("Razão: ```" + msgNoArgs + "```")
									.setColor(Color.red)
									.setThumbnail("https://cdn.icon-icons.com/icons2/1380/PNG/512/vcsconflicting_93497.png")
									.setTimestamp(Instant.now());

							RelayDAO.permaBlock(new PermaBlock(args[1]));
							us.openPrivateChannel().queue(c ->
									c.sendMessage(eb.build()).queue());
							for (String d : staffIds) {
								if (!d.equals(event.getAuthor().getId())) {
									Main.getInfo().getUserByID(d).openPrivateChannel()
											.flatMap(c -> c.sendMessage(event.getAuthor().getName() + " bloqueou o usuário " + Main.getInfo().getUserByID(args[1]) + ". Razão: \n>>> " + msgNoArgs))
											.queue();
								}
							}
							event.getChannel().sendMessage("✅ | Usuário bloqueado com sucesso!").queue();
						} catch (NumberFormatException e) {
							event.getChannel().sendMessage("❌ | ID inválido.").queue();
						}
					}
					case "alert", "a" -> {
						if (args.length < 2) return;
						String msgNoArgs = msg.replaceFirst(args[0] + " " + args[1], "").trim();

						try {
							User us = Main.getInfo().getUserByID(args[1]);
							if (us == null) {
								event.getChannel().sendMessage("❌ | Não existe nenhum usuário com esse ID.").queue();
								return;
							} else if (msgNoArgs.length() == 0) {
								event.getChannel().sendMessage("❌ | Você precisa especificar uma razão.").queue();
								return;
							}

							EmbedBuilder eb = new EmbedBuilder()
									.setTitle("Você recebeu um alerta:")
									.setDescription(msgNoArgs)
									.setColor(Color.orange)
									.setThumbnail("https://canopytools.com/wp-content/uploads/2019/10/alert-icon-17.png")
									.setTimestamp(Instant.now());

							us.openPrivateChannel().queue(c ->
									c.sendMessage(eb.build()).queue());
							for (String d : staffIds) {
								if (!d.equals(event.getAuthor().getId())) {
									Main.getInfo().getUserByID(d).openPrivateChannel()
											.flatMap(c -> c.sendMessage(event.getAuthor().getName() + " alertou o usuário " + Main.getInfo().getUserByID(args[1]) + ". Razão: \n>>> " + msgNoArgs))
											.queue();
								}
							}
							event.getChannel().sendMessage("✅ | Usuário alertado com sucesso!").queue();
						} catch (NumberFormatException e) {
							event.getChannel().sendMessage("❌ | ID inválido.").queue();
						}
					}
					case "mark", "m" -> {
						EmbedBuilder eb = new EmbedBuilder()
								.setTitle("Marcador de atividade:")
								.setDescription(event.getAuthor().getName() + " marcou que está acompanhando o chat de suporte.")
								.setColor(Color.decode("#800080"))
								.setThumbnail("https://iconsplace.com/wp-content/uploads/_icons/800080/256/png/support-icon-13-256.png")
								.setFooter("Aguarde alguns minutos antes de responder as mensagens dos usuários para evitar interferir.")
								.setTimestamp(Instant.now());

						for (String d : staffIds) {
							if (!d.equals(event.getAuthor().getId())) {
								Main.getInfo().getUserByID(d).openPrivateChannel()
										.flatMap(c -> c.sendMessage(eb.build()))
										.queue();
							}
						}
						event.getChannel().sendMessage("✅ | Atividade marcada com sucesso!").queue();
					}
				}
			} catch (NullPointerException ignore) {
			}
		} else {
			try {
				if (event.getMessage().getContentRaw().equalsIgnoreCase("silenciar")) {
					Account acc = AccountDAO.getAccount(event.getAuthor().getId());
					acc.setReceiveNotifs(false);
					AccountDAO.saveAccount(acc);

					event.getChannel().sendMessage("Você não receberá mais notificações de Exceed.").queue();
					return;
				}
				event.getAuthor().openPrivateChannel().queue(c -> {
					if (RelayDAO.blockedList().contains(event.getAuthor().getId())) {
						c.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-blocked")).queue();
					} else
						c.sendMessage("Mensagem enviada no canal de suporte, aguardando resposta...")
								.queue(s -> {
									EmbedBuilder eb = new ColorlessEmbedBuilder()
											.setDescription(event.getMessage().getContentRaw())
											.setAuthor(event.getAuthor().getAsTag(), event.getAuthor().getAvatarUrl())
											.setFooter(event.getAuthor().getId())
											.setTimestamp(Instant.now());

									for (String d : staffIds) {
										Main.getInfo().getUserByID(d).openPrivateChannel()
												.flatMap(ch -> ch.sendMessage(eb.build()))
												.queue();
									}
									s.delete().queueAfter(1, TimeUnit.MINUTES);
								});
				});
			} catch (Exception ignored) {
			}
		}
	}

	public static boolean isFound(GuildConfig gc, Guild g, String commandName, boolean found, Command command, User u) {
		if (command.getName().equalsIgnoreCase(commandName)) {
			found = true;
		}
		for (String alias : command.getAliases()) {
			if (alias.equalsIgnoreCase(commandName)) {
				found = true;
				break;
			}
		}
		if (!command.getCategory().isEnabled(gc, g, u)) {
			found = false;
		}
		return found;
	}

	@Override
	public void onGuildMessageDelete(@Nonnull GuildMessageDeleteEvent event) {
		Message msg = Main.getInfo().retrieveCachedMessage(event.getGuild(), event.getMessageId());

		if (msg == null || msg.getAuthor().isBot()) return;

		Helper.logToChannel(msg.getAuthor(), false, null, "Uma mensagem foi deletada no canal " + event.getChannel().getAsMention() + ":```diff\n-" + msg.getContentRaw() + "```", msg.getGuild());
	}

	private void countSpam(Member member, MessageChannel channel, Guild guild, List<Message> h) {
		if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) && h.size() >= GuildDAO.getGuildById(guild.getId()).getNoSpamAmount() && Helper.hasRoleHigherThan(guild.getSelfMember(), member)) {
			((TextChannel) channel).deleteMessagesByIds(h.stream().map(Message::getId).collect(Collectors.toList())).queue(null, Helper::doNothing);
			channel.sendMessage(":warning: | Opa, sem spam meu amigo!").queue(msg -> {
				msg.delete().queueAfter(20, TimeUnit.SECONDS, null, Helper::doNothing);
				Helper.logToChannel(member.getUser(), false, null, "Um membro estava spammando no canal " + ((TextChannel) channel).getAsMention(), guild, msg.getContentRaw());
			});

			GuildConfig gc = GuildDAO.getGuildById(guild.getId());
			if (gc.getCargoMute() != null && !gc.getCargoMute().isBlank()) try {
				Role r = guild.getRoleById(gc.getCargoMute());
				if (r != null) {
					JSONArray roles = new JSONArray(member.getRoles().stream().filter(rl -> !rl.isManaged()).map(Role::getId).collect(Collectors.toList()));

					List<Role> rls = member.getRoles().stream().filter(Role::isManaged).collect(Collectors.toList());
					rls.add(r);

					guild.modifyMemberRoles(member, rls).queue(null, Helper::doNothing);
					MutedMember mm = Helper.getOr(com.kuuhaku.controller.postgresql.MemberDAO.getMutedMemberById(member.getId()), new MutedMember(member.getId(), guild.getId(), roles));
					mm.mute(GuildDAO.getGuildById(guild.getId()).getWarnTime());

					com.kuuhaku.controller.postgresql.MemberDAO.saveMutedMember(mm);
				}
			} catch (Exception ignore) {
			}
		}
	}

	private void handleExchange(User u, Message msg) {
		if (BotExchange.isBotAdded(u.getId()) && msg.getMentionedUsers().stream().anyMatch(usr -> usr.getId().equals(Main.getSelfUser().getId()))) {
			BotExchange be = BotExchange.getById(u.getId());

			if (be.matchTrigger(msg.getContentRaw()).find()) {
				if (be.getReactionEmote() != null) msg.addReaction(be.getReactionEmote()).queue();
			} else if (be.matchConfirmation(msg.getContentRaw()).find()) {
				String[] args = msg.getContentRaw().replaceAll(be.getConfirmation(), "").replace(",", "").split(" ");
				long value = 0;
				for (String arg : args) {
					if (StringUtils.isNumeric(arg)) {
						value = Long.parseLong(arg);
						break;
					}
				}
				if (value == 0) return;

				User target = msg.getMentionedUsers().stream().filter(usr -> !usr.getId().equals(Main.getSelfUser().getId())).findFirst().orElse(null);
				if (target == null) return;

				Account acc = AccountDAO.getAccount(target.getId());
				acc.addVCredit((long) Math.ceil(value * be.getRate()), this.getClass());
				AccountDAO.saveAccount(acc);

				msg.getChannel().sendMessage("✅ | Obrigada, seus " + value + " " + be.getCurrency() + (value != 1 ? "s" : "") + " foram convertidos em " + (long) (value * be.getRate()) + " créditos voláteis com sucesso!").queue();
			}
		}
	}

	public Map<String, CopyOnWriteArrayList<SimpleMessageListener>> getHandler() {
		return toHandle;
	}

	public void addHandler(Guild guild, SimpleMessageListener sml) {
		getHandler().compute(guild.getId(), (s, evts) -> evts == null ? new CopyOnWriteArrayList<>() : evts).add(sml);
	}
}
