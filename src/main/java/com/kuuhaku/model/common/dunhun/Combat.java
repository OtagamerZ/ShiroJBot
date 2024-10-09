package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.listener.EventHandler;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.game.engine.Renderer;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.*;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.dunhun.Consumable;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.Monster;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.ClusterAction;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
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
import org.apache.commons.collections4.Bag;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class Combat implements Renderer<BufferedImage> {
	private final ScheduledExecutorService cpu = Executors.newSingleThreadScheduledExecutor();
	private final long seed = ThreadLocalRandom.current().nextLong();

	private final Dunhun game;
	private final I18N locale;
	private final InfiniteList<Actor> turns = new InfiniteList<>();
	private final BondedList<Actor> hunters = new BondedList<>(a -> {
		if (!turns.isEmpty()) turns.add(a);

		a.setFleed(false);
		a.setTeam(Team.HUNTERS);
		a.setGame(getGame());

		a.asSenshi(getLocale()).setAvailable(true);
	}, turns::remove);
	private final BondedList<Actor> keepers = new BondedList<>(a -> {
		if (!turns.isEmpty()) turns.add(a);

		a.setFleed(false);
		a.setTeam(Team.KEEPERS);
		a.setGame(getGame());

		a.asSenshi(getLocale()).setAvailable(true);
	}, turns::remove);
	private final FixedSizeDeque<String> history = new FixedSizeDeque<>(5);
	private final RandomList<Actor> rngList = new RandomList<>();
	private final List<EffectBase> effects = new ArrayList<>();

	private CompletableFuture<Runnable> lock;

	public Combat(Dunhun game, Monster... enemies) {
		this.game = game;
		this.locale = game.getLocale();

		hunters.addAll(game.getHeroes().values());
		keepers.addAll(List.of(enemies));
	}

	@Override
	public BufferedImage render(I18N locale) {
		BufferedImage bi = new BufferedImage(Drawable.SIZE.width * (hunters.size() + keepers.size()) + 64, 50 + Drawable.SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);
		g2d.setFont(Fonts.OPEN_SANS.deriveBold(60));

		int offset = 0;
		boolean divided = false;
		for (List<Actor> acts : List.of(hunters, keepers)) {
			for (Actor a : acts) {
				BufferedImage card;
				if (a.isSkipped()) {
					a.asSenshi(locale).setAvailable(false);
					BufferedImage overlay = IO.getResourceAsImage("shoukan/states/" + (a.getHp() <= 0 ? "dead" : "flee") + ".png");

					card = a.render(locale);
					Graph.overlay(card, overlay);
				} else {
					card = a.render(locale);
				}

				if (turns.get().equals(a)) {
					boolean legacy = a.asSenshi(locale).getHand().getUserDeck().getFrame().isLegacy();
					String path = "shoukan/frames/state/" + (legacy ? "old" : "new");

					Graph.overlay(card, IO.getResourceAsImage(path + "/hero.png"));
					g2d.drawString("v", offset + Drawable.SIZE.width / 2 - g2d.getFontMetrics().stringWidth("v") / 2, 40);
				} else {
					a.modAp(-a.getAp());
				}

				g2d.drawImage(card, offset, 50, null);
				offset += 255;
			}

			if (!divided) {
				BufferedImage cbIcon = IO.getResourceAsImage("dunhun/icons/combat.png");
				g2d.drawImage(cbIcon, offset, 153, null);
				offset += 64;
				divided = true;
			}
		}

		g2d.dispose();

		return bi;
	}

	public MessageEmbed getEmbed() {
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(locale.get("str/actor_turn", turns.get().getName(locale)))
				.setDescription(String.join("\n", history));

		String title = locale.get("str/hunters");
		XStringBuilder sb = new XStringBuilder();
		for (List<Actor> acts : List.of(hunters, keepers)) {
			sb.clear();
			for (Actor a : acts) {
				if (!sb.isEmpty()) sb.nextLine();
				sb.appendNewLine(a.getName(locale));
				sb.appendNewLine("HP: " + a.getHp() + "/" + a.getMaxHp());
				sb.nextLine();

				boolean rdClosed = true;
				int rd = -a.getRegDeg().peek();
				if (rd > 0) {
					sb.append("__");
					rdClosed = false;
				}

				int steps = (int) Math.ceil(a.getMaxHp() / 100d);
				for (int i = 0; i < steps; i++) {
					if (i > 0 && i % 10 == 0) sb.nextLine();
					int threshold = i * 100;

					if (!rdClosed && threshold > rd) {
						sb.append("__");
						rdClosed = true;
					}

					if (a.getHp() > 0 && a.getHp() >= threshold) sb.append('▰');
					else sb.append('▱');
				}

				sb.appendNewLine(Utils.makeProgressBar(a.getAp(), a.getMaxAp(), a.getMaxAp(), '◇', '◈'));
			}

			eb.addField(title, sb.toString(), true);
			title = locale.get("str/keepers");
		}

		eb.setImage("attachment://cards.png");

		return eb.build();
	}

	public void process() {
		Stream.of(hunters.stream(), keepers.stream())
				.flatMap(Function.identity())
				.sorted(Comparator
						.comparingInt(Actor::getInitiative).reversed()
						.thenComparingInt(a -> Calc.rng(20, seed - a.hashCode()))
				)
				.forEach(turns::add);

		loop:
		for (Actor act : turns) {
			if (game.isClosed()) break;

			try {
				if (!act.asSenshi(locale).isAvailable() || act.isSkipped()) {
					act.asSenshi(locale).reduceDebuffs(1);
					for (Skill s : act.getSkills()) {
						s.reduceCd();
					}

					if (hunters.stream().allMatch(Actor::isSkipped)) break;
					else if (keepers.stream().allMatch(Actor::isSkipped)) break;
					continue;
				}

				act.asSenshi(locale).reduceDebuffs(1);
				for (Skill s : act.getSkills()) {
					s.reduceCd();
				}

				act.modAp(act.getMaxAp());
				act.asSenshi(locale).setDefending(false);

				while (act.getAp() > 0) {
					Runnable action = reload(true).get();
					if (action != null) {
						action.run();
					}

					if (hunters.stream().allMatch(Actor::isSkipped)) break loop;
					else if (keepers.stream().allMatch(Actor::isSkipped)) break loop;
				}
			} catch (Exception e) {
				Constants.LOGGER.warn(e, e);
			} finally {
				act.getModifiers().expireMods();
				act.modHp(act.getRegDeg().next());
				act.asSenshi(locale).setAvailable(true);

				Iterator<EffectBase> it = effects.iterator();
				while (it.hasNext()) {
					EffectBase e = it.next();
					if (!e.getTarget().equals(act)) continue;

					if (e.decDuration()) it.remove();
					if (e instanceof PersistentEffect pe) {
						pe.getEffect().accept(e, act);
					}
				}
			}
		}
	}

	public CompletableFuture<Runnable> reload(boolean execute) {
		game.resetTimer();

		lock = new CompletableFuture<>();
		ClusterAction ca = game.getChannel().sendEmbed(getEmbed());

		Actor curr = turns.get();
		ButtonizeHelper helper;
		if (execute) {
			if (curr instanceof Hero h) {
				helper = new ButtonizeHelper(true)
						.setCanInteract(u -> u.getId().equals(h.getAccount().getUid()))
						.setCancellable(false);

				helper.addAction(Utils.parseEmoji("🗡"), w -> {
					List<Actor> tgts = getActors(h.getTeam().getOther()).stream()
							.map(a -> a.isSkipped() ? null : a)
							.toList();

					addSelector(w.getMessage(), helper, tgts,
							t -> lock.complete(() -> {
								attack(h, t);
								h.modAp(-1);
							})
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

						boolean validWpn = skill.getReqWeapons().isEmpty()
										   || h.getEquipment().getWeaponList()
												   .stream()
												   .anyMatch(g -> skill.getReqWeapons().contains(g.getBasetype().getStats().wpnType()));

						if (!validWpn) {
							game.getChannel().sendMessage(locale.get("error/skill_cooldown")).queue();
							return;
						}

						addSelector(w.getMessage(), helper, skill.getTargets(this, h),
								t -> lock.complete(() -> {
									skill.execute(locale, this, h, t);
									h.modAp(-skill.getApCost());

									if (skill.getCooldown() > 0) {
										skill.setCd(skill.getCooldown());
									}

									history.add(locale.get(t.equals(h) ? "str/used_skill_self" : "str/used_skill",
											h.getName(), skill.getInfo(locale).getName(), t.getName(locale))
									);
								})
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

						Consumable con = h.getConsumable(String.valueOf(selected.getFirst()));
						if (con == null) {
							game.getChannel().sendMessage(locale.get("error/invalid_consumable")).queue();
							return;
						}

						addSelector(w.getMessage(), helper, con.getTargets(this, h),
								t -> lock.complete(() -> {
									con.execute(locale, this, h, t);
									h.getConsumables().remove(con, 1);
									h.modAp(-1);

									history.add(locale.get(t.equals(h) ? "str/used_skill_self" : "str/used_skill",
											h.getName(), con.getInfo(locale).getName(), t.getName(locale))
									);
								})
						);
					});
				}

				helper.addAction(Utils.parseEmoji("🛡"), w -> lock.complete(() -> {
							h.asSenshi(locale).setDefending(true);
							h.modAp(-h.getAp());

							history.add(locale.get("str/actor_defend", h.getName()));
						}))
						.addAction(Utils.parseEmoji("💨"), w -> {
							ButtonizeHelper confirm = new ButtonizeHelper(true)
									.setCanInteract(u -> u.getId().equals(h.getAccount().getUid()))
									.setCancellable(false)
									.addAction(Utils.parseEmoji("💨"), s -> lock.complete(() -> {
										int chance = Math.min(100 - 20 * game.getTurn() + 5 * h.getAttributes().dex(), 100 - 2 * game.getTurn());

										if (Calc.chance(chance)) {
											h.setFleed(true);
											history.add(locale.get("str/actor_flee", h.getName()));
										} else {
											history.add(locale.get("str/actor_flee_fail", h.getName(), chance));
										}

										h.modAp(-h.getAp());
									}))
									.addAction(Utils.parseEmoji("↩"), v -> {
										MessageEditAction ma = helper.apply(v.getMessage().editMessageComponents());
										addSelectors(h, ma);
										ma.queue(s -> Pages.buttonize(s, helper));
									});

							confirm.apply(w.getMessage().editMessageComponents()).queue(s -> Pages.buttonize(s, confirm));
						});

				ca.apply(a -> {
					MessageCreateAction ma = helper.apply(a);
					addSelectors(h, ma);

					return ma;
				});
			} else {
				helper = null;

				cpu.schedule(() -> {
					boolean canAttack = curr.asSenshi(locale).getDmg() > 0;
					boolean canDefend = curr.asSenshi(locale).getDfs() > 0;

					try {
						List<Skill> skills = curr.getSkills().stream()
								.filter(s -> s.getApCost() <= curr.getAp() && s.getCd() == 0)
								.toList();

						boolean used = false;
						if (!skills.isEmpty() && (Calc.chance(33) || !(canAttack || (canDefend && curr.getAp() == 1)))) {
							Skill skill = Utils.getRandomEntry(skills);
							List<Actor> tgts = skill.getTargets(this, curr).stream()
									.filter(a -> a != null && !a.isSkipped())
									.toList();

							if (!tgts.isEmpty()) {
								Actor t = Utils.getWeightedEntry(rngList, Actor::getAggroScore, tgts);
								skill(skill, curr, t);

								used = true;
							}
						}

						if (!used) {
							List<Actor> tgts = getActors(curr.getTeam().getOther()).stream()
									.filter(a -> !a.isSkipped())
									.toList();

							if (canDefend) {
								double threat = tgts.stream()
										.mapToInt(Actor::getAggroScore)
										.average()
										.orElse(1);

								double risk = threat / curr.getAggroScore();
								double lifeFac = Math.max(curr.getHp() * 2d / curr.getMaxHp(), 1);

								if (!canAttack || (curr.getAp() == 1 && Calc.chance(20 * lifeFac * risk))) {
									curr.asSenshi(locale).setDefending(true);
									curr.modAp(curr.getAp());

									history.add(locale.get("str/actor_defend", curr.getName(locale)));
									return;
								}
							}

							if (canAttack) {
								attack(curr, Utils.getWeightedEntry(rngList, Actor::getAggroScore, tgts));
								curr.modAp(-1);
							}
						}
					} catch (Exception e) {
						Constants.LOGGER.error(e, e);
					} finally {
						lock.complete(null);
					}
				}, Calc.rng(3000, 5000), TimeUnit.MILLISECONDS);
			}
		} else {
			helper = null;
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
						}
					}

					game.setMessage(new Pair<>(m.getChannel().getId(), m.getId()));
				});

		return lock;
	}

	private void skill(Skill skill, Actor source, Actor target) {
		trigger(Trigger.ON_SPELL, source);
		trigger(Trigger.ON_SPELL_TARGET, target);

		skill.execute(locale, this, source, target);
		source.modAp(-skill.getApCost());

		if (skill.getCooldown() > 0) {
			skill.setCd(skill.getCooldown());
		}

		if (target.getHp() == 0) {
			trigger(Trigger.ON_KILL, source);
		}

		history.add(locale.get(target.equals(source) ? "str/used_skill_self" : "str/used_skill",
				source.getName(locale), skill.getInfo(locale).getName(), target.getName(locale))
		);
	}

	private void addSelectors(Hero h, MessageRequest<?> ma) {
		List<LayoutComponent> comps = new ArrayList<>(ma.getComponents());

		List<Skill> skills = h.getSkills();
		if (!skills.isEmpty()) {
			StringSelectMenu.Builder b = StringSelectMenu.create("skills")
					.setPlaceholder(locale.get("str/use_a_skill"))
					.setMaxValues(1);

			for (Skill s : skills) {
				String cdText = "";
				int cd = s.getCd();
				if (cd > 0) {
					cdText = " (CD: " + locale.get("str/turns_inline", cd) + ")";
				}

				b.addOption(
						s.getInfo(locale).getName() + " " + StringUtils.repeat('◈', s.getApCost()) + cdText,
						s.getId(),
						s.getDescription(locale)
				);
			}

			comps.add(ActionRow.of(b.build()));
		}

		Bag<Consumable> cons = h.getConsumables();
		if (!cons.isEmpty()) {
			StringSelectMenu.Builder b = StringSelectMenu.create("consumables")
					.setPlaceholder(locale.get("str/use_a_consumable"))
					.setMaxValues(1);

			for (Consumable c : cons.uniqueSet()) {
				b.addOption(
						c.getInfo(locale).getName() + " (x" + cons.getCount(c) + ")",
						c.getId(),
						c.getInfo(locale).getDescription()
				);
			}

			comps.add(ActionRow.of(b.build()));
		}

		ma.setComponents(comps);
	}

	private void attack(Actor source, Actor target) {
		Senshi srcSen = source.asSenshi(locale);
		Senshi tgtSen = target.asSenshi(locale);

		trigger(Trigger.ON_DEFEND, target);

		if (srcSen.isBlinded(true) && Calc.chance(50)) {
			trigger(Trigger.ON_MISS, source);

			history.add(locale.get("str/actor_miss", source.getName(locale)));
			return;
		} else {
			if (Calc.chance(tgtSen.getDodge())) {
				trigger(Trigger.ON_MISS, source);
				trigger(Trigger.ON_DODGE, target);

				history.add(locale.get("str/actor_dodge", target.getName(locale)));
				return;
			} else if (Calc.chance(tgtSen.getParry())) {
				trigger(Trigger.ON_PARRY, target);

				history.add(locale.get("str/actor_parry", target.getName(locale)));
				attack(target, source);
				return;
			}
		}

		trigger(Trigger.ON_ATTACK, source);

		boolean crit = Calc.chance(source.getCritical());
		int raw = srcSen.getDmg() * (crit ? 2 : 1);

		target.modHp(-raw);
		trigger(Trigger.ON_HIT, source);

		if (target.getHp() == 0) {
			trigger(Trigger.ON_KILL, source);
		}

		history.add(locale.get("str/actor_combat",
				source.getName(locale),
				target.getName(locale),
				-target.getHpDelta(),
				crit ? ("**(" + locale.get("str/critical") + ")**") : ""
		));
	}

	public void addSelector(Message msg, ButtonizeHelper root, List<Actor> targets, Consumer<Actor> action) {
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

		Hero h = (Hero) turns.get();
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

		helper.addAction(Utils.parseEmoji("↩"), w -> {
			MessageEditAction ma = root.apply(msg.editMessageComponents());

			addSelectors(h, ma);

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

	public List<EffectBase> getEffects() {
		return effects;
	}

	public List<Actor> getActors() {
		return Stream.of(hunters, keepers)
				.flatMap(List::stream)
				.toList();
	}

	public List<Actor> getActors(Team team) {
		return switch (team) {
			case HUNTERS -> hunters;
			case KEEPERS -> keepers;
		};
	}

	public I18N getLocale() {
		return locale;
	}

	public Dunhun getGame() {
		return game;
	}

	public void trigger(Trigger t, Actor act) {
		Iterator<EffectBase> it = effects.iterator();
		while (it.hasNext()) {
			EffectBase e = it.next();
			if (!(e instanceof TriggeredEffect te && te.getTrigger() == t)) continue;
			else if (!e.getTarget().equals(act)) continue;

			if (te.decLimit()) it.remove();
			te.getEffect().accept(e, act);
		}
	}
}