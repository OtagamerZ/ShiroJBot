package com.kuuhaku.game;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.game.engine.NullPhase;
import com.kuuhaku.game.engine.PlayerAction;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.InfiniteList;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.dunhun.Combat;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.ContinueMode;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.persistent.dunhun.*;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.dunhun.EventAction;
import com.kuuhaku.model.records.dunhun.EventDescription;
import com.kuuhaku.model.records.dunhun.Loot;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.MagicConstant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Dunhun extends GameInstance<NullPhase> {
	private final ExecutorService main = Executors.newSingleThreadExecutor();
	private final Dungeon instance;
	private final Map<String, Hero> heroes = new LinkedHashMap<>();
	private final AtomicReference<Combat> combat = new AtomicReference<>();
	private final boolean duel;
	private CompletableFuture<Void> lock;
	private Pair<String, String> message;
	private Loot loot = new Loot();

	public Dunhun(I18N locale, Dungeon instance, User... players) {
		this(locale, instance, Arrays.stream(players).map(User::getId).toArray(String[]::new));
	}

	public Dunhun(I18N locale, Dungeon instance, String... players) {
		super(locale, players);
		this.instance = instance;
		this.duel = instance.equals(Dungeon.DUEL);
		if (duel && players.length % 2 != 0) {
			getChannel().sendMessage(getString("error/invalid_duel")).queue();
			close(GameReport.OTHER);
			return;
		}

		for (String p : players) {
			Hero h = DAO.query(Hero.class, "SELECT h FROM Hero h WHERE h.account.id = ?1", p);
			if (h == null) {
				getChannel().sendMessage(getString("error/no_hero_target", "<@" + p + ">")).queue();
				close(GameReport.NO_HERO);
				return;
			}

			h.asSenshi(locale);
			heroes.put(p, h);
		}

		if (duel) {
			setTimeout(turn -> reportResult(GameReport.GAME_TIMEOUT, "str/versus_end_timeout"), 5, TimeUnit.MINUTES);
		} else {
			setTimeout(turn -> {
				if (getCombat() != null) {
					lock.complete(null);
					Actor current = getCombat().getTurns().get();
					current.modAp(-current.getAp());
					getCombat().getLock().complete(null);
					return;
				}

				reportResult(GameReport.SUCCESS, "str/dungeon_leave"
						, Utils.properlyJoin(locale.get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList())
						, getTurn()
				);
			}, 5, TimeUnit.MINUTES);
		}
	}

	@Override
	protected boolean validate(Message message) {
		return !duel || (
				getCombat() != null
				&& getCombat().getTurns().get() instanceof Hero h
				&& h.getTeam() == heroes.get(message.getAuthor().getId()).getTeam()
		);
	}

	@Override
	protected void begin() {
		CompletableFuture.runAsync(() -> {
			while (!isClosed()) {
				if (duel) {
					combat.set(new Combat(this, heroes.values()));
					getCombat().process();

					Hero winner = heroes.values().stream()
							.filter(h -> h.getHp() > 0 && !h.hasFleed())
							.findFirst().orElse(null);

					if (winner != null) {
						reportResult(GameReport.SUCCESS, "str/versus_end_win", winner.getName());
					} else {
						reportResult(GameReport.SUCCESS, "str/versus_end_draw");
					}

					break;
				}

				if (getCombat() != null) {
					getCombat().process();
				} else {
					if (Calc.chance(25)) {
						runEvent();
					} else {
						runCombat();
					}
				}

				try {
					if (lock != null) lock.get();
				} catch (ExecutionException | InterruptedException ignore) {
				}

				if (heroes.values().stream().allMatch(h -> h.getHp() <= 0 || h.hasFleed())) {
					reportResult(GameReport.SUCCESS, "str/dungeon_fail"
							, Utils.properlyJoin(getLocale().get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList())
							, getTurn()
					);
					break;
				} else {
					if (getCombat() != null && getCombat().isDone()) {
						int xpGained = 0;
						for (Actor a : getCombat().getActors(Team.KEEPERS)) {
							if (a instanceof Monster m) {
								xpGained += m.getKillXp();

								Loot lt = m.getStats().generateLoot();
								if (!lt.gear().isEmpty() || !lt.items().isEmpty()) {
									loot.add(lt);

									XStringBuilder sb = new XStringBuilder(getLocale().get("str/monster_loot"));

									for (Gear g : lt.gear()) {
										sb.appendNewLine("- " + g.getName(getLocale()) + ", " + g.getBasetype().getInfo(getLocale()).getName());
										sb.appendNewLine("-# " + g.getName(getLocale()) + ", " + g.getBasetype().getInfo(getLocale()).getName());
									}

									for (UserItem i : lt.items().uniqueSet()) {
										sb.appendNewLine("- " + i.getName(getLocale()) + " (x" + lt.items().getCount(i) + ")");
									}

									getChannel().buffer(sb.toString());
								}
							}
						}

						for (Hero h : heroes.values()) {
							int lvl = h.getStats().getLevel();
							if (lvl > getTurn() - 5) {
								xpGained = Math.max(1, getTurn() * xpGained / (lvl - 5));
							}

							h.getStats().addXp(xpGained);
							h.save();

							if (h.getStats().getLevel() > lvl) {
								getChannel().buffer(getLocale().get("str/actor_level_up", h.getName(), h.getStats().getLevel()));
							}
						}

						combat.set(null);
					}

					ContinueMode mode = heroes.values().stream()
							.map(Hero::getContMode)
							.reduce((a, b) -> Calc.chance(50) ? a : b)
							.orElse(ContinueMode.CONTINUE);

					if (mode == ContinueMode.CONTINUE) {
						nextTurn();
						getChannel().sendMessage(parsePlural(getLocale().get("str/dungeon_next_floor", getTurn()))).queue();
					} else {
						if (heroes.size() == 1) {
							Hero h = List.copyOf(heroes.values()).getFirst();

							List<String> lines = new ArrayList<>();
							for (Gear g : loot.gear()) {
								g.setOwner(h);
								g.save();
								g = g.refresh();

								lines.add("- " + g.getName(getLocale()));
							}

							for (UserItem i : loot.items().uniqueSet()) {
								h.getAccount().addItem(i, loot.items().getCount(i));
								lines.add("- " + i.getName(getLocale()) + " (x" + loot.items().getCount(i) + ")");
							}

							lines.sort(String::compareTo);
							getChannel().buffer(getLocale().get("str/dungeon_loot") + String.join("\n", lines));
						} else {
							InfiniteList<Hero> robin = new InfiniteList<>(heroes.values());
							Collections.shuffle(robin);

							List<String> lines = new ArrayList<>();
							for (Gear g : loot.gear()) {
								Hero h = robin.getNext();
								g.setOwner(h);
								g.save();
								g = g.refresh();

								lines.add("- " + h.getName() + " -> " + g.getName(getLocale()));
							}

							Map<Hero, Map<UserItem, Integer>> split = new HashMap<>();
							for (UserItem i : loot.items()) {
								Hero h = robin.getNext();
								split.computeIfAbsent(h, k -> new HashMap<>())
										.compute(i, (k, v) -> v == null ? 1 : v + 1);
							}

							for (Map.Entry<Hero, Map<UserItem, Integer>> e : split.entrySet()) {
								Hero h = e.getKey();
								for (Map.Entry<UserItem, Integer> i : e.getValue().entrySet()) {
									h.getAccount().addItem(i.getKey(), i.getValue());
									lines.add("- " + h.getName() + " -> " + i.getKey().getName(getLocale()) + " (x" + i.getValue() + ")");
								}
							}

							lines.sort(String::compareTo);
							getChannel().buffer(getLocale().get("str/dungeon_loot_split") + String.join("\n", lines));
						}

						reportResult(GameReport.SUCCESS, "str/dungeon_leave"
								, Utils.properlyJoin(getLocale().get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList())
								, getTurn()
						);
					}
				}
			}
		}, main);
	}

	private void runCombat() {
		combat.set(new Combat(this));
		for (int i = 0; i < 3; i++) {
			List<Actor> keepers = getCombat().getActors(Team.KEEPERS);
			if (!Calc.chance(100 - 50d / getPlayers().length * keepers.size())) break;

			keepers.add(Monster.getRandom());
		}

		getCombat().process();
	}

	private void runEvent() {
		lock = new CompletableFuture<>();

		Event evt = Event.getRandom();
		EventDescription ed = evt.parse(getLocale(), this);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setDescription(ed.description());

		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(5, TimeUnit.MINUTES)
				.setCanInteract(u -> Utils.equalsAny(u.getId(), getPlayers()))
				.setCancellable(false);

		Map<String, String> votes = new HashMap<>();
		for (EventAction act : ed.actions()) {
			helper.addAction(act.label(), w -> {
				if (votes.containsKey(w.getUser().getId())) return;
				votes.put(w.getUser().getId(), act.action());

				getChannel().sendMessage(getLocale().get("str/actor_chose",
						heroes.get(w.getUser().getId()).getName(), act.label())
				).queue();

				if (votes.size() >= heroes.size()) {
					eb.setDescription(Utils.getOr(evt.getAction(Utils.getRandomEntry(votes.values())).get(), "PLACEHOLDER"));

					ButtonizeHelper fin = new ButtonizeHelper(true)
							.setTimeout(5, TimeUnit.MINUTES)
							.setCanInteract(u -> Utils.equalsAny(u.getId(), getPlayers()))
							.setCancellable(false)
							.addAction(getLocale().get("str/continue"), s -> {
								lock.complete(null);
								Pages.finalizeEvent(s.getMessage(), Utils::doNothing);
							});

					fin.apply(w.getMessage().editMessageEmbeds(eb.build()))
							.queue(s -> Pages.buttonize(s, fin));
				}
			});
		}

		getChannel().sendEmbed(eb.build())
				.apply(helper::apply)
				.queue(s -> Pages.buttonize(s, helper));
	}

	@Override
	protected void runtime(User user, String value) throws InvocationTargetException, IllegalAccessException {
		Pair<Method, JSONObject> action = toAction(StringUtils.stripAccents(value).toLowerCase());
		if (action != null) {
			action.getFirst().invoke(this, action.getSecond(), user);
		}
	}

	@PlayerAction("reload")
	private void reload(JSONObject args, User u) {
		if (getCombat() != null) getCombat().getLock().complete(null);
	}

	@PlayerAction("info")
	private void info(JSONObject args, User u) {
		if (getCombat() == null || duel) return;

		EmbedBuilder eb = new ColorlessEmbedBuilder();

		for (Actor a : getCombat().getActors()) {
			if (!(a instanceof Monster m)) continue;

			XStringBuilder sb = new XStringBuilder("-# " + m.getInfo(getLocale()).getName() + "\n");

			List<String> affs = m.getAffixes().stream()
					.map(aff -> "> - " + aff.getInfo(getLocale()).getDescription())
					.toList();

			if (!affs.isEmpty()) {
				sb.appendNewLine("> ");
				sb.appendNewLine("> **" + getLocale().get("str/affixes") + "**");
				sb.appendNewLine(String.join("\n", affs));
			}

			List<String> skills = m.getSkills().stream()
					.map(s ->
							"> - " + s.getInfo(getLocale()).getName() + " " + StringUtils.repeat('◈', s.getApCost()) +
							"\n> -# " + s.getDescription(getLocale(), m)
					)
					.toList();

			if (!skills.isEmpty()) {
				sb.appendNewLine("> ");
				sb.appendNewLine("> **" + getLocale().get("str/skills") + "**");
				sb.appendNewLine(String.join("\n>\n", skills));
			}

			eb.addField(a.getName(getLocale()), sb.toString(), true);
		}

		getChannel().sendEmbed(eb.build()).queue();
	}

	@PlayerAction("players")
	private void players(JSONObject args, User u) {
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		for (Hero h : heroes.values()) {
			XStringBuilder sb = new XStringBuilder("-# " + getLocale().get("race/" + h.getRace().name()) + "\n");
			h.addHpBar(sb);

			List<String> skills = h.getSkills().stream()
					.map(s ->
							"> - " + s.getInfo(getLocale()).getName() + " " + StringUtils.repeat('◈', s.getApCost()) +
							"\n> -# " + s.getDescription(getLocale(), h)
					)
					.toList();

			if (!skills.isEmpty()) {
				sb.appendNewLine("> ");
				sb.appendNewLine("> **" + getLocale().get("str/skills") + "**");
				sb.appendNewLine(String.join("\n>\n", skills));
			}

			eb.addField(
					(h.getContMode() == ContinueMode.CONTINUE ? "🆗 " : "🚪 ") + h.getName(),
					sb.toString(),
					true
			);
		}

		getChannel().sendEmbed(eb.build()).queue();
	}

	@PlayerAction("continue")
	private void modeContinue(JSONObject args, User u) {
		Hero h = heroes.get(u.getId());
		if (h.getContMode() == ContinueMode.CONTINUE) return;

		h.setContMode(ContinueMode.CONTINUE);
		getChannel().sendMessage(getLocale().get("str/actor_continue", h.getName())).queue();
	}

	@PlayerAction("leave")
	private void modeLeave(JSONObject args, User u) {
		Hero h = heroes.get(u.getId());
		if (h.getContMode() == ContinueMode.LEAVE) return;

		h.setContMode(ContinueMode.LEAVE);
		getChannel().sendMessage(getLocale().get("str/actor_leave", h.getName())).queue();
	}

	private void reportResult(@MagicConstant(valuesFromClass = GameReport.class) byte code, String msg, Object... args) {
		getChannel().sendMessage(parsePlural(getString(msg, args)))
				.queue(m -> {
					if (message != null) {
						GuildMessageChannel channel = Main.getApp().getMessageChannelById(message.getFirst());
						if (channel != null) {
							channel.retrieveMessageById(message.getSecond())
									.flatMap(Objects::nonNull, Message::delete)
									.queue(null, Utils::doNothing);
						}
					}
				}, Utils::doNothing);

		close(code);
	}

	public Dungeon getInstance() {
		return instance;
	}

	public Map<String, Hero> getHeroes() {
		return heroes;
	}

	public Pair<String, String> getMessage() {
		return message;
	}

	public void setMessage(Pair<String, String> message) {
		this.message = message;
	}

	public Combat getCombat() {
		return combat.get();
	}

	public boolean isDuel() {
		return duel;
	}

	public void beginCombat(Monster... enemies) {
		if (this.combat.get() != null) return;
		this.combat.set(new Combat(this, enemies));
	}

	public String parsePlural(String text) {
		String plural = heroes.size() == 1 ? "S" : "P";

		return Utils.regex(getString(text), "\\[(?<S>.*?)\\|(?<P>.*?)]")
				.replaceAll(r -> r.group(plural));
	}
}
