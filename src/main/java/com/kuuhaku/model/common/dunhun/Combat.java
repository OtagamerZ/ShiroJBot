package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.helper.ButtonizeHelper;
import com.kuuhaku.Main;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.game.engine.Renderer;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.InfiniteList;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.Monster;
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
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class Combat implements Renderer<BufferedImage> {
	private final ExecutorService exec = Executors.newSingleThreadExecutor();
	private final ScheduledExecutorService cpu = Executors.newSingleThreadScheduledExecutor();
	private final long seed = ThreadLocalRandom.current().nextLong();

	private final Dunhun game;
	private final I18N locale;
	private final List<Actor> hunters;
	private final List<Actor> defenders;
	private final InfiniteList<Actor> turns = new InfiniteList<>();

	private CompletableFuture<Void> lock;
	private String lastAction = "";

	public Combat(Dunhun game) {
		this.game = game;
		this.locale = game.getLocale();

		hunters = List.copyOf(game.getHeroes().values());
		defenders = new ArrayList<>();

		for (int i = 0; i < 3; i++) {
			if (!Calc.chance(100 - 50d / hunters.size() * defenders.size())) break;

			defenders.add(Monster.getRandom());
		}

		for (List<Actor> acts : List.of(hunters, defenders)) {
			acts.forEach(a -> a.asSenshi(locale));
		}

		CompletableFuture.runAsync(this::process, exec);
	}

	@Override
	public BufferedImage render(I18N locale) {
		BufferedImage bi = new BufferedImage(255 * (hunters.size() + defenders.size()) + 64, 370, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();

		int offset = 0;
		boolean divided = false;
		for (List<Actor> acts : List.of(hunters, defenders)) {
			for (Actor a : acts) {
				BufferedImage card;
				if (a.getHp() <= 0) {
					a.asSenshi(locale).setAvailable(false);
					BufferedImage overlay = IO.getResourceAsImage("shoukan/states/dead.png");

					card = a.render(locale);
					Graph.overlay(card, overlay);
				} else {
					card = a.render(locale);
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
				.setDescription(lastAction);

		String title = locale.get("str/hunters");
		XStringBuilder sb = new XStringBuilder();
		for (List<Actor> acts : List.of(hunters, defenders)) {
			sb.clear();
			for (Actor a : acts) {
				if (!sb.isEmpty()) sb.nextLine();
				sb.appendNewLine(a.getName(locale));
				sb.appendNewLine("HP: " + a.getHp() + "/" + a.getMaxHp());

				int hp = a.getHp();
				int max = a.getMaxHp();
				while (max > 0) {
					int eMax = Math.min(max, 1000);
					sb.appendNewLine(Utils.makeProgressBar(hp, eMax, Math.round(eMax / 100f)));

					hp -= 1000;
					max -= 1000;
				}

				sb.appendNewLine(Utils.makeProgressBar(a.getAp(), a.getMaxAp(), a.getMaxAp(), '◇', '◈'));
			}

			eb.addField(title, sb.toString(), true);
			title = locale.get("str/defenders");
		}

		eb.setImage("attachment://cards.png");

		return eb.build();
	}

	private void process() {
		Stream.of(hunters.stream(), defenders.stream())
				.flatMap(Function.identity())
				.sorted(Comparator
						.comparingInt(Actor::getInitiative).reversed()
						.thenComparingInt(a -> Calc.rng(20, seed - a.hashCode()))
				)
				.forEach(turns::add);

		for (Actor act : turns) {
			if (game.isClosed()) break;
			else if (hunters.stream().noneMatch(h -> h.getHp() > 0)) break;
			else if (defenders.stream().noneMatch(h -> h.getHp() > 0)) break;
			else if (!act.asSenshi(locale).isAvailable()) continue;

			try {
				act.asSenshi(locale).setDefending(false);
				act.modAp(act.getMaxAp());

				while (act.getAp() > 0) {
					reload(true).get();
				}
			} catch (InterruptedException | ExecutionException ignore) {
			}
		}

		try {
			reload(false).get();
		} catch (InterruptedException | ExecutionException ignore) {
		}
	}

	public CompletableFuture<Void> reload(boolean execute) {
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

				helper.addAction(Utils.parseEmoji("🗡"),
								w -> addSelector(w.getMessage(), helper, defenders, t -> {
									attack(h, t);
									h.modAp(-1);

									lock.complete(null);
								})
						)
						.addAction(Utils.parseEmoji("🛡"), w -> {
							h.asSenshi(locale).setDefending(true);
							h.modAp(-1);

							lastAction = locale.get("str/actor_defend", h.getName());
							lock.complete(null);
						})
						.addAction(Utils.parseEmoji("💨"), w -> {
							System.out.println("c");
							lock.complete(null);
						});

				ca.apply(helper::apply);
			} else {
				helper = null;

				cpu.schedule(() -> {
					attack(curr, Utils.getRandomEntry(hunters));
					curr.modAp(-1);

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
		int raw = source.asSenshi(locale).getDmg();
		int def = target.asSenshi(locale).getDfs();

		int dmg;
		if (target.asSenshi(locale).isDefending()) {
			dmg = (int) Math.max(raw / 10f, (2.5 * Math.pow(raw, 2)) / (def + 2.5 * raw));
		} else {
			dmg = (int) Math.max(raw / 5f, (5 * Math.pow(raw, 2)) / (def + 5 * raw));
		}

		target.modHp(-dmg);
		lastAction = locale.get("str/actor_combat", source.getName(locale), target.getName(locale), dmg);
	}

	public void addSelector(Message msg, ButtonizeHelper root, List<Actor> targets, Consumer<Actor> action) {
		Actor single = null;
		for (Actor a : targets) {
			if (single == null && a.getHp() > 0) single = a;
			else if (a.getHp() > 0) {
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
					w -> action.accept(tgt)
			);
		}

		helper.addAction(
				Utils.parseEmoji("↩"),
				w -> root.apply(msg.editMessageComponents()).queue(s -> Pages.buttonize(s, root))
		);

		MessageEditAction act = msg.editMessageComponents();
		List<LayoutComponent> rows = helper.getComponents(act);

		int idx = 0;
		loop:
		for (LayoutComponent row : rows) {
			if (row instanceof ActionRow ar) {
				List<ItemComponent> items = ar.getComponents();
				for (int i = 0, sz = items.size(); i < sz; i++, idx++) {
					if (idx >= targets.size()) break loop;

					ItemComponent item = items.get(i);
					if (item instanceof Button b && targets.get(idx).getHp() <= 0) {
						items.set(i, b.asDisabled());
					}
				}
			}
		}

		act.setComponents(rows).queue(s -> Pages.buttonize(s, helper));
	}

	public CompletableFuture<Void> getLock() {
		return lock;
	}
 }
