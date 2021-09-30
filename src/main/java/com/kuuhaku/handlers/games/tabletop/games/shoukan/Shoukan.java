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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.api.websocket.EncoderClient;
import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.GameChannel;
import com.kuuhaku.handlers.games.tabletop.framework.GlobalGame;
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.DailyQuest;
import com.kuuhaku.model.enums.Achievement;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.model.persistent.tournament.Tournament;
import com.kuuhaku.model.records.TournamentMatch;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.websocket.DeploymentException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger.*;

public class Shoukan extends GlobalGame {
	private final Map<Side, Hand> hands;
	private final Map<Side, Pair<Race, Race>> combos;
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
	private final boolean team;
	private final boolean record;
	private final Map<Side, EnumSet<Achievement>> achievements = new HashMap<>();
	private final Map<Side, Map<Race, Integer>> summoned = Map.of(
			Side.TOP, new HashMap<>(),
			Side.BOTTOM, new HashMap<>()
	);
	private final Set<PersistentEffect> persistentEffects = new HashSet<>();
	private final TournamentMatch tourMatch;

	private Phase phase = Phase.PLAN;
	private boolean draw = false;
	private int fusionLock = 0;
	private int spellLock = 0;
	private int effectLock = 0;
	private final List<Drawable> discardBatch = new ArrayList<>();
	private boolean reroll = true;
	private boolean moveLock = false;

