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

package com.kuuhaku.command.commands.exceed;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.model.persistent.ExceedMember;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.TagIcons;
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
			if (!ExceedDAO.hasExceed(author.getId())) {
				if (args.length == 0) {
					channel.sendMessage("Exceed é um sistema global de clãs, onde todo mês o clã vencedor ira receber experiência em dobro por uma semana. A pontuação é dada pela soma da experiência de todos os membros do clã, **independente do servidor**.\n\n" +
							"Os exceeds disponíveis são:" +
							"\n" + TagIcons.getExceed(ExceedEnums.IMANITY) + "**" + ExceedEnums.IMANITY.getName() + "** - Os engenhosos humanos." +
							"\n" + TagIcons.getExceed(ExceedEnums.SEIREN) + "**" + ExceedEnums.SEIREN.getName() + "** - As curiosas sereias." +
							"\n" + TagIcons.getExceed(ExceedEnums.WEREBEAST) + "**" + ExceedEnums.WEREBEAST.getName() + "** - Os sábios bestiais." +
							"\n" + TagIcons.getExceed(ExceedEnums.ELF) + "**" + ExceedEnums.ELF.getName() + "** - Os místicos elfos." +
							"\n" + TagIcons.getExceed(ExceedEnums.EXMACHINA) + "**" + ExceedEnums.EXMACHINA.getName() + "** - Os poderosos androides." +
							"\n" + TagIcons.getExceed(ExceedEnums.FLUGEL) + "**" + ExceedEnums.FLUGEL.getName() + "** - Os divinos anjos." +
							"\n\nEscolha usando `" + prefix + "exselect EXCEED`.\n__**ESTA ESCOLHA É PERMANENTE**__").queue();
					m.delete().queue();
					return;
				}

				switch (args[0].toLowerCase()) {
					case "imanity":
						if (ExceedDAO.getPercentage(ExceedEnums.IMANITY) >= 0.5f) {
							m.editMessage(":x: | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						ExceedDAO.joinExceed(new ExceedMember(author.getId(), ExceedEnums.IMANITY.getName()));
						break;
					case "seiren":
						if (ExceedDAO.getPercentage(ExceedEnums.SEIREN) >= 0.5f) {
							m.editMessage(":x: | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						ExceedDAO.joinExceed(new ExceedMember(author.getId(), ExceedEnums.SEIREN.getName()));
						break;
					case "werebeast":
						if (ExceedDAO.getPercentage(ExceedEnums.WEREBEAST) >= 0.5f) {
							m.editMessage(":x: | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						ExceedDAO.joinExceed(new ExceedMember(author.getId(), ExceedEnums.WEREBEAST.getName()));
						break;
					case "elf":
						if (ExceedDAO.getPercentage(ExceedEnums.ELF) >= 0.5f) {
							m.editMessage(":x: | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						ExceedDAO.joinExceed(new ExceedMember(author.getId(), ExceedEnums.ELF.getName()));
						break;
					case "ex-machina":
						if (ExceedDAO.getPercentage(ExceedEnums.EXMACHINA) >= 0.5f) {
							m.editMessage(":x: | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						ExceedDAO.joinExceed(new ExceedMember(author.getId(), ExceedEnums.EXMACHINA.getName()));
						break;
					case "flügel":
						if (ExceedDAO.getPercentage(ExceedEnums.FLUGEL) >= 0.5f) {
							m.editMessage(":x: | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
							return;
						}
						ExceedDAO.joinExceed(new ExceedMember(author.getId(), ExceedEnums.FLUGEL.getName()));
						break;
					default:
						m.editMessage(":x: | Exceed inexistente.").queue();
						return;
				}

				String ex = ExceedDAO.getExceed(author.getId());

				m.editMessage("Exceed escolhido com sucesso, você agora pertence à **" + ex + "**.").queue();
				ExceedDAO.getExceedMembers(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId()))).stream().map(ExceedMember::getId).forEach(em ->
						Main.getInfo().getUserByID(em).openPrivateChannel().queue(c -> {
							try {
								c.sendMessage(author.getAsTag() + " juntou-se à " + ex + ", dê-o(a) as boas-vindas!").queue(null, Helper::doNothing);
							} catch (Exception ignore) {
							}
						}, Helper::doNothing));
				m.delete().queue();
			} else {
				m.editMessage(":x: | Você já pertence à um exceed, não é possível trocá-lo.").queue();
			}
		});
	}
}
