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
import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.ClusterAction;
import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.GameChannel;
import com.kuuhaku.handlers.games.tabletop.framework.GlobalGame;
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Phase;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Shoukan extends GlobalGame {
	private final Map<Side, Hand> hands;
	private final GameChannel channel;
	private final Arena arena = new Arena();
	private final SimpleMessageListener listener = new SimpleMessageListener() {
		@Override
		public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
			if (canInteract(event)) play(event);
		}
	};
	private final Map<String, Message> message = new HashMap<>();
	private final List<Champion> fusions = CardDAO.getFusions();
	private final boolean[] changed = {false, false, false, false, false};
	private final boolean daily;
	private Phase phase = Phase.PLAN;
	private boolean draw = false;
	private Side current = Side.BOTTOM;
	private Side next = Side.TOP;
	private int fusionLock = 0;
	private int spellLock = 0;
	private int effectLock = 0;
	private List<Drawable> discardBatch = new ArrayList<>();

	public Shoukan(ShardManager handler, GameChannel channel, int bet, JSONObject custom, boolean daily, boolean ranked, User... players) {
		super(handler, new Board(BoardSize.S_NONE, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel, ranked, custom);
		this.channel = channel;
		this.daily = daily;

		Kawaipon p1 = daily ? Helper.getDailyDeck() : KawaiponDAO.getKawaipon(players[0].getId());
		Kawaipon p2 = daily ? Helper.getDailyDeck() : KawaiponDAO.getKawaipon(players[1].getId());

		this.hands = Map.of(
				Side.TOP, new Hand(this, players[0], p1, Side.TOP),
				Side.BOTTOM, new Hand(this, players[1], p2, Side.BOTTOM)
		);

		if (custom == null)
			getHistory().setPlayers(Map.of(
					players[0].getId(), Side.TOP,
					players[1].getId(), Side.BOTTOM
			));

		setActions(
				s -> {
					close();
					channel.sendFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(msg ->
									this.message.compute(msg.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return msg;
									}));
				},
				s -> {
					if (custom == null) {
						if (ranked) {
							MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(getCurrent().getId());
							mmr.block(30, ChronoUnit.MINUTES);
							MatchMakingRatingDAO.saveMMR(mmr);
						}

						getHistory().setWinner(next);
						getBoard().awardWinner(this, daily, getBoard().getPlayers().get(1).getId());
					}
					channel.sendFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(msg ->
									this.message.compute(msg.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return msg;
									}));
				}
		);
	}

	@Override
	public void start() {
		Hand h = getHands().get(current);
		h.addMana(h.getManaPerTurn());
		AtomicBoolean shownHand = new AtomicBoolean(false);
		AtomicReference<String> previous = new AtomicReference<>("");
		channel.sendMessage(getCurrent().getAsMention() + " você começa! (Olhe as mensagens privadas)")
				.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
				.queue(s -> {
					this.message.put(s.getChannel().getId(), s);
					if (!s.getGuild().getId().equals(previous.get())) {
						previous.set(s.getGuild().getId());
						Main.getInfo().getShiroEvents().addHandler(s.getGuild(), listener);
					}
					Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
					if (!shownHand.get()) {
						shownHand.set(true);
						h.showHand();
					}
				});
	}

	@Override
	public boolean canInteract(GuildMessageReceivedEvent evt) {
		Predicate<GuildMessageReceivedEvent> condition = e -> channel.getChannels().stream().anyMatch(g -> e.getChannel().getId().equals(g.getId()));

		return condition
				.and(e -> e.getAuthor().getId().equals(getCurrent().getId()))
				.and(e -> {
					String[] args = e.getMessage().getContentRaw().split(",");
					return (args.length > 0 && StringUtils.isNumeric(args[0])) || e.getMessage().getContentRaw().equalsIgnoreCase("reload");
				})
				.and(e -> !isClosed())
				.test(evt);
	}

	@Override
	public synchronized void play(GuildMessageReceivedEvent evt) {
		Message message = evt.getMessage();
		String cmd = message.getContentRaw();
		Hand h = getHands().get(current);

		if (cmd.equalsIgnoreCase("reload")) {
			channel.sendMessage(message.getAuthor().getName() + " recriou a mensagem do jogo.")
					.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
					.queue(s -> {
						this.message.compute(s.getChannel().getId(), (id, m) -> {
							if (m != null)
								m.delete().queue(null, Helper::doNothing);
							return s;
						});
						Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
					});
			return;
		}

		String[] args = cmd.split(",");
		if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
			return;
		}
		int index = Integer.parseInt(args[0]) - 1;

		if (phase == Phase.PLAN) {
			try {
				List<SlotColumn<Champion, Equipment>> slots = arena.getSlots().get(h.getSide());
				if (args.length == 1) {
					if (index < 0 || index >= slots.size()) {
						channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
						return;
					}

					Champion c = slots.get(index).getTop();

					if (c == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					} else if (changed[index]) {
						channel.sendMessage("❌ | Você já mudou a postura dessa carta neste turno.").queue(null, Helper::doNothing);
						return;
					} else if (c.getStun() > 0) {
						channel.sendMessage("❌ | Essa carta está atordoada.").queue(null, Helper::doNothing);
						return;
					}

					ClusterAction act;
					if (c.isFlipped()) {
						c.setFlipped(false);
						act = channel.sendMessage("Carta virada para cima em modo de defesa.");
					} else if (c.isDefending()) {
						c.setDefending(false);
						act = channel.sendMessage("Carta trocada para modo de ataque.");
						if (c.hasEffect() && !c.isFlipped() && effectLock == 0) {
							c.getEffect(new EffectParameters(EffectTrigger.ON_SWITCH, this, index, h.getSide(), Duelists.of(c, index, null, -1), channel));
							if (postCombat()) return;
						}
					} else {
						c.setDefending(true);
						act = channel.sendMessage("Carta trocada para modo de defesa.");
						if (c.hasEffect() && !c.isFlipped() && effectLock == 0) {
							c.getEffect(new EffectParameters(EffectTrigger.ON_SWITCH, this, index, h.getSide(), Duelists.of(c, index, null, -1), channel));
							if (postCombat()) return;
						}
					}

					changed[index] = true;
					resetTimerKeepTurn();
					act.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							});
					return;
				}

				Drawable d = h.getCards().get(index);
				String msg;

				if (args[1].equalsIgnoreCase("d")) {
					discardBatch.add(d.copy());
					d.setAvailable(false);

					if (makeFusion(h)) return;

					resetTimerKeepTurn();
					AtomicBoolean shownHand = new AtomicBoolean(false);
					channel.sendMessage(h.getUser().getName() + " descartou a carta " + d.getCard().getName() + ".")
							.addFile(Helper.getBytes(arena.render(this, hands), "jpg"), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
								if (!shownHand.get()) {
									shownHand.set(true);
									h.showHand();
								}
							});
					return;
				}

				if (!d.isAvailable()) {
					channel.sendMessage("❌ | Essa carta já foi jogada neste turno.").queue(null, Helper::doNothing);
					return;
				}

				if (d instanceof Equipment) {
					Equipment e = (Equipment) d.copy();
					if (e.getCharm() != null && e.getCharm().equals(Charm.SPELL)) {
						if (!args[1].equalsIgnoreCase("s")) {
							channel.sendMessage("❌ | O segundo argumento precisa ser `S` se deseja jogar uma carta de feitiço.").queue(null, Helper::doNothing);
							return;
						} else if (spellLock > 0) {
							channel.sendMessage("❌ | Feitiços estão bloqueados por mais " + (fusionLock == 1 ? "turno" : "turnos") + ".").queue(null, Helper::doNothing);
							return;
						} else if (h.getMana() < e.getMana()) {
							channel.sendMessage("❌ | Você não tem mana suficiente para usar essa magia, encerre o turno reagindo com :arrow_forward: ou jogue cartas de equipamento ou campo.").queue(null, Helper::doNothing);
							return;
						} else if (args.length - 2 < e.getArgType().getArgs()) {
							channel.sendMessage(
									switch (e.getArgType()) {
										case ALLY -> "❌ | Este feitiço requer um alvo aliado.";
										case ENEMY -> "❌ | Este feitiço requer um alvo inimigo.";
										case BOTH -> "❌ | Este feitiço requer um alvo aliado e um inimigo.";
										default -> "";
									}
							).queue(null, Helper::doNothing);
							return;
						}

						Pair<Champion, Integer> allyPos = null;
						Pair<Champion, Integer> enemyPos = null;

						switch (e.getArgType()) {
							case ALLY -> {
								if (!StringUtils.isNumeric(args[2])) {
									channel.sendMessage("❌ | Índice inválido, escolha uma carta aliada para usar este feitiço.").queue(null, Helper::doNothing);
									return;
								}
								int pos = Integer.parseInt(args[2]) - 1;
								Champion target = slots.get(pos).getTop();

								if (target == null) {
									channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
									return;
								}

								allyPos = Pair.of(target, pos);
							}
							case ENEMY -> {
								if (!StringUtils.isNumeric(args[2])) {
									channel.sendMessage("❌ | Índice inválido, escolha uma carta inimiga para usar este feitiço.").queue(null, Helper::doNothing);
									return;
								}
								int pos = Integer.parseInt(args[2]) - 1;
								List<SlotColumn<Champion, Equipment>> eSlots = arena.getSlots().get(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP);
								Champion target = eSlots.get(pos).getTop();

								if (target == null) {
									channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
									return;
								}

								enemyPos = Pair.of(target, pos);
							}
							case BOTH -> {
								if (!StringUtils.isNumeric(args[2]) || !StringUtils.isNumeric(args[3])) {
									channel.sendMessage("❌ | Índice inválido, escolha uma carta aliada e uma inimiga para usar este feitiço.").queue(null, Helper::doNothing);
									return;
								}
								int pos1 = Integer.parseInt(args[2]) - 1;
								int pos2 = Integer.parseInt(args[3]) - 1;
								Champion target = slots.get(pos1).getTop();

								if (target == null) {
									channel.sendMessage("❌ | Não existe uma carta na primeira casa.").queue(null, Helper::doNothing);
									return;
								}

								allyPos = Pair.of(target, pos1);
								List<SlotColumn<Champion, Equipment>> eSlots = arena.getSlots().get(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP);
								target = eSlots.get(pos2).getTop();

								if (target == null) {
									channel.sendMessage("❌ | Não existe uma carta na segunda casa.").queue(null, Helper::doNothing);
									return;
								}

								enemyPos = Pair.of(target, pos2);
							}
						}

						d.setAvailable(false);
						h.removeMana(e.getMana());
						e.activate(h, getHands().get(next), this, allyPos == null ? -1 : allyPos.getRight(), enemyPos == null ? -1 : enemyPos.getRight());
						arena.getGraveyard().get(h.getSide()).add(e);

						if (makeFusion(h)) return;

						String result = switch (e.getArgType()) {
							case NONE -> h.getUser().getName() + " usou o feitiço " + d.getCard().getName() + ".";
							case ALLY -> {
								assert allyPos != null;
								yield h.getUser().getName() + " usou o feitiço " + d.getCard().getName() + " em " + allyPos.getLeft().getName() + ".";
							}
							case ENEMY -> {
								assert enemyPos != null;
								yield h.getUser().getName() + " usou o feitiço " + d.getCard().getName() + " em " + enemyPos.getLeft().getName() + ".";
							}
							case BOTH -> {
								assert allyPos != null && enemyPos != null;
								yield h.getUser().getName() + " usou o feitiço " + d.getCard().getName() + " em " + allyPos.getLeft().getName() + " e " + enemyPos.getLeft().getName() + ".";
							}
						};

						if (!postCombat()) {
							resetTimerKeepTurn();
							AtomicBoolean shownHand = new AtomicBoolean(false);
							channel.sendMessage(result)
									.addFile(Helper.getBytes(arena.render(this, hands), "jpg"), "board.jpg")
									.queue(s -> {
										this.message.compute(s.getChannel().getId(), (id, m) -> {
											if (m != null)
												m.delete().queue(null, Helper::doNothing);
											return s;
										});
										Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
										if (!shownHand.get()) {
											shownHand.set(true);
											h.showHand();
										}
									});
						}
						return;
					}

					if (args.length < 3) {
						channel.sendMessage("❌ | O terceiro argumento deve ser o número da casa da carta à equipar este equipamento.").queue(null, Helper::doNothing);
						return;
					}

					if (!StringUtils.isNumeric(args[1])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue(null, Helper::doNothing);
						return;
					}

					int dest = Integer.parseInt(args[1]) - 1;
					SlotColumn<Champion, Equipment> slot = slots.get(dest);

					if (slot.getBottom() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					}

					if (!StringUtils.isNumeric(args[2])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma carta para equipar esse equipamento.").queue(null, Helper::doNothing);
						return;
					}
					int toEquip = Integer.parseInt(args[2]) - 1;

					SlotColumn<Champion, Equipment> target = slots.get(toEquip);

					if (target.getTop() == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					}

					d.setAvailable(false);
					slot.setBottom(e);
					Champion t = target.getTop();
					t.setFlipped(false);
					t.addLinkedTo(e);
					e.setLinkedTo(Pair.of(toEquip, t));
					if (t.hasEffect() && effectLock == 0) {
						t.getEffect(new EffectParameters(EffectTrigger.ON_EQUIP, this, toEquip, h.getSide(), Duelists.of(t, toEquip, null, -1), channel));
						if (postCombat()) return;
					}

					if (e.getCharm() != null) {
						switch (e.getCharm()) {
							case TIMEWARP -> {
								t.getEffect(new EffectParameters(EffectTrigger.BEFORE_TURN, this, toEquip, h.getSide(), Duelists.of(t, toEquip, null, -1), channel));
								t.getEffect(new EffectParameters(EffectTrigger.AFTER_TURN, this, toEquip, h.getSide(), Duelists.of(t, toEquip, null, -1), channel));
							}
							case DOUBLETAP -> t.getEffect(new EffectParameters(EffectTrigger.ON_SUMMON, this, toEquip, h.getSide(), Duelists.of(t, toEquip, null, -1), channel));
							case DOPPELGANGER -> {
								SlotColumn<Champion, Equipment> sc = getFirstAvailableSlot(h.getSide(), true);

								if (sc != null) {
									Champion dp = t.copy();
									dp.setRedAtk(Math.round(dp.getAltAtk() * 0.25f));
									dp.setRedDef(Math.round(dp.getAltDef() * 0.25f));
									dp.setBonus(t.getBonus());

									sc.setTop(dp);
								}
							}
						}

						if (postCombat()) return;
					}

					msg = h.getUser().getName() + " equipou " + e.getCard().getName() + " em " + t.getName() + ".";
				} else if (d instanceof Champion) {
					Champion c = (Champion) d.copy();
					if (args.length < 3) {
						channel.sendMessage("❌ | O terceiro argumento deve ser `A`, `D` ou `B` para definir se a carta será posicionada em modo de ataque, defesa ou virada para baixo.").queue(null, Helper::doNothing);
						return;
					} else if (h.getMana() < ((Champion) d).getMana()) {
						channel.sendMessage("❌ | Você não tem mana suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou jogue cartas de equipamento ou campo.").queue(null, Helper::doNothing);
						return;
					}

					if (!StringUtils.isNumeric(args[1])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue(null, Helper::doNothing);
						return;
					}
					int dest = Integer.parseInt(args[1]) - 1;

					SlotColumn<Champion, Equipment> slot = slots.get(dest);

					if (slot.getTop() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					}

					switch (args[2].toLowerCase()) {
						case "a" -> {
							c.setFlipped(false);
							c.setDefending(false);
						}
						case "d" -> {
							c.setFlipped(false);
							c.setDefending(true);
						}
						case "b" -> {
							c.setFlipped(true);
							c.setDefending(true);
						}
						default -> {
							channel.sendMessage("❌ | O terceiro argumento deve ser `A`, `D` ou `B` para definir se a carta será posicionada em modo de ataque, defesa ou virada para baixo.").queue(null, Helper::doNothing);
							return;
						}
					}

					d.setAvailable(false);
					slot.setTop(c);
					if (c.hasEffect() && !c.isFlipped() && effectLock == 0) {
						c.getEffect(new EffectParameters(EffectTrigger.ON_SUMMON, this, dest, h.getSide(), Duelists.of(c, dest, null, -1), channel));
						if (postCombat()) return;
					}

					msg = h.getUser().getName() + " invocou " + (c.isFlipped() ? "uma carta virada para baixo" : c.getName()) + ".";
				} else {
					if (!args[1].equalsIgnoreCase("f")) {
						channel.sendMessage("❌ | O segundo argumento precisa ser `F` se deseja jogar uma carta de campo.").queue(null, Helper::doNothing);
						return;
					}

					Field f = (Field) d.copy();
					d.setAvailable(false);
					arena.setField(f);
					msg = h.getUser().getName() + " invocou o campo " + f.getCard().getName() + ".";
				}

				if (d instanceof Champion)
					h.removeMana(((Champion) d).getMana());

				if (makeFusion(h)) return;

				resetTimerKeepTurn();
				AtomicBoolean shownHand = new AtomicBoolean(false);
				channel.sendMessage(msg)
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg"), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							if (!shownHand.get()) {
								shownHand.set(true);
								h.showHand();
							}
						});
			} catch (IndexOutOfBoundsException e) {
				channel.sendMessage("❌ | Índice inválido, verifique a mensagem enviada por mim no privado para ver as cartas na sua mão.").queue(null, Helper::doNothing);
			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser um valor inteiro que represente uma carta na sua mão e o segundo deve ser uma casa vazia no tabuleiro.").queue(null, Helper::doNothing);
			}
		} else {
			try {
				if (args.length > 1 && !StringUtils.isNumeric(args[1])) {
					channel.sendMessage("❌ | Índice inválido, escolha uma carta para ser atacada.").queue(null, Helper::doNothing);
					return;
				}

				int[] is = {index, args.length == 1 ? 0 : Integer.parseInt(args[1]) - 1};

				List<SlotColumn<Champion, Equipment>> yourSide = arena.getSlots().get(h.getSide());
				List<SlotColumn<Champion, Equipment>> hisSide = arena.getSlots().get(next);

				if (args.length == 1) {
					if (is[0] < 0 || is[0] >= yourSide.size()) {
						channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
						return;
					}

					Champion c = yourSide.get(is[0]).getTop();

					if (c == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					} else if (hisSide.stream().anyMatch(s -> s.getTop() != null)) {
						channel.sendMessage("❌ | Ainda existem campeões no campo inimigo.").queue(null, Helper::doNothing);
						return;
					} else if (!c.isAvailable()) {
						channel.sendMessage("❌ | Essa carta já atacou neste turno.").queue(null, Helper::doNothing);
						return;
					} else if (c.isFlipped()) {
						channel.sendMessage("❌ | Você não pode atacar com cartas viradas para baixo.").queue(null, Helper::doNothing);
						return;
					} else if (c.getStun() > 0) {
						channel.sendMessage("❌ | Essa carta está atordoada.").queue(null, Helper::doNothing);
						return;
					} else if (c.isDefending()) {
						channel.sendMessage("❌ | Você não pode atacar com cartas em modo de defesa.").queue(null, Helper::doNothing);
						return;
					}

					Hand enemy = getHands().get(next);

					int yPower = Math.round(
							c.getFinAtk() *
							(arena.getField() == null || c.getLinkedTo().stream().anyMatch(e -> e.getCharm() == Charm.SOULLINK) ? 1 : arena.getField().getModifiers().optFloat(c.getRace().name(), 1f))
					);

					if (!c.getCard().getId().equals("DECOY")) enemy.removeHp(yPower);
					c.setAvailable(false);

					if (!postCombat()) {
						resetTimerKeepTurn();
						channel.sendMessage(h.getUser().getName() + " atacou diretamente " + getHands().get(next).getUser().getName() + ".")
								.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
								.queue(s -> {
									this.message.compute(s.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return s;
									});
									Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
								});
					}
					return;
				}

				Champion yours = yourSide.get(is[0]).getTop();
				Champion his = hisSide.get(is[1]).getTop();

				if (yours == null || his == null) {
					channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
					return;
				} else if (!yours.isAvailable()) {
					channel.sendMessage("❌ | Essa carta já atacou neste turno.").queue(null, Helper::doNothing);
					return;
				} else if (yours.isFlipped()) {
					channel.sendMessage("❌ | Você não pode atacar com cartas viradas para baixo.").queue(null, Helper::doNothing);
					return;
				} else if (yours.getStun() > 0) {
					channel.sendMessage("❌ | Essa carta está atordoada.").queue(null, Helper::doNothing);
					return;
				} else if (yours.isDefending()) {
					channel.sendMessage("❌ | Você não pode atacar com cartas em modo de defesa.").queue(null, Helper::doNothing);
					return;
				}

				if (yours.hasEffect() && effectLock == 0) {
					yours.getEffect(new EffectParameters(EffectTrigger.ON_ATTACK, this, is[0], h.getSide(), Duelists.of(yours, is[0], his, is[1]), channel));

					if (yours.getBonus().getSpecialData().remove("skipCombat") != null || yours.getCard().getId().equals("DECOY")) {
						yours.setAvailable(false);
						yours.resetAttribs();
						if (yours.hasEffect()) {
							yours.getEffect(new EffectParameters(EffectTrigger.POST_ATTACK, this, is[0], h.getSide(), Duelists.of(yours, is[0], his, is[1]), channel));
							if (postCombat()) return;
						}

						if (!postCombat()) {
							resetTimerKeepTurn();
							channel.sendMessage("Cálculo de combate ignorado por efeito do atacante!")
									.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
									.queue(s -> {
										this.message.compute(s.getChannel().getId(), (id, m) -> {
											if (m != null)
												m.delete().queue(null, Helper::doNothing);
											return s;
										});
										Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
									});
						}
						return;
					} else if (postCombat()) return;
				}

				if (his.hasEffect() && effectLock == 0) {
					his.getEffect(new EffectParameters(EffectTrigger.ON_DEFEND, this, is[1], next, Duelists.of(yours, is[0], his, is[1]), channel));

					if (his.getBonus().getSpecialData().remove("skipCombat") != null || his.getCard().getId().equals("DECOY")) {
						if (!postCombat()) {
							resetTimerKeepTurn();
							channel.sendMessage("Cálculo de combate ignorado por efeito do defensor!")
									.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
									.queue(s -> {
										this.message.compute(s.getChannel().getId(), (id, m) -> {
											if (m != null)
												m.delete().queue(null, Helper::doNothing);
											return s;
										});
										Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
									});
						}
						return;
					} else if (postCombat()) return;
				}

				attack(is);
			} catch (IndexOutOfBoundsException e) {
				channel.sendMessage("❌ | Índice inválido, escolha uma carta para usar no ataque e uma para ser atacada.").queue(null, Helper::doNothing);

			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser uma casa com uma carta no seu lado do tabuleiro e o segundo deve ser uma casa com uma carta no lado do inimigo.").queue(null, Helper::doNothing);
			}
		}
	}

	public void attack(int[] is) {
		Champion yours = getArena().getSlots().get(current).get(is[0]).getTop();
		Champion his = getArena().getSlots().get(next).get(is[1]).getTop();

		int yPower;
		if (!yours.getCard().getId().equals("DECOY")) {
			yPower = Math.round(
					yours.getFinAtk() *
					(arena.getField() == null || yours.getLinkedTo().stream().anyMatch(e -> e.getCharm() == Charm.SOULLINK) ? 1 : arena.getField().getModifiers().optFloat(yours.getRace().name(), 1f))
			);
		} else {
			yPower = 0;
		}

		int hPower;
		if (his.isDefending() || his.isFlipped() || his.getStun() > 0) {
			if (his.isFlipped()) {
				his.setFlipped(false);
				if (his.hasEffect() && effectLock == 0) {
					his.getEffect(new EffectParameters(EffectTrigger.ON_FLIP, this, is[1], next, Duelists.of(yours, is[0], his, is[1]), channel));
					if (postCombat()) return;
				}
			}
			hPower = Math.round(
					his.getFinDef() *
					(arena.getField() == null || his.getLinkedTo().stream().anyMatch(e -> e.getCharm() == Charm.SOULLINK) ? 1 : arena.getField().getModifiers().optFloat(his.getRace().name(), 1f))
			);
		} else if (!his.getCard().getId().equals("DECOY")) {
			hPower = Math.round(
					his.getFinAtk() *
					(arena.getField() == null || his.getLinkedTo().stream().anyMatch(e -> e.getCharm() == Charm.SOULLINK) ? 1 : arena.getField().getModifiers().optFloat(his.getRace().name(), 1f))
			);
		} else {
			hPower = 0;
		}

		if (yPower > hPower) {
			yours.setAvailable(false);
			yours.resetAttribs();

			if (yours.hasEffect() && effectLock == 0) {
				yours.getEffect(new EffectParameters(EffectTrigger.POST_ATTACK, this, is[0], current, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}
			if (his.hasEffect() && effectLock == 0) {
				his.getEffect(new EffectParameters(EffectTrigger.ON_DEATH, this, is[1], next, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				if (his.isDefending() && yours.getLinkedTo().stream().anyMatch(e -> e.getCharm() == Charm.ARMORPIERCING)) {
					int apDamage = yours.getLinkedTo().stream().filter(e -> e.getCharm() == Charm.ARMORPIERCING).mapToInt(Equipment::getAtk).sum();
					Hand enemy = getHands().get(next);
					enemy.removeHp(apDamage);
				} else if ((!his.isDefending() || his.getStun() > 0) && (getCustom() == null || !getCustom().optBoolean("semdano"))) {
					Hand enemy = getHands().get(next);
					if (yours.getBonus().getSpecialData().has("totalDamage"))
						enemy.removeHp(yPower);
					else
						enemy.removeHp(yPower - hPower);
				}
			}

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				killCard(next, is[1]);
				if (!postCombat()) {
					resetTimerKeepTurn();
					channel.sendMessage(yours.getName() + " derrotou " + his.getCard().getName() + "! (" + yPower + " > " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							});
				}
			} else if (yours.getCard().getId().equals("DECOY")) {
				resetTimerKeepTurn();
				channel.sendMessage(yours.getName() + " derrotou " + his.getCard().getName() + "? (" + yPower + " > " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			} else {
				killCard(next, is[1]);
				resetTimerKeepTurn();
				channel.sendMessage("Essa carta era na verdade uma isca!")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			}
		} else if (yPower < hPower) {
			yours.setAvailable(false);
			his.resetAttribs();

			if (yours.hasEffect() && effectLock == 0) {
				yours.getEffect(new EffectParameters(EffectTrigger.ON_SUICIDE, this, is[0], current, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}
			if (his.hasEffect() && effectLock == 0) {
				his.getEffect(new EffectParameters(EffectTrigger.POST_DEFENSE, this, is[1], next, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				if (yours.getBonus().getSpecialData().remove("noDamage") == null && (getCustom() == null || !getCustom().optBoolean("semdano"))) {
					Hand you = getHands().get(current);
					you.removeHp(hPower - yPower);
				}
			}

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				killCard(current, is[0]);
				if (!postCombat()) {
					resetTimerKeepTurn();
					channel.sendMessage(yours.getCard().getName() + " não conseguiu derrotar " + his.getName() + "! (" + yPower + " < " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							});
				}
			} else if (his.getCard().getId().equals("DECOY")) {
				killCard(current, is[0]);
				resetTimerKeepTurn();
				channel.sendMessage(yours.getName() + " não conseguiu derrotar " + his.getCard().getName() + "? (" + yPower + " > " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			} else {
				resetTimerKeepTurn();
				channel.sendMessage("Essa carta era na verdade uma isca!")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			}
		} else {
			yours.setAvailable(false);

			if (yours.hasEffect() && effectLock == 0) {
				yours.getEffect(new EffectParameters(EffectTrigger.ON_SUICIDE, this, is[0], current, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}
			if (his.hasEffect() && effectLock == 0) {
				his.getEffect(new EffectParameters(EffectTrigger.ON_DEATH, this, is[1], next, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				killCard(next, is[1]);
				killCard(current, is[0]);

				if (!postCombat()) {
					resetTimerKeepTurn();
					channel.sendMessage("As duas cartas foram destruidas! (" + yPower + " = " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							});
				}
			} else if (Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				killCard(next, is[1]);
				killCard(current, is[0]);
				resetTimerKeepTurn();
				channel.sendMessage("As duas cartas na verdade eram iscas! (" + yPower + " = " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			} else if (his.getCard().getId().equals("DECOY")) {
				killCard(next, is[1]);
				resetTimerKeepTurn();
				channel.sendMessage("As duas cartas foram destruidas? (" + yPower + " = " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			} else {
				killCard(current, is[0]);
				resetTimerKeepTurn();
				channel.sendMessage("As duas cartas foram destruidas? (" + yPower + " = " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			}
		}
	}

	public void forceAttack(int[] is) {
		Champion yours = getArena().getSlots().get(next).get(is[0]).getTop();
		Champion his = getArena().getSlots().get(current).get(is[1]).getTop();

		int yPower;
		if (!yours.getCard().getId().equals("DECOY")) {
			yPower = Math.round(
					yours.getFinAtk() *
					(arena.getField() == null || yours.getLinkedTo().stream().anyMatch(e -> e.getCharm() == Charm.SOULLINK) ? 1 : arena.getField().getModifiers().optFloat(yours.getRace().name(), 1f))
			);
		} else {
			yPower = 0;
		}

		int hPower;
		if (his.isDefending() || his.isFlipped() || his.getStun() > 0) {
			if (his.isFlipped()) {
				his.setFlipped(false);
				if (his.hasEffect() && effectLock == 0) {
					his.getEffect(new EffectParameters(EffectTrigger.ON_FLIP, this, is[1], current, Duelists.of(yours, is[0], his, is[1]), channel));
					if (postCombat()) return;
				}
			}
			hPower = Math.round(
					his.getFinDef() *
					(arena.getField() == null || his.getLinkedTo().stream().anyMatch(e -> e.getCharm() == Charm.SOULLINK) ? 1 : arena.getField().getModifiers().optFloat(his.getRace().name(), 1f))
			);
		} else if (!his.getCard().getId().equals("DECOY")) {
			hPower = Math.round(
					his.getFinAtk() *
					(arena.getField() == null || his.getLinkedTo().stream().anyMatch(e -> e.getCharm() == Charm.SOULLINK) ? 1 : arena.getField().getModifiers().optFloat(his.getRace().name(), 1f))
			);
		} else {
			hPower = 0;
		}

		if (yPower > hPower) {
			yours.setAvailable(false);
			yours.resetAttribs();

			if (yours.hasEffect() && effectLock == 0) {
				yours.getEffect(new EffectParameters(EffectTrigger.POST_ATTACK, this, is[0], next, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}
			if (his.hasEffect() && effectLock == 0) {
				his.getEffect(new EffectParameters(EffectTrigger.ON_DEATH, this, is[1], current, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				if (his.isDefending() && yours.getLinkedTo().stream().anyMatch(e -> e.getCharm() == Charm.ARMORPIERCING)) {
					int apDamage = yours.getLinkedTo().stream().filter(e -> e.getCharm() == Charm.ARMORPIERCING).mapToInt(Equipment::getAtk).sum();
					Hand enemy = getHands().get(current);
					enemy.removeHp(apDamage);
				} else if ((!his.isDefending() || his.getStun() > 0) && (getCustom() == null || !getCustom().optBoolean("semdano"))) {
					Hand enemy = getHands().get(current);
					enemy.removeHp(yPower - hPower);
				}
			}

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				killCard(current, is[1]);
				if (!postCombat()) {
					resetTimerKeepTurn();
					channel.sendMessage(yours.getName() + " derrotou " + his.getCard().getName() + "! (" + yPower + " > " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							});
				}
			} else if (yours.getCard().getId().equals("DECOY")) {
				resetTimerKeepTurn();
				channel.sendMessage(yours.getName() + " derrotou " + his.getCard().getName() + "? (" + yPower + " > " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			} else {
				resetTimerKeepTurn();
				channel.sendMessage("Essa carta era na verdade uma isca!")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			}
		} else if (yPower < hPower) {
			yours.setAvailable(false);
			his.resetAttribs();

			if (yours.hasEffect() && effectLock == 0) {
				yours.getEffect(new EffectParameters(EffectTrigger.ON_SUICIDE, this, is[0], next, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}
			if (his.hasEffect() && effectLock == 0) {
				his.getEffect(new EffectParameters(EffectTrigger.POST_DEFENSE, this, is[1], current, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				if (yours.getBonus().getSpecialData().remove("noDamage") == null && (getCustom() == null || !getCustom().optBoolean("semdano"))) {
					Hand you = getHands().get(next);
					you.removeHp(hPower - yPower);
				}
			}

			killCard(next, is[0]);
			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				if (!postCombat()) {
					resetTimerKeepTurn();
					channel.sendMessage(yours.getCard().getName() + " não conseguiu derrotar " + his.getName() + "! (" + yPower + " < " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							});
				}
			} else if (his.getCard().getId().equals("DECOY")) {
				resetTimerKeepTurn();
				channel.sendMessage(yours.getName() + " não conseguiu derrotar " + his.getCard().getName() + "? (" + yPower + " > " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			} else {
				resetTimerKeepTurn();
				channel.sendMessage("Essa carta era na verdade uma isca!")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			}
		} else {
			yours.setAvailable(false);

			if (yours.hasEffect() && effectLock == 0) {
				yours.getEffect(new EffectParameters(EffectTrigger.ON_SUICIDE, this, is[0], next, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}
			if (his.hasEffect() && effectLock == 0) {
				his.getEffect(new EffectParameters(EffectTrigger.ON_DEATH, this, is[1], current, Duelists.of(yours, is[0], his, is[1]), channel));
				if (postCombat()) return;
			}

			if (!Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				killCard(current, is[1]);
				killCard(next, is[0]);

				if (!postCombat()) {
					resetTimerKeepTurn();
					channel.sendMessage("As duas cartas foram destruidas! (" + yPower + " = " + hPower + ")")
							.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(s -> {
								this.message.compute(s.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return s;
								});
								Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							});
				}
			} else if (Helper.equalsAny("DECOY", yours.getCard().getId(), his.getCard().getId())) {
				killCard(current, is[1]);
				killCard(next, is[0]);
				resetTimerKeepTurn();
				channel.sendMessage("As duas cartas na verdade eram iscas! (" + yPower + " = " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			} else if (his.getCard().getId().equals("DECOY")) {
				killCard(current, is[1]);
				resetTimerKeepTurn();
				channel.sendMessage("As duas cartas foram destruidas? (" + yPower + " = " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			} else {
				killCard(next, is[0]);
				resetTimerKeepTurn();
				channel.sendMessage("As duas cartas foram destruidas? (" + yPower + " = " + hPower + ")")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			}
		}
	}

	private boolean makeFusion(Hand h) {
		if (fusionLock > 0) return false;
		List<Champion> champsInField = arena.getSlots().get(h.getSide())
				.stream()
				.map(SlotColumn::getTop)
				.collect(Collectors.toList());

		List<Equipment> equipsInField = arena.getSlots().get(h.getSide())
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

			if (getArena().getField() != null)
				add(getArena().getField().getCard().getId());
		}};

		Champion aFusion = fusions
				.stream()
				.filter(f ->
						f.getRequiredCards().size() > 0 &&
						allCards.containsAll(f.getRequiredCards()) &&
						h.getMana() >= f.getMana()
				)
				.findFirst()
				.orElse(null);

		if (aFusion != null) {
			List<SlotColumn<Champion, Equipment>> slts = arena.getSlots().get(h.getSide());

			for (String requiredCard : aFusion.getRequiredCards()) {
				for (int i = 0; i < slts.size(); i++) {
					SlotColumn<Champion, Equipment> column = slts.get(i);
					if (column.getTop() != null && column.getTop().getCard().getId().equals(requiredCard)) {
						banishCard(h.getSide(), i, false);
						break;
					} else if (column.getBottom() != null && column.getBottom().getCard().getId().equals(requiredCard)) {
						banishCard(h.getSide(), i, true);
						break;
					}
				}
			}

			for (int i = 0; i < slts.size(); i++) {
				SlotColumn<Champion, Equipment> slt = slts.get(i);
				if (slt.getTop() == null) {
					aFusion.setAcc(AccountDAO.getAccount(h.getUser().getId()));
					slt.setTop(aFusion.copy());
					if (aFusion.hasEffect() && effectLock == 0) {
						aFusion.getEffect(new EffectParameters(EffectTrigger.ON_SUMMON, this, i, h.getSide(), Duelists.of(aFusion, i, null, -1), channel));
						if (postCombat()) return true;
					}

					h.removeMana(aFusion.getMana());
					break;
				}
			}

			return makeFusion(h);
		}
		return false;
	}

	public void killCard(Side side, int index) {
		Champion ch = getArena().getSlots().get(side).get(index).getTop();
		if (ch == null || (ch.getBonus().getSpecialData().optBoolean("preventDeath"))) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(side);

		slts.get(index).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn<Champion, Equipment> sd = slts.get(i);
			if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == index)
				killCard(side, i);

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == index)
				unequipCard(side, i, slts);
		}

		ch.reset();
		if (!ch.isFusion())
			arena.getGraveyard().get(side).add(ch);
	}

	public void destroyCard(Side side, int index, int source) {
		Champion ch = getArena().getSlots().get(side).get(index).getTop();
		if (ch == null) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(side);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				if (eq.getCharm() == Charm.SPELLSHIELD) {
					unequipCard(side, i, slts);
					return;
				} else if (eq.getCharm() == Charm.SPELLMIRROR && source != -1) {
					destroyCard(side == Side.TOP ? Side.BOTTOM : Side.TOP, source, index);
					unequipCard(side, i, slts);
					return;
				}
			}
		}

		slts.get(index).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn<Champion, Equipment> sd = slts.get(i);
			if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == index)
				killCard(side, i);

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == index)
				unequipCard(side, i, slts);
		}

		ch.reset();
		if (!ch.isFusion())
			arena.getGraveyard().get(side).add(ch);
	}

	public void destroyCard(Side side, int index) {
		Champion ch = getArena().getSlots().get(side).get(index).getTop();
		if (ch == null) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(side);

		slts.get(index).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn<Champion, Equipment> sd = slts.get(i);
			if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == index)
				killCard(side, i);

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == index)
				unequipCard(side, i, slts);
		}

		ch.reset();
		if (!ch.isFusion())
			arena.getGraveyard().get(side).add(ch);
	}

	public void captureCard(Side side, int index, int source) {
		Champion ch = getArena().getSlots().get(side).get(index).getTop();
		if (ch == null) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(side);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				if (eq.getCharm() == Charm.SPELLSHIELD) {
					unequipCard(side, i, slts);
					return;
				} else if (eq.getCharm() == Charm.SPELLMIRROR && source != -1) {
					destroyCard(side == Side.TOP ? Side.BOTTOM : Side.TOP, source, index);
					unequipCard(side, i, slts);
					return;
				}
			}
		}

		slts.get(index).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn<Champion, Equipment> sd = slts.get(i);
			if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == index)
				killCard(side, i);

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == index)
				unequipCard(side, i, slts);
		}

		ch.reset();
		if (!ch.isFusion())
			getHands().get(side == Side.TOP ? Side.BOTTOM : Side.TOP).getCards().add(ch);
	}

	public void banishCard(Side side, int index, boolean equipment) {
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(side);
		if (equipment) {
			Equipment eq = slts.get(index).getBottom();
			if (eq == null) return;

			if (slts.get(eq.getLinkedTo().getLeft()).getTop() != null)
				slts.get(eq.getLinkedTo().getLeft()).getTop().removeLinkedTo(eq);
			eq.setLinkedTo(null);

			SlotColumn<Champion, Equipment> sd = slts.get(index);
			arena.getBanished().add(eq);
			sd.setBottom(null);
		} else {
			Champion ch = slts.get(index).getTop();
			if (ch == null) return;

			slts.get(index).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn<Champion, Equipment> sd = slts.get(i);
				if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == index)
					killCard(side, i);

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == index)
					banishCard(side, i, true);
			}

			ch.reset();
			if (!ch.isFusion())
				arena.getBanished().add(ch);
		}
	}

	public void unequipCard(Side s, int index, List<SlotColumn<Champion, Equipment>> side) {
		Equipment eq = side.get(index).getBottom();
		if (eq == null) return;

		if (side.get(eq.getLinkedTo().getLeft()).getTop() != null)
			side.get(eq.getLinkedTo().getLeft()).getTop().removeLinkedTo(eq);
		eq.setLinkedTo(null);

		SlotColumn<Champion, Equipment> sd = side.get(index);
		arena.getGraveyard().get(s).add(eq);
		sd.setBottom(null);
	}

	public Arena getArena() {
		return arena;
	}

	public Map<Side, Hand> getHands() {
		return hands;
	}

	public SlotColumn<Champion, Equipment> getFirstAvailableSlot(Side s, boolean top) {
		for (SlotColumn<Champion, Equipment> slot : arena.getSlots().get(s)) {
			if (top ? slot.getTop() == null : slot.getBottom() == null)
				return slot;
		}
		return null;
	}

	public void convertCard(Side side, int index, int source) {
		Side his = side == Side.TOP ? Side.BOTTOM : Side.TOP;
		Champion ch = getArena().getSlots().get(his).get(index).getTop();
		if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventConvert")) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(his);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				if (eq.getCharm() == Charm.SPELLSHIELD) {
					unequipCard(his, i, slts);
					return;
				} else if (eq.getCharm() == Charm.SPELLMIRROR && source != -1) {
					convertCard(his, source, index);
					unequipCard(his, i, slts);
					return;
				}
			}
		}

		SlotColumn<Champion, Equipment> sc = getFirstAvailableSlot(side, true);
		if (sc != null) {
			ch.clearLinkedTo();
			ch.setAcc(AccountDAO.getAccount(getHands().get(side).getUser().getId()));
			sc.setTop(ch);
			slts.get(index).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn<Champion, Equipment> sd = slts.get(i);
				if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == index)
					killCard(side, i);

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == index)
					unequipCard(his, i, slts);
			}
		}
	}

	public void switchCards(Side side, int index, int source) {
		Side his = side == Side.TOP ? Side.BOTTOM : Side.TOP;
		Champion ch = getArena().getSlots().get(his).get(index).getTop();
		if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventConvert")) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(his);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				if (eq.getCharm() == Charm.SPELLSHIELD) {
					unequipCard(his, i, slts);
					return;
				}
			}
		}

		ch.clearLinkedTo();
		ch.setAcc(AccountDAO.getAccount(getHands().get(side).getUser().getId()));
		slts.get(index).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn<Champion, Equipment> sd = slts.get(i);
			if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == index)
				killCard(side, i);

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == index)
				unequipCard(his, i, slts);
		}

		Champion yours = getArena().getSlots().get(side).get(source).getTop();
		List<SlotColumn<Champion, Equipment>> slots = getArena().getSlots().get(side);

		yours.clearLinkedTo();
		yours.setAcc(AccountDAO.getAccount(getHands().get(his).getUser().getId()));
		slots.get(source).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn<Champion, Equipment> sd = slts.get(i);
			if (sd.getTop() != null && sd.getTop().getCard().getId().equals("DECOY") && sd.getTop().getBonus().getSpecialData().getInt("original") == index)
				killCard(side, i);

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == index)
				unequipCard(his, i, slts);
		}

		slts.get(index).setTop(yours);
		slots.get(source).setTop(ch);
	}

	public void convertEquipments(Champion target, int pos, Side side, int index) {
		Side his = side == Side.TOP ? Side.BOTTOM : Side.TOP;
		Champion ch = getArena().getSlots().get(his).get(index).getTop();
		if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventConvert")) return;
		List<SlotColumn<Champion, Equipment>> slts = getArena().getSlots().get(his);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				if (eq.getCharm() == Charm.SPELLSHIELD) {
					unequipCard(his, i, slts);
					return;
				}
			}
		}

		for (int i = 0; i < 5; i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				SlotColumn<Champion, Equipment> sc = getFirstAvailableSlot(side, false);
				if (sc != null) {
					ch.removeLinkedTo(eq);
					slts.get(i).setBottom(null);

					target.addLinkedTo(eq);
					eq.setLinkedTo(Pair.of(pos, target));
					eq.setAcc(AccountDAO.getAccount(getHands().get(side).getUser().getId()));
					sc.setBottom(eq);
				} else return;
			}
		}
	}

	public boolean postCombat() {
		AtomicBoolean finished = new AtomicBoolean(false);
		for (Map.Entry<Side, Hand> entry : getHands().entrySet()) {
			Side s = entry.getKey();
			Hand h = entry.getValue();
			if (!finished.get()) {
				Hand op = getHands().get(s == Side.TOP ? Side.BOTTOM : Side.TOP);
				if (h.getHp() == 0) {
					if (getCustom() == null) {
						getHistory().setWinner(op.getSide());
						getBoard().awardWinner(this, daily, op.getUser().getId());
					} else close();
					finished.set(true);
					channel.sendMessage(op.getUser().getAsMention() + " zerou os pontos de vida de " + h.getUser().getAsMention() + ", temos um vencedor! (" + getRound() + " turnos)")
							.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
							.queue(msg ->
									this.message.compute(msg.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return msg;
									}));
				}
			}
		}
		return finished.get();
	}

	@Override
	public Map<String, BiConsumer<Member, Message>> getButtons() {
		AtomicReference<String> hash = new AtomicReference<>(Helper.generateHash(this));
		ShiroInfo.getHashes().add(hash.get());

		Map<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
		if (getRound() < 1 || phase == Phase.ATTACK)
			buttons.put("▶️", (mb, ms) -> {
				if (!ShiroInfo.getHashes().remove(hash.get())) return;
				User u = getCurrent();

				AtomicReference<Hand> h = new AtomicReference<>(getHands().get(current));
				h.get().getCards().removeIf(d -> !d.isAvailable());
				List<SlotColumn<Champion, Equipment>> slots = arena.getSlots().get(h.get().getSide());
				for (int i = 0; i < slots.size(); i++) {
					Champion c = slots.get(i).getTop();
					if (c != null) {
						if (c.getStun() == 0)
							c.setAvailable(true);

						c.resetAttribs();
						if (c.hasEffect() && effectLock == 0) {
							c.getEffect(new EffectParameters(EffectTrigger.AFTER_TURN, this, i, h.get().getSide(), Duelists.of(c, i, null, -1), channel));
							if (postCombat()) return;
							else if (makeFusion(h.get())) return;
						}
					}
				}

				arena.getGraveyard().get(h.get().getSide()).addAll(discardBatch);
				discardBatch.clear();
				resetTimer(this);

				phase = Phase.PLAN;
				h.set(getHands().get(current));
				h.get().decreaseSuppression();
				slots = arena.getSlots().get(h.get().getSide());
				for (int i = 0; i < slots.size(); i++) {
					Champion c = slots.get(i).getTop();
					if (c != null) {
						if (c.getStun() > 0) {
							c.reduceStun();
							c.setDefending(true);
						}

						if (c.hasEffect() && effectLock == 0) {
							c.getEffect(new EffectParameters(EffectTrigger.BEFORE_TURN, this, i, h.get().getSide(), Duelists.of(c, i, null, -1), channel));
							if (postCombat()) return;
							else if (makeFusion(h.get())) return;
						}
					}
				}
				h.get().decreaseLockTime();
				h.get().addMana(h.get().getManaPerTurn());
				AtomicBoolean shownHand = new AtomicBoolean(false);
				channel.sendMessage(u.getName() + " encerrou o turno, agora é sua vez " + getCurrent().getAsMention())
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							if (!shownHand.get()) {
								shownHand.set(true);
								h.get().showHand();
							}
							for (int i = 0; i < 5; i++) {
								changed[i] = false;
							}
						});
			});
		else
			buttons.put("▶️", (mb, ms) -> {
				if (!ShiroInfo.getHashes().remove(hash.get())) return;
				phase = Phase.ATTACK;
				draw = false;
				resetTimerKeepTurn();
				channel.sendMessage("**FASE DE ATAQUE:** Escolha uma carta do seu lado e uma carta do lado inimigo para iniciar combate")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			});
		buttons.put("\uD83D\uDCE4", (mb, ms) -> {
			if (!ShiroInfo.getHashes().remove(hash.get())) return;
			if (phase != Phase.PLAN) {
				channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
				return;
			}

			Hand h = getHands().get(current);

			int remaining = 5 - h.getCards().size();

			if (remaining <= 0) {
				channel.sendMessage("❌ | Você não pode puxar mais cartas se tiver 5 ou mais na sua mão.").queue(null, Helper::doNothing);
				return;
			}

			if (!h.manualDraw()) {
				if (getCustom() == null) {
					getHistory().setWinner(next);
					getBoard().awardWinner(this, daily, getBoard().getPlayers().get(1).getId());
				} else close();
				channel.sendMessage(getCurrent().getAsMention() + " não possui mais cartas no deck, " + getPlayerById(getBoard().getPlayers().get(1).getId()).getAsMention() + " venceu! (" + getRound() + " turnos)")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(msg ->
								this.message.compute(msg.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return msg;
								}));
				return;
			}

			remaining = 5 - h.getCards().size();
			resetTimerKeepTurn();
			AtomicBoolean shownHand = new AtomicBoolean(false);
			channel.sendMessage(getCurrent().getName() + " puxou uma carta (" + (remaining == 0 ? "não pode puxar mais cartas" : "pode puxar mais " + remaining + " carta" + (remaining == 1 ? "" : "s")) + ")")
					.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
					.queue(s -> {
						this.message.compute(s.getChannel().getId(), (id, m) -> {
							if (m != null)
								m.delete().queue(null, Helper::doNothing);
							return s;
						});
						Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						if (!shownHand.get()) {
							shownHand.set(true);
							h.showHand();
						}
					});
		});
		if (getHands().get(current).getHp() < 1500 && getHands().get(current).getDestinyDeck().size() > 0)
			buttons.put("\uD83E\uDDE7", (mb, ms) -> {
				if (!ShiroInfo.getHashes().remove(hash.get())) return;
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = getHands().get(current);
				h.destinyDraw();

				resetTimerKeepTurn();
				AtomicBoolean shownHand = new AtomicBoolean(false);
				channel.sendMessage(getCurrent().getName() + " executou um saque do destino!")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							if (!shownHand.get()) {
								shownHand.set(true);
								h.showHand();
							}
						});
			});
		buttons.put("\uD83E\uDD1D", (mb, ms) -> {
			if (!ShiroInfo.getHashes().remove(hash.get())) return;
			if (draw) {
				close();
				channel.sendMessage("Por acordo mútuo, declaro empate! (" + getRound() + " turnos)")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(msg ->
								this.message.compute(msg.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return msg;
								}));
			} else {
				User u = getCurrent();

				AtomicReference<Hand> h = new AtomicReference<>(getHands().get(current));
				h.get().getCards().removeIf(d -> !d.isAvailable());
				List<SlotColumn<Champion, Equipment>> slots = arena.getSlots().get(h.get().getSide());
				for (SlotColumn<Champion, Equipment> slot : slots) {
					Champion c = slot.getTop();
					if (c != null) {
						if (c.getStun() == 0)
							c.setAvailable(true);

						c.resetAttribs();
					}
				}

				resetTimer(this);

				phase = Phase.PLAN;
				h.set(getHands().get(current));
				h.get().decreaseSuppression();
				slots = arena.getSlots().get(h.get().getSide());
				for (int i = 0; i < slots.size(); i++) {
					Champion c = slots.get(i).getTop();
					if (c != null) {
						if (c.getStun() > 0) {
							c.reduceStun();
							c.setDefending(true);
						}

						if (c.hasEffect() && effectLock == 0) {
							c.getEffect(new EffectParameters(EffectTrigger.BEFORE_TURN, this, i, h.get().getSide(), Duelists.of(c, i, null, -1), channel));
							if (postCombat()) return;
							else if (makeFusion(h.get())) return;
						}
					}
				}
				h.get().decreaseLockTime();
				h.get().addMana(h.get().getManaPerTurn());
				AtomicBoolean shownHand = new AtomicBoolean(false);
				draw = true;
				channel.sendMessage(u.getName() + " deseja um acordo de empate, " + getCurrent().getAsMention() + " agora é sua vez, clique em \uD83E\uDD1D caso queira aceitar ou continue jogando normalmente.")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(s -> {
							this.message.compute(s.getChannel().getId(), (id, m) -> {
								if (m != null)
									m.delete().queue(null, Helper::doNothing);
								return s;
							});
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
							if (!shownHand.get()) {
								shownHand.set(true);
								h.get().showHand();
							}
							for (int i = 0; i < 5; i++) {
								changed[i] = false;
							}
						});
			}
		});
		if (getRound() > 8)
			buttons.put("\uD83C\uDFF3️", (mb, ms) -> {
				if (!ShiroInfo.getHashes().remove(hash.get())) return;
				if (getCustom() == null) {
					getHistory().setWinner(next);
					getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
				} else close();
				channel.sendMessage(getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)")
						.addFile(Helper.getBytes(arena.render(this, hands), "jpg", 0.5f), "board.jpg")
						.queue(msg ->
								this.message.compute(msg.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return msg;
								}));
			});

		return buttons;
	}

	private void recordLast() {
		Hand top = getHands().get(Side.TOP);
		Hand bot = getHands().get(Side.BOTTOM);
		getHistory().getRound(getRound() + 1).setSide(current);
		getHistory().getRound(getRound() + 1).setScript(new JSONObject() {{
			put("top", new JSONObject() {{
				put("id", top.getUser().getId());
				put("hp", top.getHp());
				put("mana", top.getMana());
				put("champions", getArena().getSlots().get(Side.TOP)
						.stream()
						.map(SlotColumn::getTop)
						.filter(Objects::nonNull)
						.count()
				);
				put("equipments", getArena().getSlots().get(Side.TOP)
						.stream()
						.map(SlotColumn::getBottom)
						.filter(Objects::nonNull)
						.count()
				);
				put("inHand", top.getCards().size());
				put("deck", top.getDeque().size());
			}});

			put("bottom", new JSONObject() {{
				put("id", bot.getUser().getId());
				put("hp", bot.getHp());
				put("mana", bot.getMana());
				put("champions", getArena().getSlots().get(Side.BOTTOM)
						.stream()
						.map(SlotColumn::getTop)
						.filter(Objects::nonNull)
						.count()
				);
				put("equipments", getArena().getSlots().get(Side.BOTTOM)
						.stream()
						.map(SlotColumn::getBottom)
						.filter(Objects::nonNull)
						.count()
				);
				put("inHand", bot.getCards().size());
				put("deck", bot.getDeque().size());
			}});
		}});
	}

	public void banishWeaklings(int threshold) {
		for (int i = 0; i < 5; i++) {
			Champion c = getArena().getSlots().get(Side.TOP).get(i).getTop();
			if (c != null && c.getMana() <= threshold && !c.isFusion())
				banishCard(
						Side.TOP,
						i,
						false
				);

			c = getArena().getSlots().get(Side.BOTTOM).get(i).getTop();
			if (c != null && c.getMana() <= threshold && !c.isFusion())
				banishCard(
						Side.BOTTOM,
						i,
						false
				);
		}

		for (Map.Entry<Side, LinkedList<Drawable>> entry : getArena().getGraveyard().entrySet()) {
			Hand h = getHands().get(entry.getKey());
			List<Champion> dead = entry.getValue().stream()
					.filter(d -> d instanceof Champion && ((Champion) d).getMana() <= threshold)
					.map(d -> (Champion) d)
					.collect(Collectors.toList());

			for (Champion c : dead) {
				entry.getValue().remove(c);
			}

			List<Champion> inHand = h.getCards().stream()
					.filter(d -> d instanceof Champion && ((Champion) d).getMana() <= threshold)
					.map(d -> (Champion) d)
					.collect(Collectors.toList());

			for (Champion c : inHand) {
				h.getCards().remove(c);
			}

			List<Champion> inDeck = h.getDeque().stream()
					.filter(d -> d instanceof Champion && ((Champion) d).getMana() <= threshold)
					.map(d -> (Champion) d)
					.collect(Collectors.toList());

			for (Champion c : inDeck) {
				h.getDeque().remove(c);
			}

			getArena().getBanished().addAll(dead);
			getArena().getBanished().addAll(inHand);
			getArena().getBanished().addAll(inDeck);
		}
	}

	public void dmUser(Hand h, String message) {
		h.getUser().openPrivateChannel()
				.flatMap(c -> c.sendMessage(message))
				.queue(null, Helper::doNothing);
	}

	public Champion getChampionFromGrave(Side s) {
		LinkedList<Drawable> grv = getArena().getGraveyard().get(s);
		for (int i = grv.size() - 1; i >= 0; i--)
			if (grv.get(i) instanceof Champion)
				return (Champion) grv.remove(i);

		return null;
	}

	public Equipment getEquipmentFromGrave(Side s) {
		LinkedList<Drawable> grv = getArena().getGraveyard().get(s);
		for (int i = grv.size() - 1; i >= 0; i--)
			if (grv.get(i) instanceof Equipment)
				return (Equipment) grv.remove(i);

		return null;
	}

	public Champion getChampionFromBanished() {
		LinkedList<Drawable> grv = getArena().getBanished();
		for (int i = grv.size() - 1; i >= 0; i--)
			if (grv.get(i) instanceof Champion)
				return (Champion) grv.remove(i);

		return null;
	}

	public Equipment getEquipmentFromBanished() {
		LinkedList<Drawable> grv = getArena().getBanished();
		for (int i = grv.size() - 1; i >= 0; i--)
			if (grv.get(i) instanceof Equipment)
				return (Equipment) grv.remove(i);

		return null;
	}

	public int getFusionLock() {
		return fusionLock;
	}

	public void addFLockTime(int time) {
		fusionLock += time;
	}

	public void decreaseFLockTime() {
		fusionLock = Math.max(0, fusionLock - 1);
	}

	public int getSpellLock() {
		return spellLock;
	}

	public void addSLockTime(int time) {
		spellLock += time;
	}

	public void decreaseSLockTime() {
		spellLock = Math.max(0, spellLock - 1);
	}

	public int getEffectLock() {
		return effectLock;
	}

	public void addELockTime(int time) {
		effectLock += time;
	}

	public void decreaseELockTime() {
		effectLock = Math.max(0, effectLock - 1);
	}

	public List<Drawable> getDiscardBatch() {
		return discardBatch;
	}

	@Override
	public void close() {
		listener.close();
		recordLast();
		super.close();
	}

	@Override
	public void resetTimer(Shoukan shkn) {
		getCurrRound().setSide(current);
		decreaseFLockTime();
		decreaseSLockTime();
		decreaseELockTime();
		super.resetTimer(shkn);

		current = next;
		next = current == Side.TOP ? Side.BOTTOM : Side.TOP;
	}
}
