package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.listener.EventHandler;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.game.engine.Renderer;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.FixedSizeDeque;
import com.kuuhaku.model.common.InfiniteList;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.persistent.dunhun.Affix;
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
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class Combat implements Renderer<BufferedImage> {
	private final ScheduledExecutorService cpu = Executors.newSingleThreadScheduledExecutor();
	private final long seed = ThreadLocalRandom.current().nextLong();

	private final Dunhun game;
	private final I18N locale;
	private final List<Actor> hunters;
	private final List<Actor> keepers;
	private final InfiniteList<Actor> turns = new InfiniteList<>();
	private final FixedSizeDeque<String> history = new FixedSizeDeque<>(5);

	private CompletableFuture<Runnable> lock;

	public Combat(Dunhun game) {
		this.game = game;
		this.locale = game.getLocale();

		hunters = List.copyOf(game.getHeroes().values());
		keepers = new ArrayList<>();

		for (int i = 0; i < 3; i++) {
			if (!Calc.chance(100 - 50d / hunters.size() * keepers.size())) break;

			keepers.add(Monster.getRandom());
		}

		Team team = Team.HUNTERS;
		for (List<Actor> acts : List.of(hunters, keepers)) {
			for (Actor a : acts) {
				a.setTeam(team);
				a.asSenshi(locale);
			}

			team = Team.KEEPERS;
		}

		ExecutorService exec = Executors.newSingleThreadExecutor();
		CompletableFuture.runAsync(this::process, exec);
	}

	@Override
	public BufferedImage render(I18N locale) {
		BufferedImage bi = new BufferedImage(255 * (hunters.size() + keepers.size()) + 64, 400, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();

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
					boolean legacy = a.asSenshi(locale).getHand().getUserDeck().getStyling().getFrame().isLegacy();
					String path = "shoukan/frames/state/" + (legacy ? "old" : "new");

					Graph.overlay(card, IO.getResourceAsImage(path + "/hero.png"));
				}

				g2d.drawImage(card, offset, 0, null);
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

					if (a.getHp() >= threshold) sb.append('▰');
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

	private void process() {
		Stream.of(hunters.stream(), keepers.stream())
				.flatMap(Function.identity())
				.sorted(Comparator
						.comparingInt(Actor::getInitiative).reversed()
						.thenComparingInt(a -> Calc.rng(20, seed - a.hashCode()))
				)
				.forEach(turns::add);

		loop: for (Actor act : turns) {
			if (game.isClosed()) break;
			else if (!act.asSenshi(locale).isAvailable()) continue;

			try {
				act.asSenshi(locale).setDefending(false);
				act.modHp(act.getRegDeg().next());
				if (act.isSkipped()) continue;

				act.modAp(act.getMaxAp());
				act.getModifiers().expireMods();

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
			}
		}

		try {
			reload(false).get();
		} catch (Exception e) {
			Constants.LOGGER.warn(e, e);
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
						})
						.addAction(Utils.parseEmoji("⚡"), w -> {
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
							} else if (h.getModifiers().isCoolingDown(skill)) {
								game.getChannel().sendMessage(locale.get("error/skill_cooldown")).queue();
								return;
							}

							addSelector(w.getMessage(), helper, skill.getTargets(this, h),
									t -> lock.complete(() -> {
										skill.execute(locale, this, h, t);
										h.modAp(-skill.getApCost());

										if (skill.getCooldown() > 0) {
											h.getModifiers().setCooldown(skill, skill.getCooldown());
										}

										history.add(locale.get(t.equals(h) ? "str/used_skill_self" : "str/used_skill",
												h.getName(), skill.getInfo(locale).getName(), t.getName(locale))
										);
									})
							);
						})
						.addAction(Utils.parseEmoji("🛡"), w -> lock.complete(() -> {
							h.asSenshi(locale).setDefending(true);
							h.modAp(-h.getAp());

							history.add(locale.get("str/actor_defend", h.getName()));
						}))
						.addAction(Utils.parseEmoji("💨"), w -> lock.complete(() -> {
							int chance = Math.min(100 - 20 * game.getTurn() + 5 * h.getAttributes().dex(), 100 - 2 * game.getTurn());

							if (Calc.chance(chance)) {
								h.setFleed(true);
								history.add(locale.get("str/actor_flee", h.getName()));
							} else {
								history.add(locale.get("str/actor_flee_fail", h.getName(), chance));
							}

							h.modAp(-h.getAp());
						}))
						.addAction(Utils.parseEmoji("\uD83D\uDCD1"), w -> {
							EmbedBuilder eb = new ColorlessEmbedBuilder();

							for (Actor a : getActors()) {
								if (!(a instanceof Monster m)) continue;

								XStringBuilder sb = new XStringBuilder("-# " + m.getInfo(locale).getName() + "\n");

								Set<Affix> affs = m.getAffixes();
								if (!affs.isEmpty()) {
									sb.appendNewLine("**" + locale.get("str/affixes") + "**");
									for (Affix aff : affs) {
										sb.appendNewLine("- " + aff.getInfo(locale).getDescription());
									}
								}

								sb.nextLine();

								List<Skill> skills = m.getSkills();
								if (!skills.isEmpty()) {
									sb.appendNewLine("**" + locale.get("str/skills") + "**");
									for (Skill skill : skills) {
										sb.appendNewLine("- " + skill.getInfo(locale).getName());
										sb.appendNewLine("-# " + skill.getInfo(locale).getDescription());
										sb.nextLine();
									}
								}

								eb.addField(a.getName(locale), sb.toString(), true);
							}

							Objects.requireNonNull(w.getHook())
									.setEphemeral(true)
									.sendMessageEmbeds(eb.build())
									.queue();
						});

				ca.apply(a -> {
					List<Skill> skills = h.getSkills();
					if (!skills.isEmpty()) {
						StringSelectMenu.Builder b = StringSelectMenu.create("skills")
								.setPlaceholder(locale.get("str/use_a_skill"))
								.setMaxValues(1);

						for (Skill s : skills) {
							String cdText = "";
							int cd = h.getModifiers().getCooldowns().getOrDefault(s.getId(), 0);
							if (cd > 0) {
								cdText = " (CD: " + locale.get("str/turns_inline", cd) + ")";
							}

							b.addOption(
									s.getInfo(locale).getName() + " " + StringUtils.repeat('◈', s.getApCost()) + cdText,
									s.getId(),
									s.getInfo(locale).getDescription()
							);
						}

						return helper.apply(a).addActionRow(b.build());
					}

					return helper.apply(a);
				});
			} else {
				helper = null;

				cpu.schedule(() -> {
					try {
						List<Skill> skills = curr.getSkills().stream()
								.filter(s -> s.getApCost() <= curr.getAp() && !curr.getModifiers().isCoolingDown(s))
								.toList();

						boolean used = false;
						if (!skills.isEmpty() && Calc.chance(33)) {
							Skill skill = Utils.getRandomEntry(skills);

							List<Actor> tgts = skill.getTargets(this, curr);
							if (!tgts.isEmpty()) {
								Actor t = Utils.getRandomEntry(tgts);
								skill.execute(locale, this, curr, t);
								curr.modAp(-skill.getApCost());

							if (skill.getCooldown() > 0) {
								curr.getModifiers().setCooldown(skill, skill.getCooldown());
							}

								history.add(locale.get(t.equals(curr) ? "str/used_skill_self" : "str/used_skill",
										curr.getName(locale), skill.getInfo(locale).getName(), t.getName(locale))
								);

								used = true;
							}
 						}

						if (!used) {
							if (curr.getAp() == 1 && Calc.chance(25)) {
								curr.asSenshi(locale).setDefending(true);
								curr.modAp(-1);

								history.add(locale.get("str/actor_defend", curr.getName(locale)));
							} else {
								attack(curr, Utils.getRandomEntry(getActors(curr.getTeam().getOther())));
								curr.modAp(-1);
							}
						}
					} catch (Exception e) {
						Constants.LOGGER.error(e, e);
					}

					lock.complete(null);
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

	private void attack(Actor source, Actor target) {
		Senshi srcSen = source.asSenshi(locale);
		Senshi tgtSen = target.asSenshi(locale);

		if (srcSen.isBlinded(true) && Calc.chance(50)) {
			history.add(locale.get("str/actor_miss", source.getName(locale)));
			return;
		} else {
			if (Calc.chance(tgtSen.getDodge())) {
				history.add(locale.get("str/actor_dodge", target.getName(locale)));
				return;
			} else if (Calc.chance(tgtSen.getParry())) {
				history.add(locale.get("str/actor_parry", target.getName(locale)));
				attack(target, source);
				return;
			}
		}

		int raw = srcSen.getDmg();
		int def = tgtSen.getDfs();

		int dmg;
		if (tgtSen.isDefending()) {
			dmg = (int) Math.max(raw / 10f, (2.5 * Math.pow(raw, 2)) / (def + 2.5 * raw));
		} else {
			dmg = (int) Math.max(raw / 5f, (5 * Math.pow(raw, 2)) / (def + 5 * raw));
		}

		target.modHp(-dmg);
		history.add(locale.get("str/actor_combat", source.getName(locale), target.getName(locale), dmg));
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

			List<Skill> skills = h.getSkills();
			if (!skills.isEmpty()) {
				StringSelectMenu.Builder b = StringSelectMenu.create("skills")
						.setPlaceholder(locale.get("str/use_a_skill"))
						.setMaxValues(1);

				for (Skill s : skills) {
					String cdText = "";
					int cd = h.getModifiers().getCooldowns().getOrDefault(s.getId(), 0);
					if (cd > 0) {
						cdText = " (CD: " + locale.get("str/turns_inline", cd) + ")";
					}

					b.addOption(
							s.getInfo(locale).getName() + " " + StringUtils.repeat('◈', s.getApCost()) + cdText,
							s.getId(),
							s.getInfo(locale).getDescription()
					);
				}

				List<LayoutComponent> comps = new ArrayList<>(ma.getComponents());
				comps.add(ActionRow.of(b.build()));

				ma.setComponents(comps);
			}

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
}
