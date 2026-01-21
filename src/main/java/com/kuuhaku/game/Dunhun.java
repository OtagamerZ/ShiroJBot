package com.kuuhaku.game;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.EmojiId;
import com.github.ygimenez.model.TextId;
import com.github.ygimenez.model.ThrowingConsumer;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.exceptions.VoidException;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.game.engine.NullPhase;
import com.kuuhaku.game.engine.PlayerAction;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.InfiniteList;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.dunhun.*;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.NodeType;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.persistent.dunhun.*;
import com.kuuhaku.model.persistent.shiro.GlobalProperty;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.ClusterAction;
import com.kuuhaku.model.records.PseudoUser;
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
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.intellij.lang.annotations.MagicConstant;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Dunhun extends GameInstance<NullPhase> {
	public static final int LEVEL_HARD = 28;
	public static final int LEVEL_BRUTAL = 56;
	public static final Map<String, String> ICONS = Utils.with(new LinkedHashMap<>(9), m -> {
		m.put("centertop", "⬆️");
		m.put("lefttop", "↖️");
		m.put("leftcenter", "⬅️");
		m.put("leftbottom", "↙️");
		m.put("centerbottom", "⬇️");
		m.put("rightbottom", "↘️");
		m.put("rightcenter", "➡️");
		m.put("righttop", "↗️");
		m.put("centercenter", "*️⃣");
	});

	private final Dungeon dungeon;
	private final Map<String, Hero> heroes = new LinkedHashMap<>();
	private final AtomicReference<Combat> combat = new AtomicReference<>();
	private final AtomicReference<Pair<Message, ButtonizeHelper>> message = new AtomicReference<>();
	private final Loot loot;
	private final AreaMap map;
	private final boolean duel;

	private CompletableFuture<Void> lock;

	public Dunhun(I18N locale, Dungeon dungeon, User... players) {
		this(locale, dungeon, Arrays.stream(players).map(User::getId).toArray(String[]::new));
	}

	public Dunhun(I18N locale, Dungeon dungeon, String... players) {
		super(locale, players);
		this.dungeon = dungeon;
		this.loot = new Loot(locale);
		this.duel = dungeon.equals(Dungeon.DUEL);
		if (duel && players.length % 2 != 0) {
			throw new GameReport(GameReport.INVALID_DUEL);
		}

		for (String p : players) {
			Hero h = DAO.query(Hero.class, "SELECT h FROM Hero h WHERE h.account.id = ?1", p);
			if (h == null) {
				throw new GameReport(GameReport.NO_HERO, p);
			}

			if (h.getInventory().size() > h.getInventoryCapacity() || h.getConsumableCount() > h.getConsumableCapacity()) {
				throw new GameReport(GameReport.OVERBURDENED, h.getName());
			} else if (h.getLevel() < dungeon.getAreaLevel()) {
				throw new GameReport(GameReport.UNDERLEVELLED, h.getName());
			}

			h.getSenshi();
			heroes.put(p, h);
		}

		if (duel) {
			this.map = null;
		} else {
			Hero leader = heroes.get(players[0]);

			DungeonRun run = DAO.find(DungeonRun.class, new DungeonRunId(leader.getId(), dungeon.getId()));
			if (run == null) {
				run = new DungeonRun(leader, dungeon);
				run.save();
			}

			if (dungeon.isInfinite()) {
				Map<String, Integer> hps = new HashMap<>();
				for (DungeonRunPlayer p : run.getPlayers()) {
					hps.put(p.getId().playerId(), p.getHp());
				}

				for (Hero h : heroes.values()) {
					h.setHp(hps.getOrDefault(h.getId(), h.getMaxHp()));
				}

				this.map = run.getMap();
				this.map.generate(this);

				Floor fl = this.map.getFloor();
				if (run.getSublevel() >= fl.size()) {
					run.setSublevel(fl.size() - 1);
				}

				Sublevel sub = fl.getSublevel(run.getSublevel());
				if (run.getPath() >= sub.size()) {
					run.setPath(sub.size() - 1);
				}
			} else {
				this.map = dungeon.init(this, run);
				this.map.generate(this);
			}

			run.setGame(this);
		}

		setTimeout(this::onTimeout, 5, TimeUnit.MINUTES);
	}

	@Override
	protected boolean validate(Message message) {
		return !duel || (
				combat.get() != null
						&& combat.get().getCurrent() instanceof Hero h
						&& h.getTeam() == heroes.get(message.getAuthor().getId()).getTeam()
		);
	}

	@Override
	protected void runtime() {
		while (!isClosed()) {
			try {
				if (duel) {
					combat.set(new Combat(this, map.getPlayerNode(), heroes.values()));
					combat.get().process();

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
					for (Hero h : heroes.values()) {
						h.getBinding().bind(this, Team.HUNTERS);
					}

					DungeonRun run = map.getRun();
					String area = getLocale().get("str/dungeon_area", run.getFloor(), run.getSublevel() + 1);
					EmbedBuilder eb = new ColorlessEmbedBuilder()
							.setTitle(dungeon.getInfo(getLocale()).getName() + " (" + area + ")")
							.setImage("attachment://dungeon.jpg")
							.setFooter(getLocale().get("str/dungeon_level", getAreaLevel()));

					if (!run.getModifiers().isEmpty()) {
						XStringBuilder sb = new XStringBuilder();
						for (RunModifier mod : run.getModifiers()) {
							sb.appendNewLine(mod.getInfo(getLocale()).getDescription());
						}

						eb.addField(getLocale().get("str/dungeon_run_modifiers"), sb.toString(), false);
					}

					Floor fl = map.getFloor();
					if (!fl.getModifiers().isEmpty()) {
						XStringBuilder sb = new XStringBuilder();
						for (RunModifier mod : fl.getModifiers()) {
							sb.appendNewLine(mod.getInfo(getLocale()).getDescription());
						}

						eb.addField(getLocale().get("str/dungeon_floor_modifiers"), sb.toString(), false);
					}

					BufferedImage bi = map.render(getLocale(), 900, 900);
					ButtonizeHelper helper = new ButtonizeHelper(true)
							.setTimeout(5, TimeUnit.MINUTES)
							.setCanInteract(u -> u.getId().equals(getModerator()) || Utils.equalsAny(u.getId(), getPlayers()))
							.setCancellable(false);

					Node currNode = map.getPlayerNode();
					List<String> order = List.copyOf(ICONS.keySet());
					List<Map.Entry<String, Node>> children = currNode.getChildren().stream()
							.filter(n -> !currNode.getBlocked().contains(n))
							.map(n -> Map.entry(currNode.getPathVerb(n), n))
							.sorted(Comparator
									.<Map.Entry<String, Node>>comparingInt(e -> order.indexOf(e.getKey()))
									.thenComparingInt(e -> -e.getValue().depth())
									.thenComparingInt(e -> e.getValue().getPath())
							)
							.toList();

					Set<Choice> choices = new LinkedHashSet<>();
					AtomicReference<String> chosenPath = new AtomicReference<>();
					for (int i = 0; i < children.size(); i++) {
						Map.Entry<String, Node> entry = children.get(i);
						String path = entry.getKey();
						Node node = entry.getValue();

						choices.add(new Choice(
								"path-" + path + '-' + i,
								Utils.parseEmoji(ICONS.get(path)),
								_ -> {
									run.setNode(node);
									chosenPath.set(getLocale().get("str/" + path));
									return null;
								}
						));
					}

					AtomicBoolean confirm = new AtomicBoolean(currNode.isSafeNode());
					choices.add(new Choice("leave", getLocale().get("str/leave_dungeon"), w -> {
						if (!confirm.get()) {
							getChannel().sendMessage(getLocale().get("alert/unsafe_area")).queue();
							confirm.set(true);
							return "CONFIRM";
						}

						if (!currNode.isSafeNode()) {
							if (defeat()) {
								run.setNode(currNode);
								run.save();
							}

							return null;
						}

						finish("str/dungeon_leave", getHeroNames(), run.getFloor(), run.getSublevel() + 1);
						return null;
					}));

					try {
						requestChoice(eb, bi, helper, choices);
						if (isClosed()) return;
					} catch (Exception ignore) {
						continue;
					}

					int floor = run.getFloor();
					if (floor != fl.getNumber()) {
						getChannel().sendMessage(parsePlural(getLocale().get("str/dungeon_next_floor",
								chosenPath.get(),
								floor, getLocale().get("str/" + (floor > 3 ? "n" : floor) + "_suffix")
						))).queue();
					} else {
						getChannel().sendMessage(parsePlural(getLocale().get("str/dungeon_next_area",
								chosenPath.get()
						))).queue();
					}

					Node nextNode = map.getPlayerNode();
					switch (nextNode.getType()) {
						case NONE, DANGER -> runCombat(nextNode);
						case EVENT -> {
							Sublevel level = nextNode.getSublevel();

							List<Hero> heroes = map.getHeroesAt(level.getFloor().getNumber(), level.getNumber());
							if (!heroes.isEmpty()) {
								runEvent(nextNode, Event.find(Event.class, "HERO_CONFLICT"));
							} else {
								runEvent(nextNode, Event.getRandom(nextNode));
							}
						}
						case REST -> runEvent(nextNode, Event.find(Event.class, "REST"));
						case RETURN -> {
							runEvent(nextNode, Event.find(Event.class, "RETURN"));
							if (map.getPlayerNode() == nextNode) {
								run.setNode(currNode);
							}
						}
						case BOSS -> beginCombat(nextNode, Boss.getRandom(nextNode));
					}

					if (combat.get() != null) {
						combat.get().process();

						if (combat.get().isDone()) {
							if (combat.get().isWin()) {
								grantCombatLoot();
							} else if (nextNode.getType() != NodeType.EVENT) {
								try {
									Collection<Hero> hs = heroes.values();
									if (hs.stream().allMatch(a -> a.getHp() <= 0)) {
										if (!defeat()) return;
									}
								} finally {
									run.setNode(currNode);
								}
							}
						}
					}

					if (dungeon.isInfinite() && run.getFloor() % 10 == 0 && heroes.size() == 1) {
						Hero h = heroes.values().iterator().next();

						try {
							GlobalProperty gp = DAO.find(GlobalProperty.class, "highest_floor_" + dungeon.getId().toLowerCase());
							if (gp == null) {
								gp = new GlobalProperty("highest_floor_" + dungeon.getId().toLowerCase(), 0);
							}

							if (run.getFloor() > NumberUtils.toInt(gp.getValue())) {
								gp.setValue(run.getFloor());
								gp.save();

								Main.getApp().getMessageChannelById("971503733202628698")
										.sendMessage(getLocale().get("achievement/dungeon_floor",
												h.getName(), h.getAccount().getName(), run.getFloor(), dungeon.getInfo(getLocale()).getName()
										))
										.queue();
//									Utils.broadcast("achievement/dungeon_floor", loc -> List.of(
//											h.getName(), h.getAccount().getName(), run.getFloor(), dungeon.getInfo(loc).getName()
//									));
							}
						} catch (Exception ignore) {
						}
					}
				} catch (Exception e) {
					Constants.LOGGER.error(e, e);
				}

				Node pNode = map.getPlayerNode();
				boolean deadEnd = pNode.getBlocked().containsAll(pNode.getChildren());
				if (dungeon.isInfinite()) {
					DungeonRun run = map.getRun();

					Set<DungeonRunPlayer> pls = run.getPlayers();
					for (Hero h : heroes.values()) {
						DungeonRunPlayer p = new DungeonRunPlayer(run, h);
						pls.remove(p);
						pls.add(p);
					}

					if (deadEnd) {
						run.delete();
					} else {
						run.save();
					}
				}

				if (deadEnd) {
					if (pNode.isFinalNode()) {
						for (Hero h : heroes.values()) {
							h.apply(n -> n.getCompletedDungeons().add(dungeon));
						}

						finish("str/dungeon_end", getHeroNames());
						return;
					}

					finish("str/dungeon_lost", getHeroNames());
					return;
				}
			} catch (Exception e) {
				Constants.LOGGER.error(e, e);
				getChannel().sendMessage(getLocale().get("error/error", e)).queue();
				close(GameReport.OTHER);
			}
		}
	}

	private boolean defeat() {
		Collection<Hero> hs = heroes.values();
		for (Hero h : hs) {
			double xpPrcnt;
			if (getAreaLevel() >= LEVEL_BRUTAL) {
				xpPrcnt = 0.4;
			} else if (getAreaLevel() >= LEVEL_HARD) {
				xpPrcnt = 0.2;
			} else {
				xpPrcnt = 0;
			}

			h.apply(n -> n.getStats().loseXp((int) (n.getStats().getLosableXp() * xpPrcnt)));
		}

		DungeonRun run = map.getRun();
		reportResult(GameReport.SUCCESS, "str/dungeon_fail",
				getHeroNames(), run.getFloor(), run.getSublevel() + 1
		);

		if (dungeon.isHardcore()) {
			try {
				if (hs.size() == 1) {
					Hero h = hs.iterator().next();
					int rank = DAO.queryNative(Integer.class,
							"SELECT rank FROM dungeon_ranking(?1) WHERE hero_id = ?2",
							dungeon.getId(), h.getId()
					);

					if (rank > 0) {
						Main.getApp().getMessageChannelById("971503733202628698")
								.sendMessage(getLocale().get("loss/dungeon_death",
										h.getName(), rank, run.getFloor(), dungeon.getInfo(getLocale()).getName()
								))
								.queue();
//						Utils.broadcast("loss/dungeon_death", loc -> List.of(
//								h.getName(), rank, run.getFloor(), dungeon.getInfo(loc).getName()
//						));
					}
				}
			} catch (Exception ignore) {
			} finally {
				run.delete();
			}

			return false;
		}


		return true;
	}

	@Override
	protected void onMessage(User user, String value) throws InvocationTargetException, IllegalAccessException {
		Pair<Method, JSONObject> action = toAction(StringUtils.stripAccents(value).toLowerCase());
		if (action != null) {
			action.getFirst().invoke(this, action.getSecond(), user);
		}
	}

	private void grantCombatLoot() {
		Loot loot = combat.get().getLoot();
		XStringBuilder sb = new XStringBuilder();
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
			getChannel().sendMessage(getLocale().get("str/dungeon_loot") + "\n" + sb).queue();
		}

		for (Hero h : heroes.values()) {
			int xp = Math.max(1, loot.xp().get());
			h.apply(n -> {
				int gain = xp;

				int lvl = n.getLevel();
				int diff = Math.abs(getAreaLevel() - lvl) - 5;

				if (diff > 0) {
					gain = (int) (gain * Math.min(Math.pow(0.8, diff), 1));
				}

				n.getStats().addXp(Math.max(1, gain));
				if (n.getLevel() > lvl) {
					getChannel().sendMessage(getLocale().get("str/actor_level_up", n.getName(), n.getLevel())).queue();
				}
			});
		}

		combat.set(null);
	}

	public boolean runCombat(Node node) {
		return runCombat(node, Utils::doNothing);
	}

	public boolean runCombat(Node node, Consumer<Combat> initializer) {
		if (combat.get() != null) return true;

		combat.set(new Combat(this, node));
		initializer.accept(combat.get());

		Set<String> pool = node.getEnemyPool();
		for (int i = 0; i < 4; i++) {
			List<Actor<?>> keepers = combat.get().getActors(Team.KEEPERS);
			if (!Calc.chance(100 - 50d / getPlayers().length * keepers.size(), getNodeRng())) break;

			Monster chosen;
			if (!pool.isEmpty()) chosen = Monster.getRandom(this, Utils.getRandomEntry(getNodeRng(), pool));
			else chosen = Monster.getRandom(this);

			if (chosen != null) {
				keepers.add(chosen);
			}
		}

		combat.get().process();
		return combat.get().isWin();
	}

	@SafeVarargs
	public final <T extends MonsterBase<T>> void beginCombat(Node node, Actor<T>... enemies) {
		if (combat.get() != null) return;
		combat.set(new Combat(this, node, enemies));
	}

	public void runEvent(Node node, Event evt) {
		if (evt == null) {
			runCombat(node);
			return;
		}

		EventDescription ed = evt.parse(this, node);

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setDescription(ed.description());

		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setTimeout(5, TimeUnit.MINUTES)
				.setCanInteract(u -> u.getId().equals(getModerator()) || Utils.equalsAny(u.getId(), getPlayers()))
				.setCancellable(false);

		Set<Choice> choices = new LinkedHashSet<>();
		for (EventAction act : ed.actions()) {
			choices.add(new Choice(act.action(), act.label(), _ ->
					evt.getAction(act.action()).get()
			));
		}

		try {
			requestChoice(eb, null, helper, choices);
		} catch (Exception ignore) {
		}

		if (combat.get() != null) {
			combat.get().process();
		}
	}

	public void requestChoice(EmbedBuilder eb, BufferedImage img, ButtonizeHelper helper, Set<Choice> choices) throws Exception {
		lock = new CompletableFuture<>();

		helper.clearActions();
		if (choices.isEmpty()) {
			helper.addAction(getLocale().get("str/continue"), s -> {
				lock.complete(null);
				Pages.finalizeEvent(s.getMessage(), Utils::doNothing);
			});
		} else {
			Map<String, String> votes = new HashMap<>();

			for (Choice c : choices) {
				ThrowingConsumer<ButtonWrapper> act = w -> {
					if (isClosed()) {
						lock.complete(null);
						return;
					}

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
						if (Objects.equals(outcome, "CONFIRM")) return;
					}

					if (outcome != null) {
						eb.setDescription(Utils.getOr(outcome, "PLACEHOLDER"));

						ButtonizeHelper fin = new ButtonizeHelper(true)
								.setTimeout(5, TimeUnit.MINUTES)
								.setCanInteract(u -> u.getId().equals(getModerator()) || Utils.equalsAny(u.getId(), getPlayers()))
								.setCancellable(false)
								.addAction(getLocale().get("str/continue"), s -> {
									lock.complete(null);
									Pages.finalizeEvent(s.getMessage(), Utils::doNothing);
								});

						Message m = w.getMessage();
						if (w.reloadMessage() != m) {
							fin.apply(w.getMessage().editMessageEmbeds(eb.build())).queue(s -> {
								Pages.buttonize(s, fin);
								message.set(new Pair<>(s, fin));
							});
						} else {
							ClusterAction ca = getChannel().sendEmbed(eb.build()).apply(helper::apply);
							if (img != null) {
								ca.addFile(IO.getBytes(img), "dungeon.jpg");
							}

							ca.queue(s -> {
								Pages.buttonize(s, fin);
								message.set(new Pair<>(s, fin));
							});
						}
					} else {
						lock.complete(null);
						Pages.finalizeEvent(w.getMessage(), Utils::doNothing);
					}
				};

				if (c.label() instanceof Emoji e) {
					helper.addAction(new EmojiId(c.id(), e, null, ButtonStyle.SECONDARY), act);
				} else {
					helper.addAction(new TextId(c.id(), String.valueOf(c.label())), act);
				}
			}
		}

		ClusterAction ca = getChannel().sendEmbed(eb.build()).apply(helper::apply);
		if (img != null) {
			ca.addFile(IO.getBytes(img), "dungeon.jpg");
		}

		ca.queue(s -> {
			Pages.buttonize(s, helper);
			message.set(new Pair<>(s, helper));
		});

		lock.get(5, TimeUnit.MINUTES);
		lock = null;

		message.get().getFirst().delete().queue(null, Utils::doNothing);
		message.set(null);
	}

	private void finish(String message, Object... args) {
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

				getChannel().buffer(
						getLocale().get("str/dungeon_loot_single") +
								"\n```" + Utils.properlyJoin(getLocale(), names) + "```"
				);
			} else {
				InfiniteList<Hero> robin = new InfiniteList<>(heroes.values());
				Collections.shuffle(robin);

				Map<String, List<String>> dist = new HashMap<>();
				for (Gear g : loot.gear()) {
					Hero h = robin.getNext();
					g.setOwner(h);

					dist.computeIfAbsent(h.getName(), _ -> new ArrayList<>()).add(g.getName(getLocale()));
				}
				DAO.insertBatch(loot.gear());

				Map<Hero, Map<UserItem, Integer>> split = new HashMap<>();
				for (UserItem i : loot.items()) {
					Hero h = robin.getNext();
					split.computeIfAbsent(h, _ -> new HashMap<>())
							.compute(i, (_, v) -> v == null ? 1 : v + 1);
				}

				for (Map.Entry<Hero, Map<UserItem, Integer>> e : split.entrySet()) {
					Hero h = e.getKey();
					for (Map.Entry<UserItem, Integer> i : e.getValue().entrySet()) {
						h.getAccount().addItem(i.getKey(), i.getValue());
						dist.computeIfAbsent(h.getName(), _ -> new ArrayList<>())
								.add(i.getKey().getName(getLocale()) + " (x" + i.getValue() + ")");
					}
				}

				XStringBuilder sb = new XStringBuilder();
				for (Map.Entry<String, List<String>> e : dist.entrySet()) {
					sb.appendNewLine(e.getKey() + ":").appendNewLine("```%s```".formatted(
							Utils.properlyJoin(getLocale(), e.getValue())
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

		reportResult(GameReport.SUCCESS, message, args);
	}

	@PlayerAction("reload")
	private void reload(JSONObject args, User u) {
		if (combat.get() != null) combat.get().getLock().complete(null);
		if (getMessage() != null) {
			ButtonizeHelper helper = getMessage().getSecond();
			if (helper != null) {
				helper.apply(getMessage().getFirst().editMessageComponents())
						.queue(s -> Pages.buttonize(s, helper), _ -> {
							if (lock != null) {
								lock.completeExceptionally(new VoidException());
							}
						});
			}
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
		if (combat.get() == null || duel) return;

		EmbedBuilder eb = new ColorlessEmbedBuilder();

		for (Actor<?> a : combat.get().getActors()) {
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

	@Override
	public void reportResult(@MagicConstant(valuesFromClass = GameReport.class) byte code, String msg, Object... args) {
		getChannel().sendMessage(parsePlural(getString(msg, args)))
				.queue(_ -> {
					if (message.get() != null) {
						message.get().getFirst().delete().queue(null, Utils::doNothing);
					}
				}, Utils::doNothing);

		close(code);
	}

	public Dungeon getDungeon() {
		return dungeon;
	}

	public Map<String, Hero> getHeroes() {
		return heroes;
	}

	public String getHeroNames() {
		return Utils.properlyJoin(getLocale(), heroes.values().stream().map(Hero::getName).toList());
	}

	public Pair<Message, ButtonizeHelper> getMessage() {
		return message.get();
	}

	public void setMessage(Message msg, ButtonizeHelper helper) {
		message.set(new Pair<>(msg, helper));
	}

	public void clearMessage() {
		message.set(null);
	}

	public Combat getCombat() {
		return combat.get();
	}

	public Random getNodeRng() {
		return map.getRun().getNodeRng();
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

	public Set<RunModifier> getModifiers() {
		Map<String, RunModifier> modifiers = new HashMap<>();

		for (RunModifier mod : map.getRun().getModifiers()) {
			modifiers.put(mod.getModFamily(), mod);
		}

		for (RunModifier mod : map.getFloor().getModifiers()) {
			RunModifier curr = modifiers.get(mod.getModFamily());
			if (curr != null && curr.getWeight() > mod.getWeight()) continue;

			modifiers.put(mod.getModFamily(), mod);
		}

		return Set.copyOf(modifiers.values());
	}

	public NodeType getAreaType() {
		Node node = map.getPlayerNode();
		if (node != null) {
			return node.getType();
		}

		return NodeType.NONE;
	}

	public int getAreaLevel() {
		return getAreaLevel(map.getFloor());
	}

	public int getAreaLevel(Floor fl) {
		if (duel) {
			return (int) heroes.values().stream()
					.mapToInt(Hero::getLevel)
					.average().orElse(1);
		}

		Node node = map.getPlayerNode();
		int extraLevel = node != null ? node.getNodeLevel() : 0;

		if (dungeon.getAreaLevel() == 0) {
			int floor = fl.getNumber();
			return 1 + Math.min(floor * 83 / 25 * floor / (floor + 50), 83) + extraLevel;
		}

		return dungeon.getAreaLevel() + extraLevel;
	}

	public String parsePlural(String text) {
		String plural = getPartySize() == 1 ? "S" : "P";

		return Utils.regex(getString(text), "\\[(?<S>[^\\[\\]]*?)\\|(?<P>.*?)]")
				.replaceAll(r -> r.group(plural));
	}

	public void send(Actor<?> source, String text) {
		if (text.isBlank()) return;

		String path = Constants.API_ROOT + (source.getId().startsWith("H:") ? "heroes/" : "monsters/");
		for (GuildMessageChannel chn : getChannel().getChannels()) {
			PseudoUser pu = new PseudoUser(source.toString(), path + source.getId(), chn);
			pu.send(null, text);
		}
	}

	private void onTimeout(int turn) {
		if (isDuel()) {
			reportResult(GameReport.GAME_TIMEOUT, "str/versus_end_timeout");
			return;
		}

		if (combat.get() != null) {
			Actor<?> current = combat.get().getCurrent();
			if (current != null) {
				combat.get().getLock().complete(() -> current.setFleed(true));
			} else {
				combat.get().getLock().complete(null);
			}

			setTimeout(this::onTimeout, 5, TimeUnit.MINUTES);
			return;
		}

		DungeonRun run = map.getRun();
		finish("str/dungeon_leave", getHeroNames(), run.getFloor(), run.getSublevel() + 1);
	}
}
