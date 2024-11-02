package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.listener.EventHandler;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.game.engine.Renderer;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.*;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.dunhun.Consumable;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.Monster;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.persistent.localized.LocalizedString;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.ClusterAction;
import com.kuuhaku.model.records.dunhun.CombatContext;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Combat implements Renderer<BufferedImage> {
	private final ScheduledExecutorService cpu = Executors.newSingleThreadScheduledExecutor();
	private final long seed = ThreadLocalRandom.current().nextLong();

	private final Dunhun game;
	private final I18N locale;
	private final List<Actor> played = new ArrayList<>();
	private final InfiniteList<Actor> actors = new InfiniteList<>();
	private final BondedList<Actor> hunters = new BondedList<>((a, it) -> {
		if (getActors(Team.HUNTERS).size() >= 6) return false;

		a.setFleed(false);
		a.setTeam(Team.HUNTERS);
		a.setGame(getGame());

		actors.add(a);
		played.add(a);

		a.getSenshi().setAvailable(true);
		return true;
	}, a -> {
		actors.remove(a);
		if (!actors.contains(a)) {
			a.setHp(0, true);
		}
	});
	private final BondedList<Actor> keepers = new BondedList<>((a, it) -> {
		if (getActors(Team.KEEPERS).size() >= 6) return false;

		a.setFleed(false);
		a.setTeam(Team.KEEPERS);
		a.setGame(getGame());

		actors.add(a);
		played.add(a);

		a.getSenshi().setAvailable(true);
		return true;
	}, a -> {
		actors.remove(a);
		if (!actors.contains(a)) {
			a.setHp(0, true);
		}
	});
	private final FixedSizeDeque<String> history = new FixedSizeDeque<>(8);
	private final RandomList<Actor> rngList = new RandomList<>();
	private final Set<EffectBase> effects = new HashSet<>();

	private CompletableFuture<Runnable> lock;
	private boolean done;
	private Actor current;

	public Combat(Dunhun game, MonsterBase<?>... enemies) {
		this.game = game;
		this.locale = game.getLocale();

		hunters.addAll(game.getHeroes().values());
		keepers.addAll(List.of(enemies));

		for (Actor a : hunters) {
			a.modAp(-a.getAp());
			a.revive(1);
			a.setFleed(false);
		}

		effects.addAll(game.getEffects());
		trigger(Trigger.ON_COMBAT);
	}

	public Combat(Dunhun game, Collection<Hero> duelists) {
		this.game = game;
		this.locale = game.getLocale();

		List<Hero> sides = List.copyOf(duelists);
		List<Actor> team = hunters;
		for (List<Hero> hs : ListUtils.partition(sides, sides.size() / 2)) {
			team.addAll(hs);
			team = keepers;

			for (Hero h : hs) {
				h.getSenshi().setFlag(Flag.NO_STUN);
				h.getSenshi().setFlag(Flag.NO_STASIS);
			}
		}

		effects.addAll(game.getEffects());
		trigger(Trigger.ON_COMBAT);
	}

	@Override
	public BufferedImage render(I18N locale) {
		BufferedImage bi = new BufferedImage(Drawable.SIZE.width * (hunters.size() + keepers.size()) + 64, 75 + Drawable.SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setFont(Fonts.OPEN_SANS.deriveBold(60));

		int i = 0;
		int offset = 0;
		boolean divided = false;
		for (List<Actor> acts : List.of(hunters, keepers)) {
			for (Actor a : acts) {
				BufferedImage card;
				if (a.isOutOfCombat()) {
					a.getSenshi().setAvailable(false);
					BufferedImage overlay = IO.getResourceAsImage("shoukan/states/" + (a.getHp() <= 0 ? "dead" : "flee") + ".png");

					card = a.render(locale);
					Graph.overlay(card, overlay);
				} else {
					card = a.render(locale);
				}

				if (actors.getIndex() == i) {
					boolean legacy = a.getSenshi().getHand().getUserDeck().getFrame().isLegacy();
					String path = "shoukan/frames/state/" + (legacy ? "old" : "new");

					Graph.overlay(card, IO.getResourceAsImage(path + "/hero.png"));
					g2d.drawString("v", offset + Drawable.SIZE.width / 2 - g2d.getFontMetrics().stringWidth("v") / 2, 40);
				}

				g2d.drawImage(card, offset, 50, null);
				Graph.applyTransformed(g2d, offset, 55 + Drawable.SIZE.height, g -> {
					if (a.getHp() < a.getMaxHp() / 3) g.setColor(Color.RED);

					g.drawRect(50, 0, Drawable.SIZE.width - 100, 20);
					g.fillRect(55, 5, a.getHp() * (Drawable.SIZE.width - 110) / a.getMaxHp(), 10);
				});

				offset += 255;
				i++;
			}

			if (!divided) {
				BufferedImage cbIcon = IO.getResourceAsImage("dunhun/icons/combat.png");
				g2d.drawImage(cbIcon, offset, 50 + (bi.getHeight() - 50) / 2 - cbIcon.getHeight() / 2, null);
				offset += 64;
				divided = true;
			}
		}

		g2d.dispose();

		return bi;
	}

	public MessageEmbed getEmbed() {
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/actor_turn", current.getName(locale)))
				.setDescription(String.join("\n", history))
				.setFooter(locale.get("str/combat_footer"));

		if (!game.isDuel()) {
			eb.setAuthor(locale.get("str/dungeon_floor", game.getTurn()));
		}

		String title = locale.get("str/hunters");
		XStringBuilder sb = new XStringBuilder();
		for (List<Actor> acts : List.of(hunters, keepers)) {
			sb.clear();
			for (Actor a : acts) {
				if (!sb.isEmpty()) sb.nextLine();
				sb.appendNewLine(a.getName(locale));
				a.addHpBar(sb);
				a.addApBar(sb);
			}

			eb.addField(title, sb.toString(), true);
			title = locale.get("str/keepers");
		}

		eb.setImage("attachment://cards.png");

		return eb.build();
	}

	public void process() {
		if (done) return;

		actors.sort(Comparator
				.comparingInt(Actor::getInitiative).reversed()
				.thenComparingInt(n -> Calc.rng(20, seed - n.hashCode()))
		);

		loop:
		for (Actor turn : actors) {
			if (game.isClosed()) break;
			else if (hunters.stream().allMatch(Actor::isOutOfCombat)) break;
			else if (keepers.stream().allMatch(Actor::isOutOfCombat)) break;

			current = turn;
			if (current == null) {
				break;
			}

			try {
				try {
					Supplier<Boolean> skip = () -> !current.getSenshi().isAvailable()
												   || current.getSenshi().isStasis()
												   || current.isOutOfCombat();
					boolean skipped = skip.get();

					current.getSenshi().reduceDebuffs(1);
					current.getSenshi().reduceStasis(1);
					for (Skill s : current.getSkills()) {
						s.reduceCd();
					}

					if (skipped) {
						if (hunters.stream().allMatch(Actor::isOutOfCombat)) break;
						else if (keepers.stream().allMatch(Actor::isOutOfCombat)) break;
						continue;
					}

					current.modAp(current.getMaxAp());
					current.getSenshi().setDefending(false);
					trigger(Trigger.ON_TURN_BEGIN, current, current);

					while (current == actors.get() && !skip.get() && current.getAp() > 0) {
						trigger(Trigger.ON_TICK);

						Runnable action = reload().join();
						if (action != null) {
							action.run();
						}

						if (hunters.stream().allMatch(Actor::isOutOfCombat)) break loop;
						else if (keepers.stream().allMatch(Actor::isOutOfCombat)) break loop;
					}

					trigger(Trigger.ON_TURN_END, current, current);
				} finally {
					current.getModifiers().expireMods(current.getSenshi());
					current.getSenshi().setAvailable(true);

					if (!current.getSenshi().isStasis()) {
						current.modHp(current.getRegDeg().next(), false);
						trigger(Trigger.ON_DEGEN, current, current);
					}

					Iterator<EffectBase> it = effects.iterator();
					while (it.hasNext()) {
						EffectBase e = it.next();
						if (!e.getOwner().equals(current)) {
							if (!getActors().contains(e.getOwner())) it.remove();
							continue;
						}

						if (e.decDuration()) it.remove();
						if (e instanceof PersistentEffect pe) {
							pe.getEffect().accept(e, new CombatContext(Trigger.ON_TURN_END, current, current));
						}
					}
				}
			} catch (Exception e) {
				Constants.LOGGER.error(e, e);
			}
		}

		done = true;
		for (EffectBase e : effects) {
			if (e.getOwner() instanceof Hero) {
				game.getEffects().add(e);
			}
		}

		Pair<String, String> previous = game.getMessage();
		if (previous != null) {
			GuildMessageChannel channel = Main.getApp().getMessageChannelById(previous.getFirst());
			if (channel != null) {
				channel.retrieveMessageById(previous.getSecond())
						.flatMap(Objects::nonNull, Message::delete)
						.queue(null, Utils::doNothing);
				game.setMessage(null);
			}
		}
	}

	public CompletableFuture<Runnable> reload() {
		game.resetTimer();

		lock = new CompletableFuture<>();

		ClusterAction ca;
		if (current instanceof Hero h) {
			ca = game.getChannel().sendMessage("<@" + h.getAccount().getUid() + ">").embed(getEmbed());
		} else {
			ca = game.getChannel().sendEmbed(getEmbed());
		}

		ButtonizeHelper helper;
		for (Actor a : getActors()) {
			a.getModifiers().removeIf(a.getSenshi(), m -> m.getExpiration() == 0);
		}

		if (current instanceof Hero h) {
			helper = new ButtonizeHelper(true)
					.setCanInteract(u -> u.getId().equals(h.getAccount().getUid()))
					.setCancellable(false);

			helper.addAction(Utils.parseEmoji("🗡"), w -> {
				List<Actor> tgts = getActors(h.getTeam().getOther()).stream()
						.map(a -> a.isOutOfCombat() ? null : a)
						.toList();

				addSelector(w.getMessage(), helper, tgts,
						t -> lock.complete(() -> attack(h, t))
				);
			});

			if (!h.getSkills().isEmpty()) {
				helper.addAction(Utils.parseEmoji("⚡"), w -> {
					EventHandler handle = Pages.getHandler();
					List<?> selected = handle.getDropdownValues(handle.getEventId(w.getMessage())).get("skills");
					if (selected == null || selected.isEmpty()) {
						game.getChannel().sendMessage(locale.get("error/no_skill_selected")).queue();
						return;
					}

					Skill skill = h.getSkill(String.valueOf(selected.getFirst()));
					if (skill == null) {
						game.getChannel().sendMessage(locale.get("error/invalid_skill")).queue();
						return;
					} else if (skill.getApCost() > h.getAp()) {
						game.getChannel().sendMessage(locale.get("error/not_enough_ap")).queue();
						return;
					} else if (skill.getCd() > 0) {
						game.getChannel().sendMessage(locale.get("error/skill_cooldown")).queue();
						return;
					}

					JSONArray tags = h.getWeaponTags();
					if (!tags.containsAll(skill.getReqTags())) {
						game.getChannel().sendMessage(locale.get("error/invalid_weapon")).queue();
						return;
					}

					addSelector(w.getMessage(), helper, skill.getTargets(this, h),
							t -> lock.complete(() -> skill(skill, h, t))
					);
				});
			}

			if (!h.getConsumables().isEmpty()) {
				helper.addAction(Utils.parseEmoji("\uD83E\uDED9"), w -> {
					EventHandler handle = Pages.getHandler();
					List<?> selected = handle.getDropdownValues(handle.getEventId(w.getMessage())).get("consumables");
					if (selected == null || selected.isEmpty()) {
						game.getChannel().sendMessage(locale.get("error/no_consumable_selected")).queue();
						return;
					}

					Consumable con = DAO.find(Consumable.class, String.valueOf(selected.getFirst()));
					if (con == null) {
						game.getChannel().sendMessage(locale.get("error/invalid_consumable")).queue();
						return;
					}

					addSelector(w.getMessage(), helper, con.getTargets(this, h),
							t -> lock.complete(() -> {
								con.execute(locale, this, h, t);
								h.getSpentConsumables().add(con);
								h.modAp(-1);

								history.add(locale.get(t.equals(h) ? "str/used_skill_self" : "str/used_skill",
										h.getName(), con.getName(locale), t.getName(locale))
								);
							})
					);
				});
			}

			helper.addAction(Utils.parseEmoji("🛡"), w -> lock.complete(() -> {
						h.getSenshi().setDefending(true);
						h.modAp(-h.getAp());

						history.add(locale.get("str/actor_defend", h.getName()));
					}))
					.addAction(Utils.parseEmoji("💨"), w -> {
						ButtonizeHelper confirm = new ButtonizeHelper(true)
								.setCanInteract(u -> u.getId().equals(h.getAccount().getUid()))
								.setCancellable(false)
								.addAction(Utils.parseEmoji("💨"), s -> lock.complete(() -> {
									h.setFleed(true);
									h.modAp(-h.getAp());
								}))
								.addAction(Utils.parseEmoji(Constants.RETURN), v -> {
									MessageEditAction ma = helper.apply(v.getMessage().editMessageComponents());
									addDropdowns(h, ma);
									ma.queue(s -> Pages.buttonize(s, helper));
								});

						confirm.apply(w.getMessage().editMessageComponents()).queue(s -> Pages.buttonize(s, confirm));
					});

			ca.apply(a -> {
				MessageCreateAction ma = helper.apply(a);
				addDropdowns(h, ma);

				return ma;
			});
		} else {
			helper = null;

			MonsterBase<?> curr = (MonsterBase<?>) current;
			cpu.schedule(() -> {
				try {
					boolean canAttack = curr.getSenshi().getDmg() > 0;
					boolean canDefend = curr.getSenshi().getDfs() > 0;

					List<Actor> tgts = getActors(curr.getTeam().getOther()).stream()
							.filter(a -> !a.isOutOfCombat())
							.toList();

					if (curr.getAp() == 1) {
						double threat = tgts.stream()
								.mapToInt(a -> a.getHp() * a.getAggroScore() / a.getMaxHp())
								.average()
								.orElse(1);

						double risk = threat / (curr.getHp() * (double) curr.getAggroScore() / curr.getMaxHp());
						if (curr instanceof Monster && risk > 5 && Calc.chance(20)) {
							curr.setFleed(true);

							game.getChannel().sendMessage(locale.get("str/actor_flee", curr.getName(locale))).queue();
							return;
						}

						if (canDefend && Calc.chance(5 * risk)) {
							curr.getSenshi().setDefending(true);
							curr.modAp(-curr.getAp());

							history.add(locale.get("str/actor_defend", curr.getName(locale)));
							return;
						}
					}

					boolean forcing = false;
					List<Skill> skills = new ArrayList<>();
					for (Skill s : curr.getSkills()) {
						if (s.getApCost() > curr.getAp() || s.getCd() > 0) continue;

						Boolean canUse = s.canCpuUse(this, (MonsterBase<?>) curr);
						if (canUse == null) {
							if (!forcing) skills.add(s);
						} else if (canUse) {
							if (!forcing) skills.clear();
							forcing = true;
							skills.add(s);
						}
					}

					if (!skills.isEmpty() && (forcing || !canAttack || Calc.chance(33))) {
						Skill skill = Utils.getRandomEntry(skills);

						tgts = skill.getTargets(this, curr).stream()
								.filter(a -> a != null && !a.isOutOfCombat())
								.toList();

						if (!tgts.isEmpty()) {
							Actor t = Utils.getWeightedEntry(rngList, a -> a.getTeam() == curr.getTeam() ? 1 : a.getAggroScore(), tgts);

							skill(skill, curr, t);
							return;
						}
					}

					if (tgts.isEmpty()) {
						curr.getSenshi().setDefending(true);
						curr.modAp(-curr.getAp());

						history.add(locale.get("str/actor_defend", curr.getName(locale)));
						return;
					}

					attack(curr, Utils.getWeightedEntry(rngList, Actor::getAggroScore, tgts));
				} catch (Exception e) {
					Constants.LOGGER.error(e, e);
				} finally {
					lock.complete(null);
				}
			}, Calc.rng(3000, 5000), TimeUnit.MILLISECONDS);
		}

		ca.addFile(IO.getBytes(render(game.getLocale()), "png"), "cards.png")
				.queue(m -> {
					if (helper != null) {
						Pages.buttonize(m, helper);
					}

					Pair<String, String> previous = game.getMessage();
					if (previous != null) {
						GuildMessageChannel channel = Main.getApp().getMessageChannelById(previous.getFirst());
						if (channel != null) {
							channel.retrieveMessageById(previous.getSecond())
									.flatMap(Objects::nonNull, Message::delete)
									.queue(null, Utils::doNothing);
							game.setMessage(null);
						}
					}

					game.setMessage(new Pair<>(m.getChannel().getId(), m.getId()));
				});

		return lock;
	}

	private void addDropdowns(Hero h, MessageRequest<?> ma) {
		List<LayoutComponent> comps = new ArrayList<>(ma.getComponents());

		List<Skill> skills = h.getSkills();
		if (!skills.isEmpty()) {
			StringSelectMenu.Builder b = StringSelectMenu.create("skills")
					.setPlaceholder(locale.get("str/use_a_skill"))
					.setMaxValues(1);

			for (Skill s : skills) {
				String cdText = "";
				String reqTags = Utils.properlyJoin(locale.get("str/and")).apply(
						s.getReqTags().stream()
								.map(t -> LocalizedString.get(locale, "tag/" + t, "???"))
								.toList()
				);

				int cd = s.getCd();
				if (cd > 0) {
					cdText = " (CD: " + locale.get("str/turns_inline", cd) + ")";
				}

				if (!reqTags.isBlank()) {
					reqTags = " [" + locale.get("str/requires", reqTags) + "]";
				}

				b.addOption(
						s.getInfo(locale).getName() + " " + StringUtils.repeat('◈', s.getApCost()) + cdText + reqTags,
						s.getId(),
						StringUtils.abbreviate(s.getDescription(locale, h), 100)
				);
			}

			comps.add(ActionRow.of(b.build()));
		}

		Map<Consumable, Integer> cons = h.getConsumables();
		if (!cons.isEmpty()) {
			StringSelectMenu.Builder b = StringSelectMenu.create("consumables")
					.setPlaceholder(locale.get("str/use_a_consumable"))
					.setMaxValues(1);

			for (Consumable c : cons.keySet()) {
				b.addOption(
						c.getName(locale) + " (x" + cons.getOrDefault(c, 0) + ")",
						c.getId(),
						StringUtils.abbreviate(c.getDescription(locale), 100)
				);
			}

			comps.add(ActionRow.of(b.build()));
		}

		ma.setComponents(comps);
	}

	public void attack(Actor source, Actor target) {
		source.modAp(-1);

		trigger(Trigger.ON_DEFEND, target, source);

		history.add(locale.get("str/actor_combat", source.getName(locale), target.getName(locale)));

		Senshi srcSen = source.getSenshi();
		Senshi tgtSen = target.getSenshi();
		if (srcSen.isBlinded(true) && Calc.chance(50)) {
			trigger(Trigger.ON_MISS, source, target);

			history.add(locale.get("str/actor_miss", source.getName(locale)));
			return;
		} else if (!tgtSen.isSleeping() && !tgtSen.isStunned() && !tgtSen.isStasis()) {
			if (Calc.chance(tgtSen.getDodge())) {
				trigger(Trigger.ON_MISS, source, target);
				trigger(Trigger.ON_DODGE, target, source);

				history.add(locale.get("str/actor_dodge", target.getName(locale)));
				return;
			} else if (Calc.chance(tgtSen.getParry())) {
				trigger(Trigger.ON_PARRY, target, source);

				history.add(locale.get("str/actor_parry", target.getName(locale)));
				attack(target, source);
				return;
			}
		}

		trigger(Trigger.ON_ATTACK, source, target);
		target.modHp(-srcSen.getDmg(), Calc.chance(source.getCritical()));
		trigger(Trigger.ON_HIT, source, target);

		if (target.getHp() == 0) {
			trigger(Trigger.ON_KILL, source, target);
		}
	}

	public void skill(Skill skill, Actor source, Actor target) {
		source.modAp(-skill.getApCost());

		trigger(Trigger.ON_SPELL, source, target);
		trigger(Trigger.ON_SPELL_TARGET, target, source);

		history.add(locale.get(target.equals(source) ? "str/used_skill_self" : "str/used_skill",
				source.getName(locale), skill.getInfo(locale).getName(), target.getName(locale))
		);

		try {
			if (source.getTeam() != target.getTeam() && Calc.chance(target.getSenshi().getDodge())) {
				trigger(Trigger.ON_MISS, source, target);
				trigger(Trigger.ON_DODGE, target, source);

				history.add(locale.get("str/actor_dodge", target.getName(locale)));
				return;
			}

			skill.execute(locale, this, source, target);
		} finally {
			if (skill.getCooldown() > 0) {
				skill.setCd(skill.getCooldown());
			}

			if (target.getHp() == 0) {
				trigger(Trigger.ON_KILL, source, target);
			}
		}
	}

	public void addSelector(Message msg, ButtonizeHelper root, List<Actor> targets, Consumer<Actor> action) {
		if (targets.stream().allMatch(Objects::isNull)) {
			game.getChannel().sendMessage(locale.get("error/no_targets")).queue();
			return;
		}

		Actor single = null;
		for (Actor a : targets) {
			if (single == null) single = a;
			else if (a != null) {
				single = null;
				break;
			}
		}

		if (single != null) {
			action.accept(single);
			return;
		}

		Hero h = (Hero) current;
		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setCanInteract(u -> u.getId().equals(h.getAccount().getUid()))
				.setCancellable(false);

		for (int i = 0; i < targets.size(); i++) {
			Actor tgt = targets.get(i);
			helper.addAction(
					Utils.parseEmoji(Utils.fancyNumber(i + 1)),
					w -> {
						if (tgt != null) {
							action.accept(tgt);
						}
					}
			);
		}

		helper.addAction(Utils.parseEmoji(Constants.RETURN), w -> {
			MessageEditAction ma = root.apply(msg.editMessageComponents());

			addDropdowns(h, ma);

			ma.queue(s -> Pages.buttonize(s, root));
		});

		MessageEditAction act = msg.editMessageComponents();
		List<LayoutComponent> rows = helper.getComponents(act);

		int idx = 0;
		loop:
		for (LayoutComponent row : rows) {
			if (row instanceof ActionRow ar) {
				List<ItemComponent> items = ar.getComponents();
				for (int i = 0, sz = items.size(); i < sz; i++, idx++) {
					if (idx >= targets.size()) break loop;

					Actor tgt = targets.get(idx);
					ItemComponent item = items.get(i);
					if (item instanceof Button b && tgt == null) {
						items.set(i, b.asDisabled());
					}
				}
			}
		}

		act.setComponents(rows).queue(s -> Pages.buttonize(s, helper));
	}

	public CompletableFuture<Runnable> getLock() {
		return lock;
	}

	public Set<EffectBase> getEffects() {
		return effects;
	}

	public List<Actor> getPlayed() {
		return played;
	}

	public List<Actor> getActors() {
		return getActors(false);
	}

	public List<Actor> getActors(boolean removeDead) {
		return actors.values().stream()
				.filter(a -> !(removeDead || a.isOutOfCombat()))
				.toList();
	}

	public List<Actor> getActors(Team team) {
		return getActors(team, false);
	}

	public List<Actor> getActors(Team team, boolean removeDead) {
		List<Actor> out = switch (team) {
			case HUNTERS -> hunters;
			case KEEPERS -> keepers;
		};

		if (removeDead) {
			out = out.stream()
					.filter(a -> !a.isOutOfCombat())
					.toList();
		}

		return out;
	}

	public I18N getLocale() {
		return locale;
	}

	public Dunhun getGame() {
		return game;
	}

	public FixedSizeDeque<String> getHistory() {
		return history;
	}

	public Actor getCurrent() {
		return current;
	}

	public boolean isDone() {
		return done;
	}

	public void trigger(Trigger t) {
		trigger(t, null, null);
	}

	public void trigger(Trigger t, Actor from, Actor to) {
		Iterator<EffectBase> it = effects.iterator();
		while (it.hasNext()) {
			EffectBase e = it.next();
			if (from == null) {
				from = e.getOwner();
			}

			if (!(e instanceof TriggeredEffect te) || te.isLocked() || !Utils.equalsAny(t, te.getTriggers())) continue;
			else if (!e.getOwner().equals(from)) {
				if (!getActors().contains(e.getOwner())) it.remove();
				continue;
			}

			if (te.decLimit()) it.remove();

			try {
				te.lock();
				te.getEffect().accept(e, new CombatContext(t, from, to));
			} finally {
				te.unlock();
			}
		}

		if (from != null && to != null) {
			from.trigger(t, to);
		}
	}
}
