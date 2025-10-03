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
import com.kuuhaku.model.common.shoukan.ValueMod;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.dunhun.*;
import com.kuuhaku.model.persistent.localized.LocalizedString;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.ClusterAction;
import com.kuuhaku.model.records.dunhun.CombatContext;
import com.kuuhaku.model.records.dunhun.Loot;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class Combat implements Renderer<BufferedImage> {
	private final ScheduledExecutorService cpu = Executors.newSingleThreadScheduledExecutor();

	private final Dunhun game;
	private final Node node;
	private final InfiniteList<Actor<?>> actors = new InfiniteList<>();
	private final BondedList<Actor<?>> hunters = new BondedList<>(
			(a, it) -> onAddActor(a, Team.HUNTERS), this::onRemoveActor
	);
	private final BondedList<Actor<?>> keepers = new BondedList<>(
			(a, it) -> onAddActor(a, Team.KEEPERS), this::onRemoveActor
	);
	private final FixedSizeDeque<String> history = new FixedSizeDeque<>(8);
	private final RandomList<Actor<?>> rngList = new RandomList<>();
	private final TimedMap<EffectBase> effects = new TimedMap<>();
	private final Loot loot = new Loot();

	private CompletableFuture<Runnable> lock;
	private boolean done, win;

	public Combat(Dunhun game, Node node, MonsterBase<?>... enemies) {
		this.game = game;
		this.node = node;

		hunters.addAll(game.getHeroes().values());
		keepers.addAll(Stream.of(enemies)
				.filter(Objects::nonNull)
				.toList()
		);

		for (Actor<?> a : hunters) {
			a.setAp(0);
			if (a.getHp() <= 0) {
				a.setHp(1);
			}
		}
	}

	public Combat(Dunhun game, Node node, Collection<Hero> duelists) {
		this.game = game;
		this.node = node;

		List<Hero> sides = List.copyOf(duelists);
		List<Actor<?>> team = hunters;
		for (List<Hero> hs : ListUtils.partition(sides, sides.size() / 2)) {
			team.addAll(hs);
			team = keepers;

			for (Hero h : hs) {
				h.getSenshi().setFlag(Flag.NO_STASIS);
				h.getSenshi().setFlag(Flag.NO_PARALYSIS);
				h.getSenshi().setFlag(Flag.NO_SLEEP);
			}
		}
	}

	public boolean onAddActor(Actor<?> actor, Team team) {
		if (getActors(team).size() >= 6) return false;

		actor.getBinding().bind(getGame(), team);
		getActors(team.getOther()).remove(actor);
		actors.add(actor);

		actor.setFleed(false);
		actor.getSenshi().setAvailable(true);
		trigger(Trigger.ON_INITIALIZE, actor, actor);
		return true;
	}

	public void onRemoveActor(Actor<?> actor) {
		trigger(Trigger.ON_REMOVE, actor, actor);
		actor.getBinding().unbind();
		actors.remove(actor);
	}

	@Override
	public BufferedImage render(I18N locale) {
		BufferedImage bi = new BufferedImage(Drawable.SIZE.width * (hunters.size() + keepers.size()) + 64, 80 + Drawable.SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setFont(Fonts.OPEN_SANS.deriveBold(60));

		int offset = 0;
		boolean divided = false;
		for (List<Actor<?>> acts : List.of(hunters, keepers)) {
			for (Actor<?> a : acts) {
				BufferedImage card;
				if (a.isOutOfCombat()) {
					a.getSenshi().setAvailable(false);
					BufferedImage overlay = IO.getResourceAsImage("shoukan/states/" + (a.getHp() <= 0 ? "dead" : "flee") + ".png");

					card = a.render();
					Graph.overlay(card, overlay);
				} else {
					card = a.render();
				}

				if (Objects.equals(a, getCurrent())) {
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
				.setTitle(getLocale().get("str/actor_turn", getCurrent().getName()))
				.setDescription(String.join("\n", history))
				.setFooter(getLocale().get("str/combat_footer"));

		if (!game.isDuel()) {
			eb.setAuthor(getLocale().get("str/dungeon_area",
					node.getSublevel().getFloor().getFloor(), node.getSublevel().getSublevel() + 1
			));
		} else {
			String teamA = Utils.properlyJoin(getLocale().get("str/and")).apply(hunters.stream().map(Actor::getName).toList());
			String teamB = Utils.properlyJoin(getLocale().get("str/and")).apply(keepers.stream().map(Actor::getName).toList());

			eb.setAuthor(getLocale().get("str/dungeon_duel", teamA, teamB));
		}

		String title = getLocale().get("str/hunters");
		XStringBuilder sb = new XStringBuilder();
		for (List<Actor<?>> acts : List.of(hunters, keepers)) {
			sb.clear();
			for (Actor<?> a : acts) {
				if (!sb.isEmpty()) sb.nextLine();
				sb.appendNewLine(a.getName());
				a.addHpBar(sb);
				a.addApBar(sb);
			}

			eb.addField(title, sb.toString(), true);
			title = getLocale().get("str/keepers");
		}

		eb.setImage("attachment://cards.png");

		return eb.build();
	}

	public void process() {
		if (done) return;

		trigger(Trigger.ON_COMBAT);

		rngList.clear();
		for (Actor<?> actor : actors.values()) {
			rngList.add(actor, actor.getInitiative());
		}

		actors.clear();
		while (!rngList.isEmpty()) {
			actors.add(rngList.remove());
		}

		combat:
		for (Actor<?> actor : actors) {
			if (checkCombatEnd()) break;

			try {
				try {
					actor.getSenshi().reduceDebuffs(1);
					actor.getSenshi().reduceStasis(1);
					for (Skill s : actor.getSkills()) {
						s.reduceCd();
					}

					actor.setAp(actor.getMaxAp());
					actor.getSenshi().setDefending(false);
					actor.getModifiers().expireMods(actor);

					trigger(Trigger.ON_TURN_BEGIN, actor, actor, new AtomicInteger());

					while (actor == getCurrent() && actor.getAp() > 0) {
						if (!actor.getSenshi().isAvailable() || actor.isOutOfCombat()) break;
						else if (checkCombatEnd()) break combat;

						trigger(Trigger.ON_TICK);
						Runnable action = reload().join();
						if (action != null) {
							action.run();
						}
					}

					trigger(Trigger.ON_TURN_END, actor, actor, new AtomicInteger());
				} finally {
					actor.getSenshi().setAvailable(true);

					if (!actor.getSenshi().isStasis()) {
						actor.applyRegDeg();
					}
				}
			} catch (Exception e) {
				Constants.LOGGER.error(e, e);
			}
		}

		trigger(win ? Trigger.ON_VICTORY : Trigger.ON_DEFEAT);
		done = true;

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

	private boolean checkCombatEnd() {
		if (game.isClosed()) return true;
		else if (hunters.stream().allMatch(Actor::isOutOfCombat)) return true;
		else if (keepers.stream().allMatch(Actor::isOutOfCombat)) {
			win = true;
			return true;
		}

		return false;
	}

	public synchronized CompletableFuture<Runnable> reload() {
		game.resetTimer();

		lock = new CompletableFuture<>();

		ClusterAction ca;
		if (getCurrent() instanceof Hero h) {
			ca = game.getChannel().sendMessage("<@" + h.getAccount().getUid() + ">").embed(getEmbed());
		} else {
			ca = game.getChannel().sendEmbed(getEmbed());
		}

		ButtonizeHelper helper;
		for (Actor<?> a : getActors()) {
			a.getModifiers().removeIf(a, m -> m.getExpiration() == 0);
		}

		if (getCurrent() instanceof Hero h) {
			helper = new ButtonizeHelper(true)
					.setCanInteract(u -> u.getId().equals(h.getAccount().getUid()))
					.setCancellable(false);

			helper.addAction(Utils.parseEmoji("🗡️"), w -> {
				List<Actor<?>> tgts = new ArrayList<>();
				for (Actor<?> a : getActors(h.getTeam().getOther())) {
					Actor<?> actor = a.isOutOfCombat() ? null : a;
					tgts.add(actor);
				}

				addSelector(w.getMessage(), helper, tgts,
						t -> lock.complete(() -> attack(h, t, null))
				);
			});

			List<Skill> skills = h.getSkills();
			if (!skills.isEmpty()) {
				helper.addAction(Utils.parseEmoji("⚡"), w -> {
					EventHandler handle = Pages.getHandler();
					Map<String, List<?>> values = handle.getDropdownValues(handle.getEventId(w.getMessage()));
					if (values == null) {
						game.getChannel().sendMessage(getLocale().get("error/no_values")).queue();
						return;
					}

					List<?> selected = values.get("skills");
					if (selected == null || selected.isEmpty()) {
						game.getChannel().sendMessage(getLocale().get("error/no_skill_selected")).queue();
						return;
					}

					Skill skill = h.getSkill(String.valueOf(selected.getFirst()));
					if (skill == null) {
						game.getChannel().sendMessage(getLocale().get("error/invalid_skill")).queue();
						return;
					} else if (skill.getStats().getCost() > h.getAp()) {
						game.getChannel().sendMessage(getLocale().get("error/not_enough_ap")).queue();
						return;
					} else if (skill.getCooldown() > 0) {
						game.getChannel().sendMessage(getLocale().get("error/skill_cooldown")).queue();
						return;
					}

					JSONArray tags = h.getEquipment().getWeaponTags();
					if (!tags.containsAll(skill.getRequirements().tags())) {
						game.getChannel().sendMessage(getLocale().get("error/invalid_weapon")).queue();
						return;
					}

					addSelector(w.getMessage(), helper, skill.getTargets(h),
							t -> lock.complete(() -> skill(skill, h, t))
					);
				});
			}

			if (!h.getConsumables().isEmpty()) {
				helper.addAction(Utils.parseEmoji("🫙"), w -> {
					EventHandler handle = Pages.getHandler();
					List<?> selected = handle.getDropdownValues(handle.getEventId(w.getMessage())).get("consumables");
					if (selected == null || selected.isEmpty()) {
						game.getChannel().sendMessage(getLocale().get("error/no_consumable_selected")).queue();
						return;
					}

					Consumable con = DAO.find(Consumable.class, String.valueOf(selected.getFirst()));
					if (con == null) {
						game.getChannel().sendMessage(getLocale().get("error/invalid_consumable")).queue();
						return;
					}

					addSelector(w.getMessage(), helper, con.getTargets(h),
							t -> lock.complete(() -> {
								con.execute(h, t);
								h.consumeAp(1);

								history.add(getLocale().get(t.equals(h) ? "str/used_self" : "str/used",
										h.getName(), con.getName(getLocale()), t.getName())
								);
							})
					);
				});
			}

			helper.addAction(Utils.parseEmoji("🛡️"), w -> lock.complete(() -> {
						h.getSenshi().setDefending(true);
						h.setAp(0);

						history.add(getLocale().get("str/actor_defend", h.getName()));
					}))
					.addAction(Utils.parseEmoji("💨"), w -> {
						ButtonizeHelper confirm = new ButtonizeHelper(true)
								.setCanInteract(u -> u.getId().equals(h.getAccount().getUid()))
								.setCancellable(false)
								.addAction(Utils.parseEmoji("💨"), s -> lock.complete(() ->
										h.setFleed(true)
								))
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

			Actor<?> curr = getCurrent();
			cpu.schedule(() -> {
				try {
					boolean canAttack = curr.getSenshi().getDmg() > 0;
					boolean canDefend = curr.getSenshi().getDfs() > 0;
					Function<Actor<?>, Integer> criteria = a -> {
						if (a.getTeam() == curr.getTeam()) return a.getAggroScore();

						Senshi sen = a.getSenshi();
						return (int) (a.getAggroScore() * (1 - sen.getDodge() / 100d) * (1 - sen.getParry() / 100d));
					};

					List<Actor<?>> attackTgts = getActors(curr.getTeam().getOther()).stream()
							.filter(a -> !a.isOutOfCombat())
							.toList();

					if (curr.getAp() == 1) {
						double threat = attackTgts.stream()
								.mapToInt(a -> a.getHp() * a.getAggroScore() / a.getMaxHp())
								.average()
								.orElse(1);

						double risk = threat / (curr.getHp() * (double) curr.getAggroScore() / curr.getMaxHp());
						if (curr instanceof Monster && risk > 5 && Calc.chance(20)) {
							curr.setFleed(true);
							game.getChannel().sendMessage(getLocale().get("str/actor_flee", curr.getName())).queue();
							return;
						}

						if (canDefend && Calc.chance(5 * risk)) {
							curr.getSenshi().setDefending(true);
							curr.setAp(0);

							history.add(getLocale().get("str/actor_defend", curr.getName()));
							return;
						}
					}

					AtomicBoolean force = new AtomicBoolean();
					List<Skill> skills = collectCpuSkills(curr, force);

					if (!skills.isEmpty()) {
						Skill skill = Utils.getRandomEntry(skills);

						List<Actor<?>> spellTgts = skill.getTargets(curr).stream()
								.filter(Objects::nonNull)
								.toList();

						if (!spellTgts.isEmpty()) {
							Actor<?> t = Utils.getWeightedEntry(rngList, criteria, spellTgts);

							if (force.get() || !canAttack || Calc.chance(50)) {
								skill(skill, curr, t);
								return;
							}
						}
					}

					if (attackTgts.isEmpty()) {
						curr.getSenshi().setDefending(true);
						curr.setAp(0);

						history.add(getLocale().get("str/actor_defend", curr.getName()));
						return;
					}

					attack(curr, Utils.getWeightedEntry(rngList, criteria, attackTgts), null);
				} catch (Exception e) {
					Constants.LOGGER.error(e, e);
				} finally {
					lock.complete(null);
				}
			}, Calc.rng(3000, 5000), TimeUnit.MILLISECONDS);
		}

		ca.addFile(IO.getBytes(render(getLocale()), "png"), "cards.png")
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

	private List<Skill> collectCpuSkills(Actor<?> source, AtomicBoolean force) {
		List<Skill> skills = new ArrayList<>();
		for (Skill s : source.getSkills()) {
			if (s.getStats().getCost() > source.getAp() || s.getCooldown() > 0) continue;

			switch (s.canCpuUse(source, null)) {
				case ANY -> skills.add(s);
				case FORCE -> {
					skills.clear();
					skills.add(s);
					force.set(true);
					return skills;
				}
			}
		}

		return skills;
	}

	private void addDropdowns(Hero h, MessageRequest<?> ma) {
		List<LayoutComponent> comps = new ArrayList<>(ma.getComponents());

		List<Skill> skills = h.getSkills();
		if (!skills.isEmpty()) {
			StringSelectMenu.Builder b = StringSelectMenu.create("skills")
					.setPlaceholder(getLocale().get("str/use_a_skill"))
					.setMaxValues(1);

			for (Skill s : skills) {
				String cdText = "";
				String reqTags = Utils.properlyJoin(getLocale().get("str/and")).apply(
						s.getRequirements().tags().stream()
								.map(t -> LocalizedString.get(getLocale(), "tag/" + t, "???"))
								.toList()
				);

				int cd = s.getCooldown();
				if (cd > 0) {
					cdText = " (CD: " + getLocale().get("str/turns_inline", cd) + ")";
				}

				if (!reqTags.isBlank()) {
					reqTags = " [" + getLocale().get("str/requires", reqTags) + "]";
				}

				b.addOption(
						s.getInfo(getLocale()).getName() + " " + StringUtils.repeat('◈', s.getStats().getCost()) + cdText + reqTags,
						s.getId(),
						StringUtils.abbreviate(s.getDescription(getLocale(), h).replace("*", ""), 100)
				);
			}

			comps.add(ActionRow.of(b.build()));
		}

		Set<Consumable> cons = h.getConsumables();
		if (!cons.isEmpty()) {
			StringSelectMenu.Builder b = StringSelectMenu.create("consumables")
					.setPlaceholder(getLocale().get("str/use_a_consumable"))
					.setMaxValues(1);

			for (Consumable c : cons) {
				b.addOption(
						c.getName(getLocale()) + " (x" + c.getCount() + ")",
						c.getId(),
						StringUtils.abbreviate(c.getDescription(getLocale()), 100)
				);
			}

			comps.add(ActionRow.of(b.build()));
		}

		ma.setComponents(comps);
	}

	public void attack(Actor<?> source, Actor<?> target) {
		attack(source, target, null);
	}

	public void attack(Actor<?> source, Actor<?> target, Double damageMult) {
		if (damageMult == null) {
			source.consumeAp(1);
			history.add(getLocale().get("str/actor_combat", source.getName(), target.getName()));
		}

		trigger(Trigger.ON_DEFEND, target, source);

		Senshi srcSen = source.getSenshi();
		Senshi tgtSen = target.getSenshi();
		if (damageMult == null) {
			if (source.getTeam() != target.getTeam()) {
				if (srcSen.isBlinded(true) && Calc.chance(50)) {
					trigger(Trigger.ON_MISS, source, target);

					history.add(getLocale().get("str/actor_miss", source.getName()));
					return;
				} else if (!srcSen.hasFlag(Flag.TRUE_STRIKE, true) && !tgtSen.isSleeping() && !tgtSen.isStasis()) {
					if (Calc.chance(tgtSen.getDodge())) {
						trigger(Trigger.ON_MISS, source, target);
						trigger(Trigger.ON_DODGE, target, source);

						history.add(getLocale().get("str/actor_dodge", target.getName()));
						return;
					} else if (Calc.chance(tgtSen.getParry())) {
						trigger(Trigger.ON_PARRY, target, source);

						history.add(getLocale().get("str/actor_parry", target.getName()));
						attack(target, source, null);
						return;
					}
				}
			}

			damageMult = 1d;
		}

		AtomicInteger dmg = new AtomicInteger((int) (srcSen.getDmg() * damageMult));

		trigger(Trigger.ON_ATTACK, source, target, dmg);
		target.modHp(source, -dmg.get(), source.getCritical());
		trigger(Trigger.ON_HIT, source, target, dmg);

		if (target.getHp() == 0) {
			trigger(Trigger.ON_KILL, source, target, dmg);
		}
	}

	public void skill(Skill skill, Actor<?> source, Actor<?> target) {
		source.consumeAp(skill.getStats().getCost());

		trigger(Trigger.ON_SPELL, source, target);
		trigger(Trigger.ON_SPELL_TARGET, target, source);

		history.add(getLocale().get(target.equals(source) ? "str/used_self" : "str/used",
				source.getName(), skill.getInfo(getLocale()).getName(), target.getName())
		);

		try {
			Senshi srcSen = source.getSenshi();
			Senshi tgtSen = target.getSenshi();
			if (source.getTeam() != target.getTeam()) {
				if (srcSen.isBlinded(true) && Calc.chance(50)) {
					trigger(Trigger.ON_MISS, source, target);

					history.add(getLocale().get("str/actor_miss", source.getName()));
					return;
				} else if (!srcSen.hasFlag(Flag.TRUE_STRIKE, true) && !tgtSen.isSleeping() && !tgtSen.isStasis()) {
					if (Calc.chance(tgtSen.getDodge())) {
						trigger(Trigger.ON_MISS, source, target);
						trigger(Trigger.ON_DODGE, target, source);

						history.add(getLocale().get("str/actor_dodge", target.getName()));
						return;
					} else if (Calc.chance(tgtSen.getParry())) {
						trigger(Trigger.ON_PARRY, target, source);

						history.add(getLocale().get("str/actor_parry", target.getName()));
						attack(target, source, null);
						return;
					}
				}
			}

			skill.execute(source, target);
		} finally {
			if (skill.getCooldown() > 0) {
				skill.setCooldown(skill.getCooldown());
			}

			if (target.getHp() == 0) {
				trigger(Trigger.ON_KILL, source, target);
			}
		}
	}

	public void addSelector(Message msg, ButtonizeHelper root, List<Actor<?>> targets, Consumer<Actor<?>> action) {
		if (targets.stream().allMatch(Objects::isNull)) {
			game.getChannel().sendMessage(getLocale().get("error/no_targets")).queue();
			return;
		}

		Actor<?> single = null;
		for (Actor<?> a : targets) {
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

		Hero h = (Hero) getCurrent();
		ButtonizeHelper helper = new ButtonizeHelper(true)
				.setCanInteract(u -> u.getId().equals(h.getAccount().getUid()))
				.setCancellable(false);

		for (int i = 0; i < targets.size(); i++) {
			Actor<?> tgt = targets.get(i);
			helper.addAction(
					String.valueOf(i + 1),
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

					Actor<?> tgt = targets.get(idx);
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

	public TimedMap<EffectBase> getEffects() {
		return effects;
	}

	public InfiniteList<Actor<?>> getTurns() {
		return actors;
	}

	public List<Actor<?>> getActors() {
		return getActors(false);
	}

	public List<Actor<?>> getActors(boolean removeDead) {
		return actors.values().stream()
				.filter(a -> !(removeDead || a.isOutOfCombat()))
				.toList();
	}

	public List<Actor<?>> getActors(Team team) {
		return getActors(team, false);
	}

	public List<Actor<?>> getActors(Team team, boolean removeDead) {
		List<Actor<?>> out = switch (team) {
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
		return game.getLocale();
	}

	public Dunhun getGame() {
		return game;
	}

	public FixedSizeDeque<String> getHistory() {
		return history;
	}

	public Actor<?> getCurrent() {
		return actors.getCurrent();
	}

	public Loot getLoot() {
		return loot;
	}

	public boolean isDone() {
		return done;
	}

	public boolean isWin() {
		return win;
	}

	public void trigger(Trigger t) {
		for (Actor<?> a : actors.values()) {
			trigger(t, a, a);
		}
	}

	public void trigger(Trigger t, Actor<?> source, Actor<?> target) {
		trigger(t, source, target, new AtomicInteger());
	}

	public void trigger(Trigger t, Actor<?> source, Actor<?> target, AtomicInteger value) {
		if (t == Trigger.ON_TICK) {
			source.getModifiers().removeIf(source, ValueMod::isExpired);
		} else if (Utils.equalsAny(t, Trigger.ON_VICTORY, Trigger.ON_DEFEAT)) {
			source.getRegDeg().clear();
		}

		CombatContext context = new CombatContext(t, source, target, value);
		Set<EffectBase> effects = new HashSet<>(this.effects.getValues());
		for (RunModifier mod : game.getModifiers()) {
			EffectBase e = mod.toEffect(game);
			if (e != null) {
				effects.add(e);
			}
		}
		effects.removeIf(EffectBase::isClosed);

		for (EffectBase e : effects) {
			if (e.isLocked()) continue;
			else if (e instanceof TriggeredEffect te) {
				if (!Utils.equalsAny(t, te.getTriggers())) continue;
				te.decLimit();
			}

			try {
				e.lock();
				e.getEffect().accept(e, context);
			} finally {
				e.unlock();
			}
		}

		this.effects.reduceTime();
		if (source != null) {
			source.trigger(t, Utils.getOr(target, source), context.value());
		}
	}
}
