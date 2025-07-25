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
import com.kuuhaku.model.common.dunhun.Combat;
import com.kuuhaku.model.common.dunhun.MonsterBase;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.ContinueMode;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.persistent.dunhun.*;
import com.kuuhaku.model.persistent.user.DynamicProperty;
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
import org.apache.commons.lang3.math.NumberUtils;
import org.intellij.lang.annotations.MagicConstant;

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
	private final boolean duel;
	private CompletableFuture<Void> lock;
	private Pair<String, String> message;

	public Dunhun(I18N locale, Dungeon dungeon, User... players) {
		this(locale, dungeon, Arrays.stream(players).map(User::getId).toArray(String[]::new));
	}

	public Dunhun(I18N locale, Dungeon dungeon, String... players) {
		super(locale, players);
		this.dungeon = dungeon;
		this.duel = dungeon.equals(Dungeon.DUEL);
		if (duel && players.length % 2 != 0) {
			getChannel().sendMessage(getString("error/invalid_duel")).queue();
			close(GameReport.OTHER);
			return;
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
			setTimeout(turn -> reportResult(GameReport.GAME_TIMEOUT, "str/versus_end_timeout"), 5, TimeUnit.MINUTES);
		} else {
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
				reportResult(GameReport.SUCCESS, "str/dungeon_leave",
						Utils.properlyJoin(getLocale().get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList()),
						getTurn()
				);

				lock.complete(null);
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
							if (getTurn() % 10 == 0) {
								beginCombat(Boss.getRandom());
							} else {
								boolean skipped = false;
								if (dungeon.getMonsterPool().isEmpty()) {
									int skip = heroes.values().stream()
											.map(h -> h.getAccount().getDynValue("skip_floor_" + dungeon.getId().toLowerCase(), "0"))
											.mapToInt(Integer::parseInt)
											.max().orElse(0);

									if (skip > 0 && getTurn() == 1) {
										runEvent(DAO.find(Event.class, "CHECKPOINT"));
										skipped = true;
									}
								}

								if (!skipped) {
									if (Calc.chance(10)) {
										runEvent();
									} else {
										runCombat();
									}
								}
							}
						}
					} catch (Exception e) {
						Constants.LOGGER.error(e, e);
					}

					if (lock != null) {
						lock.join();
						event.set(null);
					}

					if (getCombat() != null) {
						getCombat().process();
					}

					Collection<Hero> hs = heroes.values();
					if (hs.stream().allMatch(a -> a.isOutOfCombat())) {
						for (Hero h : hs) {
							if (h.getHp() > 0) continue;

							h.getStats().loseXp(h.getStats().getLosableXp() * getAreaLevel() / 100);
							h.save();
						}

						reportResult(GameReport.SUCCESS, "str/dungeon_fail",
								Utils.properlyJoin(getLocale().get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList()),
								getTurn()
						);
						break;
					} else {
						if (getCombat() != null && getCombat().isDone()) {
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

								if (dungeon.getMonsterPool().isEmpty() && getTurn() % 10 == 0) {
									DynamicProperty prop = h.getAccount().getDynamicProperty("skip_floor_" + dungeon.getId().toLowerCase());
									prop.setValue(Math.max(NumberUtils.toInt(prop.getValue()), getTurn()));
									prop.save();
								}
							}

							combat.set(null);
						}

						ContinueMode mode = heroes.values().stream()
								.map(Hero::getContMode)
								.reduce((a, b) -> Calc.chance(50) ? a : b)
								.orElse(ContinueMode.CONTINUE);

						if (!isClosed() && mode == ContinueMode.CONTINUE) {
							nextTurn();
							getChannel().sendMessage(parsePlural(getLocale().get("str/dungeon_next_floor", getTurn()))).queue();
						} else {
							finish();
							reportResult(GameReport.SUCCESS, "str/dungeon_leave",
									Utils.properlyJoin(getLocale().get("str/and")).apply(heroes.values().stream().map(Hero::getName).toList()),
									getTurn()
							);
						}
					}
				} catch (Exception e) {
					Constants.LOGGER.error(e, e);
					getChannel().sendMessage(getLocale().get("error/error", e)).queue();
					close(GameReport.OTHER);
				}
			}
		}, main);
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

		getCombat().process();
	}

	public void runEvent() {
		runEvent(Event.getRandom());
	}

	public void runEvent(Event evt) {
		if (evt == null) {
			runCombat();
			return;
		}

		lock = new CompletableFuture<>();
		EventDescription ed = evt.parse(this);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setDescription(ed.description());

		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(5, TimeUnit.MINUTES)
				.setCanInteract(u -> Utils.equalsAny(u.getId(), getPlayers()))
				.setCancellable(false);

		Map<String, String> votes = new HashMap<>();
		if (!ed.actions().isEmpty()) {
			for (EventAction act : ed.actions()) {
				helper.addAction(act.label(), w -> {
					if (votes.containsKey(w.getUser().getId())) return;
					votes.put(w.getUser().getId(), act.action());

					getChannel().sendMessage(getLocale().get("str/actor_chose",
							heroes.get(w.getUser().getId()).getName(), act.label())
					).queue();

					if (votes.size() >= getPartySize()) {
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
								.queue(s -> {
									event.set(new Pair<>(s, fin));
									Pages.buttonize(s, fin);
								});
					}
				});
			}
		} else {
			helper.addAction(getLocale().get("str/continue"), s -> {
				lock.complete(null);
				Pages.finalizeEvent(s.getMessage(), Utils::doNothing);
			});
		}

		getChannel().sendEmbed(eb.build())
				.apply(helper::apply)
				.queue(s -> {
					Pages.buttonize(s, helper);
					event.set(new Pair<>(s, helper));
				});
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

			if (a instanceof Hero h) {
				eb.addField((h.getContMode() == ContinueMode.CONTINUE ? "🆗 " : "🚪 ") + h.getName(), desc, true);
			} else {
				eb.addField(a.getName(), desc, true);
			}
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
			return 1 + Math.max(0, (getTurn() - 1) / 10 * 5);
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
