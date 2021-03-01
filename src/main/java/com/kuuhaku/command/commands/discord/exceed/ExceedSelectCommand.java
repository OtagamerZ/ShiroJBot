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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.TagIcons;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.ExceedMember;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

@Command(
		name = "exceedselect",
		aliases = {"exselect", "souex"},
		category = Category.EXCEED
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_EXT_EMOJI
})
public class ExceedSelectCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:loading:697879726630502401> Analisando dados...").queue(m -> {
			ExceedMember em = ExceedDAO.getExceedMember(author.getId());
			if (em == null || em.getExceed().isBlank()) {
				if (em != null && em.isBlocked()) {
					m.editMessage("❌ | Você não pode entrar em nenhum Exceed até o mês que vem.").queue();
					return;
				} else if (args.length == 0) {
					String text = """
							Exceed é um sistema global de clãs, onde todo mês o clã vencedor irá receber experiência em dobro por uma semana. A pontuação é baseada em diversos fatores, incluindo vitorias em minigames e experiência acumulada nos perfis dos membros, **independente do servidor**.
							Os exceeds disponíveis são:
							%s | **%s** - Os engenhosos humanos.
							%s | **%s** - As curiosas sereias.
							%s | **%s** - Os sábios bestiais.
							%s | **%s** - Os místicos elfos.
							%s | **%s** - Os poderosos androides.
							%s | **%s** - Os divinos anjos.
														
							Escolha usando `%sexselect EXCEED`.
							"""
							.formatted(
									TagIcons.getExceed(ExceedEnum.IMANITY), ExceedEnum.IMANITY.getName(),
									TagIcons.getExceed(ExceedEnum.SEIREN), ExceedEnum.SEIREN.getName(),
									TagIcons.getExceed(ExceedEnum.WEREBEAST), ExceedEnum.WEREBEAST.getName(),
									TagIcons.getExceed(ExceedEnum.ELF), ExceedEnum.ELF.getName(),
									TagIcons.getExceed(ExceedEnum.EXMACHINA), ExceedEnum.EXMACHINA.getName(),
									TagIcons.getExceed(ExceedEnum.FLUGEL), ExceedEnum.FLUGEL.getName(),
									prefix
							);

					channel.sendMessage(text).queue();
					m.delete().queue();
					return;
				}

				ExceedEnum ex = ExceedEnum.getByName(args[0]);

				if (ex == null) {
					m.editMessage("❌ | Exceed inexistente.").queue();
					return;
				} else if (ExceedDAO.getPercentage(ex) >= 0.5f) {
					m.editMessage("❌ | Este Exceed já possui muitos membros, por favor escolha outro.").queue();
					return;
				}

				if (em == null)
					ExceedDAO.saveExceedMember(new ExceedMember(author.getId(), ex.getName()));
				else {
					em.setExceed(ex.getName());
					ExceedDAO.saveExceedMember(em);
				}

				String e = ExceedDAO.getExceed(author.getId());

				m.editMessage("✅ | Exceed escolhido com sucesso, você agora pertence à **" + e + "**!").queue(null, Helper::doNothing);
				for (ExceedMember exceedMember : ExceedDAO.getExceedMembers(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())))) {
					String exm = exceedMember.getUid();
					User u = Main.getInfo().getUserByID(exm);
					if (u != null) {
						Account acc = AccountDAO.getAccount(u.getId());
						if (acc.isReceivingNotifs()) u.openPrivateChannel().queue(c -> {
							try {
								c.sendMessage("""
										%s juntou-se à %s, hooray!! :tada:
										**(Não responda esta mensagem)**
										Digite `silenciar` para parar de receber notificações de Exceed (não pode ser desfeito).
										""".formatted(author.getAsTag(), ex.getName())).queue(null, Helper::doNothing);
							} catch (Exception ignore) {
							}
						}, Helper::doNothing);
					}
				}
			} else {
				m.editMessage("❌ | Você já pertence à um exceed, não é possível trocá-lo.").queue();
			}
		});
	}
}
