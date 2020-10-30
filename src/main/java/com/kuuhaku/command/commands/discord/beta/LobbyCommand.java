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

package com.kuuhaku.command.commands.discord.beta;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.LobbyDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Lobby;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LobbyCommand extends Command {

	public LobbyCommand(@NonNls String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public LobbyCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public LobbyCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public LobbyCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		com.kuuhaku.model.persistent.Member mb = MemberDAO.getMemberByMid(author.getId()).stream().max(Comparator.comparingInt(com.kuuhaku.model.persistent.Member::getLevel)).orElseThrow();
		List<Lobby> lobbies = LobbyDAO.getLobbies();

		if (args.length == 0) {
			if (lobbies.size() == 0) {
				channel.sendMessage("❌ | Não há nenhuma sala criada.").queue();
				return;
			}

			List<Page> pages = new ArrayList<>();
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":crossed_flags: | Salas de jogos")
					.setFooter("Digite `" + prefix + "lobby entrar ID SENHA` para entrar em uma sala.");

			List<List<Lobby>> chunks = Helper.chunkify(lobbies, 10);
			chunks.forEach(c -> {
				for (Lobby l : c) {
					eb.addField(
							"`" + l.getId() + "` | " + l.getName(),
							"""
									Jogadores: %d/%d
									Senha: %s
									""".formatted(l.getPlayers().size(), l.getMaxPlayers(), l.hasPassword() ? "Sim" : "Não"),
							false
					);
				}

				pages.add(new Page(PageType.EMBED, eb.build()));
				eb.clearFields();
			});

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
			);
		}

		if (Helper.equalsAny(args[0], "entrar", "join")) {
			if (!StringUtils.isNumeric(args[1])) {
				channel.sendMessage("❌ | O ID da sala deve ser um valor numérico.").queue();
				return;
			}

			String pass = String.join(" ", args)
					.replaceFirst("(?i)(entrar|join) ([0-9]+) ", "");
			Lobby lb = LobbyDAO.getLobby(Integer.parseInt(args[0]));
			if (lb.hasPassword() && !lb.checkPassword(pass)) {
				channel.sendMessage("❌ | Senha incorreta.").queue();
				return;
			}

			Guild g = Main.getInfo().getGuildByID(ShiroInfo.getLobbyServerID());
			g.getVoiceChannels().get(0)
					.createInvite()
					.setMaxUses(1)
					.setMaxAge(1L, TimeUnit.MINUTES)
					.queue(inv -> channel.sendMessage("Convite para a sala: " + inv.getUrl()).queue());
		} else if (Helper.equalsAny(args[0], "criar", "create")) {
			if (mb.getLevel() < 30) {
				channel.sendMessage("❌ | Você precisa ser level 30 ou maior para poder criar uma sala de jogos.").queue();
				return;
			} else if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
				channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
				return;
			}

			String[] params = String.join(" ", args)
					.replaceFirst("(?i)(criar|create) ", "")
					.split(";");

			if (params.length < 2) {
				channel.sendMessage("❌ | Você precisa informar um nome para a sala e o máximo de jogadores.").queue();
				return;
			} else if (LobbyDAO.getLobby(author.getId()) != null) {
				channel.sendMessage("❌ | Você já possui uma sala ativa.").queue();
				return;
			} else if (params[0].length() > 32) {
				channel.sendMessage("❌ | Nome muito grande, por favor digite um nome com 32 caractéres ou menos.").queue();
				return;
			} else if (!StringUtils.isNumeric(params[1])) {
				channel.sendMessage("❌ | O limite de jogadores deve ser um valor inteiro.").queue();
				return;
			}

			int maxPlayers;

			try {
				maxPlayers = Integer.parseInt(params[1]);
			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | O limite de jogadores deve ser um valor inteiro.").queue();
				return;
			}

			if (Helper.between(maxPlayers, 2, 9)) {
				channel.sendMessage("❌ | O limite de jogadores deve ser maior que 2 e menor ou igual a 8.").queue();
				return;
			}

			Lobby lb = new Lobby()
					.setOwner(author.getId())
					.setName(params[0])
					.setMaxPlayers(maxPlayers);

			if (params.length == 3) {
				lb.setPassword(params[2]);
			}

			String hash = Helper.generateHash(guild, author);
			ShiroInfo.getHashes().add(hash);
			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("""
					Será criada uma sala de jogos com as seguintes configurações:
					**Nome:** %s
					**Jogadores:** %s
					**Senha:** %s
									
					Deseja confirmar?
					""".formatted(lb.getName(), lb.getMaxPlayers(), lb.hasPassword() ? params[2] : "Nenhuma")
			).queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (member1, message1) -> {
						LobbyDAO.saveLobby(lb);

						Guild g = Main.getInfo().getGuildByID(ShiroInfo.getLobbyServerID());
						g.getVoiceChannels().get(0)
								.createInvite()
								.setMaxUses(1)
								.setMaxAge(1L, TimeUnit.MINUTES)
								.queue(inv -> {
									s.delete().queue(null, Helper::doNothing);
									channel.sendMessage("Sala criada com sucesso: " + inv.getUrl()).queue();
								});
					}), true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()), ms -> {
						ShiroInfo.getHashes().remove(hash);
						Main.getInfo().getConfirmationPending().invalidate(author.getId());
					})
			);
		}
	}
}
