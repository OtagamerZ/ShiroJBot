/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.game;

import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.command.misc.SynthesizeCommand;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.ActivationException;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.game.engine.PhaseConstraint;
import com.kuuhaku.game.engine.PlayerAction;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.shoukan.*;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Role;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.shoukan.*;
import com.kuuhaku.model.persistent.shoukan.history.HistoryTurn;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.ClusterAction;
import com.kuuhaku.model.records.PseudoUser;
import com.kuuhaku.model.records.SelectionAction;
import com.kuuhaku.model.records.SelectionCard;
import com.kuuhaku.model.records.shoukan.*;
import com.kuuhaku.util.Bit32;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.collections4.list.TreeList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriConsumer;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Stream;

import static com.kuuhaku.model.enums.shoukan.Trigger.*;

public class Shoukan extends GameInstance<Phase> {
	public static final String GIF_PATH = Constants.SHOUKAN_ASSETS + "gifs/";
	public static final String SKIN_PATH = Constants.SHOUKAN_ASSETS + "skins/";

	private final Arcade arcade;
	private final Arena arena;
	private final Map<Side, Hand> hands;
	private final Map<String, String> messages = new HashMap<>();
	private final Set<EffectOverTime> eots = new HashSet<>();
	private final Set<TriggerBind> bindings = new HashSet<>();
	private final List<HistoryTurn> turns = new TreeList<>();
	private final JSONObject data = new JSONObject();

	private int tick;
	private Side winner;

	private byte state = 0b100;
	/*
	0xF
	  └ 00 11111
	       ││││└ singleplayer
	       │││└─ cheats
	       ││└── restoring
	       │└──── lock
	       └───── sending
	 */

	public Shoukan(I18N locale, Arcade arcade, User p1, User p2) {
		this(locale, arcade, p1.getId(), p2.getId());
	}

	public Shoukan(I18N locale, Arcade arcade, String p1, String p2) {
		super(locale, new String[]{p1, p2});

		this.arcade = arcade;
		this.arena = new Arena(this);

		setSingleplayer(p1.equals(p2));
		this.hands = Map.of(Side.TOP, new Hand(p1, this, Side.TOP), Side.BOTTOM, new Hand(p2, this, Side.BOTTOM));

		setTimeout(turn -> {
			if (getCurrent().selectionPending() && isLocked()) {
				reportResult(GameReport.GAME_TIMEOUT, getCurrentSide(), "str/game_wo", "<@" + getOther().getUid() + ">");
			}

			reportResult(GameReport.GAME_TIMEOUT, getOtherSide(), "str/game_wo", "<@" + getCurrent().getUid() + ">");
		}, 5, TimeUnit.MINUTES);
	}

	@Override
	protected boolean validate(Message message) {
		return isSingleplayer()
			   || getTurn() % 2 == ArrayUtils.indexOf(getPlayers(), message.getAuthor().getId())
			   || hands.values().stream().anyMatch(h -> h.getUid().equals(message.getAuthor().getId()) && h.selectionPending());
	}

	@Override
	protected void begin() {
		setRestoring(false);

		for (Hand h : hands.values()) {
			h.loadCards();
			h.resetDraws();
			h.manualDraw(h.getRemainingDraws());

			if (isNotVoided()) {
				Deck deck = h.getUserDeck();
				if (deck.getEvogearRaw().size() >= 5 && !deck.getFieldsRaw().isEmpty()) {
					if (h.getRealDeck().parallelStream().allMatch(d -> d.getStashRef() != null && d.getStashRef().isChrome())) {
						h.getAccount().setDynValue("all_chrome", true);
					}
				}

				if (h.getCards().parallelStream().filter(d -> d instanceof Field).count() >= 3) {
					h.getAccount().setDynValue("cartographer", true);
				}
			}

			if (h.getOrigins().isPure(Race.BEAST)) {
				double prcnt = Calc.prcntToInt(h.getUserDeck().getEvoWeight(), 24);
				h.getRegDeg().add(Math.max(0, h.getBase().hp() * prcnt), 0);
			}
		}

		setPhase(Phase.PLAN);

		Hand curr = getCurrent();
		curr.modMP(curr.getBase().mpGain().get());

		trigger(ON_TURN_BEGIN, curr.getSide());

		try {
			arena.render(getLocale()).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

		reportEvent("str/game_start", false, false, "<@" + curr.getUid() + ">");
	}

	@Override
	protected void runtime(User user, String value) {
		Side current = getCurrentSide();
		Hand hand = hands.values().stream()
				.sorted(Comparator.comparing(h -> h.getSide() == current, Comparator.reverseOrder()))
				.filter(h -> h.getUid().equals(user.getId()))
				.findFirst().orElseThrow();

		Pair<Method, JSONObject> action = toAction(value.toLowerCase().replace(" ", ""), m -> (!isLocked() || (hand.selectionPending() && m.getName().startsWith("sel"))) && hand.selectionPending() == m.getName().startsWith("sel") || m.getName().startsWith("deb"));

		execAction(hand, action);
	}

	private void execAction(Hand hand, Pair<Method, JSONObject> action) {
		if (action == null) return;

		Method m = action.getFirst();
		try {
			if (isLocked() && (!m.getName().startsWith("sel") && !m.getName().startsWith("deb"))) {
				return;
			} else if (m.getName().startsWith("deb")) {
				setCheated(true);
			}

			setLocked(true);
			m.invoke(this, hand.getSide(), action.getSecond());
		} catch (ActivationException e) {
			getChannel().sendMessage(getString(e.getMessage())).queue();
		} catch (Exception e) {
			if (e.getCause() instanceof StackOverflowError) {
				Constants.LOGGER.error("Fatal error at {}", m.getName(), e);
				getChannel().sendMessage(getString("error/match_termination", GameReport.STACK_OVERFLOW)).queue();
				close(GameReport.STACK_OVERFLOW);
				return;
			}

			Constants.LOGGER.error("Failed to execute method {}", m.getName(), e);
		} finally {
			setLocked(false);
		}
	}

	@PlayerAction("reload")
	private boolean reload(Side side, JSONObject args) {
		reportEvent("str/game_reload", false, false, getCurrent().getName());
		return true;
	}

	// DEBUG START

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("set_name,(?<name>\\S+)")
	private boolean debSetName(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			String name = args.getString("name");
			curr.setName(name);

			reportEvent("SET_NAME -> " + name, false, false);
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("set_hp,(?<value>\\d+)")
	private boolean debSetHp(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			int val = args.getInt("value");
			curr.setHP(val);

			reportEvent("SET_HP -> " + val, false, false);
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("set_mp,(?<value>\\d+)")
	private boolean debSetMp(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			int val = args.getInt("value");
			curr.setMP(val);

			reportEvent("SET_MP -> " + val, false, false);
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("set_origin,(?<major>\\w+)(?:,(?<minor>[\\w,]+))?")
	private boolean debSetOrigin(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			Race major = args.getEnum(Race.class, "major", Race.NONE);

			Set<Race> minors = new HashSet<>();
			JSONArray races = new JSONArray(Arrays.asList(args.getString("minor").split(",")));
			for (int i = 0; i < races.size(); i++) {
				Race minor = races.getEnum(Race.class, i);
				if (minor != null) {
					minors.add(minor);
				}
			}

			curr.setOrigin(Origin.from(curr.getUserDeck().isVariant(), major, minors.toArray(Race[]::new)));
			reportEvent("SET_ORIGIN -> " + curr.getOrigins(), false, false);
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("add(?<cards>(,[\\w-]+)+)")
	private boolean debAddCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			String ids = args.getString("cards").toUpperCase();
			List<String> added = new ArrayList<>();

			for (String id : ids.split(",")) {
				CardType type = Bit32.toEnumSet(CardType.class, DAO.queryNative(Integer.class, "SELECT get_type(?1)", id)).stream().findFirst().orElse(CardType.KAWAIPON);

				Drawable<?> d = switch (type) {
					case KAWAIPON, SENSHI -> DAO.find(Senshi.class, id);
					case EVOGEAR -> DAO.find(Evogear.class, id);
					case FIELD -> DAO.find(Field.class, id);
				};

				if (d != null) {
					added.add(id);
					curr.getCards().add(d.copy());
				}
			}

			if (!added.isEmpty()) {
				reportEvent("ADD_CARD -> " + String.join(", ", added), false, false);
				return true;
			}
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("undraw(?:,(?<amount>\\d+))?")
	private boolean debUndraw(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			int valid = (int) curr.getCards().parallelStream().filter(Drawable::isAvailable).count();

			int amount = Math.min(args.getInt("amount", 1), valid);
			for (int i = 0; i < amount; i++) {
				curr.getRealDeck().add(curr.getCards().removeLast(Drawable::isAvailable));
			}

			reportEvent("UNDRAW -> " + amount, false, false);
			return true;
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("next_tick")
	private boolean debApplyTick(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			reportEvent("NEXT_TICK", true, false);
			return true;
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("save_history")
	private boolean debSaveHistory(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			new MatchHistory(this, "none", getTurns()).save();

			reportEvent("SAVE_HISTORY", true, false);
			return true;
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("terminate")
	private boolean debTerminate(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			reportResult(GameReport.SUCCESS, null, "GAME_TERMINATE");
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("set_val,(?<key>\\w+)=(?<value>.+)")
	private boolean debSetVal(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			String key = args.getString("key");
			String value = args.getString("value");
			if (key.isBlank() || value.isBlank()) return false;

			data.put(key, value);
			reportEvent("SET_VAL -> " + key + " = " + value, true, false);
			return true;
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("empower,(?<indexes>\\d+(,\\d+)*)")
	private boolean debEmpower(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (Account.hasRole(curr.getUid(), false, Role.TESTER)) {
			String ids = args.getString("indexes");
			List<String> added = new ArrayList<>();

			for (String idx : ids.split(",")) {
				int i = Integer.parseInt(idx) - 1;
				if (Utils.between(i, 0, curr.getCards().size())) {
					if (curr.getCards().get(i) instanceof EffectHolder<?> eh) {
						eh.setFlag(Flag.EMPOWERED);
						added.add(eh.getId());
					}
				}
			}

			if (!added.isEmpty()) {
				reportEvent("EMPOWER -> " + String.join(", ", added), false, false);
				return true;
			}
		}

		return false;
	}

	// DEBUG END

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\d+),(?<mode>[adb]),(?<inField>[1-5])(?<notCombat>,nc)?")
	private boolean placeCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!Utils.between(args.getInt("inHand"), 1, curr.getCards().size())) {
			getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
			return false;
		}

		Drawable<?> d = curr.getCards().get(args.getInt("inHand") - 1);
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		if (curr.getOrigins().synergy() == Race.LICH && curr.hasSummoned()) {
			getChannel().sendMessage(getString("error/already_summoned")).queue();
			return false;
		}

		return putCard(curr, d, args);
	}

	private boolean putCard(Hand curr, Drawable<?> d, JSONObject args) {
		if (d instanceof Senshi s) {
			if (isInvalidAction(curr, s)) return false;
		} else {
			if (d instanceof Evogear e && e.isSpell() && args.getString("mode").equals("b")) {
				return putCard(curr, new TrapSpell(e), args);
			} else if (curr.getLockTime(Lock.BLIND) > 0) {
				CardState state = switch (args.getString("mode")) {
					case "d" -> CardState.DEFENSE;
					case "b" -> CardState.FLIPPED;
					default -> CardState.ATTACK;
				};

				blindFail(curr, d, "str/place_card_fail", curr.getName(), d, state.toString(getLocale()));
				return true;
			}

			getChannel().sendMessage(getString("error/wrong_card_type")).queue();
			return false;
		}

		SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);
		if (slot.isLocked()) {
			int time = slot.getLock();

			if (time == -1) {
				getChannel().sendMessage(getString("error/slot_locked_perma")).queue();
			} else {
				getChannel().sendMessage(getString("error/slot_locked", time)).queue();
			}
			return false;
		}

		if ((args.has("notCombat") && slot.hasBottom()) || (!args.has("notCombat") && slot.hasTop())) {
			getChannel().sendMessage(getString("error/slot_occupied")).queue();
			return false;
		}

		int mult = 1;
		if (curr.getOrigins().synergy() == Race.DULLAHAN) {
			mult = 2;
		}

		switch (args.getString("mode")) {
			case "d" -> s.setDefending(true);
			case "b" -> s.setFlipped(true);
		}

		if (curr.getOrigins().synergy() != Race.HERALD || curr.hasSummoned()) {
			curr.consumeHP(s.getHPCost() / mult);
			curr.consumeMP(s.getMPCost() / mult);
		}

		List<Drawable<?>> consumed = curr.consumeSC(s.getSCCost() / mult);
		if (!consumed.isEmpty()) {
			s.getStats().getData().put("consumed", consumed);
		}

		if (args.has("notCombat")) {
			slot.setBottom(s);
		} else {
			slot.setTop(s);
		}

		curr.setSummoned(true);
		reportEvent("str/place_card", true, false, curr.getName(), s, s.getState().toString(getLocale()));
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\d+),(?<inField>[1-5])")
	private boolean equipCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!Utils.between(args.getInt("inHand"), 1, curr.getCards().size())) {
			getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
			return false;
		}

		Drawable<?> d = curr.getCards().get(args.getInt("inHand") - 1);
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		if (d instanceof Senshi s && curr.getOrigins().synergy() == Race.SHIKIGAMI) {
			d = new EquippableSenshi(s);

			EquippableSenshi es = (EquippableSenshi) d;
			es.getStats().getData().put("_shiki", true);
			es.getStats().getAttrMult().set(-0.4);
		}

		if (d instanceof Evogear chosen && !chosen.isSpell()) {
			if (isInvalidAction(curr, d)) return false;
		} else {
			if (curr.getLockTime(Lock.BLIND) > 0) {
				curr.getGraveyard().add(d);
				curr.modLockTime(Lock.BLIND, chance(50) ? -1 : 0);

				if (d instanceof EquippableSenshi es) {
					curr.getCards().remove(es.getOriginal());
				}

				SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);
				if (!slot.hasTop()) {
					getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
					return false;
				}

				Senshi target = slot.getTop();
				reportEvent("str/equip_card_fail", true, false, curr.getName(), d, target);
				return true;
			}

			getChannel().sendMessage(getString("error/wrong_card_type")).queue();
			return false;
		}

		SlotColumn slot;
		if (chosen.isParasite()) {
			slot = arena.getSlots(curr.getSide().getOther()).get(args.getInt("inField") - 1);
		} else {
			slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);
		}

		if (!slot.hasTop()) {
			getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
			return false;
		}

		Senshi target = slot.getTop();
		if (target.getEquipments().stream().anyMatch(e -> chosen instanceof EquippableSenshi && e.getStats().getData().has("_shiki"))) {
			getChannel().sendMessage(getString("error/only_one_shikigami")).queue();
			return false;
		}

		curr.consumeHP(chosen.getHPCost());
		curr.consumeMP(chosen.getMPCost());
		List<Drawable<?>> consumed = curr.consumeSC(chosen.getSCCost());
		if (!consumed.isEmpty()) {
			chosen.getStats().getData().put("consumed", consumed);
		}

		if (d instanceof EquippableSenshi es) {
			curr.getCards().remove(es.getOriginal());
		}

		target.getEquipments().add(chosen);
		reportEvent("str/equip_card", true, false, curr.getName(), chosen, target);
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\d+)f")
	private boolean placeField(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!Utils.between(args.getInt("inHand"), 1, curr.getCards().size())) {
			getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
			return false;
		}

		Drawable<?> d = curr.getCards().get(args.getInt("inHand") - 1);
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		if (!(d instanceof Field chosen)) {
			if (curr.getLockTime(Lock.BLIND) > 0) {
				curr.getGraveyard().add(d);
				curr.modLockTime(Lock.BLIND, chance(50) ? -1 : 0);

				reportEvent("str/equip_card_fail", true, false, curr.getName(), d);
				return true;
			}

			getChannel().sendMessage(getString("error/wrong_card_type")).queue();
			return false;
		}

		arena.setField(chosen);
		reportEvent("str/place_field", true, false, curr.getName(), chosen);
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inField>[1-5]),f(?<notCombat>,nc)?")
	private boolean flipCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);