	public Shoukan(ShardManager handler, GameChannel channel, int bet, JSONObject custom, boolean daily, boolean ranked, boolean record, TournamentMatch match, User... players) {
		super(handler, new Board(BoardSize.S_NONE, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel, ranked, custom);
		this.channel = channel;
		this.team = players.length == 4;
		this.record = record;
		this.tourMatch = match;

		if (team) {
			List<Deck> kps = daily ?
					Collections.nCopies(4, Helper.getDailyDeck()) :
					List.of(
							KawaiponDAO.getKawaipon(players[2].getId()).getDeck(),
							KawaiponDAO.getKawaipon(players[0].getId()).getDeck(),
							KawaiponDAO.getKawaipon(players[3].getId()).getDeck(),
							KawaiponDAO.getKawaipon(players[1].getId()).getDeck()
					);

			this.hands = Map.of(
					Side.TOP, new TeamHand(this, List.of(players[2], players[0]), kps.subList(0, 2), Side.TOP),
					Side.BOTTOM, new TeamHand(this, List.of(players[3], players[1]), kps.subList(2, 4), Side.BOTTOM)
			);
		} else {
			Deck p1 = daily ? Helper.getDailyDeck() : KawaiponDAO.getKawaipon(players[0].getId()).getDeck();
			Deck p2 = daily ? Helper.getDailyDeck() : KawaiponDAO.getKawaipon(players[1].getId()).getDeck();

			this.hands = Map.of(
					Side.TOP, new Hand(this, players[0], p1, Side.TOP),
					Side.BOTTOM, new Hand(this, players[1], p2, Side.BOTTOM)
			);
		}
		this.combos = Map.of(
				Side.TOP, hands.get(Side.TOP).getCombo(),
				Side.BOTTOM, hands.get(Side.BOTTOM).getCombo()
		);

		if (custom == null) {
			getHistory().setPlayers(Map.of(
					players[0].getId(), Side.TOP,
					players[1].getId(), Side.BOTTOM
			));
		} else if (custom.has("test")) {
			for (Object o : custom.getJSONArray("test")) {
				String id = String.valueOf(o);
				Drawable d;
				d = CardDAO.getChampion(id);
				if (d == null) d = CardDAO.getEquipment(id);
				if (d == null) d = CardDAO.getField(id);
				if (d == null) continue;

				for (Hand h : hands.values())
					h.getCards().add(d.copy());
			}
		}

		if (ranked)
			for (Map.Entry<Side, Hand> e : hands.entrySet()) {
				Set<Achievement> achs = e.getValue().getAcc().getAchievements();
				if (!achs.isEmpty())
					achievements.put(e.getKey(), EnumSet.complementOf(EnumSet.copyOf(achs)));
				else
					achievements.put(e.getKey(), EnumSet.allOf(Achievement.class));
			}

		setActions(
				s -> {
					close();
					channel.sendFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
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

						setWo();
						getHistory().setWinner(getNextSide());
						getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
						close();
					}
					channel.sendFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
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
		Hand h = hands.get(getCurrentSide());
		h.addMana(h.getManaPerTurn());
		if (h.getCombo().getRight() == Race.BESTIAL)
			h.addMana(1);

		AtomicBoolean shownHand = new AtomicBoolean(false);
		AtomicReference<String> previous = new AtomicReference<>("");
		channel.sendMessage(getCurrent().getAsMention() + " você começa! (Olhe as mensagens privadas)")
				.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
				.queue(s -> {
					this.message.put(s.getChannel().getId(), s);
					if (!s.getGuild().getId().equals(previous.get())) {
						previous.set(s.getGuild().getId());
						ShiroInfo.getShiroEvents().addHandler(s.getGuild(), listener);
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
		Predicate<GuildMessageReceivedEvent> condition = e -> channel.getChannels().parallelStream().anyMatch(g -> g != null && e.getChannel().getId().equals(g.getId()));

		return condition
				.and(e -> e.getAuthor().getId().equals(getCurrent().getId()))
				.and(e -> {
					String[] args = e.getMessage().getContentRaw().split(",");
					return (args.length > 0 && StringUtils.isNumeric(args[0])) || e.getMessage().getContentRaw().equalsIgnoreCase("reload");
				})
				.and(e -> isOpen())
				.and(e -> !moveLock)
				.test(evt);
	}

	@Override
	public synchronized void play(GuildMessageReceivedEvent evt) {
		Message message = evt.getMessage();
		String cmd = message.getContentRaw();
		Hand h = hands.get(getCurrentSide());

		if (cmd.equalsIgnoreCase("reload")) {
			reportEvent(h, message.getAuthor().getName() + " recriou a mensagem do jogo.", false, false);
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
				List<SlotColumn> slots = arena.getSlots().get(getCurrentSide());
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
					} else if (c.isStasis()) {
						channel.sendMessage("❌ | Essa carta está inalvejável.").queue(null, Helper::doNothing);
						return;
					} else if (c.isStunned()) {
						channel.sendMessage("❌ | Essa carta está atordoada.").queue(null, Helper::doNothing);
						return;
					} else if (c.isSleeping()) {
						channel.sendMessage("❌ | Essa carta está dormindo.").queue(null, Helper::doNothing);
						return;
					}

					if (applyPersistentEffects(ON_SWITCH, getCurrentSide(), index)) return;
					if (applyEffect(ON_SWITCH, c, index, getCurrentSide(), Pair.of(c, index), null)) return;

					String msg;
					if (c.isFlipped()) {
						c.setFlipped(false);
						c.setDefending(true);
						msg = "Carta virada para cima em modo de defesa.";
					} else if (c.isDefending()) {
						c.setDefending(false);
						msg = "Carta trocada para modo de ataque.";
					} else {
						c.setDefending(true);
						msg = "Carta trocada para modo de defesa.";
					}

					changed[index] = true;
					reportEvent(h, msg, true, false);
					return;
				}

				Drawable d = h.getCards().get(index);

				if (!d.isAvailable()) {
					channel.sendMessage("❌ | Essa carta já foi jogada neste turno.").queue(null, Helper::doNothing);
					return;
				}

				if (args[1].equalsIgnoreCase("d") && args.length < 3) {
					discardBatch.add(d);
					d.setAvailable(false);

					if (makeFusion(h)) return;

					reportEvent(h, h.getUser().getName() + " descartou a carta " + d.getCard().getName() + ".", true, false);
					return;
				}

				String msg;
				if (d instanceof Equipment) {
					Equipment e = (Equipment) d.deepCopy();

					if (e.getCharm() != null && e.getCharm().equals(Charm.SPELL)) {
						if (!args[1].equalsIgnoreCase("s")) {
							channel.sendMessage("❌ | O segundo argumento precisa ser `S` se deseja jogar uma carta de feitiço.").queue(null, Helper::doNothing);
							return;
						} else if (spellLock > 0) {
							channel.sendMessage("❌ | Feitiços estão bloqueados por mais " + (fusionLock == 1 ? "turno" : "turnos") + ".").queue(null, Helper::doNothing);
							return;
						} else if (!h.isNullMode() && (h.getMana() < e.getMana())) {
							channel.sendMessage("❌ | Você não tem mana suficiente para usar essa magia, encerre o turno reagindo com :arrow_forward: ou jogue cartas de equipamento ou campo.").queue(null, Helper::doNothing);
							return;
						} else if (h.getHp() <= e.getBlood()) {
							channel.sendMessage("❌ | Você não tem HP suficiente para usar essa magia, encerre o turno reagindo com :arrow_forward: ou jogue cartas de equipamento ou campo.").queue(null, Helper::doNothing);
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
								List<SlotColumn> eSlots = arena.getSlots().get(getNextSide());
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
								List<SlotColumn> eSlots = arena.getSlots().get(getNextSide());
								target = eSlots.get(pos2).getTop();

								if (target == null) {
									channel.sendMessage("❌ | Não existe uma carta na segunda casa.").queue(null, Helper::doNothing);
									return;
								}

								enemyPos = Pair.of(target, pos2);
							}
						}

						reroll = false;
						d.setAvailable(false);
						h.removeMana(e.getMana());
						h.removeHp(e.getBlood());
						e.activate(h, hands.get(getNextSide()), this, allyPos == null ? -1 : allyPos.getRight(), enemyPos == null ? -1 : enemyPos.getRight());
						if (e.getTier() >= 4)
							arena.getBanished().add(e.copy());
						else
							arena.getGraveyard().get(getCurrentSide()).add(e.copy());

						if (makeFusion(h)) return;

						String result = switch (e.getArgType()) {
							case NONE -> h.getUser().getName() + " usou o feitiço " + d.getCard().getName() + ".";
							case ALLY -> {
								assert allyPos != null;
								if (allyPos.getLeft().isFlipped()) {
									allyPos.getLeft().setFlipped(false);
									allyPos.getLeft().setDefending(true);
								}

								yield "%s usou o feitiço %s em %s.".formatted(
										h.getUser().getName(),
										d.getCard().getName(),
										allyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : allyPos.getLeft().getName()
								);
							}
							case ENEMY -> {
								assert enemyPos != null;
								if (enemyPos.getLeft().isFlipped()) {
									enemyPos.getLeft().setFlipped(false);
									enemyPos.getLeft().setDefending(true);
								}

								yield "%s usou o feitiço %s em %s.".formatted(
										h.getUser().getName(),
										d.getCard().getName(),
										enemyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : enemyPos.getLeft().getName()
								);
							}
							case BOTH -> {
								assert allyPos != null && enemyPos != null;
								if (allyPos.getLeft().isFlipped()) {
									allyPos.getLeft().setFlipped(false);
									allyPos.getLeft().setDefending(true);
								}
								if (enemyPos.getLeft().isFlipped()) {
									enemyPos.getLeft().setFlipped(false);
									enemyPos.getLeft().setDefending(true);
								}

								yield "%s usou o feitiço %s em %s e %s.".formatted(
										h.getUser().getName(),
										d.getCard().getName(),
										allyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : allyPos.getLeft().getName(),
										enemyPos.getLeft().isFlipped() ? "uma carta virada para baixo" : enemyPos.getLeft().getName()
								);
							}
						};

						if (!postCombat()) reportEvent(h, result, true, false);
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
					SlotColumn slot = slots.get(dest);

					if (slot.getBottom() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					}

					if (!StringUtils.isNumeric(args[2])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma carta para equipar esse equipamento.").queue(null, Helper::doNothing);
						return;
					}
					int toEquip = Integer.parseInt(args[2]) - 1;

					SlotColumn target = slots.get(toEquip);

					if (target.getTop() == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					}

					reroll = false;
					d.setAvailable(false);
					slot.setBottom(e);
					Champion t = target.getTop();
					if (t.isFlipped()) {
						t.setFlipped(false);
						t.setDefending(true);
					}
					t.addLinkedTo(e);
					e.setLinkedTo(Pair.of(toEquip, t));
					if (applyPersistentEffects(ON_EQUIP, getCurrentSide(), toEquip)) return;
					if (applyEffect(ON_EQUIP, t, toEquip, getCurrentSide(), Pair.of(t, toEquip), null)) return;

					if (e.getCharm() != null) {
						switch (e.getCharm()) {
							case TIMEWARP -> {
								if (t.hasEffect()) {
									t.getEffect(new EffectParameters(BEFORE_TURN, this, toEquip, getCurrentSide(), Duelists.of(t, toEquip, null, -1), channel));
									t.getEffect(new EffectParameters(AFTER_TURN, this, toEquip, getCurrentSide(), Duelists.of(t, toEquip, null, -1), channel));
								}
							}
							case DOUBLETAP -> {
								if (t.hasEffect())
									t.getEffect(new EffectParameters(ON_SUMMON, this, toEquip, getCurrentSide(), Duelists.of(t, toEquip, null, -1), channel));
							}
							case DOPPELGANGER -> {
								SlotColumn sc = getFirstAvailableSlot(getCurrentSide(), true);

								if (sc != null) {
									t.removeAtk(Math.round(t.getAltAtk() * 0.25f));
									t.removeDef(Math.round(t.getAltDef() * 0.25f));

									Champion dp = t.copy();
									dp.setBonus(t.getBonus().copy());
									dp.getBonus().removeMana(dp.getMana() / 2);
									dp.setGravelocked(true);

									sc.setTop(dp);
								}
							}
						}

						if (postCombat()) return;
					}

					msg = h.getUser().getName() + " equipou " + e.getCard().getName() + " em " + t.getName() + ".";
				} else if (d instanceof Champion) {
					Champion c = (Champion) d.deepCopy();
					if (args.length < 3) {
						channel.sendMessage("❌ | O terceiro argumento deve ser `A`, `D` ou `B` para definir se a carta será posicionada em modo de ataque, defesa ou virada para baixo.").queue(null, Helper::doNothing);
						return;
					} else if (!h.isNullMode() && (h.getMana() < c.getMana())) {
						channel.sendMessage("❌ | Você não tem mana suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou jogue cartas de equipamento ou campo.").queue(null, Helper::doNothing);
						return;
					} else if ((h.isNullMode() && h.getHp() <= c.getBaseStats() / 2) || h.getHp() <= c.getBlood()) {
						channel.sendMessage("❌ | Você não tem HP suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou jogue cartas de equipamento ou campo.").queue(null, Helper::doNothing);
						return;
					}

					if (!StringUtils.isNumeric(args[1])) {
						channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue(null, Helper::doNothing);
						return;
					}
					int dest = Integer.parseInt(args[1]) - 1;

					SlotColumn slot = slots.get(dest);

					if (slot.getTop() != null) {
						channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					} else if (isSlotDisabled(getCurrentSide(), dest)) {
						channel.sendMessage("❌ | Essa casa está indisponível.").queue(null, Helper::doNothing);
						return;
					}

					switch (args[2].toLowerCase(Locale.ROOT)) {
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

					reroll = false;
					d.setAvailable(false);
					slot.setTop(c);
					if (applyPersistentEffects(ON_SUMMON, getCurrentSide(), dest)) return;
					if (!c.isFlipped() && applyEffect(ON_SUMMON, c, dest, getCurrentSide(), Pair.of(c, dest), null))
						return;

					summoned.get(getCurrentSide()).merge(c.getRace(), 1, Integer::sum);

					msg = h.getUser().getName() + " invocou " + (c.isFlipped() ? "uma carta virada para baixo" : c.getName() + " em posição de " + (c.isDefending() ? "defesa" : "ataque")) + ".";

					if (c.getMana() > 0) {
						if (h.isNullMode())
							h.removeHp(c.getBaseStats() / 2);
						else
							h.removeMana(c.getMana());
					} else
						h.removeHp(c.getBlood());
				} else {
					Field f = (Field) d.deepCopy();
					if (!args[1].equalsIgnoreCase("f")) {
						channel.sendMessage("❌ | O segundo argumento precisa ser `F` se deseja jogar uma carta de campo.").queue(null, Helper::doNothing);
						return;
					}

					reroll = false;
					d.setAvailable(false);
					arena.setField(f);
					msg = h.getUser().getName() + " invocou o campo " + f.getCard().getName() + ".";
				}

				if (makeFusion(h)) return;
				reportEvent(h, msg, true, false);
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

				List<SlotColumn> yourSide = arena.getSlots().get(getCurrentSide());
				List<SlotColumn> hisSide = arena.getSlots().get(getNextSide());

				if (args.length == 1) {
					if (is[0] < 0 || is[0] >= yourSide.size()) {
						channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
						return;
					}

					Champion c = yourSide.get(is[0]).getTop();

					if (c == null) {
						channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
						return;
					} else if (hisSide.parallelStream().anyMatch(s -> s.getTop() != null)) {
						channel.sendMessage("❌ | Ainda existem campeões no campo inimigo.").queue(null, Helper::doNothing);
						return;
					} else if (!c.isAvailable()) {
						channel.sendMessage("❌ | Essa carta já atacou neste turno.").queue(null, Helper::doNothing);
						return;
					} else if (c.isFlipped()) {
						channel.sendMessage("❌ | Você não pode atacar com cartas viradas para baixo.").queue(null, Helper::doNothing);
						return;
					} else if (c.isStasis()) {
						channel.sendMessage("❌ | Essa carta está inalvejável.").queue(null, Helper::doNothing);
						return;
					} else if (c.isStunned()) {
						channel.sendMessage("❌ | Essa carta está atordoada.").queue(null, Helper::doNothing);
						return;
					} else if (c.isSleeping()) {
						channel.sendMessage("❌ | Essa carta está dormindo.").queue(null, Helper::doNothing);
						return;
					} else if (c.isDefending()) {
						channel.sendMessage("❌ | Você não pode atacar com cartas em modo de defesa.").queue(null, Helper::doNothing);
						return;
					}

					Hand you = hands.get(getCurrentSide());
					Hand op = hands.get(getNextSide());

					float demonFac = 1 - op.getMitigation();

					if (h.getCombo().getRight() == Race.DEMON)
						demonFac *= 1.25f;
					if (op.getCombo().getRight() == Race.DEMON)
						demonFac *= 1.33f;

					int yPower = Math.round(c.getFinAtk() * (getRound() < 2 ? 0.5f : 1));

					if (!c.isDecoy()) {
						op.removeHp(Math.round(yPower * demonFac));
						if (op.getMana() > 0) {
							int toSteal = (int) Math.min(
									op.getMana(),
									c.getLinkedTo().parallelStream()
											.filter(e -> e.getCharm() == Charm.DRAIN)
											.count()
							);

							you.addMana(toSteal);
							op.removeMana(toSteal);
						}
					}

					c.setAvailable(false);

					if (!postCombat()) {
						reportEvent(h,
								"%s atacou %s causando %s de dano direto!%s%s".formatted(
										c.getName(),
										hands.get(getNextSide()).getUser().getName(),
										yPower,
										getRound() < 2 ? " (dano reduzido por ser o 1º turno)" : "",
										demonFac > 1
												? " (efeito de raça: dano direto aumentado em " + Math.round(yPower * demonFac - yPower) + ")"
												: demonFac < 1
												? " (efeito de raça: dano direto reduzido em " + Math.round(yPower * demonFac - yPower) + ")"
												: ""
								)
								, true, false);
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
				} else if (yours.isStasis()) {
					channel.sendMessage("❌ | Essa carta está inalvejável.").queue(null, Helper::doNothing);
					return;
				} else if (yours.isStunned()) {
					channel.sendMessage("❌ | Essa carta está atordoada.").queue(null, Helper::doNothing);
					return;
				} else if (yours.isSleeping()) {
					channel.sendMessage("❌ | Essa carta está dormindo.").queue(null, Helper::doNothing);
					return;
				} else if (yours.isDefending()) {
					channel.sendMessage("❌ | Você não pode atacar com cartas em modo de defesa.").queue(null, Helper::doNothing);
					return;
				}

				attack(Pair.of(getCurrentSide(), is[0]), Pair.of(getNextSide(), is[1]));
			} catch (IndexOutOfBoundsException e) {
				channel.sendMessage("❌ | Índice inválido, escolha uma carta para usar no ataque e uma para ser atacada.").queue(null, Helper::doNothing);

			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser uma casa com uma carta no seu lado do tabuleiro e o segundo deve ser uma casa com uma carta no lado do inimigo.").queue(null, Helper::doNothing);
			}
		}
	}

	private void reportEvent(Hand h, String msg, boolean resetTimer, boolean changeTurn) {
		for (Side s : Side.values()) {
			List<SlotColumn> slts = arena.getSlots().get(s);
			Hand hd = getHands().get(s);

			boolean heroInField = isHeroInField(s);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn slot = slts.get(i);
				if (slot.getTop() == null) continue;

				Champion c = slot.getTop();
				if (applyEffect(GAME_TICK, c, i, s, Pair.of(c, i), null)) return;

				if (c.getCard().getId().equals(h.getUser().getId()) && hd.getHero() != null && !heroInField) {
					c.setHero(hd.getHero());
				} else {
					c.setHero(null);
				}
			}
		}

		BufferedImage bi = arena.render(this, hands);
		if (resetTimer) resetTimerKeepTurn();
		AtomicBoolean shownHand = new AtomicBoolean(false);
		moveLock = true;
		channel.sendMessage(msg)
				.addFile(Helper.writeAndGet(bi, String.valueOf(this.hashCode()), "jpg"))
				.queue(s -> {
					this.message.compute(s.getChannel().getId(), (id, m) -> {
						if (m != null)
							m.delete().queue(null, Helper::doNothing);
						return s;
					});
					Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
					moveLock = false;
					if (!shownHand.get() && h != null) {
						shownHand.set(true);
						h.showHand();
					}

					if (changeTurn)
						for (int i = 0; i < 5; i++) changed[i] = false;
				});

		if (record) {
			try {
				getFrames().add(Helper.compress(Helper.atob(getArena().addHands(bi, hands.values()), "jpg")));
			} catch (IOException ignore) {
			}
		}
	}

	public void attack(Pair<Side, Integer> atkr, Pair<Side, Integer> defr) {
		Champion yours = getArena().getSlots().get(atkr.getLeft()).get(atkr.getRight()).getTop();
		Champion his = getArena().getSlots().get(defr.getLeft()).get(defr.getRight()).getTop();

		Pair<Champion, Integer> attacker = Pair.of(yours, atkr.getRight());
		Pair<Champion, Integer> defender = Pair.of(his, defr.getRight());

		if (his.isStasis()) {
			channel.sendMessage("❌ | Você não pode atacar cartas inalvejáveis.").queue();
			return;
		} else if (yours.isDuelling()) {
			channel.sendMessage("❌ | " + yours.getName() + " só pode atacar " + his.getName() + " (DUELO).").queue();
			return;
		} else if (his.isDuelling()) {
			channel.sendMessage("❌ | " + his.getName() + " só pode ser atacado por " + yours.getName() + " (DUELO).").queue();
			return;
		}

		if (yours.isDefending()) return;

		/* PRE-ATTACK */
		{
			if (applyPersistentEffects(ON_ATTACK, atkr.getLeft(), atkr.getRight())) return;
			if (atkr.getRight() > 0) {
				Champion c = arena.getSlots().get(atkr.getLeft()).get(atkr.getRight() - 1).getTop();
				if (c != null && applyEffect(ATTACK_ASSIST, c, atkr.getRight(), atkr.getLeft(), attacker, defender))
					return;
			}
			if (atkr.getRight() < 4) {
				Champion c = arena.getSlots().get(atkr.getLeft()).get(atkr.getRight() + 1).getTop();
				if (c != null && applyEffect(ATTACK_ASSIST, c, atkr.getRight(), atkr.getLeft(), attacker, defender))
					return;
			}
			if (applyEffect(ON_ATTACK, yours, atkr.getRight(), atkr.getLeft(), attacker, defender)) return;

			if (yours.getBonus().popFlag(Flag.SKIPCOMBAT)) {
				yours.resetAttribs();
				his.resetAttribs();

				if (applyPersistentEffects(POST_ATTACK, atkr.getLeft(), atkr.getRight())) return;
				if (applyEffect(POST_ATTACK, yours, atkr.getRight(), atkr.getLeft(), attacker, defender)) return;

				reportEvent(null, "Cálculo de combate ignorado por efeito do atacante!", true, false);
				return;
			}
		}

		/* PRE-DEFENSE */
		{
			if (applyPersistentEffects(ON_DEFEND, defr.getLeft(), defr.getRight())) return;
			if (defr.getRight() > 0) {
				Champion c = arena.getSlots().get(defr.getLeft()).get(defr.getRight() - 1).getTop();
				if (c != null && applyEffect(DEFENSE_ASSIST, c, defr.getRight(), defr.getLeft(), attacker, defender))
					return;
			}
			if (defr.getRight() < 4) {
				Champion c = arena.getSlots().get(defr.getLeft()).get(defr.getRight() + 1).getTop();
				if (c != null && applyEffect(DEFENSE_ASSIST, c, defr.getRight(), defr.getLeft(), attacker, defender))
					return;
			}
			if (applyEffect(ON_DEFEND, his, defr.getRight(), defr.getLeft(), attacker, defender)) return;

			if (his.getBonus().popFlag(Flag.SKIPCOMBAT)) {
				yours.resetAttribs();
				his.resetAttribs();

				if (applyPersistentEffects(POST_DEFENSE, defr.getLeft(), defr.getRight())) return;
				if (applyEffect(POST_DEFENSE, his, defr.getRight(), defr.getLeft(), attacker, defender)) return;

				reportEvent(null, "Cálculo de combate ignorado por efeito do defensor!", true, false);
				return;
			}
		}

		int yPower = Math.round((yours.isDecoy() ? 0 : yours.getFinAtk()) * (his.isSleeping() ? 1.25f : 1));

		int hPower;
		if (his.isDefending()) {
			if (his.isFlipped()) {
				his.setFlipped(false);
				if (applyPersistentEffects(ON_FLIP, defr.getLeft(), defr.getRight())) return;
				if (applyEffect(ON_FLIP, his, defr.getRight(), defr.getLeft(), attacker, defender)) return;
			}

			hPower = his.getFinDef();
		} else {
			hPower = his.isDecoy() ? 0 : his.getFinAtk();
		}

		int dodge = his.getDodge();
		boolean dodged = dodge >= 100 || (dodge > 0 && Helper.chance(dodge));

		yours.setAvailable(false);
		yours.resetAttribs();
		his.resetAttribs();

		Hand you = hands.get(atkr.getLeft());
		Hand op = hands.get(defr.getLeft());

		/* ATTACK SUCCESS */
		if (yPower > hPower) {
			if (dodged) {
				if (applyPersistentEffects(ON_MISS, atkr.getLeft(), atkr.getRight())) return;
				if (applyEffect(ON_MISS, yours, atkr.getRight(), atkr.getLeft(), attacker, defender)) return;

				if (applyPersistentEffects(ON_DODGE, defr.getLeft(), defr.getRight())) return;
				if (applyEffect(ON_DODGE, his, defr.getRight(), defr.getLeft(), attacker, defender)) return;

				reportEvent(null, his.getName() + " esquivou do ataque de " + yours.getName() + "! (" + Helper.roundToString(dodge, 1) + "%)", true, false);
			} else {
				if (applyPersistentEffects(POST_ATTACK, atkr.getLeft(), atkr.getRight())) return;
				if (applyEffect(POST_ATTACK, yours, atkr.getRight(), atkr.getLeft(), attacker, defender)) return;

				if (applyPersistentEffects(BEFORE_DEATH, defr.getLeft(), defr.getRight())) return;
				if (applyEffect(BEFORE_DEATH, his, defr.getRight(), defr.getLeft(), attacker, defender)) return;

				float demonFac = 1 - op.getMitigation();
				if (you.getCombo().getRight() == Race.DEMON)
					demonFac *= 1.25f;
				if (op.getCombo().getRight() == Race.DEMON)
					demonFac *= 1.33f;

				if (yours.isDecoy()) {
					reportEvent(null, yours.getName() + " derrotou " + his.getCard().getName() + "? (" + yPower + " > " + hPower + ")", true, false);
				} else if (his.isDecoy()) {
					killCard(defr.getLeft(), defr.getRight(), his.getId());
					reportEvent(null, "Essa carta era na verdade uma isca!", true, false);
				}

				boolean noDmg = (his.isDefending() && !(his.isSleeping() || his.isStunned()))
								|| his.getBonus().popFlag(Flag.NODAMAGE)
								|| (getCustom() != null && getCustom().getBoolean("semdano"));

				int dmg;
				if (noDmg)
					dmg = Math.round(
							yours.getLinkedTo().parallelStream()
									.filter(e -> e.getCharm() == Charm.ARMORPIERCING)
									.mapToInt(Equipment::getAtk)
									.sum() * demonFac
					);
				else
					dmg = Math.round((yours.getBonus().hasFlag(Flag.ALLDAMAGE) ? yPower : yPower - hPower) * demonFac);

				if (op.getMana() > 0) {
					int toSteal = (int) Math.min(
							op.getMana(),
							yours.getLinkedTo().parallelStream()
									.filter(e -> e.getCharm() == Charm.DRAIN)
									.count()
					);

					you.addMana(toSteal);
					op.removeMana(toSteal);
				}

				Hero h = his.getHero();
				if (h != null) {
					int aux = dmg - h.getHp();
					h.setHp(h.getHp() - dmg);
					dmg = aux;
				}

				if (h == null || h.getHp() == 0) {
					op.removeHp(dmg);
					killCard(defr.getLeft(), defr.getRight(), his.getId());

					Hero y = yours.getHero();
					if (y != null) {
						y.addXp(1);
					}
				}

				if (applyPersistentEffects(AFTER_DEATH, defr.getLeft(), defr.getRight())) return;
				if (applyEffect(AFTER_DEATH, his, defr.getRight(), defr.getLeft(), attacker, defender)) return;

				if (!postCombat()) {
					String msg = "%s derrotou %s! (%d > %d)%s%s".formatted(
							yours.getName(),
							his.getCard().getName(),
							yPower,
							hPower,
							demonFac > 1
									? " (efeito de raça: dano direto aumentado em " + Math.round(dmg * demonFac - dmg) + ")"
									: demonFac < 1
									? " (efeito de raça: dano direto reduzido em " + Math.round(dmg * demonFac - dmg) + ")"
									: "",
							his.isSleeping() ? " (alvo dormindo: +25% dano final)" : ""
					);

					reportEvent(null, msg, true, false);
				} else return;
			}
		}

		/* ATTACK FAILED */
		else if (yPower < hPower) {
			if (applyPersistentEffects(ON_SUICIDE, atkr.getLeft(), atkr.getRight())) return;
			if (applyEffect(ON_SUICIDE, yours, atkr.getRight(), atkr.getLeft(), attacker, defender)) return;

			if (applyPersistentEffects(POST_DEFENSE, defr.getLeft(), defr.getRight())) return;
			if (applyEffect(POST_DEFENSE, his, defr.getRight(), defr.getLeft(), attacker, defender)) return;

			float demonFac = 1 - you.getMitigation();
			if (op.getCombo().getRight() == Race.DEMON)
				demonFac *= 1.25f;
			if (you.getCombo().getRight() == Race.DEMON)
				demonFac *= 1.33f;

			if (yours.isDecoy()) {
				killCard(atkr.getLeft(), atkr.getRight(), yours.getId());
				reportEvent(null, yours.getName() + " não conseguiu derrotar " + his.getCard().getName() + "? (" + yPower + " < " + hPower + ")", true, false);
			} else if (his.isDecoy()) {
				reportEvent(null, "Essa carta era na verdade uma isca!", true, false);
			}

			boolean noDmg = yours.getBonus().popFlag(Flag.NODAMAGE)
							|| (getCustom() != null && getCustom().getBoolean("semdano"));

			int dmg;
			if (noDmg)
				dmg = Math.round(
						his.getLinkedTo().parallelStream()
								.filter(e -> e.getCharm() == Charm.ARMORPIERCING)
								.mapToInt(Equipment::getAtk)
								.sum() * demonFac
				);
			else
				dmg = Math.round((his.getBonus().hasFlag(Flag.ALLDAMAGE) ? hPower : hPower - yPower) * demonFac);

			if (you.getMana() > 0) {
				int toSteal = (int) Math.min(
						you.getMana(),
						his.getLinkedTo().parallelStream()
								.filter(e -> e.getCharm() == Charm.DRAIN)
								.count()
				);

				op.addMana(toSteal);
				you.removeMana(toSteal);
			}

			Hero h = yours.getHero();
			if (h != null) {
				int aux = dmg - h.getHp();
				h.setHp(h.getHp() - dmg);
				dmg = aux;
			}

			if (h == null || h.getHp() == 0) {
				you.removeHp(dmg);
				killCard(atkr.getLeft(), atkr.getRight(), yours.getId());

				Hero y = his.getHero();
				if (y != null) {
					y.addXp(1);
				}
			}

			if (!postCombat()) {
				String msg = "%s não conseguiu derrotar %s! (%d < %d)%s%s".formatted(
						yours.getName(),
						his.getCard().getName(),
						yPower,
						hPower,
						demonFac > 1
								? " (efeito de raça: dano direto aumentado em " + Math.round(dmg * demonFac - dmg) + ")"
								: demonFac < 1
								? " (efeito de raça: dano direto reduzido em " + Math.round(dmg * demonFac - dmg) + ")"
								: "",
						his.isSleeping() ? " (alvo dormindo: +25% dano final)" : ""
				);

				reportEvent(null, msg, true, false);
			} else return;
		}

		/* ATTACK CLASHED */
		else {
			if (applyPersistentEffects(ON_SUICIDE, atkr.getLeft(), atkr.getRight())) return;
			if (applyEffect(ON_SUICIDE, yours, atkr.getRight(), atkr.getLeft(), attacker, defender)) return;

			if (applyPersistentEffects(BEFORE_DEATH, defr.getLeft(), defr.getRight())) return;
			if (applyEffect(BEFORE_DEATH, his, defr.getRight(), defr.getLeft(), attacker, defender)) return;

			if (yours.isDecoy() && his.isDecoy()) {
				killCard(atkr.getLeft(), atkr.getRight(), yours.getId());
				killCard(defr.getLeft(), defr.getRight(), his.getId());
				reportEvent(null, "As duas cartas eram iscas!", true, false);
			} else if (yours.isDecoy()) {
				killCard(atkr.getLeft(), atkr.getRight(), yours.getId());
				reportEvent(null, "Ambas as cartas foram destruídas? (" + yPower + " = " + hPower + ")", true, false);
			} else if (his.isDecoy()) {
				killCard(defr.getLeft(), defr.getRight(), his.getId());
				reportEvent(null, "Essa carta era na verdade uma isca!", true, false);
			}

			float yDmg = yours.getLinkedTo().parallelStream()
					.filter(e -> e.getCharm() == Charm.ARMORPIERCING)
					.mapToInt(Equipment::getAtk)
					.sum();

			float hDmg = his.getLinkedTo().parallelStream()
					.filter(e -> e.getCharm() == Charm.ARMORPIERCING)
					.mapToInt(Equipment::getAtk)
					.sum();

			op.removeHp(Math.round(yDmg));
			if (op.getMana() > 0 || you.getMana() > 0) {
				int yToSteal = (int) Math.min(
						op.getMana(),
						yours.getLinkedTo().parallelStream()
								.filter(e -> e.getCharm() == Charm.DRAIN)
								.count()
				);
				int hToSteal = (int) Math.min(
						you.getMana(),
						his.getLinkedTo().parallelStream()
								.filter(e -> e.getCharm() == Charm.DRAIN)
								.count()
				);


				if (op.getMana() > 0) {
					int toSteal = Math.max(0, yToSteal - hToSteal);

					you.addMana(toSteal);
					op.removeMana(toSteal);
				}
				if (you.getMana() > 0) {
					int toSteal = Math.max(0, hToSteal - yToSteal);

					op.addMana(toSteal);
					you.removeMana(toSteal);
				}
			}

			you.removeHp(Math.round(hDmg));

			killCard(atkr.getLeft(), atkr.getRight(), yours.getId());
			killCard(defr.getLeft(), defr.getRight(), his.getId());

			if (applyPersistentEffects(AFTER_DEATH, defr.getLeft(), defr.getRight())) return;
			if (applyEffect(AFTER_DEATH, his, defr.getRight(), defr.getLeft(), attacker, defender)) return;

			if (!postCombat()) {
				String msg = "Ambas as cartas foram destruídas! (%d = %d)%s".formatted(
						yPower,
						hPower,
						his.isSleeping() ? " (alvo dormindo: +25% dano final)" : ""
				);

				reportEvent(null, msg, true, false);
			} else return;
		}

		if (atkr.getRight() > 0) {
			Champion c = arena.getSlots().get(atkr.getLeft()).get(atkr.getRight() - 1).getTop();
			if (c != null)
				applyEffect(POST_ATTACK_ASSIST, c, atkr.getRight(), atkr.getLeft(), attacker, defender);
		}
		if (atkr.getRight() < 4) {
			Champion c = arena.getSlots().get(atkr.getLeft()).get(atkr.getRight() + 1).getTop();
			if (c != null)
				applyEffect(POST_ATTACK_ASSIST, c, atkr.getRight(), atkr.getLeft(), attacker, defender);
		}

		if (defr.getRight() > 0) {
			Champion c = arena.getSlots().get(defr.getLeft()).get(defr.getRight() - 1).getTop();
			if (c != null)
				applyEffect(POST_DEFENSE_ASSIST, c, defr.getRight(), defr.getLeft(), attacker, defender);
		}
		if (defr.getRight() < 4) {
			Champion c = arena.getSlots().get(defr.getLeft()).get(defr.getRight() + 1).getTop();
			if (c != null)
				applyEffect(POST_DEFENSE_ASSIST, c, defr.getRight(), defr.getLeft(), attacker, defender);
		}

		postCombat();
	}

	private boolean makeFusion(Hand h) {
		if (fusionLock > 0) return false;
		List<SlotColumn> slts = arena.getSlots().get(getCurrentSide());

		List<String> champsInField = slts.parallelStream()
				.map(SlotColumn::getTop)
				.map(c -> c == null || c.isSealed() ? null : c.getCard().getId())
				.collect(Collectors.toList());

		List<String> equipsInField = slts.parallelStream()
				.map(SlotColumn::getBottom)
				.map(dr -> dr == null ? null : dr.getCard().getId())
				.collect(Collectors.toList());

		String field = getArena().getField() != null ? getArena().getField().getCard().getId() : "DEFAULT";

		Champion aFusion = fusions
				.parallelStream()
				.filter(f ->
						f.getRequiredCards().size() > 0 &&
						!f.canFuse(champsInField, equipsInField, field).isEmpty() &&
						((h.isNullMode() && h.getHp() > f.getBaseStats() / 2) || h.getMana() >= f.getMana()) &&
						h.getHp() > f.getBlood()
				)
				.findFirst()
				.map(Champion::copy)
				.orElse(null);

		if (aFusion != null) {
			for (Map.Entry<String, Pair<Integer, Boolean>> material : aFusion.canFuse(champsInField, equipsInField, field).entrySet()) {
				Pair<Integer, Boolean> p = material.getValue();
				banishCard(getCurrentSide(), p.getLeft(), p.getRight());
			}

			SlotColumn sc = getFirstAvailableSlot(getCurrentSide(), true);
			if (sc != null) {
				aFusion.bind(h);
				sc.setTop(aFusion);
				if (applyPersistentEffects(ON_SUMMON, getCurrentSide(), sc.getIndex())) return true;
				if (applyEffect(ON_SUMMON, aFusion, sc.getIndex(), getCurrentSide(), Pair.of(aFusion, sc.getIndex()), null))
					return true;

				if (aFusion.getMana() > 0) {
					if (h.isNullMode())
						h.removeHp(aFusion.getBaseStats() / 2);
					else
						h.removeMana(aFusion.getMana());
				}

				if (aFusion.getBlood() > 0)
					h.removeHp(aFusion.getBlood());
			}

			return makeFusion(h);
		}
		return false;
	}

	public void killCard(Side to, int target, int id) {
		Champion targetChamp = getArena().getSlots().get(to).get(target).getTop();
		if (targetChamp == null || targetChamp.getBonus().hasFlag(Flag.NODEATH) || targetChamp.getId() != id) return;
		List<SlotColumn> slts = getArena().getSlots().get(to);

		slts.get(target).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn sd = slts.get(i);
			Champion c = sd.getTop();
			if (c != null && c.isDecoy() && c.getBonus().getSpecialData().getInt("original") == target)
				killCard(to, i, c.getId());

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
				unequipCard(to, i, slts);
		}

		for (SlotColumn slot : slts) {
			if (slot.getTop() == null) continue;

			Champion c = slot.getTop();
			c.getBonus().setAtk(target, 0);
			c.getBonus().setDef(target, 0);
		}

		if (applyPersistentEffects(ON_DESTROY, to, target)) return;
		if (applyEffect(ON_DESTROY, targetChamp, target, to, null, null)) return;

		targetChamp.reset();
		if (targetChamp.canGoToGrave())
			arena.getGraveyard().get(to).add(targetChamp.deepCopy());
	}

	public void destroyCard(Side to, int target, Side from, int source) {
		Champion targetChamp = getArena().getSlots().get(to).get(target).getTop();
		if (targetChamp == null) return;
		List<SlotColumn> slts = getArena().getSlots().get(to);

		Champion sourceChamp = getArena().getSlots().get(from).get(source).getTop();

		double chance = 100;
		if (sourceChamp != null) {
			int sourceMana = sourceChamp.getMana() + (sourceChamp.isFusion() ? 5 : 0);
			int targetMana = targetChamp.getMana() + (targetChamp.isFusion() ? 5 : 0);

			chance -= sourceChamp.getDodge() * 0.75;
			if (sourceMana <= targetMana)
				chance -= Math.max(targetMana * 25d / sourceMana, 25);
		}

		if (chance == 100 || Helper.chance(chance)) {
			for (int i = 0; i < slts.size(); i++) {
				Equipment eq = slts.get(i).getBottom();
				if (eq != null && eq.getLinkedTo().getLeft() == target) {
					if (eq.getCharm() == Charm.SPELLSHIELD || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLSHIELD) {
						unequipCard(to, i, slts);
						return;
					} else if ((eq.getCharm() == Charm.SPELLMIRROR || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLMIRROR) && sourceChamp != null) {
						destroyCard(from, source, to, target);
						unequipCard(to, i, slts);
						return;
					}
				}
			}

			slts.get(target).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn sd = slts.get(i);
				Champion c = sd.getTop();
				if (c != null && c.isDecoy() && c.getBonus().getSpecialData().getInt("original") == target)
					killCard(to, i, c.getId());

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
					unequipCard(to, i, slts);
			}

			for (SlotColumn slot : slts) {
				if (slot.getTop() == null) continue;

				Champion c = slot.getTop();
				c.getBonus().setAtk(target, 0);
				c.getBonus().setDef(target, 0);
			}

			if (applyPersistentEffects(ON_DESTROY, to, target)) return;
			if (applyEffect(ON_DESTROY, targetChamp, target, to, null, null)) return;

			targetChamp.reset();
			if (targetChamp.canGoToGrave())
				arena.getGraveyard().get(to).add(targetChamp.copy());
		} else {
			channel.sendMessage("Efeito de " + sourceChamp.getName() + " errou. (" + Helper.roundToString(chance, 1) + "%)").queue();
		}
	}

	public void destroyCard(Side to, int target) {
		Champion targetChamp = getArena().getSlots().get(to).get(target).getTop();
		if (targetChamp == null) return;
		List<SlotColumn> slts = getArena().getSlots().get(to);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == target) {
				if (eq.getCharm() == Charm.SPELLSHIELD || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLSHIELD) {
					unequipCard(to, i, slts);
					return;
				}
			}
		}

