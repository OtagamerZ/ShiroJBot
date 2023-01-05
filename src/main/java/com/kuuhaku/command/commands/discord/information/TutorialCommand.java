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

package com.kuuhaku.command.commands.discord.information;

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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Command(
		name = "tutorial",
		aliases = {"start", "begin", "comecar"},
		category = Category.INFO
)
public class TutorialCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (true) {
			channel.sendMessage("❌ | Comando desabilitado.").queue();
			return;
		}

		Account acc = AccountDAO.getAccount(author.getId());
		if (acc.hasCompletedTutorial()) {
			channel.sendMessage("❌ | Você já completou o tutorial.").queue();
			return;
		}

		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		AtomicInteger stage = new AtomicInteger(acc.getTutorialStage());
		AtomicBoolean finished = new AtomicBoolean();
		Runnable r = () -> {
			Main.getInfo().getConfirmationPending().remove(author.getId());

			acc.setTutorialStage(stage.get());
			AccountDAO.saveAccount(acc);
			Main.getInfo().getIgnore().remove(author.getId());
		};
		try {
			AtomicReference<CompletableFuture<Boolean>> next = new AtomicReference<>();
			Message msg;

			if (stage.get() < 1) {
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(firstStep()).complete();
				Pages.buttonize(
						msg,
						Map.of(Helper.parseEmoji("▶️"), wrapper -> next.get().complete(true)),
						ShiroInfo.USE_BUTTONS, true, 5, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						s -> {
							next.get().complete(false);
							if (!finished.get()) {
								finished.set(true);
								r.run();
							}
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
				stage.getAndIncrement();
			}

			if (stage.get() < 2) {
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(secondStep(prefix)).complete();
				Helper.awaitMessage(author,
						channel,
						m -> {
							if (m.getContentRaw().equalsIgnoreCase(prefix + "atm")) {
								next.get().complete(true);
								return true;
							} else return false;
						},
						5, TimeUnit.MINUTES, () -> {
							{
								next.get().complete(false);
								if (!finished.get()) {
									finished.set(true);
									r.run();
								}
							}
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
				stage.getAndIncrement();
			}

			if (stage.get() < 3) {
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(thirdStep()).complete();
				Pages.buttonize(
						msg,
						Map.of(Helper.parseEmoji("▶️"), wrapper -> next.get().complete(true)),
						ShiroInfo.USE_BUTTONS, true, 5, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						s -> {
							next.get().complete(false);
							if (!finished.get()) {
								finished.set(true);
								r.run();
							}
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
				stage.getAndIncrement();
			}

			if (stage.get() < 4) {
				Main.getInfo().getIgnore().add(author.getId());
				KawaiponCard kc = new KawaiponCard(Objects.requireNonNull(CardDAO.getCard("QUEEN")), false);
				EmbedBuilder eb = new EmbedBuilder()
						.setAuthor("Uma carta " + kc.getCard().getRarity().toString().toUpperCase(Locale.ROOT) + " Kawaipon apareceu neste servidor!")
						.setTitle(kc.getName() + " (" + kc.getCard().getAnime().toString() + ")")
						.setColor(Color.orange)
						.setFooter("Digite `" + prefix + "coletar` para adquirir esta carta (necessário: " + Helper.separate(kc.getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE) + " CR).", null)
						.setImage("attachment://kawaipon.png");

				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(fourthStep(prefix), eb.build())
						.addFile(Helper.writeAndGet(kc.getCard().drawCard(false), "kp_" + kc.getCard().getId(), "png"), "kawaipon.png")
						.complete();
				Helper.awaitMessage(author,
						channel,
						m -> {
							if (m.getContentRaw().equalsIgnoreCase(prefix + "coletar")) {
								Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
								kp.getCards().add(kc);
								KawaiponDAO.saveKawaipon(kp);

								AccountDAO.saveAccount(acc);

								channel.sendMessage("✅ | " + author.getAsMention() + " adquiriu a carta `" + kc.getName() + "` com sucesso!").queue();
								Executors.newSingleThreadScheduledExecutor().schedule(() -> next.get().complete(true), 1, TimeUnit.SECONDS);
								return true;
							} else return false;
						},
						5, TimeUnit.MINUTES, () -> {
							next.get().complete(false);
							if (!finished.get()) {
								finished.set(true);
								r.run();
							}
						}
				);

				if (!next.get().get()) return;
				Main.getInfo().getIgnore().remove(author.getId());
				msg.delete().queue(null, Helper::doNothing);
				stage.getAndIncrement();
			}

			if (stage.get() < 5) {
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(fifthStep(prefix)).complete();
				Helper.awaitMessage(author,
						channel,
						m -> {
							if (m.getContentRaw().equalsIgnoreCase(prefix + "kps no_game_no_life")) {
								next.get().complete(true);
								return true;
							} else return false;
						},
						5, TimeUnit.MINUTES, () -> {
							next.get().complete(false);
							if (!finished.get()) {
								finished.set(true);
								r.run();
							}
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
				stage.getAndIncrement();
			}

			if (stage.get() < 6) {
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(sixthStep(prefix)).complete();
				Pages.buttonize(
						msg,
						Map.of(Helper.parseEmoji("▶️"), wrapper -> next.get().complete(true)),
						ShiroInfo.USE_BUTTONS, true, 5, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						s -> {
							next.get().complete(false);
							if (!finished.get()) {
								finished.set(true);
								r.run();
							}
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
				stage.getAndIncrement();
			}

			if (stage.get() < 7) {
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(seventhStep(prefix)).complete();
				Helper.awaitMessage(author,
						channel,
						m -> {
							if (m.getContentRaw().equalsIgnoreCase(prefix + "kps elegiveis")) {
								next.get().complete(true);
								return true;
							} else return false;
						},
						5, TimeUnit.MINUTES, () -> {
							next.get().complete(false);
							if (!finished.get()) {
								finished.set(true);
								r.run();
							}
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
				stage.getAndIncrement();
			}

			if (stage.get() < 8) {
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(eightStep(prefix)).complete();
				Helper.awaitMessage(author,
						channel,
						m -> {
							if (m.getContentRaw().equalsIgnoreCase(prefix + "kps evogear")) {
								next.get().complete(true);
								return true;
							} else return false;
						},
						5, TimeUnit.MINUTES, () -> {
							next.get().complete(false);
							if (!finished.get()) {
								finished.set(true);
								r.run();
							}
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
				stage.getAndIncrement();
			}

			if (stage.get() < 9) {
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(ninethStep(prefix)).complete();
				Helper.awaitMessage(author,
						channel,
						m -> {
							if (m.getContentRaw().equalsIgnoreCase(prefix + "kps campo")) {
								next.get().complete(true);
								return true;
							} else return false;
						},
						5, TimeUnit.MINUTES, () -> {
							next.get().complete(false);
							if (!finished.get()) {
								finished.set(true);
								r.run();
							}
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
				stage.getAndIncrement();
			}

			if (stage.get() < 10) {
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(tenthStep(prefix)).complete();
				Pages.buttonize(
						msg,
						Map.of(Helper.parseEmoji("▶️"), wrapper -> next.get().complete(true)),
						ShiroInfo.USE_BUTTONS, true, 5, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						s -> {
							next.get().complete(false);
							if (!finished.get()) {
								finished.set(true);
								r.run();
							}
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
				stage.getAndIncrement();
			}

			if (stage.get() < 11) {
				next.set(new CompletableFuture<>());
				msg = channel.sendMessageEmbeds(finalStep(prefix)).complete();
				Pages.buttonize(
						msg,
						Map.of(Helper.parseEmoji("✅"), wrapper -> next.get().complete(true)),
						ShiroInfo.USE_BUTTONS, true, 5, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						s -> {
							next.get().complete(false);
							if (!finished.get()) {
								finished.set(true);
								r.run();
							}
						}
				);

				if (!next.get().get()) return;
				msg.delete().queue(null, Helper::doNothing);
				stage.getAndIncrement();

				Main.getInfo().getConfirmationPending().remove(author.getId());

				acc.addSCredit(25000, this.getClass());
				acc.completeTutorial();
				acc.setTutorialStage(stage.get());
				AccountDAO.saveAccount(acc);
				channel.sendMessage(author.getAsMention() + " recebeu **25.000** CR de iniciante!").queue();
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
						Ok, vamos começar falando sobre os **CR**.

						CR é a principal moeda usada em meus módulos, eles são necessários para várias coisas como coletar cartas, apostar em jogos, comprar melhorias, comprar itens na loja, etc.
						Existem muitas formas de conseguir CR, as mais populares são:
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

						Com este comando você pode ver quantos CR você possui, além dos CR voláteis, gemas acumuladas e a data do seu último voto.
						- **CR voláteis:** São iguais aos CR normais, mas só podem ser usados para transações comigo além de perder 25% a cada hora.
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
						Para coletá-las você precisa de CR (conforme escrito abaixo da imagem), mas como estamos em um tutorial esta carta sairá de graça.

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

	private MessageEmbed sixthStep(String prefix) {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						Nossa, faltam muitas ainda não? Mas não se preocupe, com o tempo você irá completar todas as coleções, pode confiar!

						Existem 2 tipos de cartas:
						- **Normais:** Cartas que podem ser convertidas para o Shoukan, além de serem usadas como material para sintetizar equipamentos evogear.
						- **Cromadas:** Cartas que possuem uma paleta alternativa, podendo também serem usadas como material para sintetizar campos.

						Se estiver com dificuldades para ver a imagem ou quiser ver a lista em formato de texto, basta usar o comando `%srestante` e o nome do anime, igual você fez antes.

						Para continuar, clique em ▶️.
						""".formatted(prefix))
				.build();
	}

	private MessageEmbed seventhStep(String prefix) {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						Sobre o Shoukan, recomendo ler [este guia](https://github.com/OtagamerZ/ShiroJBot/wiki/Shoukan---O-duelo-entre-invocadores) quando tiver um tempo, ele foi escrito por meu Nii-chan e contém informações muito úteis sobre como jogar!
						Você sabia? Jogadores que leem o guia têm 99%% de chance de não passar vergonha na primeira partida!

						Para poder jogar, você vai precisar montar um deck:
						- Para adicionar cartas, use `%sconverter` e o nome da carta (apenas as que você possui na coleção).
						- Para remover cartas, use `%sreverter` e o nome da carta (apenas se você já não possuir uma na coleção).
						OBS: Para ver seu deck use o comando `%sdeck`.

						Aliás, a carta que você acabou de coletar (a Queen) é elegível, então você pode adicioná-la em seu deck.

						Para continuar, digite `%skps elegiveis`.
						""".formatted(prefix, prefix, prefix, prefix))
				.build();
	}

	private MessageEmbed eightStep(String prefix) {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						**Senshi**

						A sua linha de frente, campeões que irão lutar até o fim para lhe trazer a vitória.
						Cada um possui seus próprios atributos, efeitos, raças e classes, permitindo uma ampla customização do seu deck.

						Certos campeões também possuem fusões - versões muito mais poderosas que podem alterar o rumo de uma partida.

						Se quiser (e eu recomendo) ler mais sobre as raças e classes, basta usar este mesmo comando mas informando a raça/classe que deseja ver.
						Adicionalmente, usar várias cartas de uma mesma raça garantem efeitos únicos e poderosos durante o jogo, então planeje bem sua estratégia!

						Para continuar, digite `%skps evogear`.
						""".formatted(prefix))
				.build();
	}

	private MessageEmbed ninethStep(String prefix) {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						**Evogear**

						Equipamentos e magias para adicionar um "tempero" às suas cartas.
						Eles são obtidos através de sínteses ou drops e são essenciais em qualquer deck, seja ele de um guerreiro ou de um mago.

						Você possui 24 espaços para adicionar evogears, mas eles consomem espaços de acordo com o tier:
						- **Tier 1:** Evogears geralmente baratos e de atributos inferiores, mas consomem apenas 1 espaço do deck permitindo usar uma grande quantidade deles.
						- **Tier 2:** Evogears de atributos medianos e efeitos variados, digamos que "um meio termo" entre os tiers 1 e 3.
						- **Tier 3:** Evogears poderosos e efeitos capazes de virar o jogo ou garantir a vitoria. Apesar de fortes deve-se usá-los com moderação pois ocupam 3 espaços do deck.
						- **Tier 4 (mítico):** Evogears extremamente impactantes que criam oportunidades de vitória instantânea se usados corretamente. Um deck pode possuir apenas 1 evogear mítico e são banidos logo após o uso, então a escolha deve levar em consideração o seu deck como um todo.

						Para obter um evogear você precisa sintetizar 3 cartas normais usando o comando `%ssintetizar`, aumentando a chance de conseguir tiers maiores dependendo da raridade das cartas usadas.

						Para continuar, digite `%skps campo`.
						""".formatted(prefix, prefix))
				.build();
	}

	private MessageEmbed tenthStep(String prefix) {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						**Campo**

						Campos são modificadores globais que afetam cartas de determinadas raças, tanto positivamente quanto negativamente.
						Geralmente são usados campos que sinergizam com seu deck, mas também podem ser usados como medida anti-deck contra o oponente.

						A raça da carta pode ser descoberta olhando no canto esquerdo superior, logo antes do nome.
						Para obter um campo você precisa sintetizar 3 cartas cromadas usando o comando `%ssintetizar`.

						Para continuar, clique em ▶️.
						""".formatted(prefix))
				.build();
	}

	private MessageEmbed finalStep(String prefix) {
		return new ColorlessEmbedBuilder()
				.setDescription("""
						Isso é tudo, acha que está pronto para começar sua jornada?
						Para aprofundar ainda mais nas mecânicas do Shoukan recomendo encontrar um jogador disposto a fazer um contrato de aprendiz, ele dará à ambos uma recompensa após completá-lo.
						Para fazer um, use o comando `%stutor` e mencione o usuário disposto a treinar você para tornar-se um mestre invocador!

						**Mas espere, é perigoso ir sozinho!**
						Aqui, como recompensa por completar o tutorial e para lhe ajudar a começar sua jornada, vou te dar **25.000** CR de iniciante.
						Eles são iguais aos CR voláteis mas não expiram com o tempo.
						Também vou te dar um deck muuuuito especial que permite que você teste os campeões sem precisar coletar, dê uma olhadinha nos seus `%sdecks`!

						Boa sorte jogador, esperarei seu sucesso nos campos de invocação!

						Para terminar, clique em ✅.
						""".formatted(prefix, prefix))
				.setImage("https://c.tenor.com/kDAUCWniovoAAAAC/no-game-no-life-thumbs-up.gif")
				.build();
	}
}
