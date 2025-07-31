package com.kuuhaku.game;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.game.engine.NullPhase;
import com.kuuhaku.game.engine.PlayerAction;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.InfiniteList;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.dunhun.*;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.persistent.dunhun.*;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.ClusterAction;
import com.kuuhaku.model.records.dunhun.Choice;
import com.kuuhaku.model.records.dunhun.EventAction;
import com.kuuhaku.model.records.dunhun.EventDescription;
import com.kuuhaku.model.records.dunhun.Loot;
import com.kuuhaku.model.records.id.DungeonRunId;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.MagicConstant;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Dunhun extends GameInstance<NullPhase> {
	private final ExecutorService main = Executors.newSingleThreadExecutor();
	private final Dungeon dungeon;
	private final Map<String, Hero> heroes = new LinkedHashMap<>();
	private final AtomicReference<Combat> combat = new AtomicReference<>();
	private final AtomicReference<Pair<Message, ButtonizeHelper>> event = new AtomicReference<>();
	private final Loot loot = new Loot();
	private final AreaMap map;
	private final boolean duel;
	private Pair<String, String> message;

	public Dunhun(I18N locale, Dungeon dungeon, User... players) {
		this(locale, dungeon, Arrays.stream(players).map(User::getId).toArray(String[]::new));
	}

	public Dunhun(I18N locale, Dungeon dungeon, String... players) {
		super(locale, players);
		this.dungeon = dungeon;
		this.duel = dungeon.equals(Dungeon.DUEL);
		if (duel && players.length % 2 != 0) {
			throw new GameReport(GameReport.INVALID_DUEL);
		}

		for (String p : players) {
			Hero h = DAO.query(Hero.class, "SELECT h FROM Hero h WHERE h.account.id = ?1", p);
			if (h == null) {
				throw new GameReport(GameReport.NO_HERO, p);
			}

			if (h.getInventory().size() > h.getInventoryCapacity()) {
				throw new GameReport(GameReport.OVERBURDENED, h.getName());
			} else if (h.getStats().getLevel() < dungeon.getAreaLevel()) {
				throw new GameReport(GameReport.UNDERLEVELLED, h.getName());
			}

			h.getBinding().bind(this, Team.HUNTERS);
			heroes.put(p, h);
		}

		if (duel) {
			this.map = null;
			setTimeout(turn -> reportResult(GameReport.GAME_TIMEOUT, "str/versus_end_timeout"), 5, TimeUnit.MINUTES);
		} else {
			if (dungeon.getFloors().isEmpty()) {
				Hero leader = heroes.get(players[0]);
				DungeonRun run = DAO.find(DungeonRun.class, new DungeonRunId(leader.getId(), dungeon.getId()));
				if (run == null) {
					run = new DungeonRun(leader, dungeon);
				}

				this.map = new AreaMap(run.getSeed(), run.getFloor());
				this.map.generate();

				Floor fl = this.map.getFloor();
				if (run.getSublevel() >= fl.size()) {
					run.setSublevel(fl.size() - 1);
				}

				Sublevel sub = fl.getSublevel(run.getSublevel());
				if (run.getPath() >= sub.size()) {
					run.setPath(sub.size() - 1);
				}

				this.map.setRun(run);
				for (RunModifier mod : run.getModifiers()) {
					mod.apply(this);
				}
			} else {
				// TODO Pre-generated maps
				this.map = new AreaMap(0, 0);
			}

			setTimeout(turn -> {
				if (getCombat() != null) {
					Actor<?> current = getCombat().getCurrent();
					if (current != null) {
						getCombat().getLock().complete(() -> {
							current.setFleed(true);
							current.setAp(0);
						});
					} else {
						getCombat().getLock().complete(null);
					}

					return;
				}

				finish();

				DungeonRun run = map.getRun();
				reportResult(GameReport.SUCCESS, "str/dungeon_leave",
						Utils.properlyJoin(getLocale().get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList()),
						run.getFloor(), run.getSublevel()
				);
			}, 5, TimeUnit.MINUTES);
		}
	}

	@Override
	protected boolean validate(Message message) {
		return !duel || (
				getCombat() != null
				&& getCombat().getCurrent() instanceof Hero h
				&& h.getTeam() == heroes.get(message.getAuthor().getId()).getTeam()
		);
	}

	@Override
	protected void begin() {
		dungeon.init(this);

		CompletableFuture.runAsync(() -> {
			while (!isClosed()) {
				try {
					if (duel) {
						combat.set(new Combat(this, heroes.values()));
						getCombat().process();

						Hero winner = heroes.values().stream()
								.filter(h -> !h.isOutOfCombat())
								.findFirst().orElse(null);

						if (winner != null) {
							reportResult(GameReport.SUCCESS, "str/versus_end_win", winner.getName());
						} else {
							reportResult(GameReport.SUCCESS, "str/versus_end_draw");
						}

						break;
					}

					try {
						List<Runnable> floors = dungeon.getFloors();
						if (!floors.isEmpty()) {
							//TODO Pre-generated layout
							int floor = getTurn() - 1;
							if (floor >= floors.size()) {
								finish();
								reportResult(GameReport.SUCCESS, "str/dungeon_end",
										Utils.properlyJoin(getLocale().get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList())
								);

								break;
							}

							floors.get(floor).run();
						} else {
							map.generate();
							PlayerPos pos = map.getPlayerPos();

							XStringBuilder sb = new XStringBuilder();
							for (RunModifier mod : map.getRun().getModifiers()) {
								sb.appendNewLine(mod.getInfo(getLocale()).getDescription());
							}

							EmbedBuilder eb = new ColorlessEmbedBuilder()
									.setTitle(dungeon.getInfo(getLocale()).getName())
									.addField(getLocale().get("str/dungeon_modifiers"), sb.toString(), false)
									.setImage("attachment://dungeon.png")
									.setFooter(getLocale().get("str/dungeon_area", pos.getFloor(), pos.getSublevel()), null);

							ButtonizeHelper helper = new ButtonizeHelper(true)
									.setTimeout(5, TimeUnit.MINUTES)
									.setCanInteract(u -> Utils.equalsAny(u.getId(), getPlayers()))
									.setCancellable(false);

							Node pn = map.getPlayerNode();
							Set<Choice> choices = new LinkedHashSet<>();
							for (Node node : pn.getChildren()) {
								choices.add(new Choice(
										"path-" + node.getPath(),
										Utils.fancyNumber(node.getPath()),
										w -> {
											pos.setNode(node);
											return null;
										}
								));
							}

							choices.add(new Choice("leave", "", w -> {
								finish();

								DungeonRun run = map.getRun();
								reportResult(GameReport.SUCCESS, "str/dungeon_leave",
										Utils.properlyJoin(getLocale().get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList()),
										run.getFloor(), run.getSublevel()
								);
								return null;
							}));

							BufferedImage bi = map.render(600, 800);
							requestChoice(eb, bi, helper, choices);

							getChannel().sendMessage(parsePlural(getLocale().get("str/dungeon_next_area",
									getLocale().get("str/" + (pos.getPath() > 3 ? "n" : pos.getPath()) + "_suffix")
							))).queue();

							pn = map.getPlayerNode();
							switch (pn.getType()) {
								case NONE -> runCombat();
								case EVENT -> runEvent(Event.getRandom(pn));
								case REST -> runEvent(Event.find(Event.class, "REST"));
								// TODO case DANGER -> ;
								case BOSS -> beginCombat(Boss.getRandom());
							}
						}
					} catch (Exception e) {
						Constants.LOGGER.error(e, e);
					}

					Collection<Hero> hs = heroes.values();
					if (hs.stream().allMatch(Actor::isOutOfCombat)) {
						for (Hero h : hs) {
							if (h.getHp() > 0) continue;

							h.getStats().loseXp(h.getStats().getLosableXp() * getAreaLevel() / 100);
							h.save();
						}

						DungeonRun run = map.getRun();
						reportResult(GameReport.SUCCESS, "str/dungeon_fail",
								Utils.properlyJoin(getLocale().get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList()),
								run.getFloor(), run.getSublevel()
						);
						break;
					} else if (getCombat() != null && getCombat().isDone()) {
						grantCombatLoot();
					}

					map.getRun().save();
				} catch (Exception e) {
					Constants.LOGGER.error(e, e);
					getChannel().sendMessage(getLocale().get("error/error", e)).queue();
					close(GameReport.OTHER);
				}
			}
		}, main);
	}

	private void grantCombatLoot() {
		int xpGained = 0;
		double mf = 1 + heroes.values().stream()
				.mapToDouble(h -> h.getModifiers().getMagicFind().get())
				.sum() + (getAreaLevel() * 0.02);

		Loot loot = getCombat().getLoot();
		XStringBuilder sb = new XStringBuilder();
		for (Actor<?> a : getCombat().getActors(Team.KEEPERS)) {
			if (a instanceof MonsterBase<?> m && m.getHp() == 0) {
				if (m.getStats().isMinion()) continue;
				xpGained += m.getKillXp();

				Loot lt = m.getStats().generateLoot(m);
				double dropFac = 10 * switch (m.getRarityClass()) {
					case NORMAL -> 1;
					case MAGIC -> 1.2;
					case RARE -> 1.5;
					case UNIQUE -> 2.5;
				} * mf;

				while (Calc.chance(dropFac)) {
					lt.gear().add(Gear.getRandom(m, null));
					dropFac /= 2;
				}

				List<Object[]> bases = DAO.queryAllUnmapped("""
						SELECT id
							 , weight
						FROM v_dunhun_global_drops
						WHERE weight > 0
						"""
				);

				RandomList<String> rl = new RandomList<>();
				for (Object[] i : bases) {
					rl.add((String) i[0], ((Number) i[1]).intValue());
				}

				if (!rl.entries().isEmpty()) {
					dropFac = 5 * switch (m.getRarityClass()) {
						case NORMAL -> 1;
						case MAGIC -> 1.2;
						case RARE -> 1.5;
						case UNIQUE -> 2.5;
					} * mf;

					while (Calc.chance(dropFac)) {
						lt.items().add(DAO.find(UserItem.class, rl.get()));
						dropFac /= 2;
					}
				}

				loot.add(lt);
			}
		}

		if (!loot.gear().isEmpty() || !loot.items().isEmpty()) {
			this.loot.add(loot);

			for (Gear g : loot.gear()) {
				String name = g.getName(getLocale());
				if (g.getRarityClass() == RarityClass.RARE) {
					name += ", " + g.getBasetype().getInfo(getLocale()).getName();
				}

				sb.appendNewLine("- " + name);
			}

			for (UserItem i : loot.items().uniqueSet()) {
				sb.appendNewLine("- " + i.getName(getLocale()) + " (x" + loot.items().getCount(i) + ")");
			}
		}

		if (!sb.isBlank()) {
			getChannel().buffer(getLocale().get("str/dungeon_loot") + "\n" + sb);
		}

		for (Hero h : heroes.values()) {
			int xp = Math.max(1, xpGained);
			DAO.apply(Hero.class, h.getId(), n -> {
				int gain = xp;

				int lvl = n.getStats().getLevel();
				int diff = Math.abs(getAreaLevel() - lvl) - 5;

				if (diff > 0) {
					gain = (int) (gain * Math.min(Math.pow(0.8, diff), 1));
				}

				n.getStats().addXp(Math.max(1, gain));
				if (n.getStats().getLevel() > lvl) {
					getChannel().sendMessage(getLocale().get("str/actor_level_up", n.getName(), n.getStats().getLevel())).queue();
				}
			});
		}

		combat.set(null);
	}

	public void runCombat(Collection<String> pool) {
		runCombat(pool.toArray(String[]::new));
	}

	public void runCombat(String... pool) {
		combat.set(new Combat(this));
		for (int i = 0; i < 4; i++) {
			List<Actor<?>> keepers = getCombat().getActors(Team.KEEPERS);
			if (!Calc.chance(100 - 50d / getPlayers().length * keepers.size())) break;

			if (pool.length > 0) keepers.add(Monster.getRandom(this, Utils.getRandomEntry(pool)));
			else keepers.add(Monster.getRandom(this));
		}

		if (getCombat().getActors(Team.KEEPERS).isEmpty()) {
			combat.set(null);
			return;
		}

		getCombat().process();
	}

	public void runEvent(Event evt) {
		if (evt == null) {
			runCombat();
			return;
		}

		EventDescription ed = evt.parse(this);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setDescription(ed.description());

		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(5, TimeUnit.MINUTES)
				.setCanInteract(u -> Utils.equalsAny(u.getId(), getPlayers()))
				.setCancellable(false);

		Set<Choice> choices = new LinkedHashSet<>();
		for (EventAction act : ed.actions()) {
			choices.add(new Choice(act.action(), act.label(), w ->
					evt.getAction(act.action()).get()
			));
		}

		requestChoice(eb, null, helper, choices);

		if (getCombat() != null) {
			getCombat().process();
		}
	}

	public void requestChoice(EmbedBuilder eb, BufferedImage img, ButtonizeHelper helper, Set<Choice> choices) {
		CompletableFuture<Void> lock = new CompletableFuture<>();

		helper.clearActions();
		if (choices.isEmpty()) {
			helper.addAction(getLocale().get("str/continue"), s -> {
				lock.complete(null);
				Pages.finalizeEvent(s.getMessage(), Utils::doNothing);
			});
		} else {
			Map<String, String> votes = new HashMap<>();

			for (Choice c : choices) {
				helper.addAction(c.label(), w -> {
					if (getPartySize() <= 1) {
						votes.put(w.getUser().getId(), c.id());
					} else {
						if (votes.containsKey(w.getUser().getId())) return;
						votes.put(w.getUser().getId(), c.id());

						getChannel().sendMessage(getLocale().get("str/actor_chose",
								heroes.get(w.getUser().getId()).getName(), c.label()
						)).queue();

						if (votes.size() < getPartySize()) return;
					}

					String chosen = Utils.getRandomEntry(votes.values());
					Choice choice = choices.parallelStream()
							.filter(i -> i.id().equals(chosen))
							.findAny().orElse(null);

					eb.clear();
					String outcome;
					if (choice == null) {
						outcome = "UNKNOWN_CHOICE";
					} else {
						outcome = choice.action().apply(w);
					}

					if (outcome != null) {
						eb.setDescription(Utils.getOr(outcome, "PLACEHOLDER"));

						ButtonizeHelper fin = new ButtonizeHelper(true)
								.setTimeout(5, TimeUnit.MINUTES)
								.setCanInteract(u -> Utils.equalsAny(u.getId(), getPlayers()))
								.setCancellable(false)
								.addAction(getLocale().get("str/continue"), s -> {
									lock.complete(null);
									Pages.finalizeEvent(s.getMessage(), Utils::doNothing);
								});

						fin.apply(w.getMessage().editMessageEmbeds(eb.build())).queue(s -> {
							event.set(new Pair<>(s, fin));
							Pages.buttonize(s, fin);
						});
					} else {
						lock.complete(null);
						Pages.finalizeEvent(w.getMessage(), Utils::doNothing);
					}
				});
			}
		}

		ClusterAction ca = getChannel().sendEmbed(eb.build()).apply(helper::apply);
		if (img != null) {
			ca.addFile(IO.getBytes(img), "dungeon.png");
		}

		ca.queue(s -> {
			Pages.buttonize(s, helper);
			event.set(new Pair<>(s, helper));
		});

		lock.join();
		event.set(null);
	}

	private void finish() {
		getChannel().clearBuffer();

		if (!loot.gear().isEmpty() || !loot.items().isEmpty()) {
			if (getPartySize() == 1) {
				Hero h = List.copyOf(heroes.values()).getFirst();

				List<String> names = new ArrayList<>();
				for (Gear g : loot.gear()) {
					g.setOwner(h);
					g.save();

					names.add(g.getName(getLocale()));
				}

				for (UserItem i : loot.items().uniqueSet()) {
					h.getAccount().addItem(i, loot.items().getCount(i));
					names.add(i.getName(getLocale()) + " (x" + loot.items().getCount(i) + ")");
				}

				getChannel().buffer(getLocale().get(
						"str/dungeon_loot_single") + "\n" + "```" + Utils.properlyJoin(getLocale().get("str/and")).apply(names) + "```"
				);
			} else {
				InfiniteList<Hero> robin = new InfiniteList<>(heroes.values());
				Collections.shuffle(robin);

				Map<String, List<String>> dist = new HashMap<>();
				for (Gear g : loot.gear()) {
					Hero h = robin.getNext();
					g.setOwner(h);

					dist.computeIfAbsent(h.getName(), k -> new ArrayList<>()).add(g.getName(getLocale()));
				}
				DAO.insertBatch(loot.gear());

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
						dist.computeIfAbsent(h.getName(), k -> new ArrayList<>())
								.add(i.getKey().getName(getLocale()) + " (x" + i.getValue() + ")");
					}
				}

				XStringBuilder sb = new XStringBuilder();
				for (Map.Entry<String, List<String>> e : dist.entrySet()) {
					sb.appendNewLine(e.getKey() + ":").appendNewLine("```%s```".formatted(
							Utils.properlyJoin(getLocale().get("str/and")).apply(e.getValue())
					));
				}

				if (sb.length() > Message.MAX_CONTENT_LENGTH * 0.75) {
					sb.clear();
					for (Map.Entry<String, List<String>> e : dist.entrySet()) {
						sb.appendNewLine(e.getKey() + ":");
						for (String l : e.getValue()) {
							sb.appendNewLine("- " + l);
						}
						sb.nextLine();
					}

					getChannel().sendMessage(getLocale().get("str/dungeon_loot_split"))
							.addFile(sb.toString().getBytes(StandardCharsets.UTF_8), "loot.txt")
							.queue();
				} else {
					getChannel().buffer(getLocale().get("str/dungeon_loot_split") + "\n" + sb);
				}
			}
		}
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
		if (getEvent() != null) {
			ButtonizeHelper helper = getEvent().getSecond();
			helper.apply(getEvent().getFirst().editMessageComponents())
					.queue(s -> Pages.buttonize(s, helper));
		}
	}

	@PlayerAction("hunters")
	private void hunters(JSONObject args, User u) {
		info(Team.HUNTERS);
	}

	@PlayerAction("keepers")
	private void keepers(JSONObject args, User u) {
		info(Team.KEEPERS);
	}

	private void info(Team team) {
		if (getCombat() == null || duel) return;

		EmbedBuilder eb = new ColorlessEmbedBuilder();

		for (Actor<?> a : getCombat().getActors()) {
			if (a.getTeam() != team) continue;

			XStringBuilder sb = new XStringBuilder("#-# " + a.getName());

			if (a instanceof Monster m) {
				List<String> affs = m.getAffixes().stream()
						.map(aff -> "- " + aff.getInfo(getLocale()).getDescription())
						.toList();

				if (!affs.isEmpty()) {
					sb.appendNewLine("#");
					sb.appendNewLine("**" + getLocale().get("str/affixes") + "**");
					sb.appendNewLine(String.join("\n", affs));
				}
			}

			List<String> skills = a.getSkills().stream()
					.map(s ->
							"- " + s.getInfo(getLocale()).getName() + " " + StringUtils.repeat('◈', s.getStats().getCost()) +
							"\n" + s.getDescription(getLocale(), a).lines()
									.map(l -> "-# " + l)
									.collect(Collectors.joining("\n"))
					)
					.toList();

			if (!skills.isEmpty()) {
				sb.nextLine();
				sb.appendNewLine("**" + getLocale().get("str/skills") + "**");
				sb.appendNewLine(String.join("\n\n", skills));
			}

			String desc = sb.toString().lines()
					.map(l -> l.startsWith("#") ? l.substring(1) : "> " + l)
					.collect(Collectors.joining("\n"));

			eb.addField(a.getName(), desc, true);
		}

		getChannel().sendEmbed(eb.build()).queue();
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
							message = null;
						}
					}
				}, Utils::doNothing);

		for (Hero h : heroes.values()) {
			DAO.apply(Hero.class, h.getId(), n -> {
				for (Consumable c : h.getConsumables()) {
					n.setConsumableCount(c.getId(), c.getCount());
				}
			});
		}

		close(code);
	}

	public Dungeon getDungeon() {
		return dungeon;
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

	public Pair<Message, ButtonizeHelper> getEvent() {
		return event.get();
	}

	public Loot getLoot() {
		return loot;
	}

	public AreaMap getMap() {
		return map;
	}

	public boolean isDuel() {
		return duel;
	}

	public int getPartySize() {
		return heroes.size();
	}

	public int getAreaLevel() {
		if (duel) {
			return (int) heroes.values().stream()
					.mapToInt(h -> h.getStats().getLevel())
					.average().orElse(1);
		} else if (dungeon.getAreaLevel() == 0) {
			Node pn = map.getPlayerNode();
			return 1 + Math.max(0, pn.depth() / Floor.AREAS_PER_FLOOR * 5);
		}

		return dungeon.getAreaLevel();
	}

	@SafeVarargs
	public final <T extends MonsterBase<T>> void beginCombat(MonsterBase<T>... enemies) {
		if (this.combat.get() != null) return;
		this.combat.set(new Combat(this, enemies));
	}

	public String parsePlural(String text) {
		String plural = getPartySize() == 1 ? "S" : "P";

		return Utils.regex(getString(text), "\\[(?<S>[^\\[\\]]*?)\\|(?<P>.*?)]")
				.replaceAll(r -> r.group(plural));
	}
}
