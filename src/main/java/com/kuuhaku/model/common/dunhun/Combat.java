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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public class Combat implements Renderer<BufferedImage> {
	private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private final long seed = ThreadLocalRandom.current().nextLong();
	private final Dunhun game;
	private final I18N locale;

	private String lastAction = "";
	private List<Actor> hunters;
	private List<Actor> defenders;
	private final InfiniteList<Actor> turns = new InfiniteList<>();

	public Combat(Dunhun game) {
		this.game = game;
		this.locale = game.getLocale();

		hunters = List.copyOf(game.getHeroes().values());
		defenders = List.of(Monster.getRandom());

		process();
	}

	@Override
	public BufferedImage render(I18N locale) {
		BufferedImage bi = new BufferedImage(255 * (game.getHeroes().size() + 1) + 64, 370, BufferedImage.TYPE_INT_ARGB);
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
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		String title = locale.get("str/hunters");
		XStringBuilder sb = new XStringBuilder();
		for (List<Actor> acts : List.of(hunters, defenders)) {
			sb.clear();
			for (Actor a : acts) {
				if (!sb.isEmpty()) sb.nextLine();

				sb.appendNewLine(a.getName(locale) + "『" + a.getHp() + "/" + a.getMaxHp() + "』");
				sb.appendNewLine(Utils.makeProgressBar(a.getHp(), a.getMaxHp(), 10));
				sb.appendNewLine(Utils.makeProgressBar(a.getAp(), a.getMaxAp(), a.getMaxAp(), '◇', '◈'));
			}

			eb.addField(title, sb.toString(), true);
			title = locale.get("str/defenders");
		}

		eb.setImage("attachment://cards.png");

		return eb.build();
	}

	private void process() {
		/* TODO Uncomment
		Stream.of(heroes.stream(), enemies.stream())
				.flatMap(Function.identity())
				.sorted(Comparator
						.comparingInt(Actor::getInitiative).reversed()
						.thenComparingInt(a -> Calc.rng(20, seed - a.hashCode()))
				)
				.forEach(turns::add);
		 */

		turns.addAll(hunters);
		turns.addAll(defenders);

		for (Actor act : turns) {
			if (game.isClosed()) break;
			else if (hunters.stream().noneMatch(h -> h.getHp() > 0) || defenders.stream().noneMatch(h -> h.getHp() > 0)) break;
			else if (!act.asSenshi(locale).isAvailable()) continue;

			try {
				reload().get();
			} catch (InterruptedException | ExecutionException ignore) {
			}
		}
	}

	public CompletableFuture<Void> reload() {
		game.resetTimer();

		CompletableFuture<Void> lock = new CompletableFuture<>();
		ClusterAction ca = game.getChannel().sendEmbed(getEmbed())
				.addFile(IO.getBytes(render(game.getLocale()), "png"), "cards.png");

		Actor curr = turns.get();
		ButtonizeHelper helper;
		if (curr instanceof Hero h) {
			h.asSenshi(locale).setDefending(false);

			helper = new ButtonizeHelper(true)
					.setCanInteract(u -> u.getId().equals(h.getAccount().getUid()))
					.setCancellable(false)
					.addAction(Utils.parseEmoji("🗡"), w -> {
						attack(h, Utils.getRandomEntry(defenders));
						lock.complete(null);
					})
					.addAction(Utils.parseEmoji("🛡"), w -> {
						System.out.println("b");
						lock.complete(null);
					})
					.addAction(Utils.parseEmoji("💨"), w -> {
						System.out.println("c");
						lock.complete(null);
					});

			ca.apply(helper::apply);
		} else {
			helper = null;

			exec.schedule(() -> {
				attack(curr, Utils.getRandomEntry(hunters));
				lock.complete(null);
			}, Calc.rng(3000, 5000), TimeUnit.MILLISECONDS);
		}

		ca.queue(m -> {
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

	private int attack(Actor attacker, Actor defender) {
		int raw = attacker.asSenshi(locale).getDmg();
		int def = defender.asSenshi(locale).getDfs();

		int dmg;
		if (defender.asSenshi(locale).isDefending()) {
			dmg = (int) Math.max(raw / 10f, (2.5 * Math.pow(raw, 2)) / (def + 2.5 * raw));
		} else {
			dmg = (int) Math.max(raw / 5f, (5 * Math.pow(raw, 2)) / (def + 5 * raw));
		}

		defender.modHp(-dmg);
		lastAction = locale.get("str/actor_combat", attacker, defender, dmg);

		return dmg;
	}
}
