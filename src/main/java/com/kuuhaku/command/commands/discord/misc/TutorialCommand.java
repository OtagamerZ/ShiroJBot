/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Command(
		name = "tutorial",
		aliases = {"start", "begin", "comecar"},
		category = Category.MISC
)
public class TutorialCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (!ShiroInfo.getStaff().contains(author.getId())) return;

		Account acc = AccountDAO.getAccount(author.getId());
		/*
		if (acc.hasStarted()) {
			channel.sendMessage("❌ | Você já completou o tutorial.").queue();
		}
		 */

		Runnable r = () -> {
			channel.sendMessage("❌ | Tempo expirado, por favor use o comando novamente.").queue();
			Main.getInfo().getIgnore().remove(author.getId());
		};
		try {
			AtomicReference<CompletableFuture<Boolean>> next = new AtomicReference<>();
			Message msg;

			{
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(firstStep()).complete();
				Pages.buttonize(
						msg,
						Map.of("▶️", (mb, ms) -> next.get().complete(true)),
						true, 2, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						s -> {
							next.get().complete(false);
							r.run();
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
			}

			{
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(secondStep(prefix)).complete();
				Helper.awaitMessage(author,
						channel,
						m -> {
							if (m.getContentRaw().equals(prefix + "atm")) {
								next.get().complete(true);
								return true;
							} else return false;
						},
						2, TimeUnit.MINUTES, r
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
			}

			{
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(thirdStep()).complete();
				Pages.buttonize(
						msg,
						Map.of("▶️", (mb, ms) -> next.get().complete(true)),
						true, 2, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						s -> {
							next.get().complete(false);
							r.run();
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
			}

			{
				Main.getInfo().getIgnore().add(author.getId());
				KawaiponCard kc = new KawaiponCard(CardDAO.getCard("MIKO"), false);
				EmbedBuilder eb = new EmbedBuilder()
						.setAuthor("Uma carta " + kc.getCard().getRarity().toString().toUpperCase(Locale.ROOT) + " Kawaipon apareceu neste servidor!")
						.setTitle(kc.getName() + " (" + kc.getCard().getAnime().toString() + ")")
						.setColor(Color.orange)
						.setFooter("Digite `" + prefix + "coletar` para adquirir esta carta (necessário: " + Helper.separate(kc.getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE) + " créditos).", null)
						.setImage("attachment://kawaipon.png");

				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(fourthStep(prefix), eb.build())
						.addFile(Helper.writeAndGet(kc.getCard().drawCard(false), "kp_" + kc.getCard().getId(), "png"), "kawaipon.png")
						.complete();
				Helper.awaitMessage(author,
						channel,
						m -> {
							if (m.getContentRaw().equals(prefix + "coletar")) {
								Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
								kp.getCards().add(kc);
								KawaiponDAO.saveKawaipon(kp);

								channel.sendMessage("✅ | " + author.getAsMention() + " adquiriu a carta `" + kc.getName() + "` com sucesso!").queue();
								next.get().complete(true);
								return true;
							} else return false;
						},
						2, TimeUnit.MINUTES, r
				);

				if (!next.get().get()) return;
				Main.getInfo().getIgnore().remove(author.getId());
				msg.delete().queue(null, Helper::doNothing);
			}

			{
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(fifthStep(prefix)).complete();
				Helper.awaitMessage(author,
						channel,
						m -> {
							if (m.getContentRaw().equals(prefix + "kps no_game_no_life")) {
								next.get().complete(true);
								return true;
							} else return false;
						},
						2, TimeUnit.MINUTES, r
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
			}

			{
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(sixthStep()).complete();
				Pages.buttonize(
						msg,
						Map.of("▶️", (mb, ms) -> next.get().complete(true)),
						true, 2, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						s -> {
							next.get().complete(false);
							r.run();
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
			}
		} catch (ExecutionException | InterruptedException ignore) {
		}
	}

	private MessageEmbed firstStep() {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						**Olá, sou Shiro J. Bot, mas pode me chamar só de Shiro, prazer em conhecê-lo(a)!**
						Estou feliz que tenha decidido aprender a usar meus comandos e começar sua jornada para coletar kawaipons e disputar batalhas no campo Shoukan.

						Ops, fui muito rapida? Não se preocupe, vou te explicar com calma!
						Quando estiver pronto(a), clique em ▶️.
						""")
				.build();
	}

	private MessageEmbed secondStep(String prefix) {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						Ok, vamos começar falando sobre os **créditos** (CR).
												
						Crédito é a principal moeda usada em meus módulos, eles são necessários para várias coisas como coletar cartas, apostar em jogos, comprar melhorias, comprar itens na loja, etc.
						Existem muitas formas de conseguir créditos, as mais populares são:
						- Votando em mim
						- Completando tarefas diárias
						- Abrindo drops
						- Vendendo cartas
						- Jogando minigames
												
						Para continuar, digite `%satm`.
						""".formatted(prefix))
				.build();
	}

	private MessageEmbed thirdStep() {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						Muito bem!
												
						Com este comando você pode ver quantos créditos você possui, além dos créditos voláteis, dívida, gemas acumuladas e a data do seu último voto.
						- **Créditos voláteis:** São iguais aos créditos normais, mas só podem ser usados para transações comigo além de perder 25% a cada hora.
						- **Dívidas:** Caso você faça um empréstimo de créditos (ou seja pego abusando de algum comando) você receberá um valor de dívida, fazendo com que você não receba mais créditos até terminar de pagá-la.
						- **Gemas:** A moeda VIP para quem vota frequentemente em mim, com ela você consegue comprar melhorias exóticas na loja exclusiva.
												
						Para continuar, clique em ▶️.
						""")
				.build();
	}

	private MessageEmbed fourthStep(String prefix) {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						Olha só!
												
						Essa é uma carta kawaipon, elas aparecem conforme o movimento nos chats do servidor e podem ser coletadas para adicionar à sua coleção.
						Para coletá-las você precisa de créditos (conforme escrito abaixo da imagem), mas como estamos em um tutorial esta carta sairá de graça.
												
						Pegue-a, rápido!
						Para continuar, digite `%scoletar`.
						""".formatted(prefix))
				.build();
	}

	private MessageEmbed fifthStep(String prefix) {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						Ótimo, agora vamos ver quantas cartas faltam!
												
						Para continuar, digite `%skps no_game_no_life`.
						""".formatted(prefix))
				.build();
	}

	private MessageEmbed sixthStep() {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						Nossa, faltam muitas ainda não? Mas não se preocupe, com o tempo você irá completar todas as coleções, pode confiar!
												
						Existem 2 tipos de cartas:
						- **Normais:** Cartas que podem ser convertidas para o Shoukan, além de serem usadas como material para sintetizar equipamentos evogear.
						- **Cromadas:** Cartas que possuem uma paleta alternativa, podendo também sertem usadas como material para sintetizar campos.
												
						Se estiver com dificuldades para ver a imagem ou quiser ver a lista em formato de texto, basta usar o comando `restante` e o nome do anime, igual você fez antes.
												
						Para continuar, clique em ▶️.
						""")
				.build();
	}

	private MessageEmbed seventhStep() {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						Sobre o Shoukan, recomendo ler [este guia](https://www.reddit.com/r/ShiroJBot/comments/jkbjtd/shoukan_o_duelo_entre_invocadores/) quando tiver um tempo, ele foi escrito por meu Nii-chan e contém informações muito úteis sobre como jogar!
						Você sabia? Jogadores que leem o guia têm 99% de chance de não passar vergonha na primeira partida! 
						""")
				.build();
	}
}
