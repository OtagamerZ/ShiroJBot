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
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Shoukan extends Game {
	private final Map<Side, Hand> hands;
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
	private final List<Champion> ultimates = CardDAO.getFusions();
	private final boolean[] changed = {false, false, false, false, false};

	public Shoukan(JDA handler, TextChannel channel, int bet, JSONObject custom, User... players) {
		super(handler, new Board(BoardSize.S_NONE, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel, custom);
		this.channel = channel;

		Kawaipon p1 = KawaiponDAO.getKawaipon(players[0].getId());
		Kawaipon p2 = KawaiponDAO.getKawaipon(players[1].getId());

		this.hands = Map.of(
				Side.TOP, new Hand(this, players[0], new ArrayList<>() {{
					addAll(p1.getChampions());
					addAll(p1.getEquipments());
				}}, Side.TOP),
				Side.BOTTOM, new Hand(this, players[1], new ArrayList<>() {{
					addAll(p2.getChampions());
					addAll(p2.getEquipments());
				}}, Side.BOTTOM)
		);

		setActions(
				s -> close(),
				s -> {
					if (custom == null) getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
				}
		);
	}

	@Override
	public void start() {
		Map<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
		buttons.put("▶️", (mb, ms) -> {
			getHandById(getCurrent().getId()).getCards().removeIf(d -> !d.isAvailable());
			if (getRound() < 1 || phase == Phase.ATTACK) {
				User u = getCurrent();
				resetTimer();

				phase = Phase.PLAN;
				Hand hd = getHandById(getCurrent().getId());
				arena.getSlots().get(getHandById(mb.getId()).getSide()).forEach(s -> {
					if (s.getTop() != null) {
						Champion c = (Champion) s.getTop();
						c.setAvailable(true);
						c.resetAttribs();
					}
				});

				List<SlotColumn<Drawable, Drawable>> slots = arena.getSlots().get(hd.getSide());
				for (int i = 0; i < slots.size(); i++) {
					if (slots.get(i).getTop() != null) {
						Champion c = (Champion) slots.get(i).getTop();
						if (c.hasEffect())
							c.getEffect(new EffectParameters(phase, EffectTrigger.ON_TURN, this, i, hd.getSide(), Duelists.of(c, i, null, -1)));
					}
				}
				hd.addMana(hd.getManaPerTurn());

				if (this.message != null) this.message.delete().queue();
				this.message = channel.sendMessage(u.getAsMention() + " encerrou o turno, agora é sua vez " + getCurrent().getAsMention())
						.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
				Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
				hd.showHand();
				for (int i = 0; i < 5; i++) {
					changed[i] = false;
				}
				return;
			}

			channel.sendMessage("**FASE DE ATAQUE:** Escolha uma carta do seu lado e uma carta do lado inimigo para iniciar combate").queue();
			phase = Phase.ATTACK;
			resetTimerKeepTurn();
		});
		buttons.put("\uD83D\uDCE4", (mb, ms) -> {
			if (phase != Phase.PLAN) {
				channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue();
				return;
			}

			Hand h = getHandById(getCurrent().getId());

			int remaining = 5 - h.getCards().size();

			if (remaining <= 0) {
				channel.sendMessage("❌ | Você não pode puxar mais cartas se tiver 5 cartas ou mais na sua mão.").queue();
				return;
			}

			if (h.getDeque().size() == 0) {
				if (this.message != null) this.message.delete().queue();
				this.message = channel.sendMessage(getCurrent().getAsMention() + " não possui mais cartas, " + getPlayerById(getBoard().getPlayers().get(1).getId()).getAsMention() + " venceu! (" + getRound() + " turnos)")
						.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();

				if (getCustom().isEmpty())
					getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
				close();
			}

			h.draw();
			h.showHand();
			remaining = 5 - h.getCards().size();
			if (this.message != null) this.message.delete().queue();
			this.message = channel.sendMessage(getCurrent().getAsMention() + " puxou uma carta (" + (remaining == 0 ? "não pode puxar mais cartas" : "pode puxar mais " + remaining + " carta" + (remaining == 1 ? "" : "s")) + ")")
					.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
			Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
			resetTimerKeepTurn();
		});
		buttons.put("\uD83C\uDFF3️", (mb, ms) -> {
			if (this.message != null) this.message.delete().queue();
			this.message = channel.sendMessage(getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)").complete();
			if (getCustom().isEmpty())
				getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
			close();
		});

		Hand h = getHandById(getCurrent().getId());
		h.addMana(h.getManaPerTurn());
		message = channel.sendMessage(getCurrent().getAsMention() + " você começa! (Olhe as mensagens privadas)")
				.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
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
		Hand h = getHandById(getCurrent().getId());

		Map<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
		buttons.put("▶️", (mb, ms) -> {
			getHandById(getCurrent().getId()).getCards().removeIf(d -> !d.isAvailable());
			if (getRound() < 1 || phase == Phase.ATTACK) {
				User u = getCurrent();
				resetTimer();

				phase = Phase.PLAN;
				Hand hd = getHandById(getCurrent().getId());
				arena.getSlots().get(getHandById(mb.getId()).getSide()).forEach(s -> {
					if (s.getTop() != null) {
						Champion c = (Champion) s.getTop();
						c.setAvailable(true);
						c.resetAttribs();
					}
				});

				List<SlotColumn<Drawable, Drawable>> slots = arena.getSlots().get(hd.getSide());
				for (int i = 0; i < slots.size(); i++) {
					if (slots.get(i).getTop() != null) {
						Champion c = (Champion) slots.get(i).getTop();
						if (c.hasEffect())
							c.getEffect(new EffectParameters(phase, EffectTrigger.ON_TURN, this, i, hd.getSide(), Duelists.of(c, i, null, -1)));
					}
				}
				hd.addMana(hd.getManaPerTurn());

				if (this.message != null) this.message.delete().queue();
				this.message = channel.sendMessage(u.getAsMention() + " encerrou o turno, agora é sua vez " + getCurrent().getAsMention())
						.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
				Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
				hd.showHand();
				for (int i = 0; i < 5; i++) {
					changed[i] = false;
				}
				return;
			}

			channel.sendMessage("**FASE DE ATAQUE:** Escolha uma carta do seu lado e uma carta do lado inimigo para iniciar combate").queue();
			phase = Phase.ATTACK;
			resetTimerKeepTurn();
		});
		buttons.put("\uD83D\uDCE4", (mb, ms) -> {
			if (phase != Phase.PLAN) {
				channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue();
				return;
			}

			Hand hd = getHandById(getCurrent().getId());

			int remaining = 5 - hd.getCards().size();

			if (remaining <= 0) {
				channel.sendMessage("❌ | Você não pode puxar mais cartas se tiver 5 cartas ou mais na sua mão.").queue();
				return;
			}

			if (hd.getDeque().size() == 0) {
				if (this.message != null) this.message.delete().queue();
				this.message = channel.sendMessage(getCurrent().getAsMention() + " não possui mais cartas, " + getPlayerById(getBoard().getPlayers().get(1).getId()).getAsMention() + " venceu! (" + getRound() + " turnos)")
						.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();

				if (getCustom().isEmpty())
					getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
				close();
			}

			hd.draw();
			hd.showHand();
			remaining = 5 - hd.getCards().size();
			if (this.message != null) this.message.delete().queue();
			this.message = channel.sendMessage(getCurrent().getAsMention() + " puxou uma carta (" + (remaining == 0 ? "não pode puxar mais cartas" : "pode puxar mais " + remaining + " carta" + (remaining == 1 ? "" : "s")) + ")")
					.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
			Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
			resetTimerKeepTurn();
		});
		buttons.put("\uD83C\uDFF3️", (mb, ms) -> {
			if (this.message != null) this.message.delete().queue();
			this.message = channel.sendMessage(getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)").complete();
			if (getCustom().isEmpty())
				getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
			close();
		});

		String[] args = cmd.split(",");
		if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage("❌ | Índice inválido.").queue();
			return;
		}
		int index = Integer.parseInt(args[0]) - 1;

		if (phase == Phase.PLAN) {
			try {
				List<SlotColumn<Drawable, Drawable>> slots = arena.getSlots().get(h.getSide());
				if (args.length == 1) {
					if (index < 0 || index >= slots.size()) {
						channel.sendMessage("❌ | Índice inválido.").queue();
						return;
					}

					Champion c = (Champion) slots.get(index).getTop();

					if (c == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue();
						return;
					} else if (changed[index]) {
						channel.sendMessage("❌ | Você já mudou a postura dessa carta neste turno.").queue();
						return;
					}

					c.setDefending(c.isFlipped() || !c.isDefending());

					if (c.hasEffect() && !c.isFlipped())
						c.getEffect(new EffectParameters(phase, EffectTrigger.ON_SWITCH, this, index, h.getSide(), Duelists.of(c, index, null, -1)));

					if (c.isFlipped()) {
						c.setFlipped(false);

						if (this.message != null) this.message.delete().queue();
						this.message = channel.sendMessage("Carta virada para cima em modo de defesa.")
								.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
						Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						changed[index] = true;
					} else if (!c.isDefending()) {
						if (this.message != null) this.message.delete().queue();
						this.message = channel.sendMessage("Carta trocada para modo de ataque.")
								.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
						Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						changed[index] = true;
					} else {
						if (this.message != null) this.message.delete().queue();
						this.message = channel.sendMessage("Carta trocada para modo de defesa.")
								.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
						Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						changed[index] = true;
					}

					resetTimerKeepTurn();
					return;
				}

				Drawable d = h.getCards().get(index);

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

					int dest = Integer.parseInt(args[1]) - 1;
					SlotColumn<Drawable, Drawable> slot = slots.get(dest);

					if (slot.getBottom() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue();
						return;
					}

					if (!StringUtils.isNumeric(args[2])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma carta para equipar esse equipamento.").queue();
						return;
					}
					int toEquip = Integer.parseInt(args[2]) - 1;

					SlotColumn<Drawable, Drawable> target = slots.get(toEquip);

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
					((Equipment) tp).setLinkedTo(Pair.of(toEquip, t.getCard()));
				} else {
					if (args.length < 3) {
						channel.sendMessage("❌ | O terceiro argumento deve ser `S` ou `N` para definir se a carta estará virada para baixo ou não.").queue();
						return;
					} else if (h.getMana() < ((Champion) d).getMana()) {
						channel.sendMessage("❌ | Você não tem mana suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou jogue cartas de equipamento.").queue();
						return;
					}

					if (!StringUtils.isNumeric(args[1])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue();
						return;
					}
					int dest = Integer.parseInt(args[1]) - 1;

					SlotColumn<Drawable, Drawable> slot = slots.get(dest);

					if (slot.getTop() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue();
						return;
					}

					Champion tp = (Champion) d.copy();
					tp.setFlipped(args[2].equalsIgnoreCase("s"));
					slot.setTop(tp);
					if (tp.hasEffect() && !tp.isFlipped())
						tp.getEffect(new EffectParameters(phase, EffectTrigger.ON_SUMMON, this, dest, h.getSide(), Duelists.of(tp, dest, null, -1)));
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
						for (int i = 0; i < slts.size(); i++) {
							if (slts.get(i).getTop() != null && slts.get(i).getTop().getCard().getId().equals(requiredCard)) {
								killCard(h.getSide(), i, getArena().getSlots().get(h.getSide()));
								break;
							} else if (slts.get(i).getBottom() != null && slts.get(i).getBottom().getCard().getId().equals(requiredCard)) {
								((Equipment) slts.get(i).getBottom()).setLinkedTo(null);

								arena.getGraveyard().get(h.getSide()).add(slts.get(i).getBottom());
								slts.get(i).setBottom(null);
								break;
							}
						}
					}

					for (SlotColumn<Drawable, Drawable> slt : slts) {
						if (slt.getTop() == null) {
							slt.setTop(aFusion.copy());
							if (aFusion.hasEffect() && !aFusion.isFlipped())
								aFusion.getEffect(new EffectParameters(phase, EffectTrigger.ON_SUMMON, this, Integer.parseInt(args[1]) - 1, h.getSide(), Duelists.of(aFusion, Integer.parseInt(args[1]) - 1, null, -1)));
							break;
						}
					}
				}

				if (this.message != null) this.message.delete().queue();
				this.message = channel.sendFile(Helper.getBytes(arena.render(hands), "jpg"), "board.jpg").complete();
				Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
				h.showHand();
				resetTimerKeepTurn();
			} catch (IndexOutOfBoundsException e) {
				channel.sendMessage("❌ | Índice inválido, verifique a mensagem enviada por mim no privado para ver as cartas na sua mão.").queue();
			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser um valor inteiro que represente uma carta na sua mão e o segundo deve ser uma casa vazia no tabuleiro.").queue();
			}
		} else {
			try {
				if (args.length > 1 && !StringUtils.isNumeric(args[1])) {
					channel.sendMessage("❌ | Índice inválido, escolha uma carta para ser atacada.").queue();
					return;
				}

				int[] is = {index, args.length == 1 ? 0 : Integer.parseInt(args[1]) - 1};

				List<SlotColumn<Drawable, Drawable>> yourSide = arena.getSlots().get(h.getSide());
				List<SlotColumn<Drawable, Drawable>> hisSide = arena.getSlots().get(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP);

				if (args.length == 1) {
					if (is[0] < 0 || is[0] >= yourSide.size()) {
						channel.sendMessage("❌ | Índice inválido.").queue();
						return;
					}

					Champion c = (Champion) yourSide.get(is[0]).getTop();

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
					} else if (c.isDefending()) {
						channel.sendMessage("❌ | Você não pode atacar com cartas em modo de defesa.").queue();
						return;
					}

					Hand enemy = getHandById(getBoard().getPlayers().get(1).getId());

					int yPower = c.getAtk() + c.getLinkedTo().stream().mapToInt(Equipment::getAtk).sum();

					enemy.removeHp(yPower);
					c.setAvailable(false);

					if (this.message != null) this.message.delete().queue();
					this.message = channel.sendMessage("Você atacou diretamente o inimigo.")
							.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
					Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
					if (!postCombat()) resetTimerKeepTurn();
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
				} else if (yours.isDefending()) {
					channel.sendMessage("❌ | Você não pode atacar com cartas em modo de defesa.").queue();
					return;
				}

				if (yours.hasEffect())
					yours.getEffect(new EffectParameters(phase, EffectTrigger.ON_ATTACK, this, is[0], h.getSide(), Duelists.of(yours, is[0], his, is[1])));

				if (his.hasEffect())
					his.getEffect(new EffectParameters(phase, EffectTrigger.ON_DEFEND, this, is[1], h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, Duelists.of(yours, is[0], his, is[1])));

				int yPower = yours.getEAtk() + yours.getLinkedTo().stream().mapToInt(Equipment::getAtk).sum();

				int hPower;
				if (his.isDefending() || his.isFlipped()) {
					if (his.isFlipped()) {
						his.setFlipped(false);
						his.setDefending(true);
						if (his.hasEffect())
							his.getEffect(new EffectParameters(phase, EffectTrigger.ON_FLIP, this, is[1], h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, Duelists.of(yours, is[0], his, is[1])));
					}
					hPower = his.getEDef() + his.getLinkedTo().stream().mapToInt(Equipment::getDef).sum();
				} else
					hPower = his.getEAtk() + his.getLinkedTo().stream().mapToInt(Equipment::getAtk).sum();

				if (yPower > hPower) {
					yours.setAvailable(false);
					yours.resetAttribs();
					if (yours.hasEffect())
						yours.getEffect(new EffectParameters(phase, EffectTrigger.POST_ATTACK, this, is[0], h.getSide(), Duelists.of(yours, is[0], his, is[1])));
					if (his.hasEffect())
						his.getEffect(new EffectParameters(phase, EffectTrigger.ON_DEATH, this, is[1], h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, Duelists.of(yours, is[0], his, is[1])));
					killCard(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, is[1], hisSide);

					if (this.message != null) this.message.delete().queue();
					this.message = channel.sendMessage("Sua carta derrotou a carta inimiga! (" + yPower + " > " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
				} else if (yPower < hPower) {
					his.resetAttribs();
					if (yours.hasEffect())
						yours.getEffect(new EffectParameters(phase, EffectTrigger.ON_SUICIDE, this, is[0], h.getSide(), Duelists.of(yours, is[0], his, is[1])));
					if (his.hasEffect())
						his.getEffect(new EffectParameters(phase, EffectTrigger.POST_DEFENSE, this, is[1], h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, Duelists.of(yours, is[0], his, is[1])));
					killCard(h.getSide(), is[0], yourSide);

					if (this.message != null) this.message.delete().queue();
					this.message = channel.sendMessage("Sua carta foi derrotada pela carta inimiga! (" + yPower + " < " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
				} else {
					killCard(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, is[1], hisSide);
					killCard(h.getSide(), is[0], yourSide);

					if (this.message != null) this.message.delete().queue();
					this.message = channel.sendMessage("As duas cartas foram destruidas! (" + yPower + " = " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();
				}

				Pages.buttonize(this.message, buttons, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
				if (!postCombat()) resetTimerKeepTurn();
			} catch (IndexOutOfBoundsException e) {
				channel.sendMessage("❌ | Índice inválido, escolha uma carta para usar no ataque e uma para ser atacada.").queue();
			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser uma casa com uma carta no seu lado do tabuleiro e o segundo deve ser uma casa com uma carta no lado do inimigo.").queue();
			}
		}
	}

	public void killCard(Side s, int index, List<SlotColumn<Drawable, Drawable>> side) {
		Champion ch = (Champion) side.get(index).getTop();
		if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventDeath", false)) return;
		ch.reset();
		if (ch.getRequiredCards().size() == 0)
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

	public void unequipCard(Side s, int index, List<SlotColumn<Drawable, Drawable>> side) {
		Equipment eq = (Equipment) side.get(index).getBottom();

		if (side.get(eq.getLinkedTo().getLeft()).getTop() != null)
			((Champion) side.get(eq.getLinkedTo().getLeft()).getTop()).removeLinkedTo(eq);
		eq.setLinkedTo(null);

		SlotColumn<Drawable, Drawable> sd = side.get(index);
		arena.getGraveyard().get(s).add(eq);
		sd.setBottom(null);
	}

	public Arena getArena() {
		return arena;
	}

	public Map<Side, Hand> getHands() {
		return hands;
	}

	public Hand getHandById(String id) {
		return hands.values().stream().filter(h -> h.getUser().getId().equals(id)).findFirst().orElseThrow();
	}

	public SlotColumn<Drawable, Drawable> getFirstAvailableSlot(Side s, boolean top) {
		for (SlotColumn<Drawable, Drawable> slot : arena.getSlots().get(s)) {
			if (top ? slot.getTop() == null : slot.getBottom() == null)
				return slot;
		}
		return null;
	}

	public void convertCard(Side side, int index) {
		Side his = side == Side.TOP ? Side.BOTTOM : Side.TOP;
		Champion ch = (Champion) getArena().getSlots().get(his).get(index).getTop();
		if (ch == null) return;
		SlotColumn<Drawable, Drawable> sc = getFirstAvailableSlot(side, true);
		if (sc != null) {
			ch.clearLinkedTo();
			sc.setTop(ch);
			List<SlotColumn<Drawable, Drawable>> slts = getArena().getSlots().get(his);
			slts.get(index).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				if (slts.get(i).getBottom() != null && ((Equipment) slts.get(i).getBottom()).getLinkedTo().getLeft() == index)
					unequipCard(his, i, slts);
			}
		}
	}

	public void convertEquipments(Champion target, int pos, Side side, int index) {
		Side his = side == Side.TOP ? Side.BOTTOM : Side.TOP;
		Champion ch = (Champion) getArena().getSlots().get(his).get(index).getTop();
		if (ch == null) return;
		List<SlotColumn<Drawable, Drawable>> slts = getArena().getSlots().get(his);
		for (int i = 0; i < 5; i++) {
			Equipment eq = (Equipment) slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				SlotColumn<Drawable, Drawable> sc = getFirstAvailableSlot(side, false);
				if (sc != null) {
					ch.removeLinkedTo(eq);
					slts.get(i).setBottom(null);

					target.addLinkedTo(eq);
					eq.setLinkedTo(Pair.of(pos, target.getCard()));
					sc.setBottom(eq);
				} else return;
			}
		}
	}

	public boolean postCombat() {
		if (getHandById(getBoard().getPlayers().get(1).getId()).getHp() == 0) {
			if (this.message != null) this.message.delete().queue();
			this.message = channel.sendMessage(getCurrent().getAsMention() + " zerou os pontos de vida de " + getPlayerById(getBoard().getPlayers().get(1).getId()).getAsMention() + ", temos um vencedor! (" + getRound() + " turnos)")
					.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg").complete();

			if (getCustom().isEmpty())
				getBoard().awardWinner(this, getCurrent().getId());
			close();
			return true;
		}
		return false;
	}

	@Override
	public void close() {
		super.close();
		getHandler().removeEventListener(listener);
	}
}