		slts.get(target).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn sd = slts.get(i);
			Champion c = sd.getTop();
			if (c != null && c.isDecoy() && c.getBonus().getSpecialData().getInt("original") == target)
				killCard(to, i, c.getId());

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
				unequipCard(to, i, slts);
		}

		for (SlotColumn slot : slts) {
			if (slot.getTop() == null) continue;

			Champion c = slot.getTop();
			c.getBonus().setAtk(target, 0);
			c.getBonus().setDef(target, 0);
		}

		if (applyPersistentEffects(ON_DESTROY, to, target)) return;
		if (applyEffect(ON_DESTROY, targetChamp, target, to, null, null)) return;

		targetChamp.reset();
		if (!targetChamp.isFusion())
			arena.getGraveyard().get(to).add(targetChamp.copy());
	}

	public void dizimateCard(Side to, int target) {
		Champion targetChamp = getArena().getSlots().get(to).get(target).getTop();
		if (targetChamp == null) return;
		List<SlotColumn> slts = getArena().getSlots().get(to);

		slts.get(target).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn sd = slts.get(i);
			Champion c = sd.getTop();
			if (c != null && c.isDecoy() && c.getBonus().getSpecialData().getInt("original") == target)
				killCard(to, i, c.getId());

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
				unequipCard(to, i, slts);
		}

		for (SlotColumn slot : slts) {
			if (slot.getTop() == null) continue;

			Champion c = slot.getTop();
			c.getBonus().setAtk(target, 0);
			c.getBonus().setDef(target, 0);
		}

		if (applyPersistentEffects(ON_DESTROY, to, target)) return;
		if (applyEffect(ON_DESTROY, targetChamp, target, to, null, null)) return;

		targetChamp.reset();
		if (!targetChamp.isFusion())
			arena.getGraveyard().get(to).add(targetChamp.copy());
	}

	public void captureCard(Side to, int target, Side from, int source, boolean withFusion) {
		Champion targetChamp = getArena().getSlots().get(to).get(target).getTop();
		if (targetChamp == null) return;
		List<SlotColumn> slts = getArena().getSlots().get(to);

		Champion sourceChamp = getArena().getSlots().get(from).get(source).getTop();

		double chance = 100;
		if (sourceChamp != null) {
			int sourceMana = sourceChamp.getMana() + (sourceChamp.isFusion() ? 5 : 0);
			int targetMana = targetChamp.getMana() + (targetChamp.isFusion() ? 5 : 0);

			chance -= sourceChamp.getDodge() * 0.75;
			if (sourceMana <= targetMana)
				chance -= Math.max(targetMana * 25d / sourceMana, 25);
		}

		if (chance == 100 || Helper.chance(chance)) {
			for (int i = 0; i < slts.size(); i++) {
				Equipment eq = slts.get(i).getBottom();
				if (eq != null && eq.getLinkedTo().getLeft() == target) {
					if (eq.getCharm() == Charm.SPELLSHIELD || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLSHIELD) {
						unequipCard(to, i, slts);
						return;
					} else if ((eq.getCharm() == Charm.SPELLMIRROR || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLMIRROR)) {
						captureCard(from, source, to, target, withFusion);
						unequipCard(to, i, slts);
						return;
					}
				}
			}

			slts.get(target).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn sd = slts.get(i);
				Champion c = sd.getTop();
				if (c != null && c.isDecoy() && c.getBonus().getSpecialData().getInt("original") == target)
					killCard(to, i, c.getId());

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
					unequipCard(to, i, slts);
			}

			for (SlotColumn slot : slts) {
				if (slot.getTop() == null) continue;

				Champion c = slot.getTop();
				c.getBonus().setAtk(target, 0);
				c.getBonus().setDef(target, 0);
			}

			if (applyPersistentEffects(ON_DESTROY, to, target)) return;
			if (applyEffect(ON_DESTROY, targetChamp, target, to, null, null)) return;

			targetChamp.reset();
			if (!targetChamp.isFusion() || withFusion)
				hands.get(to == Side.TOP ? Side.BOTTOM : Side.TOP).getCards().add(targetChamp.copy());
		} else {
			channel.sendMessage("Efeito de " + sourceChamp.getName() + " errou. (" + Helper.roundToString(chance, 1) + "%)").queue();
		}
	}

	public void captureCard(Side to, int target, boolean withFusion) {
		Champion targetChamp = getArena().getSlots().get(to).get(target).getTop();
		if (targetChamp == null) return;
		List<SlotColumn> slts = getArena().getSlots().get(to);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == target) {
				if (eq.getCharm() == Charm.SPELLSHIELD || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLSHIELD) {
					unequipCard(to, i, slts);
					return;
				}
			}
		}

		slts.get(target).setTop(null);
		for (int i = 0; i < slts.size(); i++) {
			SlotColumn sd = slts.get(i);
			Champion c = sd.getTop();
			if (c != null && c.isDecoy() && c.getBonus().getSpecialData().getInt("original") == target)
				killCard(to, i, c.getId());

			if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
				unequipCard(to, i, slts);
		}

		for (SlotColumn slot : slts) {
			if (slot.getTop() == null) continue;

			Champion c = slot.getTop();
			c.getBonus().setAtk(target, 0);
			c.getBonus().setDef(target, 0);
		}

		if (applyPersistentEffects(ON_DESTROY, to, target)) return;
		if (applyEffect(ON_DESTROY, targetChamp, target, to, null, null)) return;

		targetChamp.reset();
		if (!targetChamp.isFusion() || withFusion)
			hands.get(to == Side.TOP ? Side.BOTTOM : Side.TOP).getCards().add(targetChamp.copy());
	}

	public void banishCard(Side to, int target, boolean equipment) {
		List<SlotColumn> slts = getArena().getSlots().get(to);
		if (equipment) {
			Equipment eq = slts.get(target).getBottom();
			if (eq == null) return;

			if (slts.get(eq.getLinkedTo().getLeft()).getTop() != null)
				slts.get(eq.getLinkedTo().getLeft()).getTop().removeLinkedTo(eq);
			eq.setLinkedTo(null);

			SlotColumn sd = slts.get(target);
			arena.getBanished().add(eq);
			sd.setBottom(null);
		} else {
			Champion targetChamp = slts.get(target).getTop();
			if (targetChamp == null) return;

			slts.get(target).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn sd = slts.get(i);
				Champion c = sd.getTop();
				if (c != null && c.isDecoy() && c.getBonus().getSpecialData().getInt("original") == target)
					killCard(to, i, c.getId());

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
					banishCard(to, i, true);
			}

			for (SlotColumn slot : slts) {
				if (slot.getTop() == null) continue;

				Champion c = slot.getTop();
				c.getBonus().setAtk(target, 0);
				c.getBonus().setDef(target, 0);
			}

			if (applyPersistentEffects(ON_DESTROY, to, target)) return;
			if (applyEffect(ON_DESTROY, targetChamp, target, to, null, null)) return;

			targetChamp.reset();
			if (targetChamp.canGoToGrave())
				arena.getBanished().add(targetChamp.copy());
		}
	}

	public void unequipCard(Side s, int index, List<SlotColumn> side) {
		Equipment eq = side.get(index).getBottom();
		if (eq == null) return;

		if (side.get(eq.getLinkedTo().getLeft()).getTop() != null)
			side.get(eq.getLinkedTo().getLeft()).getTop().removeLinkedTo(eq);
		eq.setLinkedTo(null);

		SlotColumn sd = side.get(index);
		sd.setBottom(null);

		eq.reset();
		if (!eq.isParasite() || eq.isEffectOnly())
			if (eq.getTier() >= 4)
				arena.getBanished().add(eq.copy());
			else
				arena.getGraveyard().get(s).add(eq.copy());
	}

	public Arena getArena() {
		return arena;
	}

	public Side getSideById(String id) {
		return hands.values().parallelStream()
				.filter(h -> h.getUser().getId().equals(id) || (h instanceof TeamHand th && th.getUsers().parallelStream().anyMatch(u -> u.getId().equals(id))))
				.map(Hand::getSide)
				.findFirst().orElse(null);
	}

	public Map<Side, Hand> getHands() {
		return hands;
	}

	public Map<Side, Pair<Race, Race>> getCombos() {
		return combos;
	}

	public SlotColumn getFirstAvailableSlot(Side s, boolean top) {
		List<SlotColumn> get = arena.getSlots().get(s);
		for (int i = 0; i < get.size(); i++) {
			SlotColumn slot = get.get(i);
			if (top ? (slot.getTop() == null && !isSlotDisabled(s, i)) : slot.getBottom() == null)
				return slot;
		}
		return null;
	}

	public void convertCard(Side to, int target, Side from, int source) {
		Champion targetChamp = getArena().getSlots().get(to).get(target).getTop();
		if (targetChamp == null || targetChamp.getBonus().hasFlag(Flag.NOCONVERT)) return;
		List<SlotColumn> slts = getArena().getSlots().get(to);

		Champion sourceChamp = getArena().getSlots().get(from).get(source).getTop();

		double chance = 100;
		if (sourceChamp != null) {
			int sourceMana = sourceChamp.getMana() + (sourceChamp.isFusion() ? 5 : 0);
			int targetMana = targetChamp.getMana() + (targetChamp.isFusion() ? 5 : 0);

			chance -= sourceChamp.getDodge() * 0.75;
			if (sourceMana <= targetMana)
				chance -= Math.max(targetMana * 25d / sourceMana, 25);
		}

		if (chance == 100 || Helper.chance(chance)) {
			for (int i = 0; i < slts.size(); i++) {
				Equipment eq = slts.get(i).getBottom();
				if (eq != null && eq.getLinkedTo().getLeft() == target) {
					if (eq.getCharm() == Charm.SPELLSHIELD || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLSHIELD) {
						unequipCard(to, i, slts);
						return;
					} else if (eq.getCharm() == Charm.SPELLMIRROR || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLMIRROR) {
						convertCard(from, source, to, target);
						unequipCard(to, i, slts);
						return;
					}
				}
			}

			SlotColumn sc = getFirstAvailableSlot(from, true);
			if (sc != null) {
				targetChamp.clearLinkedTo();
				targetChamp.bind(hands.get(from));
				sc.setTop(targetChamp);
				slts.get(target).setTop(null);
				for (int i = 0; i < slts.size(); i++) {
					SlotColumn sd = slts.get(i);
					Champion c = sd.getTop();
					if (c != null && c.isDecoy() && c.getBonus().getSpecialData().getInt("original") == target)
						killCard(to, i, c.getId());

					if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
						unequipCard(to, i, slts);
				}

				for (SlotColumn slot : slts) {
					if (slot.getTop() == null) continue;

					Champion c = slot.getTop();
					c.getBonus().setAtk(target, 0);
					c.getBonus().setDef(target, 0);
				}
			}
		} else {
			channel.sendMessage("Efeito de " + sourceChamp.getName() + " errou. (" + Helper.roundToString(chance, 1) + "%)").queue();
		}
	}

	public void convertCard(Side to, int target) {
		Side from = to == Side.TOP ? Side.BOTTOM : Side.TOP;
		Champion targetChamp = getArena().getSlots().get(to).get(target).getTop();
		if (targetChamp == null || targetChamp.getBonus().hasFlag(Flag.NOCONVERT)) return;
		List<SlotColumn> slts = getArena().getSlots().get(to);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == target) {
				if (eq.getCharm() == Charm.SPELLSHIELD || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLSHIELD) {
					unequipCard(to, i, slts);
					return;
				}
			}
		}

		SlotColumn sc = getFirstAvailableSlot(from, true);
		if (sc != null) {
			targetChamp.clearLinkedTo();
			targetChamp.bind(hands.get(from));
			sc.setTop(targetChamp);
			slts.get(target).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn sd = slts.get(i);
				Champion c = sd.getTop();
				if (c != null && c.isDecoy() && c.getBonus().getSpecialData().getInt("original") == target)
					killCard(to, i, c.getId());

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
					unequipCard(to, i, slts);
			}

			for (SlotColumn slot : slts) {
				if (slot.getTop() == null) continue;

				Champion c = slot.getTop();
				c.getBonus().setAtk(target, 0);
				c.getBonus().setDef(target, 0);
			}
		}
	}

	public void switchCards(Side to, int target, Side from, int source) {
		Champion targetChamp = getArena().getSlots().get(to).get(target).getTop();
		if (targetChamp == null || targetChamp.getBonus().hasFlag(Flag.NOCONVERT)) return;
		List<SlotColumn> slts = getArena().getSlots().get(to);

		Champion sourceChamp = getArena().getSlots().get(from).get(source).getTop();

		double chance = 100;
		if (sourceChamp != null) {
			int sourceMana = sourceChamp.getMana() + (sourceChamp.isFusion() ? 5 : 0);
			int targetMana = targetChamp.getMana() + (targetChamp.isFusion() ? 5 : 0);

			chance -= sourceChamp.getDodge() * 0.75;
			if (sourceMana <= targetMana)
				chance -= Math.max(targetMana * 25d / sourceMana, 25);
		}

		if (chance == 100 || Helper.chance(chance)) {
			for (int i = 0; i < slts.size(); i++) {
				Equipment eq = slts.get(i).getBottom();
				if (eq != null && eq.getLinkedTo().getLeft() == target) {
					if (eq.getCharm() == Charm.SPELLSHIELD || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLSHIELD) {
						unequipCard(to, i, slts);
						return;
					}
				}
			}

			targetChamp.clearLinkedTo();
			targetChamp.bind(hands.get(from));
			slts.get(target).setTop(null);
			for (int i = 0; i < slts.size(); i++) {
				SlotColumn sd = slts.get(i);
				Champion c = sd.getTop();
				if (c != null && c.isDecoy() && c.getBonus().getSpecialData().getInt("original") == target)
					killCard(to, i, c.getId());

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == source)
					unequipCard(to, i, slts);
			}

			for (SlotColumn slot : slts) {
				if (slot.getTop() == null) continue;

				Champion c = slot.getTop();
				c.getBonus().setAtk(target, 0);
				c.getBonus().setDef(target, 0);
			}

			if (sourceChamp == null) return;
			List<SlotColumn> slots = getArena().getSlots().get(from);

			sourceChamp.clearLinkedTo();
			targetChamp.bind(hands.get(to));
			slots.get(source).setTop(null);
			for (int i = 0; i < slots.size(); i++) {
				SlotColumn sd = slots.get(i);
				Champion c = sd.getTop();
				if (c != null && c.isDecoy() && c.getBonus().getSpecialData().getInt("original") == target)
					killCard(to, i, c.getId());

				if (sd.getBottom() != null && sd.getBottom().getLinkedTo().getLeft() == target)
					unequipCard(from, i, slots);
			}

			for (SlotColumn slot : slts) {
				if (slot.getTop() == null) continue;

				Champion c = slot.getTop();
				c.getBonus().setAtk(target, 0);
				c.getBonus().setDef(target, 0);
			}

			slts.get(target).setTop(sourceChamp);
			slots.get(source).setTop(targetChamp);
		} else {
			channel.sendMessage("Efeito de " + sourceChamp.getName() + " errou. (" + Helper.roundToString(chance, 1) + "%)").queue();
		}
	}

	public void convertEquipment(Champion target, int pos, Side to, int index) {
		Side his = to == Side.TOP ? Side.BOTTOM : Side.TOP;
		Champion targetChamp = getArena().getSlots().get(his).get(index).getTop();
		if (targetChamp == null || targetChamp.getBonus().hasFlag(Flag.NOCONVERT)) return;
		List<SlotColumn> slts = getArena().getSlots().get(his);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				if (eq.getCharm() == Charm.SPELLSHIELD || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLSHIELD) {
					unequipCard(his, i, slts);
					return;
				}
			}
		}

		for (int i = 0; i < 5; i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				SlotColumn sc = getFirstAvailableSlot(to, false);
				if (sc != null) {
					targetChamp.removeLinkedTo(eq);
					slts.get(i).setBottom(null);

					target.addLinkedTo(eq);
					eq.setLinkedTo(Pair.of(pos, target));
					targetChamp.bind(hands.get(to));
					sc.setBottom(eq);
				} else return;

				break;
			}
		}
	}

	public void convertEquipments(Champion target, int pos, Side to, int index) {
		Side his = to == Side.TOP ? Side.BOTTOM : Side.TOP;
		Champion targetChamp = getArena().getSlots().get(his).get(index).getTop();
		if (targetChamp == null || targetChamp.getBonus().hasFlag(Flag.NOCONVERT)) return;
		List<SlotColumn> slts = getArena().getSlots().get(his);

		for (int i = 0; i < slts.size(); i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				if (eq.getCharm() == Charm.SPELLSHIELD || targetChamp.getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SPELLSHIELD) {
					unequipCard(his, i, slts);
					return;
				}
			}
		}

		for (int i = 0; i < 5; i++) {
			Equipment eq = slts.get(i).getBottom();
			if (eq != null && eq.getLinkedTo().getLeft() == index) {
				SlotColumn sc = getFirstAvailableSlot(to, false);
				if (sc != null) {
					targetChamp.removeLinkedTo(eq);
					slts.get(i).setBottom(null);

					target.addLinkedTo(eq);
					eq.setLinkedTo(Pair.of(pos, target));
					targetChamp.bind(hands.get(to));
					sc.setBottom(eq);
				} else return;
			}
		}
	}

	public boolean lastTick() {
		for (Side s : Side.values()) {
			Hand h = hands.get(s);
			Hand op = hands.get(s == Side.TOP ? Side.BOTTOM : Side.TOP);
			List<SlotColumn> slts = arena.getSlots().get(s);

			if (h.getHp() <= 0) {
				for (int i = 0; i < 5; i++) {
					Equipment e = slts.get(i).getBottom();
					if (e == null) continue;

					applyEffect(ON_LOSE, e, i, s);
				}
			} else {
				for (int i = 0; i < 5; i++) {
					Equipment e = slts.get(i).getBottom();
					if (e == null) continue;

					applyEffect(ON_WIN, e, i, s);
				}
			}

			if (h.getHp() > 0 && op.getHp() > 0) return true;
		}

		return false;
	}

	public boolean postCombat() {
		boolean finished = false;
		for (Map.Entry<Side, Hand> entry : hands.entrySet()) {
			Hand h = entry.getValue();
			Hand op = hands.get(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP);

			if (h.getHp() <= 0) {
				if (lastTick()) return false;

				if (getCustom() == null) {
					getHistory().setWinner(op.getSide());
					getBoard().awardWinner(this, op.getUser().getId());
				}

				String msg;
				if (team)
					msg = ((TeamHand) op).getMentions() + " zeraram os pontos de vida de " + ((TeamHand) h).getMentions() + ", temos os vencedores! (" + getRound() + " turnos)";
				else
					msg = op.getUser().getAsMention() + " zerou os pontos de vida de " + h.getUser().getAsMention() + ", temos um vencedor! (" + getRound() + " turnos)";

				close();
				finished = true;
				channel.sendMessage(msg)
						.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
						.queue(ms ->
								this.message.compute(ms.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return ms;
								}));
				break;
			}
		}

		return finished;
	}

	@Override
	public Map<String, ThrowingBiConsumer<Member, Message>> getButtons() {
		ThrowingBiConsumer<Member, Message> skip = (mb, ms) -> {
			User u = getCurrent();

			AtomicReference<Hand> h = new AtomicReference<>(hands.get(getCurrentSide()));
			h.get().getCards().removeIf(d -> !d.isAvailable());
			List<SlotColumn> slots = arena.getSlots().get(getCurrentSide());

			if (applyPersistentEffects(AFTER_TURN, getCurrentSide(), -1)) return;
			for (int i = 0; i < slots.size(); i++) {
				Champion c = slots.get(i).getTop();
				if (c != null) {
					c.setAvailable(!c.isStasis() && !c.isStunned() && !c.isSleeping());
					c.resetAttribs();
					if (applyEffect(AFTER_TURN, c, i, getCurrentSide(), Pair.of(c, i), null)
						|| makeFusion(h.get())
					) return;
				}
			}

			for (Drawable d : discardBatch) {
				d.setAvailable(true);
			}
			if (team && h.get().getCombo().getLeft() == Race.BESTIAL) {
				h.get().getDeque().addAll(
						discardBatch.stream()
								.filter(d -> {
									if (d instanceof Champion c) return c.canGoToGrave();
									else if (d instanceof Equipment e) return !e.isEffectOnly();
									else return true;
								})
								.map(Drawable::copy)
								.collect(Collectors.toList())
				);
				Collections.shuffle(h.get().getDeque());
			} else {
				arena.getGraveyard().get(getCurrentSide()).addAll(
						discardBatch.stream()
								.filter(d -> {
									if (d instanceof Champion c) return c.canGoToGrave();
									else if (d instanceof Equipment e) return !e.isEffectOnly();
									else return true;
								})
								.map(Drawable::copy)
								.collect(Collectors.toList())
				);
			}
			discardBatch.clear();

			if (getRound() > 0) reroll = false;
			resetTimer(this);

			phase = Phase.PLAN;
			h.set(hands.get(getCurrentSide()));
			h.get().decreaseSuppression();
			h.get().decreaseLockTime();
			h.get().decreaseNullTime();
			slots = arena.getSlots().get(getCurrentSide());

			h.get().addMana(h.get().getManaPerTurn());
			if (h.get().getCombo().getLeft() == Race.DEMON) {
				Hand op = hands.get(getNextSide());
				h.get().addMana((int) (Math.max(0f, op.getBaseHp() - op.getHp()) / op.getBaseHp() * 5));
				if (h.get().getHp() < h.get().getBaseHp() / 3f) {
					h.get().addHp(Math.round((h.get().getBaseHp() - h.get().getHp()) * 0.1f));

					if (applyPersistentEffects(ON_HEAL, h.get().getSide(), -1)) return;
				}
			}

			switch (h.get().getCombo().getRight()) {
				case BESTIAL -> {
					if (getRound() <= 1)
						h.get().addMana(1);
				}
				case ELF -> {
					if (getRound() > 1 && getRound() - (h.get().getSide() == Side.TOP ? 1 : 0) % 3 == 0)
						h.get().addMana(1);
				}
			}

			if (applyPersistentEffects(BEFORE_TURN, getCurrentSide(), -1)) return;
			for (int i = 0; i < slots.size(); i++) {
				SlotColumn sc = slots.get(i);

				Champion c = sc.getTop();
				if (c != null) {
					if (c.isStasis()) c.reduceStasis();
					else if (c.isStunned()) c.reduceStun();
					else if (c.isSleeping()) c.reduceSleep();

					if (applyEffect(BEFORE_TURN, c, i, getCurrentSide(), Pair.of(c, i), null)
						|| makeFusion(h.get())
					) return;
				}

				if (sc.isUnavailable()) {
					sc.setUnavailable(-1);
				}
			}

			String msg = u.getName() + " encerrou o turno, agora é sua vez " + getCurrent().getAsMention() + " (turno " + getRound() + ")";

			reportEvent(h.get(), msg, false, true);
		};

		Map<String, ThrowingBiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
		if (getRound() < 1 || phase == Phase.ATTACK)
			buttons.put("▶️", skip);
		else {
			buttons.put("▶️", (mb, ms) -> {
				phase = Phase.ATTACK;
				draw = false;
				reroll = false;
				reportEvent(null, "**FASE DE ATAQUE:** Escolha uma carta do seu lado e uma carta do lado inimigo para iniciar combate", true, false);
			});
			buttons.put("⏩", (mb, ms) -> {
				draw = false;
				reroll = false;
				skip.accept(mb, ms);
			});
		}
		if (phase == Phase.PLAN) {
			buttons.put("\uD83D\uDCE4", (mb, ms) -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = hands.get(getCurrentSide());

				int remaining = h.getMaxCards() - h.getCards().size();
				if (remaining <= 0) {
					channel.sendMessage("❌ | Você não pode puxar mais cartas se tiver " + h.getMaxCards() + " ou mais na sua mão.").queue(null, Helper::doNothing);
					return;
				}

				if (!h.manualDraw()) {
					if (getCustom() == null) {
						getHistory().setWinner(getNextSide());
						getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
					}

					String msg;
					if (team)
						msg = getCurrent().getAsMention() + " não possui mais cartas no deck, " + ((TeamHand) hands.get(getNextSide())).getMentions() + " venceram! (" + getRound() + " turnos)";
					else
						msg = getCurrent().getAsMention() + " não possui mais cartas no deck, " + hands.get(getNextSide()).getUser().getAsMention() + " venceu! (" + getRound() + " turnos)";

					close();
					channel.sendMessage(msg)
							.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
							.queue(mm ->
									this.message.compute(mm.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return mm;
									}));

					return;
				}

				remaining = h.getMaxCards() - h.getCards().size();
				reportEvent(h, getCurrent().getName() + " puxou uma carta. (" + (remaining == 0 ? "não pode puxar mais cartas" : "pode puxar mais " + remaining + " carta" + (remaining == 1 ? "" : "s")) + ")", true, false);
			});
			buttons.put("\uD83D\uDCE6", (mb, ms) -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = hands.get(getCurrentSide());

				int remaining = h.getMaxCards() - h.getCards().size();
				if (remaining <= 0) {
					channel.sendMessage("❌ | Você não pode puxar mais cartas se tiver " + h.getMaxCards() + " ou mais na sua mão.").queue(null, Helper::doNothing);
					return;
				}

				int toDraw = Math.min(remaining, h.getDeque().size());
				for (int i = 0; i < toDraw; i++) {
					h.manualDraw();
				}

				if (toDraw == 1)
					reportEvent(h, getCurrent().getName() + " puxou uma carta.", true, false);
				else
					reportEvent(h, getCurrent().getName() + " puxou " + toDraw + " cartas.", true, false);
			});
		}
		if (reroll && getRound() == 1 && phase == Phase.PLAN)
			buttons.put("\uD83D\uDD04", (mb, ms) -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = hands.get(getCurrentSide());
				h.redrawHand();

				reroll = false;
				reportEvent(h, getCurrent().getName() + " rolou novamente as cartas na mão!", true, false);
			});
		if (hands.get(getCurrentSide()).getHp() < hands.get(getCurrentSide()).getBaseHp() / 3 && hands.get(getCurrentSide()).getDestinyDeck().size() > 0 && phase == Phase.PLAN)
			buttons.put("\uD83E\uDDE7", (mb, ms) -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				Hand h = hands.get(getCurrentSide());
				h.destinyDraw();

				reportEvent(h, getCurrent().getName() + " executou um saque do destino!", true, false);
			});
		if (phase == Phase.PLAN && tourMatch == null)
			buttons.put("\uD83E\uDD1D", (mb, ms) -> {
				if (phase != Phase.PLAN) {
					channel.sendMessage("❌ | Você só pode pedir empate na fase de planejamento.").queue(null, Helper::doNothing);
					return;
				}

				if (draw) {
					String msg = "Por acordo mútuo, declaro empate! (" + getRound() + " turnos)";

					close();
					channel.sendMessage(msg)
							.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
							.queue(mm ->
									this.message.compute(mm.getChannel().getId(), (id, m) -> {
										if (m != null)
											m.delete().queue(null, Helper::doNothing);
										return mm;
									}));
				} else {
					User u = getCurrent();

					AtomicReference<Hand> h = new AtomicReference<>(hands.get(getCurrentSide()));
					h.get().getCards().removeIf(d -> !d.isAvailable());
					List<SlotColumn> slots = arena.getSlots().get(getCurrentSide());

					if (applyPersistentEffects(AFTER_TURN, getCurrentSide(), -1)) return;
					for (int i = 0; i < slots.size(); i++) {
						Champion c = slots.get(i).getTop();
						if (c != null) {
							c.setAvailable(!c.isStasis() && !c.isStunned() && !c.isSleeping());
							c.resetAttribs();
							if (applyEffect(AFTER_TURN, c, i, getCurrentSide(), Pair.of(c, i), null)
								|| makeFusion(h.get())
							) return;
						}
					}

					for (Drawable d : discardBatch) {
						d.setAvailable(true);
					}
					if (team && h.get().getCombo().getLeft() == Race.BESTIAL) {
						h.get().getDeque().addAll(
								discardBatch.stream()
										.map(Drawable::copy)
										.collect(Collectors.toList())
						);
						Collections.shuffle(h.get().getDeque());
					} else {
						arena.getGraveyard().get(getCurrentSide()).addAll(
								discardBatch.stream()
										.map(Drawable::copy)
										.collect(Collectors.toList())
						);
					}
					discardBatch.clear();

					if (getRound() > 0) reroll = false;
					resetTimer(this);

					phase = Phase.PLAN;
					h.set(hands.get(getCurrentSide()));
					h.get().decreaseSuppression();
					h.get().decreaseLockTime();
					h.get().decreaseNullTime();
					slots = arena.getSlots().get(getCurrentSide());

					h.get().addMana(h.get().getManaPerTurn());
					if (h.get().getCombo().getLeft() == Race.DEMON) {
						Hand op = hands.get(getNextSide());
						h.get().addMana((int) (Math.max(0f, op.getBaseHp() - op.getHp()) / op.getBaseHp() * 5));
						if (h.get().getHp() < h.get().getBaseHp() / 3f) {
							h.get().addHp(Math.round((h.get().getBaseHp() - h.get().getHp()) * 0.1f));

							if (applyPersistentEffects(ON_HEAL, h.get().getSide(), -1)) return;
						}
					}

					switch (h.get().getCombo().getRight()) {
						case BESTIAL -> {
							if (getRound() <= 1)
								h.get().addMana(1);
						}
						case ELF -> {
							if (getRound() > 1 && getRound() - (h.get().getSide() == Side.TOP ? 1 : 0) % 3 == 0)
								h.get().addMana(1);
						}
					}

					if (applyPersistentEffects(BEFORE_TURN, getCurrentSide(), -1)) return;
					for (int i = 0; i < slots.size(); i++) {
						SlotColumn sc = slots.get(i);

						Champion c = sc.getTop();
						if (c != null) {
							if (c.isStasis()) c.reduceStasis();
							else if (c.isStunned()) c.reduceStun();
							else if (c.isSleeping()) c.reduceSleep();

							if (applyEffect(BEFORE_TURN, c, i, getCurrentSide(), Pair.of(c, i), null)
								|| makeFusion(h.get())
							) return;
						}

						if (sc.isUnavailable()) {
							sc.setUnavailable(-1);
						}
					}

					draw = true;
					reportEvent(h.get(), u.getName() + " deseja um acordo de empate, " + getCurrent().getAsMention() + " agora é sua vez, clique em \uD83E\uDD1D caso queira aceitar ou continue jogando normalmente.", false, true);
				}
			});
		if (getRound() > 8)
			buttons.put("\uD83C\uDFF3️", (mb, ms) -> {
				if (getCustom() == null) {
					getHistory().setWinner(getNextSide());
					getBoard().awardWinner(this, getBoard().getPlayers().get(1).getId());
				}

				String msg = getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)";

				close();
				channel.sendMessage(msg)
						.addFile(Helper.writeAndGet(arena.render(this, hands), String.valueOf(this.hashCode()), "jpg"))
						.queue(mm ->
								this.message.compute(mm.getChannel().getId(), (id, m) -> {
									if (m != null)
										m.delete().queue(null, Helper::doNothing);
									return mm;
								}));
			});

		return buttons;
	}

	private void recordLast() {
		Hand top = hands.get(Side.TOP);
		Hand bot = hands.get(Side.BOTTOM);
		getHistory().getRound(getRound() + 1).setSide(getCurrentSide());
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
			Hand h = hands.get(entry.getKey());
			List<Champion> dead = entry.getValue().parallelStream()
					.filter(d -> d instanceof Champion c && c.getMana() <= threshold)
					.map(d -> (Champion) d)
					.collect(Collectors.toList());

			for (Champion c : dead) {
				entry.getValue().remove(c);
			}

			List<Champion> inHand = h.getCards().parallelStream()
					.filter(d -> d instanceof Champion c && c.getMana() <= threshold)
					.map(d -> (Champion) d)
					.collect(Collectors.toList());

			for (Champion c : inHand) {
				h.getCards().remove(c);
			}

			List<Champion> inDeck = h.getDeque().parallelStream()
					.filter(d -> d instanceof Champion c && c.getMana() <= threshold)
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

	public void setFusionLock(int fusionLock) {
		this.fusionLock = fusionLock;
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

	public void setSpellLock(int spellLock) {
		this.spellLock = spellLock;
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

	public void setEffectLock(int effectLock) {
		this.effectLock = effectLock;
	}

	public void addELockTime(int time) {
		effectLock += time;
	}

	public void decreaseELockTime() {
		effectLock = Math.max(0, effectLock - 1);
	}

	public boolean isSlotDisabled(Side side, int slot) {
		return arena.getSlots().get(side).get(slot).isUnavailable();
	}

	public void disableSlot(Side side, int slot, int time) {
		arena.getSlots().get(side).get(slot).setUnavailable(time);
	}

	public List<Drawable> getDiscardBatch() {
		return discardBatch;
	}

	public Set<PersistentEffect> getPersistentEffects() {
		return persistentEffects;
	}

	public void addPersistentEffect(PersistentEffect pe) {
		if (pe == null) return;

		Set<PersistentEffect> aux = Set.copyOf(persistentEffects);
		for (PersistentEffect curr : aux) {
			if (curr.equals(pe)) {
				float bias = Helper.prcnt(pe.getTurns(), Math.max(1, curr.getTurns())) * Helper.prcnt(pe.getLimit(), Math.max(1, curr.getLimit()));

				if (bias > 1) {
					persistentEffects.remove(curr);
					persistentEffects.add(pe);
				}

				return;
			}
		}

		persistentEffects.add(pe);
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	public void removePersistentEffect(Drawable card) {
		persistentEffects.remove(card);
	}

	public boolean applyPersistentEffects(EffectTrigger trigger, Side to, int index) {
		if (persistentEffects.size() > 0) {
			Iterator<PersistentEffect> i = persistentEffects.iterator();
			while (i.hasNext()) {
				PersistentEffect e = i.next();
				if (e.getTarget() == null || e.getTarget() == to) {
					if (trigger == AFTER_TURN && e.getTurns() > 0) {
						e.decreaseTurn();
					}

					if (e.getTriggers().contains(trigger)) {
						e.activate(to, index);
					}
				}

				if (e.getTurns() == 0 || e.getLimit() == 0) {
					channel.sendMessage(":timer: | O efeito " + e.getSource() + " expirou!").queue();
					i.remove();
				}
			}

			return postCombat();
		}

		return false;
	}

	public boolean applyEffect(EffectTrigger trigger, Champion activator, int index, Side side, Pair<Champion, Integer> attacker, Pair<Champion, Integer> defender) {
		if (activator.hasEffect() && effectLock == 0) {
			if ((defender != null && (defender.getLeft().isDuelling() || defender.getLeft().getBonus().popFlag(Flag.NOEFFECT)))
				|| (attacker != null && (attacker.getLeft().isDuelling() || attacker.getLeft().getBonus().popFlag(Flag.NOEFFECT)))
			) return false;

			activator.getEffect(new EffectParameters(trigger, this, index, side, Duelists.of(attacker, defender), channel));
			for (Equipment e : activator.getLinkedTo()) {
				if (e.isParasite() && e.hasEffect())
					applyEffect(trigger, e, index, side);
			}

			return postCombat();
		}

		return false;
	}

	public void applyEffect(EffectTrigger trigger, Champion activator, int index, Side side, Duelists duelists) {
		if (activator.hasEffect() && effectLock == 0) {
			if ((duelists.getDefender() != null && (duelists.getDefender().isDuelling() || duelists.getDefender().getBonus().popFlag(Flag.NOEFFECT)))
				|| (duelists.getAttacker() != null && (duelists.getAttacker().isDuelling() || duelists.getAttacker().getBonus().popFlag(Flag.NOEFFECT)))
			) return;

			activator.getEffect(new EffectParameters(trigger, this, index, side, duelists, channel));
			for (Equipment e : activator.getLinkedTo()) {
				if (e.isParasite() && e.hasEffect())
					applyEffect(trigger, e, index, side);
			}

			postCombat();
		}
	}

	public void applyEffect(EffectTrigger trigger, Equipment activator, int index, Side side) {
		if (activator.hasEffect() && effectLock == 0) {
			activator.getEffect(new EffectParameters(trigger, this, index, side, Duelists.of(null, null), channel));
		}
	}

	public void sendWebhookMessage(String message, String gif, Drawable d) {
		for (TextChannel channel : channel.getChannels()) {
			try {
				Webhook wh = Helper.getOrCreateWebhook(channel, "Shiro");
				Card c = d.getCard();

				WebhookMessageBuilder wmb = new WebhookMessageBuilder()
						.setContent(message)
						.setAvatarUrl("https://api.%s/card?name=%s&anime=%s".formatted(System.getenv("SERVER_URL"), c.getId(), c.getAnime().getName()))
						.setUsername(c.getName());

				if (gif != null) {
					wmb.addEmbeds(new WebhookEmbedBuilder()
							.setImageUrl(ShiroInfo.RESOURCES_URL + "/shoukan/gifs/" + gif + ".gif")
							.build());
				}

				try {
					if (wh == null) return;
					WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();
					wc.send(wmb.build()).get();
				} catch (InterruptedException | ExecutionException e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			} catch (InsufficientPermissionException | InterruptedException | ExecutionException | NullPointerException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		}
	}

	public int getAttributeSum(Side s, boolean attacking) {
		List<SlotColumn> slts = arena.getSlots().get(s);

		return slts.parallelStream()
				.map(SlotColumn::getTop)
				.filter(Objects::nonNull)
				.mapToInt(c -> attacking
						? c.isDefending() ? 0 : c.getFinAtk()
						: c.isDefending() ? c.getFinDef() : c.getFinAtk()
				).sum();
	}

	public boolean isInGraveyard(Side s, String id) {
		return arena.getGraveyard().get(s).parallelStream()
				.map(d -> d.getCard().getId())
				.anyMatch(id::equalsIgnoreCase);
	}

	public Champion evolveTo(Champion from, String to, EffectParameters ep) {
		Hand h = hands.get(ep.getSide());
		Champion nc = CardDAO.getChampion(to);
		assert nc != null;
		if (((h.isNullMode() && !(h.getHp() <= nc.getBaseStats() / 2)) || h.getMana() < nc.getMana()) || h.getHp() <= nc.getBlood())
			return from;

		if (nc.getMana() > 0) {
			if (h.isNullMode())
				h.removeHp(nc.getBaseStats() / 2);
			else
				h.removeMana(nc.getMana());
		}

		if (nc.getBlood() > 0)
			h.removeHp(nc.getBlood());

		nc.bind(h);
		nc.setLinkedTo(from.getLinkedTo());
		nc.setDefending(from.isDefending());
		nc.setFlipped(from.isFlipped());

		int index = ep.getTrigger().isDefensive()
				? ep.getDuelists().getDefenderPos()
				: ep.getDuelists().getAttackerPos();

		banishCard(ep.getSide(), index, false);
		arena.getSlots().get(ep.getSide()).get(index).setTop(nc);
		applyEffect(ON_SUMMON, nc, index, ep.getSide(), ep.getDuelists());
		applyPersistentEffects(ON_SUMMON, ep.getSide(), index);
		return nc;
	}

	public boolean isHeroInField(Side s) {
		Hand h = getHands().get(s);
		if (h.getHero() == null) return false;

		for (SlotColumn sc : arena.getSlots().get(s)) {
			if (sc.getTop() != null && sc.getTop().getHero().equals(h.getHero()))
				return true;
		}

		return false;
	}

	@Override
	public void close() {
		if (!isOpen()) return;
		listener.close();
		recordLast();
		super.close();

		for (Map.Entry<Side, EnumSet<Achievement>> e : achievements.entrySet()) {
			e.getValue().removeIf(a -> !a.isValid(this, e.getKey(), true));
		}

		if (!draw && getCustom() == null) {
			for (Side s : Side.values()) {
				Hand h = hands.get(s);
				if (h instanceof TeamHand th) {
					for (int i = 0; i < 2; i++) {
						Account acc = AccountDAO.getAccount(h.getUser().getId());

						if (acc.hasPendingQuest()) {
							Map<DailyTask, Integer> pg = acc.getDailyProgress();
							DailyQuest dq = DailyQuest.getQuest(Long.parseLong(acc.getUid()));
							int summons = summoned.get(s).getOrDefault(dq.getChosenRace(), 0);
							pg.merge(DailyTask.RACE_TASK, summons, Integer::sum);
							acc.setDailyProgress(pg);
						}

						acc.getAchievements().addAll(achievements.getOrDefault(s, EnumSet.noneOf(Achievement.class)));
						AccountDAO.saveAccount(acc);

						if (h.getHero() != null) {
							CardDAO.saveHero(h.getHero());
						}

						th.next();
					}
				} else {
					Account acc = AccountDAO.getAccount(h.getUser().getId());

					if (acc.hasPendingQuest()) {
						Map<DailyTask, Integer> pg = acc.getDailyProgress();
						DailyQuest dq = DailyQuest.getQuest(Long.parseLong(acc.getUid()));
						int summons = summoned.get(s).getOrDefault(dq.getChosenRace(), 0);
						pg.merge(DailyTask.RACE_TASK, summons, Integer::sum);
						acc.setDailyProgress(pg);
					}

					acc.getAchievements().addAll(achievements.getOrDefault(s, EnumSet.noneOf(Achievement.class)));
					AccountDAO.saveAccount(acc);

					if (h.getHero() != null) {
						CardDAO.saveHero(h.getHero());
					}
				}
			}

			if (tourMatch != null) {
				int winner = switch (getHistory().getWinner()) {
					case TOP -> tourMatch.topIndex();
					case BOTTOM -> tourMatch.botIndex();
				};

				Tournament t = TournamentDAO.getTournament(tourMatch.id());
				if (t.getPhase(tourMatch.phase()).isLast())
					t.setTPResult(winner);
				else
					t.setResult(tourMatch.phase(), winner);

				TournamentDAO.save(t);
			}
		}

		if (!getFrames().isEmpty() && Main.getInfo().getEncoderClient() != null) {
			try {
				getFrames().add(Helper.compress(Helper.atob(getArena().addHands(arena.render(this, hands), hands.values()), "jpg")));
			} catch (IOException ignore) {
			}

			channel.sendMessage("Deseja baixar o replay desta partida?")
					.queue(s -> Pages.buttonize(s, Map.of(
									Helper.ACCEPT, (mb, ms) -> {
										ms.delete().queue(null, Helper::doNothing);
										ms.getChannel().sendMessage("<a:loading:697879726630502401> Aguardando conexão com API...")
												.flatMap(m -> {
													while (!Main.getInfo().isEncoderConnected()) {
														try {
															Main.getInfo().setEncoderClient(new EncoderClient(ShiroInfo.SOCKET_ROOT + "/encoder"));
															Thread.sleep(2000);
														} catch (URISyntaxException | DeploymentException | IOException | InterruptedException ignore) {
														}
													}

													return m.editMessage("<a:loading:697879726630502401> Processando replay...");
												})
												.queue(m -> {
													EmbedBuilder eb = new EmbedBuilder();
													try {
														String url = Main.getInfo().getEncoderClient()
																.requestEncoding(String.valueOf(hashCode()), getFrames())
																.get(1, TimeUnit.HOURS);

														if (url == null) throw new TimeoutException();
														eb.setColor(Color.green)
																.setTitle("Replay pronto!")
																.setDescription("[Clique aqui](" + url + ") para baixar o replay desta partida (o replay poderá ser baixado durante os próximos 30 minutos).");
													} catch (Exception e) {
														Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
														eb.setColor(Color.red)
																.setTitle("Erro!")
																.setDescription("Houve um erro ao processar o replay, meus desenvolvedores já foram notificados.");
													}

													m.editMessage(mb.getUser().getAsMention())
															.setEmbeds(eb.build())
															.queue(null, Helper::doNothing);
												});
									}), true, 1, TimeUnit.MINUTES,
							u -> hands.values().parallelStream().anyMatch(h -> h.getUser().getId().equals(u.getId()))
					));
		}
	}

	public Side getCurrentSide() {
		return getRound() % 2 == 0 ? Side.BOTTOM : Side.TOP;
	}

	public Side getNextSide() {
		return getCurrentSide() == Side.TOP ? Side.BOTTOM : Side.TOP;
	}

	public boolean isTeam() {
		return team;
	}

	public boolean isReroll() {
		return reroll;
	}

	@Override
	public void resetTimer(Shoukan shkn) {
		for (Map.Entry<Side, EnumSet<Achievement>> e : achievements.entrySet()) {
			e.getValue().removeIf(a -> !a.isValid(this, e.getKey(), false));
		}

		getCurrRound().setSide(getCurrentSide());
		decreaseFLockTime();
		decreaseSLockTime();
		decreaseELockTime();

		if (team) ((TeamHand) hands.get(getCurrentSide())).next();
		super.resetTimer(shkn);
	}
}