		boolean nc = args.has("notCombat");
		if ((nc && !slot.hasBottom()) || (!nc && !slot.hasTop())) {
			getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
			return false;
		}

		Senshi d = nc ? slot.getBottom() : slot.getTop();
		if (d.hasStatusEffect()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		} else if (d.hasSwitched()) {
			getChannel().sendMessage(getString("error/card_switched")).queue();
			return false;
		}

		if (d.isFlipped()) {
			d.setFlipped(false);
		} else {
			d.setDefending(!d.isDefending());
		}

		d.setSwitched(true);
		reportEvent("str/flip_card", true, false, curr.getName(), d, d.getState().toString(getLocale()));
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inField>[1-5]),p")
	private boolean promoteCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);

		if (!slot.hasBottom()) {
			getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
			return false;
		} else if (slot.hasTop()) {
			getChannel().sendMessage(getString("error/promote_blocked")).queue();
			return false;
		}

		Senshi d = slot.getBottom();
		if (d.hasStatusEffect()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		slot.swap();

		reportEvent("str/promote_card", true, false, curr.getName(), d);
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inField>[1-5]),s(?<notCombat>,nc)?")
	private boolean sacrificeCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);

		boolean nc = args.has("notCombat");
		if ((nc && !slot.hasBottom()) || (!nc && !slot.hasTop())) {
			getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
			return false;
		}

		double mult = 0.5;
		if (curr.getOther().getOrigins().synergy() == Race.INFERNAL) {
			mult *= 2;
		}

		Senshi chosen = nc ? slot.getBottom() : slot.getTop();
		if (chosen.hasStatusEffect()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		} else if (chosen.isFixed()) {
			getChannel().sendMessage(getString("error/card_fixed")).queue();
			return false;
		} else if ((int) (chosen.getHPCost() * mult) >= curr.getHP()) {
			getChannel().sendMessage(getString("error/not_enough_hp_sacrifice")).queue();
			return false;
		} else if ((int) (chosen.getMPCost() * mult) > curr.getMP()) {
			getChannel().sendMessage(getString("error/not_enough_mp_sacrifice")).queue();
			return false;
		}

		curr.consumeHP((int) (chosen.getHPCost() * mult));
		curr.consumeMP((int) (chosen.getMPCost() * mult));

		trigger(ON_SACRIFICE, chosen.asSource(ON_SACRIFICE));

		curr.getGraveyard().add(chosen);

		reportEvent("str/sacrifice_card", true, false, curr.getName(), chosen);
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inField>\\[[1-5](,[1-5])*]),s(?<notCombat>,nc)?")
	private boolean sacrificeBatch(Side side, JSONObject args) {
		Hand curr = hands.get(side);

		int hp = 0;
		int mp = 0;

		List<Drawable<?>> cards = new ArrayList<>();
		JSONArray batch = new JSONArray(args.getString("inField"));
		for (Object o : batch) {
			int idx = ((Number) o).intValue();
			SlotColumn slot = arena.getSlots(curr.getSide()).get(idx - 1);

			boolean nc = args.has("notCombat");
			if ((nc && !slot.hasBottom()) || (!nc && !slot.hasTop())) {
				getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
				return false;
			}

			double mult = 0.5;
			if (curr.getOther().getOrigins().synergy() == Race.INFERNAL) {
				mult *= 2;
			}

			Senshi chosen = nc ? slot.getBottom() : slot.getTop();
			if (chosen.hasStatusEffect()) {
				getChannel().sendMessage(getString("error/card_unavailable")).queue();
				return false;
			} else if (chosen.isFixed()) {
				getChannel().sendMessage(getString("error/card_fixed")).queue();
				return false;
			} else if ((int) (chosen.getHPCost() * mult) >= curr.getHP()) {
				getChannel().sendMessage(getString("error/not_enough_hp_sacrifice")).queue();
				return false;
			} else if ((int) (chosen.getMPCost() * mult) > curr.getMP()) {
				getChannel().sendMessage(getString("error/not_enough_mp_sacrifice")).queue();
				return false;
			}

			hp += chosen.getHPCost() / 2;
			mp += chosen.getMPCost() / 2;
			cards.add(chosen);
		}

		curr.consumeHP(hp);
		curr.consumeMP(mp);

		for (Drawable<?> chosen : cards) {
			trigger(ON_SACRIFICE, chosen.asSource(ON_SACRIFICE));
		}

		curr.getGraveyard().addAll(cards);

		reportEvent("str/sacrifice_card", true, false, curr.getName(), Utils.properlyJoin(getString("str/and")).apply(cards.stream().map(Drawable::toString).toList()));
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\d+),d")
	private boolean discardCard(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!Utils.between(args.getInt("inHand"), 1, curr.getCards().size())) {
			getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
			return false;
		}

		Drawable<?> d = curr.getCards().get(args.getInt("inHand") - 1);
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		curr.getDiscard().add(d);

		if (curr.getOrigins().synergy() == Race.FAMILIAR && d instanceof Senshi s) {
			for (Drawable<?> c : curr.getCards()) {
				if (c.isAvailable() && c instanceof Senshi it && it.getElement() == s.getElement()) {
					it.getStats().getMana().set(-1);
				}
			}
		}

		reportEvent("str/discard_card", true, false, curr.getName(), d);
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\[\\d+(,\\d+)*]),d")
	private boolean discardBatch(Side side, JSONObject args) {
		Hand curr = hands.get(side);

		List<Drawable<?>> cards = new ArrayList<>();
		JSONArray batch = new JSONArray(args.getString("inHand"));
		for (Object o : batch) {
			int idx = ((Number) o).intValue();
			if (!Utils.between(idx, 1, curr.getCards().size())) {
				getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
				return false;
			}

			Drawable<?> d = curr.getCards().get(idx - 1);
			if (!d.isAvailable() || d.isManipulated()) {
				getChannel().sendMessage(getString("error/card_unavailable")).queue();
				return false;
			}

			cards.add(d);
		}

		curr.getDiscard().addAll(cards);

		if (curr.getOrigins().synergy() == Race.FAMILIAR) {
			for (Drawable<?> c : cards) {
				if (c instanceof Senshi s) {
					for (Drawable<?> d : curr.getCards()) {
						if (d.isAvailable() && d instanceof Senshi it && it.getElement() == s.getElement()) {
							it.getStats().getMana().set(-1);
						}
					}
				}
			}
		}

		reportEvent("str/discard_card", true, false, curr.getName(), Utils.properlyJoin(getString("str/and")).apply(cards.stream().map(Drawable::toString).toList()));
		return true;
	}

	@PhaseConstraint("PLAN")
	@PlayerAction("(?<inHand>\\d+)s(?:,(?<target1>[1-5]))?(?:,(?<target2>[1-5]))?")
	private boolean activate(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!Utils.between(args.getInt("inHand"), 1, curr.getCards().size())) {
			getChannel().sendMessage(getString("error/invalid_hand_index")).queue();
			return false;
		}

		Drawable<?> d = curr.getCards().get(args.getInt("inHand") - 1);
		if (!d.isAvailable() || d.isManipulated()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		}

		if (d instanceof Evogear chosen && chosen.isSpell()) {
			if (isInvalidAction(curr, d)) return false;
		} else {
			if (curr.getLockTime(Lock.BLIND) > 0) {
				blindFail(curr, d, "str/activate_card_fail", d);
				return true;
			}

			getChannel().sendMessage(getString("error/wrong_card_type")).queue();
			return false;
		}

		Targeting tgt = switch (chosen.getTargetType()) {
			case NONE -> new Targeting(curr, -1, -1);
			case ALLY -> new Targeting(curr, args.getInt("target1") - 1, -1);
			case ENEMY -> new Targeting(curr, -1, args.getInt("target1") - 1);
			case BOTH -> new Targeting(curr, args.getInt("target1") - 1, args.getInt("target2") - 1);
		};

		List<Drawable<?>> stack = (chosen.getTier() > 3 ? arena.getBanned() : curr.getGraveyard());
		if (!tgt.validate(chosen.getTargetType())) {
			getChannel().sendMessage(getString("error/target", getString("str/target_" + chosen.getTargetType()))).queue();
			return false;
		}

		List<Drawable<?>> consumed = curr.consumeSC(chosen.getSCCost());
		if (!consumed.isEmpty()) {
			chosen.getStats().getData().put("consumed", consumed);
		}

		try {
			if (!chosen.execute(chosen.toParameters(tgt))) {
				if (!chosen.isAvailable()) {
					if (!chosen.hasFlag(Flag.FREE_ACTION, true)) {
						stack.add(chosen);
					}

					reportEvent("str/effect_interrupted", true, false, chosen);
					return true;
				}

				return false;
			}
		} finally {
			curr.consumeHP(chosen.getHPCost());
			curr.consumeMP(chosen.getMPCost());
		}

		if (!chosen.hasFlag(Flag.FREE_ACTION, true)) {
			stack.add(chosen);
		}

		reportEvent("str/activate_card", true, false, curr.getName(), chosen);
		return true;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("(?<choices>\\d+(,\\d+)*)")
	private boolean selSelect(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!curr.selectionPending()) return false;

		SelectionAction selection = curr.getSelection();
		String[] choices = args.getString("choices").split(",");

		List<Integer> indexes = new ArrayList<>();
		for (String choice : choices) {
			int idx = Integer.parseInt(choice) - 1;
			if (!Utils.between(idx, 0, selection.cards().size() - 1)) {
				getChannel().sendMessage(getString("error/invalid_selection_index")).queue();
				return false;
			}

			indexes.add(idx);
		}

		for (Integer idx : indexes) {
			List<Integer> selIdxs = selection.indexes();
			if (selIdxs.contains(idx)) {
				selIdxs.remove(idx);
			} else {
				selIdxs.add(idx);
			}

			if (selIdxs.size() > selection.range().max()) {
				selIdxs.removeFirst();
			}
		}

		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("ok")
	private boolean selConfirm(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		if (!curr.selectionPending()) return false;

		SelectionAction sel = curr.getSelection();
		if (sel.indexes().size() < sel.range().min()) {
			getChannel().sendMessage(getString("error/wrong_selection_amount", sel.range().min())).queue();
			return false;
		}

		List<Drawable<?>> cards = new ArrayList<>();
		for (int i : sel.indexes()) {
			cards.add(sel.cards().get(i).card());
		}

		int tick = this.tick;
		curr.getSelection().result()
				.add(t -> {
					if (tick == this.tick) {
						if (sel.source() != null) {
							reportEvent(getString("str/effect_choice", curr.getName(), sel.indexes().size(), sel.source()), true, false);
						} else {
							reportEvent(getString("str/effect_choice_ns", curr.getName(), sel.indexes().size()), true, false);
						}
					}

					return t;
				})
				.process(cards);
		return false;
	}

	@PhaseConstraint({"PLAN", "COMBAT"})
	@PlayerAction("(?<inField>[1-5])a(?:,(?<target1>[1-5]))?(?:,(?<target2>[1-5]))?")
	private boolean special(Side side, JSONObject args) {
		Hand curr = hands.get(side);
		SlotColumn slot = arena.getSlots(curr.getSide()).get(args.getInt("inField") - 1);

		if (!slot.hasTop()) {
			getChannel().sendMessage(getString("error/missing_card", slot.getIndex() + 1)).queue();
			return false;
		}

		Senshi d = slot.getTop();
		if (d.hasStatusEffect()) {
			getChannel().sendMessage(getString("error/card_unavailable")).queue();
			return false;
		} else if (d.isFlipped()) {
			getChannel().sendMessage(getString("error/card_flipped")).queue();
			return false;
		} else if (d.getCooldown() > 0) {
			getChannel().sendMessage(getString("error/card_cooldown", d.getCooldown())).queue();
			return false;
		} else if (getPhase() == Phase.PLAN && curr.getMP() < 1) {
			getChannel().sendMessage(getString("error/not_enough_mp")).queue();
			return false;
		} else if (!d.hasAbility()) {
			getChannel().sendMessage(getString("error/card_no_special")).queue();
			return false;
		} else if (d.isSealed()) {
			getChannel().sendMessage(getString("error/card_sealed")).queue();
			return false;
		}

		int locktime = Math.max(curr.getLockTime(Lock.ABILITY), curr.getLockTime(Lock.EFFECT));
		if (locktime > 0 && !d.hasTrueEffect()) {
			getChannel().sendMessage(getString("error/ability_locked", locktime)).queue();
			return false;
		}

		locktime = curr.getLockTime(Lock.EFFECT);
		if (locktime > 0 && !d.hasTrueEffect()) {
			getChannel().sendMessage(getString("error/effect_locked", locktime)).queue();
			return false;
		}

		TargetType type = d.getTargetType();
		Targeting tgt = switch (type) {
			case NONE -> new Targeting(curr, -1, -1);
			case ALLY -> new Targeting(curr, args.getInt("target1") - 1, -1);
			case ENEMY -> new Targeting(curr, -1, args.getInt("target1") - 1);
			case BOTH -> new Targeting(curr, args.getInt("target1") - 1, args.getInt("target2") - 1);
		};

		Senshi enemy = tgt.enemy();
		if (enemy != null) {
			if (d.getTarget() != null && !Objects.equals(d.getTarget(), enemy)) {
				getChannel().sendMessage(getString("error/card_taunted", d.getTarget(), d.getTarget().getIndex() + 1)).queue();
				return false;
			}
		}

		if (!tgt.validate(type)) {
			getChannel().sendMessage(getString("error/target", getString("str/target_" + type))).queue();
			return false;
		}

		if (!trigger(ON_ACTIVATE, d.asSource(ON_ACTIVATE), tgt.targets(ON_EFFECT_TARGET))) {
			if (!d.isAvailable()) {
				curr.consumeMP(1);
				reportEvent("str/effect_interrupted", true, false, d);
				return true;
			}

			return false;
		}

		if (getPhase() == Phase.PLAN) {
			curr.consumeMP(1);
		} else if (!d.hasFlag(Flag.FREE_ACTION, true)) {
			d.setAvailable(false);
		}

		reportEvent("str/card_special", true, false, curr.getName(), d);
		return !curr.selectionPending();
	}

	@PhaseConstraint("COMBAT")
	@PlayerAction("(?<inField>[1-5]),self")
	private boolean selfDamage(Side side, JSONObject args) {
		Hand you = hands.get(side);
		if (you.getLockTime(Lock.TAUNT) == 0) {
			getChannel().sendMessage(getString("error/not_taunted")).queue();
			return false;
		}

		SlotColumn yourSlot = arena.getSlots(you.getSide()).get(args.getInt("inField") - 1);

		if (!yourSlot.hasTop()) {
			getChannel().sendMessage(getString("error/missing_card", yourSlot.getIndex() + 1)).queue();
			return false;
		}

		Senshi ally = yourSlot.getTop();
		attack(ally, you, SendMode.REGULAR, SendMode.SEND);

		return false;
	}

	@PhaseConstraint("COMBAT")
	@PlayerAction("(?<inField>[1-5])(?:,(?<target>[1-5]))?")
	private boolean attack(Side side, JSONObject args) {
		Hand you = hands.get(side);
		SlotColumn yourSlot = arena.getSlots(you.getSide()).get(args.getInt("inField") - 1);

		if (!yourSlot.hasTop()) {
			getChannel().sendMessage(getString("error/missing_card", yourSlot.getIndex() + 1)).queue();
			return false;
		}

		Senshi ally = yourSlot.getTop();
		Hand op = you.getOther();

		Senshi enemy = null;
		if (args.has("target")) {
			SlotColumn opSlot = arena.getSlots(op.getSide()).get(args.getInt("target") - 1);

			if (!opSlot.hasTop()) {
				if (!opSlot.hasBottom()) {
					getChannel().sendMessage(getString("error/missing_card", opSlot.getIndex() + 1)).queue();
					return false;
				}

				enemy = opSlot.getBottom();
			} else {
				enemy = opSlot.getTop();
			}
		}

		if (enemy == null) {
			attack(ally, op, SendMode.REGULAR, SendMode.SEND);
		} else {
			attack(ally, enemy, SendMode.REGULAR, SendMode.SEND);
		}

		return false;
	}

	private boolean isInvalidAction(Hand curr, Drawable<?> card) {
		if (card instanceof Evogear e && e.isSpell()) {
			if (e.isPassive()) {
				getChannel().sendMessage(getString("error/card_passive")).queue();
				return true;
			}

			int locktime = Math.max(curr.getLockTime(Lock.SPELL), curr.getLockTime(Lock.EFFECT));
			if (locktime > 0 && !e.hasTrueEffect()) {
				getChannel().sendMessage(getString("error/spell_locked", locktime)).queue();
				return true;
			}
		}

		double mult = 1;
		if (curr.getOrigins().synergy() == Race.DULLAHAN) {
			mult = 0.5;
		}

		if (card.getHPCost() * mult >= curr.getHP()) {
			getChannel().sendMessage(getString("error/not_enough_hp")).queue();
			return true;
		} else if (card.getMPCost() * mult > curr.getMP()) {
			getChannel().sendMessage(getString("error/not_enough_mp")).queue();
			return true;
		} else if (card.getSCCost() * mult > curr.getDiscard().size()) {
			getChannel().sendMessage(getString("error/not_enough_sc")).queue();
			return true;
		}

		return false;
	}

	private void blindFail(Hand curr, Drawable<?> card, String message, Object... args) {
		curr.getGraveyard().add(card);
		curr.modLockTime(Lock.BLIND, chance(50) ? -1 : 0);

		if (card instanceof Senshi) {
			curr.setSummoned(true);
		}

		reportEvent(message, true, false, curr.getName(), args);
	}

	public boolean attack(Senshi source, Senshi target, SendMode... mode) {
		return attack(source, target, null, mode);
	}

	public boolean attack(Senshi source, Senshi target, Integer damage, SendMode... mode) {
		return processAttack(source, target, target.getSide(), damage, mode);
	}

	public boolean attack(Senshi source, Hand target, SendMode... mode) {
		return attack(source, target, null, mode);
	}

	public boolean attack(Senshi source, Hand target, Integer damage, SendMode... mode) {
		return processAttack(source, null, target.getSide(), damage, mode);
	}

	private boolean processAttack(Senshi source, @Nullable Senshi target, Side tgtSide, Integer damage, SendMode... mode) {
		if (isClosed()) return false;
		Set<SendMode> md = EnumSet.of(SendMode.NONE, mode);

		if (source == null || ((md.contains(SendMode.REGULAR) && !source.canAttack()) || !source.isAvailable())) {
			if (md.contains(SendMode.SEND)) {
				getChannel().sendMessage(getString("error/card_cannot_attack")).queue();
			}

			return false;
		}

		Hand you = source.getHand();
		Hand op = hands.get(tgtSide);
		int pHP = you.getHP();
		int eHP = op.getHP();

		if (source.getTarget() != null && !Objects.equals(source.getTarget(), target)) {
			if (md.contains(SendMode.SEND)) {
				getChannel().sendMessage(getString("error/card_taunted", source.getTarget(), source.getTarget().getIndex() + 1)).queue();
			}

			return false;
		}

		int posHash = 0;
		if (target != null) {
			if (source.isDefending() && target.isStunned()) {
				if (md.contains(SendMode.SEND)) {
					getChannel().sendMessage(getString("error/target_stunned_in_def")).queue();
				}

				return false;
			} else if (target.isStasis() || target.getIndex() == -1) {
				if (md.contains(SendMode.SEND)) {
					getChannel().sendMessage(getString("error/card_untargetable")).queue();
				}

				return false;
			}

			Target t = target.asTarget(ON_DEFEND);
			posHash = target.posHash();
			trigger(ON_ATTACK, source.asSource(ON_ATTACK), t);
		} else {
			if (!arena.isFieldEmpty(tgtSide) && !source.hasFlag(Flag.DIRECT, true)) {
				if (md.contains(SendMode.SEND)) {
					getChannel().sendMessage(getString("error/field_not_empty")).queue();
				}

				return false;
			}

			trigger(ON_DIRECT, source.asSource(ON_DIRECT));
			trigger(ON_ATTACK, source.asSource(ON_ATTACK));
		}

		if (damage == null) {
			damage = source.getActiveAttr();
		}

		int dmg = damage;
		int direct = 0;
		int lifesteal = you.getBase().lifesteal() + (int) source.getStats().getLifesteal().get();
		if (you.getOrigins().synergy() == Race.VAMPIRE && you.isLowLife()) {
			lifesteal += 7;
		}

		int thorns = (you.getLockTime(Lock.CHARM) > 0 ? 20 : 0);
		if (target != null) {
			thorns += (int) target.getStats().getThorns().get();
		}

		double dmgMult = 1;
		if (dmg > 0 && (getTurn() < 3 || you.getLockTime(Lock.TAUNT) > 0 || source.hasCharm(Charm.BARRAGE))) {
			dmgMult /= 2;
		}

		boolean hit = true;
		boolean win = false;
		String outcome = "";
		try {
			boolean validTarget = true;
			if (target != null) {
				validTarget = posHash == target.posHash();
				outcome = getString("str/combat_skip");
			}

			if (validTarget && ((md.contains(SendMode.REGULAR) && source.canAttack()) || source.isAvailable())) {
				boolean ignore = source.hasFlag(Flag.NO_COMBAT, true);
				if (target != null) {
					if (!ignore) {
						ignore = target.getIndex() == -1 || target.hasFlag(Flag.IGNORE_COMBAT, true);
					}

					if (!ignore) {
						target.setFlipped(false);

						if (target.isSupporting()) {
							outcome = getString("str/combat_success", dmg, 0);

							for (Senshi s : target.getNearby()) {
								s.awaken();
							}

							op.getGraveyard().add(target);

							dmg = 0;
							win = true;
						} else {
							boolean dbl = op.getOrigins().synergy() == Race.CYBERBEAST && chance(20);
							boolean unstop = source.hasFlag(Flag.UNSTOPPABLE, true);

							int enemyStats = target.getActiveAttr(dbl);
							if (source.hasFlag(Flag.IGNORE_EQUIP, true)) {
								enemyStats -= target.getActiveEquips(dbl);
							}

							if (!unstop && dmg < enemyStats) {
								outcome = getString("str/combat_defeat", dmg, enemyStats);
								trigger(ON_SUICIDE, source.asSource(ON_SUICIDE));

								for (Senshi s : source.getNearby()) {
									s.awaken();
								}

								if (md.contains(SendMode.REGULAR)) {
									if (!source.hasFlag(Flag.NO_DAMAGE, true)) {
										you.modHP((int) -((enemyStats - dmg) * dmgMult));
									}

									you.getGraveyard().add(source);
								}

								dmg = 0;
							} else {
								int parry = target.getParry();
								int dodge = target.getDodge();
								boolean tParry, tDodge;

								if (!source.hasFlag(Flag.TRUE_STRIKE, true)) {
									if (source.isBlinded(true) && chance(50)) {
										outcome = getString("str/combat_miss");
										trigger(ON_MISS, source.asSource(ON_MISS));

										dmg = 0;
										hit = false;
									} else if (!unstop && target.isAvailable() && ((tDodge = target.hasFlag(Flag.TRUE_PARRY, true)) || chance(parry))) {
										outcome = getString("str/combat_parry", parry);
										if (tDodge) {
											outcome += " **(" + getString("flag/true_parry") + ")**";
										}

										trigger(NONE, source.asSource(), target.asTarget(ON_PARRY));
										attack(target, source);
										source.setAvailable(false);

										dmg = 0;
										hit = false;
									} else if ((tParry = target.hasFlag(Flag.TRUE_DODGE, true)) || chance(dodge)) {
										outcome = getString("str/combat_dodge", dodge);
										if (tParry) {
											outcome += " **(" + getString("flag/true_dodge") + ")**";
										}

										trigger(ON_MISS, source.asSource(ON_MISS), target.asTarget(ON_DODGE));

										dmg = 0;
										hit = false;
									}
								}

								if (hit) {
									if (unstop || dmg > enemyStats) {
										outcome = getString("str/combat_success", dmg, enemyStats);
										if (unstop) {
											outcome += " **(" + getString("flag/unstoppable") + ")**";
										}

										trigger(ON_HIT, source.asSource(ON_HIT), target.asTarget(ON_LOSE));

										if (target.isDefending() || target.hasFlag(Flag.NO_DAMAGE, true)) {
											dmg = 0;
										} else {
											dmg = Math.max(0, dmg - enemyStats);
										}

										for (Senshi s : target.getNearby()) {
											s.awaken();
										}

										if (source.isDefending() && !source.hasFlag(Flag.ALWAYS_ATTACK, true)) {
											int duration;
											if (enemyStats == 0) {
												duration = 5;
											} else {
												duration = Utils.clamp(dmg / enemyStats, 1, 5);
											}

											if (you.getOrigins().synergy() == Race.ELEMENTAL) {
												int earth = (int) getCards(you.getSide()).parallelStream()
														.filter(s -> s.getElement() == ElementType.EARTH)
														.count();

												if (earth >= 4) {
													duration *= 2;
												}
											}

											target.setStun(duration);
											dmg = 0;
										} else {
											op.getGraveyard().add(target);
										}

										win = true;
									} else {
										outcome = getString("str/combat_clash", dmg, enemyStats);
										trigger(ON_CLASH, source.asSource(ON_SUICIDE), target.asTarget(ON_LOSE));

										for (Senshi s : target.getNearby()) {
											s.awaken();
										}

										op.getGraveyard().add(target);

										for (Senshi s : source.getNearby()) {
											s.awaken();
										}

										you.getGraveyard().add(source);

										dmg = 0;
									}
								}
							}
						}
					} else {
						hit = false;
						dmg = 0;
					}
				} else {
					if (source.isDefending() && !source.hasFlag(Flag.ALWAYS_ATTACK, true)) {
						dmg = 0;
					}
				}

				if (hit) {
					op.getData().put("last_attacked_by", source);

					for (Evogear e : source.getEquipments()) {
						JSONArray charms = e.getCharms();

						for (Object o : charms) {
							Charm c = Charm.valueOf(String.valueOf(o));
							switch (c) {
								case PIERCING -> direct += damage * c.getValue(e.getTier()) / 100;
								case WOUNDING -> {
									int val = (int) -(damage * dmgMult * c.getValue(e.getTier()) / 100);
									op.getRegDeg().add(val);
								}
								case DRAIN -> {
									int toDrain = Math.min(c.getValue(e.getTier()), op.getMP());
									if (toDrain > 0) {
										you.modMP(toDrain);
										op.modMP(-toDrain);
									}
								}
								case LIFESTEAL -> lifesteal += c.getValue(e.getTier());
							}
						}
					}

					if (target != null) {
						for (Evogear e : target.getEquipments()) {
							JSONArray charms = e.getCharms();

							for (Object o : charms) {
								Charm c = Charm.valueOf(String.valueOf(o));
								if (c == Charm.THORNS) {
									thorns += c.getValue(e.getTier());
								}
							}
						}
					}

					switch (you.getOrigins().synergy()) {
						case ELEMENTAL -> {
							int water = (int) getCards(you.getSide()).parallelStream()
									.filter(s -> s.getElement() == ElementType.WATER)
									.count();

							if (water >= 4) {
								you.modMP(1);
							}
						}
						case FALLEN -> {
							if (op.getRegDeg().peek() < 0) {
								op.getRegDeg().apply(0.2);
							}
						}
					}

					op.modHP((int) -((dmg + direct) * dmgMult));

					if (thorns > 0) {
						you.modHP(-damage * thorns / 100);
					}
					if (lifesteal > 0) {
						you.modHP(dmg * lifesteal / 100);
					}

					if (you.getOrigins().synergy() == Race.DAEMON) {
						you.modMP((int) (Math.max(0d, eHP - op.getHP()) / op.getBase().hp() * 0.05));
					}
				}
			}
		} finally {
			if (source.getIndex() != -1) {
				if (target == null || (md.contains(SendMode.REGULAR) && !source.spendAttack())) {
					source.setAvailable(false);
				}
			}
		}

		if (eHP != op.getHP()) {
			int val = eHP - op.getHP();
			outcome += "\n" + getString(val > 0 ? "str/combat_damage_dealt" : "str/combat_heal_op", Math.abs(val));

			double mult = (val > 0 ? dmgMult : op.getStats().getHealMult().get());
			if (mult != 1) {
				outcome += " (" + getString("str/value_" + (mult > 0 ? "reduction" : "increase"), Utils.roundToString((1 - mult) * 100, 2)) + ")";
			}
		}
		if (pHP != you.getHP()) {
			int val = pHP - you.getHP();
			outcome += "\n" + getString(val > 0 ? "str/combat_damage_taken" : "str/combat_heal_self", Math.abs(val));

			double mult = (val > 0 ? you.getStats().getDamageMult() : you.getStats().getHealMult()).get();
			if (mult != 1) {
				outcome += " (" + getString("str/value_" + (mult > 0 ? "reduction" : "increase"), Utils.roundToString((1 - mult) * 100, 2)) + ")";
			}
		}

		reportEvent("str/combat", true, !md.contains(SendMode.SEND), source, Utils.getOr(target, op.getName()), outcome.trim());
		return win;
	}

	public boolean activateTrap(Senshi trap, EffectParameters ep) {
		if (!(trap instanceof TrapSpell p)) return false;

		Evogear e = p.getOriginal();
		Hand hand = e.getHand();

		Targeting tgt = switch (e.getTargetType()) {
			case NONE -> new Targeting(hand, -1, -1);
			case ALLY -> {
				if (ep.allies().length == 0) {
					yield null;
				}

				yield new Targeting(hand, ep.allies()[0].index(), -1);
			}
			case ENEMY -> {
				if (ep.enemies().length == 0) {
					yield null;
				}

				yield new Targeting(hand, -1, ep.enemies()[0].index());
			}
			case BOTH -> {
				if (ep.allies().length == 0 || ep.enemies().length == 0) {
					yield null;
				}

				yield new Targeting(hand, ep.allies()[0].index(), ep.enemies()[0].index());
			}
		};

		if (tgt == null) return false;

		if (!tgt.validate(e.getTargetType())) {
			getChannel().sendMessage(getString("error/target", getString("str/target_" + e.getTargetType()))).queue();
			return false;
		}

		if (e.execute(ep.withTrigger(ON_ACTIVATE))) {
			hand.getGraveyard().add(p);
			return true;
		}

		return false;
	}

	public boolean chance(double percentage) {
		return Calc.chance(percentage, getRng());
	}

	public Arcade getArcade() {
		return arcade;
	}

	public Map<Side, Hand> getHands() {
		return hands;
	}

	public Hand getCurrent() {
		return hands.get(getCurrentSide());
	}

	public Side getCurrentSide() {
		return getTurn() % 2 == 0 ? Side.TOP : Side.BOTTOM;
	}

	public Hand getOther() {
		return hands.get(getOtherSide());
	}

	public Side getOtherSide() {
		return getTurn() % 2 == 1 ? Side.TOP : Side.BOTTOM;
	}

	public Arena getArena() {
		return arena;
	}

	public List<HistoryTurn> getTurns() {
		return turns;
	}

	public JSONObject getData() {
		return data;
	}

	public boolean isSingleplayer() {
		return Bit32.on(state, 0);
	}

	public void setSingleplayer(boolean sp) {
		state = (byte) Bit32.set(state, 0, sp);
	}

	public boolean isNotVoided() {
		return !isSingleplayer() && arcade != Arcade.CASUAL && !Bit32.on(state, 1);
	}

	public void setCheated(boolean cheated) {
		state = (byte) Bit32.set(state, 1, cheated);
	}

	public boolean isRestoring() {
		return Bit32.on(state, 2);
	}

	public void setRestoring(boolean restoring) {
		state = (byte) Bit32.set(state, 2, restoring);
	}

	public boolean isLocked() {
		return Bit32.on(state, 3);
	}

	public void setLocked(boolean locked) {
		state = (byte) Bit32.set(state, 3, locked);
	}

	public boolean isSending() {
		return Bit32.on(state, 4);
	}

	public void setSending(boolean sending) {
		state = (byte) Bit32.set(state, 4, sending);
	}

	public List<Senshi> getCards() {
		return Arrays.stream(Side.values()).flatMap(s -> getCards(s).stream()).toList();

	}

	public List<Senshi> getCards(Side side) {
		return getSlots(side).stream().flatMap(slt -> slt.getCards().stream()).filter(Objects::nonNull).toList();

	}

	public List<SlotColumn> getSlots() {
		return Arrays.stream(Side.values()).flatMap(s -> getSlots(s).stream()).toList();
	}

	public List<SlotColumn> getSlots(Side side) {
		return arena.getSlots(side);
	}

	public void iterateSlots(Consumer<Senshi> act) {
		for (Side side : Side.values()) {
			iterateSlots(side, act);
		}
	}

	public void iterateSlots(Side side, Consumer<Senshi> act) {
		for (SlotColumn slot : arena.getSlots(side)) {
			for (Senshi card : slot.getCards()) {
				if (card != null) {
					act.accept(card);
				}
			}
		}
	}

	public Senshi findCard(Side side, String id) {
		for (SlotColumn slt : getSlots(side)) {
			for (Senshi s : slt.getCards()) {
				if (s != null && s.getId().equalsIgnoreCase(id)) {
					return s;
				}
			}
		}

		return null;
	}

	public Senshi findCard(Side side, Predicate<Senshi> condition) {
		for (SlotColumn slt : getSlots(side)) {
			for (Senshi s : slt.getCards()) {
				if (s != null && condition.test(s)) {
					return s;
				}
			}
		}

		return null;
	}

	public Drawable<?> findCard(long serial) {
		Drawable<?> out = null;
		for (Hand h : hands.values()) {
			out = findCard(h.getSide(), s -> s.SERIAL == serial);
			if (out != null) break;

			out = Stream.of(h.getCards(false), h.getDiscard(false), h.getGraveyard(false), h.getRealDeck(false))
					.parallel()
					.flatMap(List::stream)
					.filter(d -> d.getSerial() == serial)
					.findFirst().orElse(null);
		}

		return out;
	}

	public BondedList<Drawable<?>> getBanned() {
		return arena.getBanned();
	}

	public List<Evogear> getEquipments(Side side) {
		return arena.getSlots(side).stream().map(SlotColumn::getTop).filter(Objects::nonNull).flatMap(s -> s.getEquipments().stream()).toList();
	}

	public void trigger(Trigger trigger) {
		if (isRestoring()) return;

		List<Side> sides = List.of(getCurrentSide(), getOtherSide());

		for (Side side : sides) {
			trigger(trigger, side);
		}

		if (trigger == ON_TICK) {
			tick++;
		}
	}

	public void trigger(Trigger trigger, Side side) {
		if (isRestoring()) return;

		EffectParameters ep = new EffectParameters(trigger, side);

		try {
			triggerBindings(ep);

			iterateSlots(side, s -> s.execute(new EffectParameters(trigger, side, s.asSource(trigger))));

			Hand h = hands.get(side);
			for (Drawable<?> card : List.copyOf(h.getCards())) {
				if (card instanceof Evogear e && e.isPassive() && e.execute(ep)) {
					List<Drawable<?>> stack = (e.getTier() > 3 ? arena.getBanned() : h.getGraveyard());
					stack.add(e);
				}
			}
		} finally {
			if (trigger == ON_TICK) {
				hands.get(side).flushStacks();
				getBanned().removeIf(d -> d.getCurrentStack() != arena.getBanned(true));
			}

			triggerEOTs(ep);
		}
	}

	public boolean trigger(Trigger trigger, Source source, Target... targets) {
		if (isRestoring()) return false;

		EffectParameters ep = new EffectParameters(trigger, source.side(), source, targets);

		try {
			triggerBindings(ep);

			boolean executed = source.execute(ep);
			for (Target t : ep.targets()) {
				t.execute(ep);
			}

			return executed;
		} finally {
			triggerEOTs(ep);
		}
	}

	public Set<EffectOverTime> getEOTs() {
		return eots;
	}

	public void triggerBindings(EffectParameters ep) {
		var binds = bindings.stream()
				.sorted(Comparator.comparing(b -> b.getHolder().getSide() == getCurrentSide(), Comparator.reverseOrder()))
				.toList();

		for (TriggerBind binding : binds) {
			if (binding.isBound(ep)) {
				EffectHolder<?> holder = binding.getHolder();
				if (holder.getIndex() == -1 && !binding.isPermanent()) {
					bindings.remove(binding);
					continue;
				}

				holder.execute(new EffectParameters(ON_DEFER_BINDING, ep.side(), new DeferredTrigger(null, ep.trigger()), ep.source(), ep.targets()));
			}
		}
	}

	public void triggerEOTs(EffectParameters ep) {
		try {
			for (EffectOverTime effect : Set.copyOf(eots)) {
				if (effect.isClosed()) {
					eots.remove(effect);
					continue;
				}

				if (effect.getSide() != null) {
					Hand h = hands.get(effect.getSide());
					if (h.getLockTime(Lock.EFFECT) > 0) {
						if (!(effect.getSource() instanceof EffectHolder<?> e) || !e.hasTrueEffect()) {
							continue;
						}
					}
				}

				Predicate<Side> checkSide = s -> effect.getSide() == null || effect.getSide() == s;
				if (checkSide.test(getCurrentSide()) && ep.trigger() == ON_TURN_BEGIN) {
					effect.decreaseTurn();
				}

				if (ep.size() == 0) {
					if (checkSide.test(ep.side()) && effect.hasTrigger(ep.trigger())) {
						effect.decreaseLimit();

						try {
							effect.getEffect().accept(effect, new EffectParameters(ep.trigger(), ep.side()));
						} catch (ActivationException ignore) {
						} catch (Exception e) {
							getChannel().sendMessage(getString("error/effect")).queue();
							Constants.LOGGER.warn("Failed to execute {} persistent effect", effect.getSource(), e);
						}
					}
				} else if (ep.source() != null) {
					if (checkSide.test(ep.source().side()) && effect.hasTrigger(ep.source().trigger())) {
						effect.decreaseLimit();

						try {
							effect.getEffect().accept(effect, new EffectParameters(ep.source().trigger(), ep.side(), ep.source(), ep.targets()));
						} catch (ActivationException ignore) {
						} catch (Exception e) {
							getChannel().sendMessage(getString("error/effect")).queue();
							Constants.LOGGER.warn("Failed to execute {} persistent effect", effect.getSource(), e);
						}
					}

					for (Target t : ep.targets()) {
						if (checkSide.test(t.side()) && effect.hasTrigger(t.trigger())) {
							effect.decreaseLimit();

							try {
								effect.getEffect().accept(effect, new EffectParameters(t.trigger(), ep.side(), ep.source(), ep.targets()));
							} catch (ActivationException ignore) {
							} catch (Exception e) {
								getChannel().sendMessage(getString("error/effect")).queue();
								Constants.LOGGER.warn("Failed to execute {} persistent effect for {}", effect.getSource(), t, e);
							}
						}
					}
				}

				if (effect.isExpired() || isClosed()) {
					effect.close();
					eots.remove(effect);

					if (!isClosed() && !effect.isPermanent() && effect.getSource() != null) {
						getChannel().sendMessage(getString("str/effect_expiration", effect.getSource())).queue();
					}
				}
			}
		} finally {
			getArena().getField().execute(ep);
			for (Target t : ep.targets()) {
				try {
					getArena().getField().execute(new EffectParameters(t.trigger(), ep.side(), ep.source(), ep.targets()));
				} catch (ActivationException ignore) {
				} catch (Exception e) {
					getChannel().sendMessage(getString("error/effect")).queue();
					Constants.LOGGER.warn("Failed to execute {} effect for {}", getArena().getField(), t, e);
				}
			}
		}
	}

	public void bind(EffectHolder<?> self, EnumMap<TriggerBind.Target, EnumSet<Trigger>> binds) {
		bindings.add(new TriggerBind(self, binds));
	}

	public void bind(EffectHolder<?> self, EnumMap<TriggerBind.Target, EnumSet<Trigger>> binds, boolean permanent) {
		bindings.add(new TriggerBind(self, binds, permanent));
	}

	public void bind(EffectHolder<?> self, EnumSet<Trigger> binds, boolean permanent) {
		bindings.add(new TriggerBind(self, binds, permanent));
	}

	public void unbind(EffectHolder<?> self) {
		bindings.removeIf(b -> !b.isPermanent() && b.getHolder().equals(self));
	}

	private BiFunction<String, String, String> replaceMessages(Message message) {
		return (chn, msg) -> {
			if (msg != null) {
				GuildMessageChannel channel = Main.getApp().getMessageChannelById(chn);
				if (channel != null) {
					channel.retrieveMessageById(msg)
							.flatMap(Objects::nonNull, Message::delete)
							.queue(null, Utils::doNothing);
				}
			}

			return message.getId();
		};
	}

	public void reportEvent(String message, boolean trigger, boolean buffer, Object... args) {
		if (isSending() || getChannel() == null) return;

		try {
			setSending(true);

			List<RestAction<?>> acts = new ArrayList<>();
			for (Map.Entry<String, String> tuple : messages.entrySet()) {
				if (tuple != null) {
					GuildMessageChannel channel = Main.getApp().getMessageChannelById(tuple.getKey());
					if (channel != null) {
						acts.add(channel.retrieveMessageById(tuple.getValue()).flatMap(Objects::nonNull, Message::editMessageComponents));
					}
				}
			}

			if (!acts.isEmpty()) {
				Pages.subGet(RestAction.allOf(acts));
			}

			List<Side> sides = List.of(getOtherSide(), getCurrentSide());
			for (Side side : sides) {
				Hand hand = hands.get(side);
				hand.getStats().removeIf(ValueMod::isExpired);
				hand.flushStacks();

				if (hand.getOrigins().synergy() == Race.SUCCUBUS && hand.isLowLife()) {
					Hand op = hand.getOther();
					if (op.getLockTime(Lock.CHARM) == 0) {
						op.modLockTime(Lock.CHARM, 1);
					}
				}

				SpecialDefeat def = hand.getDefeat();
				if (hand.getHP() == 0 || def != null) {
					trigger(ON_VICTORY, side.getOther());
					trigger(ON_DEFEAT, side);

					if (hand.getOrigins().isPure(Race.UNDEAD) && !hand.getData().getBoolean("undying")) {
						hand.modHP(hand.getBase().hp() * 0.75, true);
						hand.getData().put("undying", true);
					}

					if (hand.getOrigins().synergy() == Race.GOLEM) {
						hand.modHP(hand.getMP() * hand.getBase().hp() / 10);
						hand.consumeMP(hand.getMP());
					}

					if (def == null && hand.getHP() > 0) {
						continue;
					}

					if (def != null) {
						reportResult(GameReport.SUCCESS, hand.getOther().getSide(), "str/game_end_special", "<@" + hand.getOther().getUid() + ">");
					} else {
						reportResult(GameReport.SUCCESS, hand.getOther().getSide(), "str/game_end", "<@" + hand.getUid() + ">", "<@" + hand.getOther().getUid() + ">");
					}

					return;
				}

				iterateSlots(side, s -> {
					s.getBase().unlockAll();
					s.setLastInteraction(null);
					s.clearBlocked();
					s.getStats().getFlags().clearTemp();
					for (Evogear e : s.getEquipments()) {
						e.getStats().getFlags().clearTemp();
					}

					s.getStats().removeIf(ValueMod::isExpired);
					for (Evogear e : s.getEquipments()) {
						e.getStats().removeIf(ValueMod::isExpired);
					}
				});
			}

			if (trigger) {
				resetTimer();
				trigger(ON_TICK);
				for (Hand h : hands.values()) {
					h.getData().remove("last_attacked_by");
				}

				getCurrent().setRerolled(true);
				getCurrent().verifyCap();
			}

			Future<BufferedImage> img = arena.render(getLocale());

			ButtonizeHelper helper = getButtons();
			AtomicBoolean registered = new AtomicBoolean();
			if (buffer) {
				getChannel().buffer(getString(message, args));
			} else {
				getChannel().sendMessage(getString(message, args))
						.addFile(IO.getBytes(arena.getThumbnail(), "jpg"), "preview.jpg")
						.apply(helper::apply)
						.queue(m -> {
							Pages.buttonize(m, helper);
							messages.compute(m.getChannel().getId(), replaceMessages(m));

							if (!registered.get()) {
								if (!message.startsWith("str/game_history")) {
									getHistory().add(new HistoryLog(m.getContentDisplay(), getCurrentSide()));
								}

								registered.set(true);
							}

							try {
								byte[] bytes = IO.getBytes(img.get());
								m.editMessageAttachments(FileUpload.fromData(bytes, "game.jpg")).queue(null, Utils::doNothing);
							} catch (CancellationException ignore) {
							} catch (ExecutionException | InterruptedException e) {
								throw new RuntimeException(e);
							}
						}, Utils::doNothing);
			}
		} finally {
			setSending(false);
		}
	}

	public void reportResult(@MagicConstant(valuesFromClass = GameReport.class) byte code, Side winner, String message, Object... args) {
		if (isClosed()) return;

		turns.add(new HistoryTurn(this));

		setRestoring(true);
		for (List<SlotColumn> slts : arena.getSlots().values()) {
			for (SlotColumn slt : slts) {
				for (Senshi card : slt.getCards()) {
					if (card != null) {
						card.setFlipped(false);
					}
				}
			}
		}
		setRestoring(false);

		ClusterAction msg;
		SpecialDefeat defeat = null;
		if (winner != null) {
			defeat = hands.get(winner.getOther()).getDefeat();
		}

		if (defeat != null) {
			String special = getString(defeat.key(), defeat.args());
			msg = getChannel().sendMessage(getString(message, ArrayUtils.addFirst(args, special)));
		} else {
			msg = getChannel().sendMessage(getString(message, args));
		}

		AtomicBoolean registered = new AtomicBoolean();

		try {
			BufferedImage img = arena.render(getLocale()).get();
			byte[] bytes = IO.getBytes(img, "jpg");
			msg.addFile(bytes, "game.jpg");
		} catch (Exception ignore) {
		}

		msg.queue(m -> {
			if (!registered.get()) {
				getHistory().add(new HistoryLog(m.getContentDisplay(), getCurrentSide()));
				registered.set(true);
			}
		}, Utils::doNothing);

		List<RestAction<?>> acts = new ArrayList<>();
		for (Map.Entry<String, String> tuple : messages.entrySet()) {
			if (tuple != null) {
				GuildMessageChannel channel = Main.getApp().getMessageChannelById(tuple.getKey());
				if (channel != null) {
					acts.add(channel.retrieveMessageById(tuple.getValue()).flatMap(Objects::nonNull, Message::delete));
				}
			}
		}

		if (!acts.isEmpty()) {
			Pages.subGet(RestAction.allOf(acts));
		}

		if (winner != null) {
			this.winner = winner;
		}

		if (Utils.equalsAny(code, GameReport.SUCCESS, GameReport.GAME_TIMEOUT) && isNotVoided() && winner != null) {
			String cond = "default";
			if (code == GameReport.GAME_TIMEOUT) {
				cond = "wo";
				hands.get(winner.getOther()).getAccount().addItem("LEAVER_TICKER", 1);
			} else if (defeat != null) {
				cond = defeat.key();
			}

			new MatchHistory(this, cond, getTurns()).save();
		}

		close(code);
	}

	@SafeVarargs
	private ButtonizeHelper makeSelector(Hand hand, int buttons, int rows, TriConsumer<ButtonizeHelper, Integer, Integer> action, Map.Entry<Object, ThrowingConsumer<ButtonWrapper>>... extra) {
		ButtonizeHelper selector = new ButtonizeHelper(true)
				.setTimeout(5, TimeUnit.MINUTES)
				.setCanInteract((u, b) -> u.getId().equals(hand.getUid()))
				.setCancellable(false);

		for (int row = 0; row < rows; row++) {
			for (int col = 1; col <= buttons; col++) {
				int finalRow = row;
				int finalCol = col;
				selector.addAction(col + StringUtils.repeat(Constants.VOID, row), bw -> action.accept(selector, finalRow, finalCol));
			}
		}

		for (Map.Entry<Object, ThrowingConsumer<ButtonWrapper>> e : extra) {
			if (e.getKey() instanceof String s) {
				selector.addAction(s, e.getValue());
			} else if (e.getKey() instanceof Emoji emj) {
				selector.addAction(emj, e.getValue());
			}
		}

		return selector;
	}

	private ButtonizeHelper getButtons() {
		List<String> allowed = List.of("🪪", "🔍", "📑", "🏳️");

		Hand curr = getCurrent();
		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(5, TimeUnit.MINUTES)
				.setCanInteract((u, b) -> b != null && (u.getId().equals(curr.getUid()) || allowed.contains(b.getId())))
				.setCancellable(false);

		helper.addAction(Utils.parseEmoji("▶️"), w -> {
			if (isLocked()) return;

			if (curr.selectionPending()) {
				getChannel().sendMessage(getString("error/pending_choice")).queue();
				return;
			} else if (curr.selectionPending()) {
				getChannel().sendMessage(getString("error/pending_action")).queue();
				return;
			} else if (getPhase() == Phase.COMBAT || !curr.canAttack()) {
				if (curr.getLockTime(Lock.TAUNT) > 0) {
					List<SlotColumn> yours = getSlots(curr.getSide());
					if (yours.stream().anyMatch(sc -> sc.getTop() != null && sc.getTop().canAttack())) {
						getChannel().sendMessage(getString("error/taunt_locked", curr.getLockTime(Lock.TAUNT))).queue();
						return;
					}
				}

				nextTurn();
				return;
			}

			setPhase(Phase.COMBAT);

			List<Senshi> allCards = getCards();
			for (SlotColumn slt : getSlots(curr.getSide())) {
				for (Senshi s : slt.getCards()) {
					if (s != null && s.getIndex() != -1 && s.isBerserk()) {
						List<Senshi> valid = allCards.stream().filter(d -> !d.equals(s)).toList();
						if (!valid.isEmpty()) {
							attack(s, Utils.getRandomEntry(getRng(), valid));
							s.setAvailable(false);
						}
					}
				}
			}

			reportEvent("str/game_combat_phase", true, false, true);
		});

		if (getPhase() == Phase.PLAN && curr.canAttack()) {
			helper.addAction(Utils.parseEmoji("⏩"), w -> {
				if (isLocked()) return;

				if (curr.selectionPending()) {
					getChannel().sendMessage(getString("error/pending_choice")).queue();
					return;
				} else if (curr.selectionPending()) {
					getChannel().sendMessage(getString("error/pending_action")).queue();
					return;
				} else if (curr.getLockTime(Lock.TAUNT) > 0) {
					List<SlotColumn> yours = getSlots(curr.getSide());
					if (yours.stream().anyMatch(sc -> sc.getTop() != null && sc.getTop().canAttack())) {
						getChannel().sendMessage(getString("error/taunt_locked", curr.getLockTime(Lock.TAUNT))).queue();
						return;
					}
				}

				nextTurn();
			});
		}

		helper.addAction(Utils.parseEmoji("🪪"), w -> {
			Hand h;
			if (isSingleplayer()) {
				h = curr;
			} else {
				h = hands.values().stream()
						.filter(hand -> hand.getUid().equals(w.getUser().getId()))
						.findFirst().orElse(null);
			}

			if (h == null) return;
			else if (h.selectionPending()) {
				Objects.requireNonNull(w.getHook())
						.setEphemeral(true)
						.sendFiles(FileUpload.fromData(IO.getBytes(h.renderChoices(), "png"), "choices.png"))
						.queue();
				return;
			}

			WebhookMessageCreateAction<Message> act = Objects.requireNonNull(w.getHook())
					.setEphemeral(true)
					.sendFiles(FileUpload.fromData(IO.getBytes(h.render(), "png"), "hand.png"));

			BondedList<Drawable<?>> cards = h.getCards(false);
			if (cards.isEmpty() || h.getSide() != getCurrentSide()) {
				act.queue();
			} else {
				AtomicReference<Message> message = new AtomicReference<>();
				ButtonizeHelper source = makeSelector(h, cards.size(), 1, (parent, j, i) -> {
					List<Map.Entry<Integer, Boolean>> disable = new ArrayList<>();

					Drawable<?> card = cards.get(i - 1);
					if (card instanceof Field) {
						placeField(h.getSide(), JSONObject.of(Map.entry("inHand", i)));
						return;
					}

					List<SlotColumn> slots = getSlots(h.getSide());
					if (card instanceof Senshi) {
						for (SlotColumn slot : slots) {
							for (int k = 0; k < 2; k++) {
								if (slot.getAtRole(k > 0) != null) {
									disable.add(Map.entry(slot.getIndex(), k > 0));
								}
							}
						}
					} else if (card instanceof Evogear) {
						for (SlotColumn slot : slots) {
							if (slot.getTop() == null) {
								disable.add(Map.entry(slot.getIndex(), false));
							}
						}
					}

					ButtonizeHelper target = makeSelector(h, 5, card instanceof Senshi ? 2 : 1,
							(child, row, col) -> {
								JSONObject args = JSONObject.of(
										Map.entry("inHand", i),
										Map.entry("inField", col)
								);

								if (card instanceof Senshi) {
									if (row > 0) {
										args.put("notCombat", true);
									}

									Consumer<String> placeWithMode = m -> {
										args.put("mode", m);
										if (placeCard(h.getSide(), args)) {
											message.get().editMessageComponents().queue();
										}
									};

									ButtonizeHelper mode = new ButtonizeHelper(true)
											.setTimeout(5, TimeUnit.MINUTES)
											.setCanInteract((u, b) -> u.getId().equals(h.getUid()))
											.setCancellable(false)
											.addAction(Utils.parseEmoji("🗡️"), bw -> placeWithMode.accept("a"))
											.addAction(Utils.parseEmoji("🛡️"), bw -> placeWithMode.accept("d"))
											.addAction(Utils.parseEmoji("🎴"), bw -> placeWithMode.accept("b"))
											.addAction(Utils.parseEmoji(Constants.RETURN), bw -> disableOptions(message, child, disable));

									Pages.buttonize(message.get(), mode);
								} else if (card instanceof Evogear) {
									equipCard(h.getSide(), args);
								}
							},
							Map.entry(Utils.parseEmoji(Constants.RETURN), bw -> disableOptions(message, parent, disable))
					);

					disableOptions(message, target, disable);
				});

				source.apply(act).queue(s -> {
					message.set(s);
					Pages.buttonize(s, source);
				});
			}
		});

		if (getPhase() == Phase.COMBAT && curr.canAttack()) {
			helper.addAction(Utils.parseEmoji("⚔️"), w -> {
				Hand h;
				if (isSingleplayer()) {
					h = curr;
				} else {
					h = hands.values().stream()
							.filter(hand -> hand.getUid().equals(w.getUser().getId()))
							.findFirst().orElse(null);
				}

				if (h == null) return;

				List<SlotColumn> slots = getSlots(h.getSide());
				if (slots.parallelStream().map(SlotColumn::getTop).filter(Objects::nonNull).noneMatch(Senshi::canAttack)) {
					getChannel().sendMessage(getString("error/no_cards")).queue();
					return;
				}

				List<Map.Entry<Integer, Boolean>> sourceDisable = new ArrayList<>();
				for (SlotColumn slot : slots) {
					if (slot.getTop() == null || !slot.getTop().canAttack()) {
						sourceDisable.add(Map.entry(slot.getIndex(), false));
					}
				}

				var act = Objects.requireNonNull(w.getHook())
						.setEphemeral(true)
						.sendMessage(getString("str/select_source"));

				AtomicReference<Message> message = new AtomicReference<>();
				ButtonizeHelper source = makeSelector(h, 5, 1, (parent, j, i) -> {
					if (arena.isFieldEmpty(h.getSide().getOther())) {
						attack(h.getSide(), JSONObject.of(Map.entry("inField", i)));
						return;
					}

					List<Map.Entry<Integer, Boolean>> targetDisable = new ArrayList<>();
					List<SlotColumn> other = getSlots(h.getSide().getOther());
					for (SlotColumn slot : other) {
						for (int k = 0; k < 2; k++) {
							Senshi tgt = slot.getAtRole(k > 0);
							if (tgt == null || tgt.isStasis() || tgt.getFrontline() != null) {
								targetDisable.add(Map.entry(slot.getIndex(), k > 0));
							}
						}
					}

					ButtonizeHelper target;
					Senshi card = slots.get(i - 1).getTop();
					if (card.hasFlag(Flag.DIRECT)) {
						target = makeSelector(h, 5, 2,
								(child, row, col) -> attack(h.getSide(), JSONObject.of(
										Map.entry("inField", i),
										Map.entry("target", col)
								)),
								Map.entry(h.getOther().getName(), bw -> attack(h.getSide(), JSONObject.of(
										Map.entry("inField", i)
								))),
								Map.entry(Utils.parseEmoji(Constants.RETURN), bw ->
										disableOptions(message, m -> m.editMessage(getString("str/select_source")), parent, targetDisable)
								)
						);
					} else {
						target = makeSelector(h, 5, 2,
								(child, row, col) ->
										attack(h.getSide(), JSONObject.of(
												Map.entry("inField", i),
												Map.entry("target", col)
										)),
								Map.entry(Utils.parseEmoji(Constants.RETURN), bw ->
										disableOptions(message, m -> m.editMessage(getString("str/select_source")), parent, targetDisable)
								)
						);
					}

					disableOptions(message, m -> m.editMessage(getString("str/select_target")), target, targetDisable);
				});

				act.queue(s -> {
					message.set(s);
					disableOptions(message, source, sourceDisable);
				});
			});
		}

		helper.addAction(Utils.parseEmoji("🔍"), w -> {
			Objects.requireNonNull(w.getHook())
					.setEphemeral(true)
					.sendFiles(FileUpload.fromData(IO.getBytes(arena.renderEvogears(), "png"), "evogears.png"))
					.queue();
		});

		helper.addAction(Utils.parseEmoji("📑"), w -> {
			if (isLocked()) return;

			XStringBuilder sb = new XStringBuilder(getLocale().get("str/match_history", getTurn()));

			int i = 0;
			Iterator<HistoryLog> it = getHistory().descendingIterator();
			while (it.hasNext() && i++ < 20) {
				sb.appendNewLine(it.next().message());
			}

			Objects.requireNonNull(w.getHook())
					.setEphemeral(true)
					.sendMessage(sb.toString())
					.queue();
		});

		if (getPhase() == Phase.PLAN) {
			if (!curr.getCards().isEmpty() && (getTurn() == 1 && !curr.hasRerolled()) || curr.getOrigins().synergy() == Race.DJINN) {
				helper.addAction(Utils.parseEmoji("🔄"), w -> {
					if (isLocked()) return;

					curr.rerollHand();
					reportEvent("str/hand_reroll", true, false, curr.getName());
				});
			}

			if (!curr.getRealDeck().isEmpty() && arcade != Arcade.DECK_ROYALE) {
				int rem = curr.getRemainingDraws();
				if (rem > 0) {
					helper.addAction(Utils.parseEmoji("📤"), w -> {
						if (isLocked()) return;

						if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_choice")).queue();
							return;
						} else if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_action")).queue();
							return;
						}

						curr.manualDraw();
						Objects.requireNonNull(w.getHook())
								.setEphemeral(true)
								.sendFiles(FileUpload.fromData(IO.getBytes(curr.render(), "png"), "cards.png"))
								.queue();

						reportEvent("str/draw_card", true, false, curr.getName(), 1, "");
					});

					if (rem > 1) {
						helper.addAction(Utils.parseEmoji("📦"), w -> {
							if (isLocked()) return;

							if (curr.selectionPending()) {
								getChannel().sendMessage(getString("error/pending_choice")).queue();
								return;
							} else if (curr.selectionPending()) {
								getChannel().sendMessage(getString("error/pending_action")).queue();
								return;
							}

							curr.manualDraw(curr.getRemainingDraws());
							Objects.requireNonNull(w.getHook())
									.setEphemeral(true)
									.sendFiles(FileUpload.fromData(IO.getBytes(curr.render(), "png"), "cards.png"))
									.queue();

							reportEvent("str/draw_card", true, false, curr.getName(), rem, "s");
						});
					}
				} else if (curr.getOrigins().major() == Race.DIVINITY && !curr.isCritical()) {
					helper.addAction(Utils.parseEmoji("1212407741046325308"), w -> {
						if (isLocked()) return;

						if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_choice")).queue();
							return;
						} else if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_action")).queue();
							return;
						}

						int eths = (int) curr.getCards().stream()
								.filter(Drawable::isEthereal)
								.count();

						Drawable<?> d = curr.manualDraw();
						d.setEthereal(true);

						int loss = (int) Math.max(2, curr.getBase().hp() * 0.06) * eths;
						curr.setHP(Math.max(1, curr.getHP() - loss));
						Objects.requireNonNull(w.getHook())
								.setEphemeral(true)
								.sendFiles(FileUpload.fromData(IO.getBytes(curr.render(), "png"), "cards.png"))
								.queue();

						reportEvent("str/draw_card", true, false, curr.getName(), 1, "");
					});
				}

				if (curr.canUseDestiny() && !Utils.equalsAny(curr.getOrigins().major(), Race.MACHINE, Race.MYSTICAL)) {
					helper.addAction(Utils.parseEmoji("🧧"), w -> {
						if (isLocked()) return;

						if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_choice")).queue();
							return;
						} else if (curr.selectionPending()) {
							getChannel().sendMessage(getString("error/pending_action")).queue();
							return;
						}

						BondedList<Drawable<?>> deque = curr.getRealDeck();
						List<SelectionCard> cards = new ArrayList<>();
						cards.add(new SelectionCard(deque.getFirst(), false));
						if (deque.size() > 2) {
							cards.add(new SelectionCard(deque.get((deque.size() - 1) / 2), false));
						}
						if (deque.size() > 1) {
							cards.add(new SelectionCard(deque.getLast(), false));
						}

						try {
							curr.requestChoice(null, cards, ds -> {
								curr.getCards().add(ds.getFirst());
								curr.setUsedDestiny(true);

								Objects.requireNonNull(w.getHook())
										.setEphemeral(true)
										.sendFiles(FileUpload.fromData(IO.getBytes(curr.render(), "png"), "cards.png"))
										.queue();

								reportEvent("str/destiny_draw", true, false, curr.getName());
							});
						} catch (ActivationException e) {
							getChannel().sendMessage(getString(e.getMessage())).queue();
						}
					});
				}
			}

			if (curr.canUseDestiny() && Utils.equalsAny(curr.getOrigins().major(), Race.MACHINE, Race.MYSTICAL)) {
				helper.addAction(Utils.parseEmoji("⚡"), w -> {
					if (isLocked()) return;

					if (curr.selectionPending()) {
						getChannel().sendMessage(getString("error/pending_choice")).queue();
						return;
					} else if (curr.selectionPending()) {
						getChannel().sendMessage(getString("error/pending_action")).queue();
						return;
					}

					List<SelectionCard> valid = curr.getCards().stream().filter(d -> {
						if (d instanceof Evogear e && e.isAvailable()) {
							return e.isSpell() == (curr.getOrigins().major() == Race.MYSTICAL);
						}

						return false;
					}).map(d -> new SelectionCard(d, false)).toList();

					if (valid.isEmpty()) {
						getChannel().sendMessage(getString("err/empty_selection")).queue();
						return;
					}

					try {
						curr.requestChoice(null, valid, ds -> {
							((Evogear) ds.getFirst()).setFlag(Flag.EMPOWERED);
							curr.setUsedDestiny(true);

							if (curr.getOrigins().major() == Race.MACHINE) {
								reportEvent("str/martial_empower", true, false, curr.getName());
							} else {
								reportEvent("str/arcane_empower", true, false, curr.getName());
							}
						});
					} catch (ActivationException e) {
						getChannel().sendMessage(getString(e.getMessage())).queue();
					}
				});
			}

			if (curr.getOriginCooldown() == 0) {
				if (curr.getOrigins().major() == Race.SPIRIT) {
					List<SelectionCard> valid = curr.getCards().stream()
							.filter(Drawable::isAvailable).map(d -> new SelectionCard(d, false))
							.toList();

					if (!valid.isEmpty()) {
						helper.addAction(Utils.parseEmoji("🌀"), w -> {
							if (isLocked()) return;

							if (curr.selectionPending()) {
								getChannel().sendMessage(getString("error/pending_choice")).queue();
								return;
							} else if (curr.selectionPending()) {
								getChannel().sendMessage(getString("error/pending_action")).queue();
								return;
							}

							try {
								curr.requestChoice(null, valid, new SelectionRange(1, null), ds -> {
									List<StashedCard> material = ds.stream()
											.map(d -> new StashedCard(null, d))
											.toList();

									List<SelectionCard> pool = new ArrayList<>();
									for (int j = 0; j < 3; j++) {
										pool.add(new SelectionCard(SynthesizeCommand.rollSynthesis(curr.getUser(), material), false));
									}

									try {
										curr.requestChoice(null, pool, chosen -> {
											arena.getBanned().addAll(ds);
											curr.getCards().removeAll(ds);

											Evogear d = (Evogear) chosen.getFirst();
											curr.getCards().add(d);

											if (curr.getOrigins().isPure()) {
												d.setFlag(Flag.EMPOWERED);
											}

											if (isNotVoided() && d.getTier() == 4 && d.hasFlag(Flag.EMPOWERED)) {
												curr.getAccount().setDynValue("emp_tier_4", true);
											}

											curr.setOriginCooldown(Math.max(0, 4 - material.size()));
											Objects.requireNonNull(w.getHook())
													.setEphemeral(true)
													.sendFiles(FileUpload.fromData(IO.getBytes(curr.render(), "png"), "cards.png"))
													.queue();

											reportEvent("str/spirit_synth", true, false, curr.getName());
										});
									} catch (ActivationException e) {
										getChannel().sendMessage(getString(e.getMessage())).queue();
									}
								});
							} catch (ActivationException e) {
								getChannel().sendMessage(getString(e.getMessage())).queue();
							}
						});
					}
				}
			}

			if (curr.getOrigins().synergy() == Race.ORACLE) {
				helper.addAction(Utils.parseEmoji("🔮"), w -> {
					BufferedImage cards = curr.render(curr.getDeck().subList(0, Math.min(3, curr.getDeck().size())));
					Objects.requireNonNull(w.getHook())
							.setEphemeral(true)
							.sendFiles(FileUpload.fromData(IO.getBytes(cards, "png"), "hand.png"))
							.queue();
				});
			}

			if (isSingleplayer() || (getTurn() > 10 && curr.getLockTime(Lock.SURRENDER) == 0)) {
				helper.addAction(Utils.parseEmoji("🏳️"), w -> {
					Hand h = null;
					if (isSingleplayer()) {
						h = curr;
					} else {
						for (Hand hand : hands.values()) {
							if (hand.getUid().equals(w.getUser().getId())) {
								h = hand;
								break;
							}
						}
					}

					if (h == null) return;

					if (h.isForfeit()) {
						if (h.getSide() == getCurrentSide()) {
							getChannel().buffer(getString("str/game_forfeit_start", curr.getName()));
							nextTurn();
						} else {
							getChannel().sendMessage(getString("str/game_forfeit_start", curr.getName()));
						}

						return;
					}

					h.setForfeit(true);
					Objects.requireNonNull(w.getHook())
							.setEphemeral(true)
							.sendMessage(getString("str/confirm_forfeit"))
							.queue();
				});
			}
		}

		return helper;
	}

	private void disableOptions(AtomicReference<Message> message, ButtonizeHelper helper, List<Map.Entry<Integer, Boolean>> toDisable) {
		disableOptions(message, Message::editMessageComponents, helper, toDisable);
	}

	private void disableOptions(AtomicReference<Message> message, Function<Message, MessageEditAction> edit, ButtonizeHelper helper, List<Map.Entry<Integer, Boolean>> toDisable) {
		MessageEditAction mea = edit.apply(message.get());
		List<LayoutComponent> rows = helper.getComponents(mea);
		for (int ridx = 0; ridx < 2; ridx++) {
			LayoutComponent row = rows.get(ridx);
			if (row instanceof ActionRow ar) {
				List<ItemComponent> items = ar.getComponents();
				for (int idx = 0; idx < items.size(); idx++) {
					ItemComponent item = items.get(idx);
					if (item instanceof Button b && toDisable.contains(Map.entry(idx, ridx > 0))) {
						items.set(idx, b.asDisabled());
					}
				}
			}
		}

		mea.setComponents(rows).queue(s -> {
			message.set(s);
			Pages.buttonize(s, helper);
		});
	}

	public List<SlotColumn> getOpenSlots(Side side, boolean top) {
		return getSlots(side).stream().filter(sc -> !sc.isLocked() && !(top ? sc.hasTop() : sc.hasBottom())).toList();
	}

	public boolean putAtOpenSlot(Side side, boolean top, Senshi card) {
		List<SlotColumn> slts = getOpenSlots(side, top);
		if (slts.isEmpty()) return false;

		if (top) {
			slts.getFirst().setTop(card);
		} else {
			slts.getFirst().setBottom(card);
		}

		return true;
	}

	public boolean putAtOpenSlot(Side side, Senshi card) {
		return putAtOpenSlot(side, true, card) || putAtOpenSlot(side, false, card);
	}

	public void send(Drawable<?> source, String text) {
		send(source, text, null);
	}

	public void send(Drawable<?> source, String text, String gif) {
		if (text.isBlank() && gif == null) return;

		for (GuildMessageChannel chn : getChannel().getChannels()) {
			PseudoUser pu = new PseudoUser(source.toString(), Constants.API_ROOT + "card/" + source.getCard().getAnime().getId() + "/" + source.getCard().getId(), chn);
			if (gif != null) {
				pu.send(null, text, new WebhookEmbedBuilder().setImageUrl(GIF_PATH + gif + ".gif").build());
			} else {
				pu.send(null, text);
			}
		}
	}

	@Override
	public void nextTurn() {
		turns.add(new HistoryTurn(this));

		Hand curr = getCurrent();
		if (getOther().getOrigins().synergy() == Race.ELEMENTAL) {
			int fire = (int) getCards(getOther().getSide()).parallelStream()
					.filter(s -> s.getElement() == ElementType.FIRE)
					.count();

			if (fire >= 4) {
				try {
					curr.requestDiscard(1).get();
				} catch (Exception e) {
					curr.getRegDeg().add(-300);
					getChannel().sendMessage(getString("str/fire_burn")).queue();
				}
			}
		}

		curr.flushDiscard();
		trigger(ON_TURN_END, curr.getSide());
		curr.applyRegDeg();

		if (curr.getOrigins().synergy() == Race.ANGEL) {
			curr.getRegDeg().add(curr.getMP() * 100);
		}

		for (Lock lock : Lock.values()) {
			curr.modLockTime(lock, -1);
		}

		for (SlotColumn slt : getSlots(curr.getSide())) {
			slt.reduceLock(1);

			for (Senshi s : slt.getCards()) {
				if (s != null && s.getIndex() != -1) {
					s.reduceDebuffs(1);
					s.setAvailable(true);
					s.setSwitched(false);

					if (arcade == Arcade.DECAY) {
						s.getStats().getMana().set(-1);
						if (s.getMPCost() == 0) {
							s.getHand().getGraveyard().add(s);
						}
					}
				}
			}
		}

		if (arcade == Arcade.INSTABILITY) {
			int affected = Math.min(getTurn() / 5, 8);
			List<SlotColumn> chosen = Utils.getRandomN(Utils.flatten(arena.getSlots().values()), affected, 1);

			for (SlotColumn slt : chosen) {
				slt.setLock(1);
			}
		}

		super.nextTurn();
		setPhase(Phase.PLAN);
		curr = getCurrent();
		if (curr.isForfeit()) {
			reportResult(GameReport.SUCCESS, getOther().getSide(), "str/game_forfeit", "<@" + getCurrent().getUid() + ">");
			return;
		}

		curr.modMP(curr.getBase().mpGain().get());
		curr.reduceOriginCooldown(1);
		curr.setCanAttack(true);
		curr.setSummoned(false);
		curr.flushDiscard();
		curr.resetDraws();

		if (arcade == Arcade.DECK_ROYALE) {
			if (curr.getRealDeck().isEmpty()) {
				curr.setDefeat(new SpecialDefeat("end/royale_win", "<@" + curr.getUid() + ">"));
				reportResult(GameReport.SUCCESS, getOther().getSide(), "str/game_end_special", "<@" + curr.getOther().getUid() + ">");
				return;
			}

			curr.manualDraw(curr.getRemainingDraws());
		}

		if (curr.getOrigins().synergy() == Race.WRAITH) {
			curr.getOther().modHP((int) -(curr.getGraveyard().size() * Math.ceil(getTurn() / 2d)));
		}

		curr.getStats().expireMods();

		if (curr.getLockTime(Lock.BLIND) > 0) {
			Utils.shuffle(curr.getCards());
		}

		for (SlotColumn slt : getSlots(curr.getSide())) {
			for (Senshi s : slt.getCards()) {
				if (s != null && s.getIndex() != -1) {
					s.setAvailable(true);
					s.reduceCooldown(1);
					s.reduceStasis(1);

					s.getStats().expireMods();
					for (Evogear e : s.getEquipments()) {
						e.reduceCooldown(1);
						e.getStats().expireMods();
					}
				}
			}
		}

		trigger(ON_TURN_BEGIN, curr.getSide());
		reportEvent("str/game_turn_change", true, false, "<@" + curr.getUid() + ">", (int) Math.ceil(getTurn() / 2d));
	}

	public Side getWinner() {
		return winner;
	}

	@Override
	public void resetTimer() {
		super.resetTimer();
		getCurrent().setForfeit(false);
	}

	@Override
	public void setPhase(Phase phase) {
		super.setPhase(phase);
		Trigger trigger = switch (phase) {
			case PLAN -> ON_PLAN;
			case COMBAT -> ON_COMBAT;
		};

		trigger(trigger, getCurrentSide());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Shoukan shoukan = (Shoukan) o;
		return getSeed() == shoukan.getSeed() && arcade == shoukan.arcade && isSingleplayer() == shoukan.isSingleplayer();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSeed(), arcade, isSingleplayer());
	}
}
