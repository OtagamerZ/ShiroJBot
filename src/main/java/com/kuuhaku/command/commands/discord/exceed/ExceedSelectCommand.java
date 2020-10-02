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

package com.kuuhaku.command.commands.discord.exceed;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.TagIcons;
import com.kuuhaku.model.persistent.ExceedMember;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class ExceedSelectCommand extends Command {

	public ExceedSelectCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ExceedSelectCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ExceedSelectCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ExceedSelectCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Analisando dados...").queue(m -> {
			ExceedMember em = ExceedDAO.getExceedMember(author.getId());
			if (em == null || em.getExceed().isBlank()) {
				if (em != null && em.isBlocked()) {
					m.editMessage("❌ | Você não pode entrar em nenhum Exceed até o mês que vem.").queue();
					return;
				} else if (args.length == 0) {
					channel.sendMessage("Exceed é um sistema global de clãs, onde todo mês o clã vencedor ira receber experiência em dobro por uma semana. A pontuação é baseada em diversos fatores, incluindo vitorias em minigames e experiência acumulada nos perfis dos membros, **independente do servidor**.\n\n" +
							"Os exceeds disponíveis são:" +
							"\n" + TagIcons.getExceed(ExceedEnum.IMANITY) + "**" + ExceedEnum.IMANITY.getName() + "** - Os engenhosos humanos." +
							"\n" + TagIcons.getExceed(ExceedEnum.SEIREN) + "**" + ExceedEnum.SEIREN.getName() + "** - As curiosas sereias." +
							"\n" + TagIcons.getExceed(ExceedEnum.WEREBEAST) + "**" + ExceedEnum.WEREBEAST.getName() + "** - Os sábios bestiais." +
							"\n" + TagIcons.getExceed(ExceedEnum.ELF) + "**" + ExceedEnum.ELF.getName() + "** - Os místicos elfos." +
							"\n" + TagIcons.getExceed(ExceedEnum.EXMACHINA) + "**" + ExceedEnum.EXMACHINA.getName() + "** - Os poderosos androides." +
							"\n" + TagIcons.getExceed(ExceedEnum.FLUGEL) + "**" + ExceedEnum.FLUGEL.getName() + "** - Os divinos anjos." +
							"\n\nEscolha usando `" + prefix + "exselect EXCEED`.").queue();
					m.delete().queue();
					return;
				}

				switch (args[0].toLowerCase()) {
					case "imanity":
						if (ExceedDAO.getPercentage(ExceedEnum.IMANITY) >= 0.5f) {
							m.editMessage("❌ | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						if (em == null)
							ExceedDAO.saveExceedMember(new ExceedMember(author.getId(), ExceedEnum.IMANITY.getName()));
						else {
							em.setExceed(ExceedEnum.IMANITY.getName());
							ExceedDAO.saveExceedMember(em);
						}
						break;
					case "seiren":
						if (ExceedDAO.getPercentage(ExceedEnum.SEIREN) >= 0.5f) {
							m.editMessage("❌ | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						if (em == null)
							ExceedDAO.saveExceedMember(new ExceedMember(author.getId(), ExceedEnum.SEIREN.getName()));
						else {
							em.setExceed(ExceedEnum.SEIREN.getName());
							ExceedDAO.saveExceedMember(em);
						}
						break;
					case "werebeast":
						if (ExceedDAO.getPercentage(ExceedEnum.WEREBEAST) >= 0.5f) {
							m.editMessage("❌ | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						if (em == null)
							ExceedDAO.saveExceedMember(new ExceedMember(author.getId(), ExceedEnum.WEREBEAST.getName()));
						else {
							em.setExceed(ExceedEnum.WEREBEAST.getName());
							ExceedDAO.saveExceedMember(em);
						}
						break;
					case "elf":
						if (ExceedDAO.getPercentage(ExceedEnum.ELF) >= 0.5f) {
							m.editMessage("❌ | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						if (em == null)
							ExceedDAO.saveExceedMember(new ExceedMember(author.getId(), ExceedEnum.ELF.getName()));
						else {
							em.setExceed(ExceedEnum.ELF.getName());
							ExceedDAO.saveExceedMember(em);
						}
						break;
					case "ex-machina":
						if (ExceedDAO.getPercentage(ExceedEnum.EXMACHINA) >= 0.5f) {
							m.editMessage("❌ | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						if (em == null)
							ExceedDAO.saveExceedMember(new ExceedMember(author.getId(), ExceedEnum.EXMACHINA.getName()));
						else {
							em.setExceed(ExceedEnum.EXMACHINA.getName());
							ExceedDAO.saveExceedMember(em);
						}
						break;
					case "flügel":
						if (ExceedDAO.getPercentage(ExceedEnum.FLUGEL) >= 0.5f) {
							m.editMessage("❌ | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						if (em == null)
							ExceedDAO.saveExceedMember(new ExceedMember(author.getId(), ExceedEnum.FLUGEL.getName()));
						else {
							em.setExceed(ExceedEnum.FLUGEL.getName());
							ExceedDAO.saveExceedMember(em);
						}
						break;
					default:
						m.editMessage("❌ | Exceed inexistente.").queue();
						return;
				}

				String ex = ExceedDAO.getExceed(author.getId());

				m.editMessage("Exceed escolhido com sucesso, você agora pertence à **" + ex + "**. Entre no servidor de suporte para utilizar o Tet para conversar com outros " + ex + "s!").queue(null, Helper::doNothing);
				ExceedDAO.getExceedMembers(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId()))).stream().map(ExceedMember::getId).forEach(e -> {
							User u = Main.getInfo().getUserByID(e);
							if (u != null) {
								u.openPrivateChannel().queue(c -> {
									try {
										c.sendMessage(author.getAsTag() + " juntou-se à " + ex + ", hooray!! :tada:").queue(null, Helper::doNothing);
									} catch (Exception ignore) {
									}
								}, Helper::doNothing);
							}
						}
				);
			} else {
				m.editMessage("❌ | Você já pertence à um exceed, não é possível trocá-lo.").queue();
			}
		});
	}
}
