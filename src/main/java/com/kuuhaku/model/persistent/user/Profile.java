/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model.persistent.user;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.AutoMake;
import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.RuleAction;
import com.kuuhaku.model.persistent.guild.AutoRule;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.records.id.ProfileId;
import com.kuuhaku.schedule.MinuteSchedule;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jdesktop.swingx.graphics.BlendComposite;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "profile", indexes = @Index(columnList = "xp DESC"))
public class Profile extends DAO<Profile> implements AutoMake<Profile>, Blacklistable {
	public static final String DEFAULT_BG = "https://i.ibb.co/F5rkrmR/cap-No-Game-No-Life-S01-E01-Beginner-00-11-41-04.jpg";
	private static final Dimension SIZE = new Dimension(950, 600);

	@EmbeddedId
	private ProfileId id;

	@Column(name = "xp", nullable = false)
	private long xp;

	@Column(name = "last_xp", nullable = false)
	private long lastXp;

	@OneToMany(mappedBy = "profile", cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	private List<Warn> warns = new ArrayList<>();

	@ManyToOne(optional = false)
	@JoinColumn(name = "uid", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("uid")
	private Account account;

	@ManyToOne(optional = false)
	@JoinColumn(name = "gid", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("gid")
	private GuildConfig guild;

	@OneToOne(cascade = ALL, orphanRemoval = true)
	@PrimaryKeyJoinColumns({
			@PrimaryKeyJoinColumn(name = "uid"),
			@PrimaryKeyJoinColumn(name = "gid")
	})
	@Fetch(FetchMode.JOIN)
	private ProfileSettings settings;

	@Override
	public Profile make(JSONObject args) {
		this.id = new ProfileId(args.getString("id.uid"), args.getString("id.gid"));
		this.account = DAO.find(Account.class, id.uid());
		this.guild = DAO.find(GuildConfig.class, id.gid());
		return this;
	}

	public ProfileId getId() {
		return id;
	}

	public long getXp() {
		return xp + getQueuedXp();
	}

	public int getQueuedXp() {
		Pair<Integer, Long> val = MinuteSchedule.XP_TO_ADD.get(id.uid() + "-" + id.gid());

		if (val == null) return 0;
		else return val.getFirst();
	}

	public void addXp(int value) {
		MinuteSchedule.XP_TO_ADD.compute(id.uid() + "-" + id.gid(), (k, v) -> {
			int total = value;
			if (v != null) {
				if (System.currentTimeMillis() - v.getSecond() < 1000) {
					return v;
				}

				total += v.getFirst();
			}

			return new Pair<>(total, System.currentTimeMillis());
		});
	}

	public void applyXp() {
		Pair<Integer, Long> val = MinuteSchedule.XP_TO_ADD.remove(id.uid() + "-" + id.gid());
		if (val != null) {
			xp += val.getFirst();
			save();
		}
	}

	public int getLevel() {
		return (int) Math.max(1, Math.floor(Math.sqrt(getXp() / 100d)) + 1);
	}

	public long getXpToLevel(int level) {
		return (int) (Math.pow(level - 1, 2) * 100);
	}

	public long getXpToLevel(int from, int to) {
		return Math.max(0, getXpToLevel(to) - getXpToLevel(from));
	}

	public List<Warn> getWarns() {
		return warns;
	}

	public int getWarnCount() {
		return (int) warns.parallelStream()
				.filter(w -> w.getPardoner() == null)
				.count();
	}

	public void warn(User issuer, String reason) {
		apply(Profile.class, id, p -> p.warns.add(new Warn(p, issuer, reason)));

		AutoRule rule = null;
		int mult = 0;
		int total = getWarnCount();
		for (AutoRule r : guild.getSettings().getAutoRules()) {
			if (r.getThreshold() <= total) {
				if (r.getAction() != RuleAction.AGGRAVATE) {
					rule = r;
					mult = 0;
				} else {
					mult = Math.min(mult + 1, 4);
				}
			}
		}

		I18N locale = guild.getLocale();
		Member m = getMember();
		User u = m.getUser();

		u.openPrivateChannel()
				.flatMap(c -> c.sendMessage(locale.get("alert/warn", reason)))
				.queue(null, Utils::doNothing);

		if (rule != null) {
			String cause = locale.get("str/autorule_desc",
					locale.get("str/autorule_" + rule.getAction()),
					rule.getThreshold()
			);


			int finalMult = mult;
			u.openPrivateChannel()
					.flatMap(c -> c.sendMessage(locale.get("alert/autorule_trigger", cause, finalMult + 1)))
					.queue(null, Utils::doNothing);

			switch (rule.getAction()) {
				case MUTE -> m.timeoutFor((long) (1 * Math.pow(2, mult)), TimeUnit.MINUTES)
						.reason(cause + " (A" + (mult + 1) + ")")
						.queue(null, Utils::doNothing);
				case LOSE_XP -> {
					long range = getXpToLevel(getLevel(), getLevel() + 1);
					xp = (long) (xp - (range * (0.05 * Math.pow(2, mult))));
				}
				case DELEVEL -> {
					long range = getXpToLevel((int) (getLevel() - Math.pow(2, mult)), getLevel());
					xp = Math.max(0, xp - range);
				}
				case KICK -> m.kick()
						.reason(cause + " (x" + (mult + 1) + ")")
						.queue(null, Utils::doNothing);
				case BAN -> m.ban(7, TimeUnit.DAYS)
						.reason(cause + " (x" + (mult + 1) + ")")
						.queue(null, Utils::doNothing);
			}

			save();
		}
	}

	public Account getAccount() {
		return account;
	}

	public GuildConfig getGuild() {
		return guild;
	}

	public ProfileSettings getSettings() {
		if (settings == null) {
			this.settings = new ProfileSettings(id);
		}

		return settings;
	}

	public Member getMember() {
		Guild guild = Main.getApp().getShiro().getGuildById(getId().gid());
		assert guild != null;

		return Pages.subGet(guild.retrieveMemberById(getId().uid()));
	}

	@Override
	public boolean isBlacklisted() {
		return account.isBlacklisted();
	}

	public int getRanking() {
		return DAO.queryNative(Integer.class, """
				SELECT x.rank
				FROM (
				     SELECT p.uid
				          , rank() OVER (ORDER BY p.xp DESC)
				     FROM profile p
				     WHERE p.gid = ?2
				     ) x
				WHERE x.uid = ?1
				""", id.uid(), id.gid());
	}

	public RichCustomEmoji getLevelEmote() {
		return Main.getApp().getShiro()
				.getEmojisByName("lvl_" + (getLevel() - getLevel() % 5), false)
				.parallelStream()
				.findAny().orElseThrow();
	}

	public BufferedImage render(I18N locale) {
		BufferedImage mask = IO.getResourceAsImage("assets/masks/profile_mask.png");
		BufferedImage overlay = IO.getResourceAsImage("assets/profile_overlay.png");

		AccountSettings settings = account.getSettings();
		BufferedImage bg = IO.getImage(settings.getBackground());
		if (bg == null) {
			bg = IO.getImage(DEFAULT_BG);
		}

		bg = Graph.scaleAndCenterImage(Graph.toColorSpace(bg, BufferedImage.TYPE_INT_ARGB), SIZE.width, SIZE.height);
		BufferedImage bi = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		BufferedImage avatar = null;
		Guild g = Main.getApp().getShiro().getGuildById(id.gid());
		if (g != null) {
			Member m = g.getMemberById(id.uid());
			if (m != null) {
				avatar = IO.getImage(m.getEffectiveAvatar().getUrl(256));
			}
		}

		if (avatar == null) {
			avatar = IO.getImage(account.getUser().getEffectiveAvatar().getUrl(256));
		}

		g2d.drawImage(avatar, -23, 56, 150, 150, null);

		Graph.applyMask(bg, mask, 0);
		g2d.drawImage(bg, 0, 0, null);

		Color color;
		if (settings.getColor().equals(Color.BLACK)) {
			color = Graph.rotate(Graph.getColor(bg), 180);
		} else {
			color = settings.getColor();
		}

		Graphics2D og2d = overlay.createGraphics();
		og2d.setComposite(BlendComposite.Multiply);
		og2d.setColor(color);
		og2d.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
		og2d.dispose();

		Polygon inner = Graph.makePoly(
				6, 75,
				475, 75,
				525, 31,
				897, 31,
				944, 78,
				944, 547,
				897, 594,
				53, 594,
				6, 547
		);

		BufferedImage effect = settings.getEffect().getImage();
		Graph.applyMask(effect, mask, 0, true);
		g2d.drawImage(effect, 0, 0, null);

		Map<String, Object> replaces = new HashMap<>();
		replaces.put("guild", getGuild().getName());
		replaces.put("g_rank", locale.separate(account.getRanking()));
		replaces.put("l_rank", locale.separate(getRanking()));
		replaces.put("xp", Utils.shorten(getXp()));
		replaces.put("level", getLevel());

		for (AccountTitle title : account.getTitles()) {
			Title t = title.getTitle();
			if (t.getId().startsWith("SS_")) continue;

			replaces.put(
					t.getId().toLowerCase(),
					"#%06X,%s".formatted(
							t.getRarity().getColor(false).getRGB() & 0xFFFFFF,
							t.getInfo(locale).getName().replace(" ", "_")
					)
			);
		}

		Graph.applyTransformed(g2d, g1 -> {
			Color bgCol = new Color((200 << 24) | (color.getRGB() & 0xFFFFFF), true);

			g1.setClip(inner);
			g1.setColor(bgCol);

			RoundRectangle2D wids = new RoundRectangle2D.Double(-14, 210, 200, 50, 20, 20);

			int em = g1.getFontMetrics().getHeight();
			g1.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(20));
			for (Object o : settings.getWidgets()) {
				String s = Utils.replaceTags(String.valueOf(o), '%', replaces);
				Rectangle bounds;
				if (s.startsWith("#")) {
					bounds = Graph.getStringBounds(g1, s.substring(s.indexOf(",") + 1).replace("_", " ")).getBounds();
				} else {
					bounds = Graph.getStringBounds(g1, s).getBounds();
				}
				int y = (int) wids.getY();

				g1.setColor(bgCol);
				wids.setFrame(wids.getX(), y, 43 + bounds.getWidth(), em * 2);
				Graph.drawOutlined(g1, wids, 1, Color.BLACK);
				wids.setFrame(wids.getX(), y + wids.getHeight() + 10, 0, 0);

				g1.setColor(Color.WHITE);
				Graph.drawMultilineString(g1, s, 15, (int) (y + em * 1.5), SIZE.width, (str, px, py) -> {
					if (str.startsWith("#")) {
						String[] frags = str.split(",");

						g1.setColor(Color.decode(frags[0]));
						Graph.drawOutlinedString(g1, frags[1].replace("_", " "), px, py, 3, Color.BLACK);
					} else {
						g1.setColor(Color.WHITE);
						Graph.drawOutlinedString(g1, str, px, py, 3, Color.BLACK);
					}
				});
			}

			String bio = Utils.replaceTags(settings.getBio(), '%', replaces);
			if (!bio.isBlank()) {
				int x = (int) (SIZE.width - SIZE.width / 2d - 40);
				int lines = Graph.getLineCount(g1, bio, (int) (SIZE.width / 2d - 20));
				int h = em * 2 * lines - 3 * lines;
				int w = (int) (SIZE.width / 2d);

				Graph.applyTransformed(g1, x, SIZE.height - h - 20, g2 -> {
					g2.setColor(bgCol);
					Shape desc = new RoundRectangle2D.Double(0, 0, w, h, 20, 20);
					Graph.drawOutlined(g2, desc, 1, Color.BLACK);

					g2.setColor(Color.WHITE);
					Graph.drawMultilineString(g2, bio, 10, (int) (em * 1.5), w - 20, 3, (str, px, py) -> {
						if (str.startsWith("#")) {
							String[] frags = str.split(",");

							g2.setColor(Color.decode(frags[0]));
							Graph.drawOutlinedString(g2, frags[1].replace("_", " "), px, py, 3, Color.BLACK);
						} else {
							g2.setColor(Color.WHITE);
							Graph.drawOutlinedString(g2, str, px, py, 3, Color.BLACK);
						}
					});
				});
			}
		});

		Graph.applyMask(overlay, mask, 1);
		g2d.drawImage(overlay, 0, 0, null);

		BufferedImage emote = IO.getImage(getLevelEmote().getImageUrl());
		g2d.drawImage(emote, 6, -3, 81, 81, null);

		g2d.setColor(Color.GRAY);
		Graph.drawOutlined(g2d, new Rectangle(88, 59, 384, 10), 2, Color.BLACK);

		int lvl = getLevel();
		long lvlXp = getXpToLevel(lvl);
		long toNext = getXpToLevel(lvl + 1);
		int pad = 4;
		double prcnt = Math.max(0, Calc.prcnt(getXp() - lvlXp, toNext - lvlXp));
		int[] colors = {0x5b2d11, 0xb5b5b5, 0xd49800, 0x00d4d4, 0x9716ff, 0x0ed700, 0xe40000};

		g2d.setColor(new Color(colors[Math.max(0, (lvl % 210) / 30)]));
		g2d.fillRect(88 + pad / 2, 59 + pad / 2, (int) ((384 - pad) * prcnt), 10 - pad);

		g2d.setFont(Fonts.DOREKING.derivePlain(55));
		Graph.drawOutlinedString(g2d, String.valueOf(lvl), 88, 51, 3, Color.BLACK);

		int offset = (int) (Graph.getStringBounds(g2d, String.valueOf(lvl)).getWidth() + 10);
		g2d.setColor(Color.WHITE);
		g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(25));
		Graph.drawOutlinedString(g2d, Utils.getOr(account.getName(), "???"), 88 + offset, 25, 3, Color.BLACK);

		String details = "XP: %s/%s I Rank: ".formatted(
				Utils.shorten(getXp() - lvlXp), Utils.shorten(toNext - lvlXp)
		);
		g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(20));
		Graph.drawOutlinedString(g2d, details, 88 + offset, 51, 3, Color.BLACK);

		offset += (int) Graph.getStringBounds(g2d, details).getWidth() + 5;
		g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(12));
		Graph.drawOutlinedString(g2d, "#", 88 + offset, 45, 3, Color.BLACK);

		offset += (int) Graph.getStringBounds(g2d, "#").getWidth();
		g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(20));
		Graph.drawOutlinedString(g2d, String.valueOf(account.getRanking()), 88 + offset, 51, 3, Color.BLACK);

		AccountTitle title = account.getTitle();
		if (title != null) {
			g2d.setColor(title.getTitle().getRarity().getColor(false));
			g2d.setFont(Fonts.DOREKING.deriveBold(35));

			String str = title.getTitle().getInfo(Utils.getOr(account.getSettings().getTitleLocale(), locale)).getName();
			Graph.drawOutlinedString(g2d, str, 524 + (374 - g2d.getFontMetrics().stringWidth(str)) / 2, 70, 5, Color.BLACK);
		}

		g2d.dispose();

		return bi;
	}

	@Override
	public void beforeSave() {
		Pair<Integer, Long> queued = MinuteSchedule.XP_TO_ADD.remove(id.uid() + "-" + id.gid());
		if (queued == null) return;

		xp += queued.getFirst();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Profile profile = (Profile) o;
		return Objects.equals(id, profile.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
