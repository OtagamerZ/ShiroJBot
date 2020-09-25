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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Phase;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Shoukan extends Game {
	private final Map<String, Hand> hands;
	private final TextChannel channel;
	private final Arena arena = new Arena();
	private final ListenerAdapter listener = new ListenerAdapter() {
		@Override
		public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
			if (canInteract(event)) play(event);
		}
	};
	private Message message = null;
	private Phase phase = Phase.PLAN;
	private List<Champion> ultimates = CardDAO.getFusions();

	public Shoukan(JDA handler, TextChannel channel, int bet, User... players) {
		super(handler, new Board(BoardSize.S_NONE, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel);
		this.channel = channel;

		Kawaipon p1 = KawaiponDAO.getKawaipon(players[0].getId());
		Kawaipon p2 = KawaiponDAO.getKawaipon(players[1].getId());

		this.hands = Map.of(
				players[0].getId(), new Hand(players[0], new ArrayList<>() {{
					addAll(p1.getChampions());
					addAll(p1.getEquipments());
				}}, Side.TOP),
				players[1].getId(), new Hand(players[1], new ArrayList<>() {{
					addAll(p2.getChampions());
					addAll(p2.getEquipments());
				}}, Side.BOTTOM)
		);

		setActions(
				s -> close(),
				s -> getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId())
		);
	}

	@Override
	public void start() {
		Map<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
		buttons.put("\uD83C\uDFF3️", (mb, ms) -> {
			channel.sendMessage(getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)").queue();
			getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
			close();
		});
		buttons.put("\uD83D\uDCE4", (mb, ms) -> {
			if (phase != Phase.PLAN) {
				channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue();
				return;
			}

			Hand h = hands.get(getCurrent().getId());

			int remaining = 5 - h.getCards().size();

			if (remaining <= 0) {
				channel.sendMessage("❌ | Você não pode puxar mais cartas se tiver 5 cartas ou mais na sua mão.").queue();
				return;
			}

			if (h.getDeque().size() == 0) {
				if (this.message != null) this.message.delete().queue();
				this.message = channel.sendMessage(getCurrent().getAsMention() + " não possui mais cartas, " + getPlayerById(getBoard().getPlayers().get(1).getId()).getAsMention() + " venceu! (" + getRound() + " turnos)")
						.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();

				getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
				close();
			}

			h.draw();
			h.showHand();
			remaining = 5 - h.getCards().size();
			if (this.message != null) this.message.delete().queue();
			this.message = channel.sendMessage(getCurrent().getAsMention() + " puxou uma carta (" + (remaining == 0 ? "não pode puxar mais cartas" : "pode puxar mais " + remaining + " carta" + (remaining == 1 ? "" : "s")) + ")")
					.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
			Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
			resetTimerKeepTurn();
		});
		buttons.put("▶️", (mb, ms) -> {
			if (getRound() < 1 || phase == Phase.ATTACK) {
				User u = getCurrent();
				resetTimer();

				phase = Phase.PLAN;
				arena.getSlots().get(hands.get(u.getId()).getSide()).forEach(s -> {
					if (s.getTop() != null)
						s.getTop().setAvailable(true);
				});
				Hand hd = hands.get(getCurrent().getId());
				hd.addMana(5);

				if (this.message != null) this.message.delete().queue();
				this.message = channel.sendMessage(u.getAsMention() + " encerrou o turno, agora é sua vez " + getCurrent().getAsMention())
						.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
				Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
				hd.showHand();
				return;
			}

			channel.sendMessage("**FASE DE ATAQUE:** Escolha uma carta do seu lado e uma carta do lado inimigo para iniciar combate").queue();
			phase = Phase.ATTACK;
			hands.get(getCurrent().getId()).getCards().removeIf(d -> !d.isAvailable());
			resetTimerKeepTurn();
		});

		Hand h = hands.get(getCurrent().getId());
		h.addMana(5);
		message = channel.sendMessage(getCurrent().getAsMention() + " você começa! (Olhe as mensagens privadas)")
				.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
		Pages.buttonize(message, buttons, false, 3, TimeUnit.MINUTES, u -> u.getId().equals(getCurrent().getId()));
		getHandler().addEventListener(listener);
		h.showHand();
	}

	@Override
	public boolean canInteract(GuildMessageReceivedEvent evt) {
		Predicate<GuildMessageReceivedEvent> condition = e -> e.getChannel().getId().equals(channel.getId());

		return condition
				.and(e -> e.getAuthor().getId().equals(getCurrent().getId()))
				.and(e -> StringUtils.isNumeric(e.getMessage().getContentRaw().split(",")[0]))
				.test(evt);
	}

	@Override
	public void play(GuildMessageReceivedEvent evt) {
		Message message = evt.getMessage();
		String cmd = message.getContentRaw();
		Hand h = hands.get(getCurrent().getId());

		Map<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
		buttons.put("\uD83C\uDFF3️", (mb, ms) -> {
			channel.sendMessage(getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)").queue();
			getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
			close();
		});
		buttons.put("\uD83D\uDCE4", (mb, ms) -> {
			if (phase != Phase.PLAN) {
				channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue();
				return;
			}

			Hand hd = hands.get(getCurrent().getId());

			int remaining = 5 - hd.getCards().size();

			if (remaining <= 0) {
				channel.sendMessage("❌ | Você não pode puxar mais cartas se tiver 5 cartas ou mais na sua mão.").queue();
				return;
			}

			if (h.getDeque().size() == 0) {
				if (this.message != null) this.message.delete().queue();
				this.message = channel.sendMessage(getCurrent().getAsMention() + " não possui mais cartas, " + getPlayerById(getBoard().getPlayers().get(1).getId()).getAsMention() + " venceu! (" + getRound() + " turnos)")
						.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();

				getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
				close();
			}

			hd.draw();
			hd.showHand();
			remaining = 5 - h.getCards().size();
			if (this.message != null) this.message.delete().queue();
			this.message = channel.sendMessage(getCurrent().getAsMention() + " puxou uma carta (" + (remaining == 0 ? "não pode puxar mais cartas" : "pode puxar mais " + remaining + " carta" + (remaining == 1 ? "" : "s")) + ")")
					.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
			Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
			resetTimerKeepTurn();
		});
		buttons.put("▶️", (mb, ms) -> {
			if (getRound() < 1 || phase == Phase.ATTACK) {
				User u = getCurrent();
				resetTimer();

				phase = Phase.PLAN;
				arena.getSlots().get(hands.get(u.getId()).getSide()).forEach(s -> {
					if (s.getTop() != null)
						s.getTop().setAvailable(true);
				});
				Hand hd = hands.get(getCurrent().getId());
				hd.addMana(5);

				List<SlotColumn<Drawable, Drawable>> slots = arena.getSlots().get(hd.getSide());
				slots.forEach(s -> {
					Champion c = (Champion) s.getTop();
					if (c != null && c.getTrigger() == EffectTrigger.ON_TURN)
						c.getEffect().getEffect().accept(c.getEffectArgs(), Triple.of(this, h, c));
				});

				if (this.message != null) this.message.delete().queue();
				this.message = channel.sendMessage(u.getAsMention() + " encerrou o turno, agora é sua vez " + getCurrent().getAsMention())
						.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
				Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
				hd.showHand();
				return;
			}

			channel.sendMessage("**FASE DE ATAQUE:** Escolha uma carta do seu lado e uma carta do lado inimigo para iniciar combate").queue();
			phase = Phase.ATTACK;
			hands.get(getCurrent().getId()).getCards().removeIf(d -> !d.isAvailable());
			resetTimerKeepTurn();
		});

		String[] args = cmd.split(",");

		if (phase == Phase.PLAN) {
			try {
				List<SlotColumn<Drawable, Drawable>> slots = arena.getSlots().get(h.getSide());
				if (args.length == 1 && StringUtils.isNumeric(args[0])) {
					int index = Integer.parseInt(args[0]);

					if (index < 0 || index >= slots.size()) {
						channel.sendMessage("❌ | Índice inválido.").queue();
						return;
					}

					Champion c = (Champion) slots.get(Integer.parseInt(args[0])).getTop();

					if (c == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue();
						return;
					}

					c.setDefending(c.isFlipped() || !c.isDefending());

					if (c.isFlipped()) {
						c.setFlipped(false);

						if (this.message != null) this.message.delete().queue();
						this.message = channel.sendMessage("Carta virada para cima em modo de defesa.")
								.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
						Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, u -> u.getId().equals(getCurrent().getId()));
					} else if (!c.isDefending()) {
						if (this.message != null) this.message.delete().queue();
						this.message = channel.sendMessage("Carta trocada para modo de ataque.")
								.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
						Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, u -> u.getId().equals(getCurrent().getId()));
					} else {
						if (this.message != null) this.message.delete().queue();
						this.message = channel.sendMessage("Carta trocada para modo de defesa.")
								.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
						Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, u -> u.getId().equals(getCurrent().getId()));
					}
					return;
				}

				if (!StringUtils.isNumeric(args[0])) {
					channel.sendMessage("❌ | Índice inválido, verifique a mensagem enviada por mim no privado para ver as cartas na sua mão.").queue();
					return;
				}

				Drawable d = h.getCards().get(Integer.parseInt(args[0]));

				if (!d.isAvailable()) {
					channel.sendMessage("❌ | Essa carta já foi jogada neste turno.").queue();
					return;
				}

				if (d instanceof Equipment) {
					if (args.length < 3) {
						channel.sendMessage("❌ | O terceiro argumento deve ser o número da casa da carta à equipar este equipamento.").queue();
						return;
					}

					if (!StringUtils.isNumeric(args[1])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue();
						return;
					}

					SlotColumn<Drawable, Drawable> slot = slots.get(Integer.parseInt(args[1]));

					if (slot.getBottom() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue();
						return;
					}

					if (!StringUtils.isNumeric(args[2])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma carta para equipar esse equipamento.").queue();
						return;
					}

					SlotColumn<Drawable, Drawable> target = slots.get(Integer.parseInt(args[2]));

					if (target.getTop() == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue();
						return;
					}

					Drawable tp = d.copy();
					slot.setBottom(tp);
					Champion t = (Champion) target.getTop();
					if (t.isFlipped()) {
						t.setFlipped(false);
						t.setDefending(true);
					}
					t.addLinkedTo((Equipment) tp);
					((Equipment) tp).setLinkedTo(Pair.of(Integer.parseInt(args[2]), t.getCard()));
				} else {
					if (args.length < 3) {
						channel.sendMessage("❌ | O terceiro argumento deve ser `S` ou `N` para definir se a carta estará virada para baixo ou não.").queue();
						return;
					} else if (h.getMana() < ((Champion) d).getMana()) {
						channel.sendMessage("❌ | Você não tem mana suficiente para invocar essa carta, encerre o turno com `finalizar` ou jogue cartas de equipamento.").queue();
						return;
					}

					if (!StringUtils.isNumeric(args[1])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue();
						return;
					}

					SlotColumn<Drawable, Drawable> slot = slots.get(Integer.parseInt(args[1]));

					if (slot.getTop() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue();
						return;
					}

					Champion tp = (Champion) d.copy();
					tp.setFlipped(args[2].equalsIgnoreCase("s"));
					slot.setTop(tp);

					if (tp.getTrigger() == EffectTrigger.ON_SUMMON && !tp.isFlipped())
						tp.getEffect().getEffect().accept(tp.getEffectArgs(), Triple.of(this, h, tp));
				}
				d.setAvailable(false);
				if (d instanceof Champion)
					h.removeMana(((Champion) d).getMana());

				List<Drawable> champsInField = arena.getSlots().get(h.getSide())
						.stream()
						.map(SlotColumn::getTop)
						.collect(Collectors.toList());

				List<Drawable> equipsInField = arena.getSlots().get(h.getSide())
						.stream()
						.map(SlotColumn::getBottom)
						.collect(Collectors.toList());

				List<String> allCards = new ArrayList<>() {{
					addAll(Stream.of(champsInField, equipsInField)
							.flatMap(List::stream)
							.filter(Objects::nonNull)
							.map(dr -> dr.getCard().getId())
							.collect(Collectors.toList())
					);
				}};

				Champion aFusion = ultimates
						.stream()
						.filter(f ->
								f.getRequiredCards().size() > 0 && allCards.containsAll(f.getRequiredCards())
						)
						.findFirst()
						.orElse(null);

				if (aFusion != null) {
					List<SlotColumn<Drawable, Drawable>> slts = arena.getSlots().get(h.getSide());
					for (String requiredCard : aFusion.getRequiredCards()) {
						for (SlotColumn<Drawable, Drawable> slt : slts) {
							if (slt.getTop() != null && slt.getTop().getCard().getId().equals(requiredCard)) {
								Champion c = (Champion) slt.getTop();

								c.setAvailable(true);
								c.setDefending(false);
								c.setFlipped(false);
								c.clearLinkedTo();

								arena.getGraveyard().get(h.getSide()).add(slt.getTop());
								slt.setTop(null);
								break;
							} else if (slt.getBottom() != null && slt.getBottom().getCard().getId().equals(requiredCard)) {
								((Equipment) slt.getBottom()).setLinkedTo(null);

								arena.getGraveyard().get(h.getSide()).add(slt.getBottom());
								slt.setBottom(null);
								break;
							}
						}
					}

					for (SlotColumn<Drawable, Drawable> slt : slts) {
						if (slt.getTop() == null) {
							slt.setTop(aFusion.copy());
							break;
						}
					}
				}

				if (this.message != null) this.message.delete().queue();
				this.message = channel.sendFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
				Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, u -> u.getId().equals(getCurrent().getId()));
				h.showHand();
			} catch (IndexOutOfBoundsException e) {
				channel.sendMessage("❌ | Índice inválido, verifique a mensagem enviada por mim no privado para ver as cartas na sua mão.").queue();
			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser um valor inteiro que represente uma carta na sua mão e o segundo deve ser uma casa vazia no tabuleiro.").queue();
			}
		} else {
			try {
				if (!StringUtils.isNumeric(args[0])) {
					channel.sendMessage("❌ | Índice inválido, escolha uma carta para usar no ataque.").queue();
					return;
				} else if (args.length > 1 && !StringUtils.isNumeric(args[1])) {
					channel.sendMessage("❌ | Índice inválido, escolha uma carta para ser atacada.").queue();
					return;
				}

				int[] is = {Integer.parseInt(args[0]), args.length == 1 ? 0 : Integer.parseInt(args[1])};

				List<SlotColumn<Drawable, Drawable>> yourSide = arena.getSlots().get(h.getSide());
				List<SlotColumn<Drawable, Drawable>> hisSide = arena.getSlots().get(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP);

				if (args.length == 1) {
					if (is[0] < 0 || is[0] >= yourSide.size()) {
						channel.sendMessage("❌ | Índice inválido.").queue();
						return;
					}

					Champion c = (Champion) yourSide.get(Integer.parseInt(args[0])).getTop();

					if (c == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue();
						return;
					} else if (hisSide.stream().anyMatch(s -> s.getTop() != null)) {
						channel.sendMessage("❌ | Ainda existem campeões no campo inimigo.").queue();
						return;
					} else if (!c.isAvailable()) {
						channel.sendMessage("❌ | Essa carta já atacou neste turno.").queue();
						return;
					} else if (c.isFlipped()) {
						channel.sendMessage("❌ | Você não pode atacar com cartas viradas para baixo.").queue();
						return;
					}

					Hand enemy = hands.get(getBoard().getPlayers().get(1).getId());

					int yPower;
					if (c.isDefending())
						yPower = c.getDef() + c.getLinkedTo().stream().mapToInt(Equipment::getDef).sum();
					else
						yPower = c.getAtk() + c.getLinkedTo().stream().mapToInt(Equipment::getAtk).sum();

					enemy.removeHp(yPower);

					if (c.getTrigger() == EffectTrigger.ON_ATTACK)
						c.getEffect().getEffect().accept(c.getEffectArgs(), Triple.of(this, h, c));

					if (this.message != null) this.message.delete().queue();
					this.message = channel.sendMessage("Você atacou o diretamente o inimigo.")
							.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
					Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, u -> u.getId().equals(getCurrent().getId()));
					return;
				}

				Champion yours = (Champion) yourSide.get(is[0]).getTop();
				Champion his = (Champion) hisSide.get(is[1]).getTop();

				if (yours == null || his == null) {
					channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue();
					return;
				} else if (!yours.isAvailable()) {
					channel.sendMessage("❌ | Essa carta já atacou neste turno.").queue();
					return;
				} else if (yours.isFlipped()) {
					channel.sendMessage("❌ | Você não pode atacar com cartas viradas para baixo.").queue();
					return;
				}

				int yPower;
				if (yours.isDefending())
					yPower = yours.getDef() + yours.getLinkedTo().stream().mapToInt(Equipment::getDef).sum();
				else
					yPower = yours.getAtk() + yours.getLinkedTo().stream().mapToInt(Equipment::getAtk).sum();

				int hPower;
				if (his.isDefending() || his.isFlipped()) {
					if (his.isFlipped()) {
						his.setFlipped(false);
						his.setDefending(true);

						if (his.getTrigger() == EffectTrigger.ON_FLIP)
							his.getEffect().getEffect().accept(his.getEffectArgs(), Triple.of(this, h, his));
					}
					hPower = his.getDef() + his.getLinkedTo().stream().mapToInt(Equipment::getDef).sum();
				} else
					hPower = his.getAtk() + his.getLinkedTo().stream().mapToInt(Equipment::getAtk).sum();

				if (yPower > hPower) {
					yours.setAvailable(false);
					killCard(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, is[1], hisSide, hands.get(getBoard().getPlayers().get(1).getId()), his);

					if (yours.getTrigger() == EffectTrigger.ON_ATTACK)
						yours.getEffect().getEffect().accept(yours.getEffectArgs(), Triple.of(this, h, yours));

					if (this.message != null) this.message.delete().queue();
					this.message = channel.sendMessage("Sua carta derrotou a carta inimiga! (" + yPower + " > " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
				} else if (yPower < hPower) {
					killCard(h.getSide(), is[0], yourSide, h, yours);

					if (this.message != null) this.message.delete().queue();
					this.message = channel.sendMessage("Sua carta foi derrotada pela carta inimiga! (" + yPower + " < " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
				} else {
					killCard(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, is[1], hisSide, hands.get(getBoard().getPlayers().get(1).getId()), his);
					killCard(h.getSide(), is[0], yourSide, h, yours);

					if (this.message != null) this.message.delete().queue();
					this.message = channel.sendMessage("As duas cartas foram destruidas! (" + yPower + " = " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();
				}

				Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, u -> u.getId().equals(getCurrent().getId()));
			} catch (IndexOutOfBoundsException e) {
				channel.sendMessage("❌ | Índice inválido, escolha uma casa que tenha um campeão nela.").queue();
			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser uma casa com uma carta no seu lado do tabuleiro e o segundo deve ser uma casa com uma carta no lado do inimigo.").queue();
			}
		}

		if (hands.get(getBoard().getPlayers().get(1).getId()).getHp() <= 0) {
			if (this.message != null) this.message.delete().queue();
			this.message = channel.sendMessage(getCurrent().getAsMention() + " zerou os pontos de vida de " + getPlayerById(getBoard().getPlayers().get(1).getId()).getAsMention() + ", temos um vencedor! (" + getRound() + " turnos)")
					.addFile(Helper.getBytes(arena.render(handToArgument()), "jpg"), "board.jpg").complete();

			getBoard().awardWinner(this, getCurrent().getId());
			close();
		}
	}

	private void killCard(Side s, int index, List<SlotColumn<Drawable, Drawable>> side, Hand h, Champion ch) {

		if (ch.getTrigger() == EffectTrigger.ON_DEATH)
			ch.getEffect().getEffect().accept(ch.getEffectArgs(), Triple.of(this, h, ch));

		ch.setAvailable(true);
		ch.setDefending(false);
		ch.setFlipped(false);
		ch.clearLinkedTo();
		if (ch.getRace() != Race.ULTIMATE)
			arena.getGraveyard().get(s).add(ch);
		side.get(index).setTop(null);
		side.forEach(sd -> {
			if (sd.getBottom() != null && ((Equipment) sd.getBottom()).getLinkedTo().getLeft() == index) {
				((Equipment) sd.getBottom()).setLinkedTo(null);
				arena.getGraveyard().get(s).add(sd.getBottom());
				sd.setBottom(null);
			}
		});
	}

	public Arena getArena() {
		return arena;
	}

	@Override
	public void close() {
		super.close();
		getHandler().removeEventListener(listener);
	}

	private Map<Side, Hand> handToArgument() {
		return hands.values()
				.stream()
				.map(h -> Pair.of(h.getSide(), h))
				.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
	}
}
