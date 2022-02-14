/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.tournament;

import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.records.TournamentMatch;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.graphics.BlendComposite;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "tournament")
public class Tournament {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description = "";

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int seed = new Random().nextInt();

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(nullable = false, name = "tournament_id")
	private Set<Participant> participants = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false, name = "tournament_id")
	private Set<String> bench = new LinkedHashSet<>();

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "tournament")
	private Bracket bracket;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean closed = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean finished = false;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int size = 0;

	private final transient Map<Integer, String> idLookup = new HashMap<>();
	private final transient Map<String, Participant> partLookup = new HashMap<>();

	private static final int H_MARGIN = 150;
	private static final int V_MARGIN = 25;
	private static final int WIDTH = 400;
	private static final int HEIGHT = 100;

	private static final Color BG_COLOR = new Color(0x2f3136);
	private static final Color PRIMARY_COLOR = new Color(0x8e9297);
	private static final Color SECONDARY_COLOR = new Color(0x40444b);

	public Tournament() {
	}

	public Tournament(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Participant> getParticipants() {
		return List.copyOf(participants);
	}

	public int getSize() {
		return size == 0 ? Math.max(8, Helper.roundToBit(participants.size())) : size;
	}

	public Map<String, Participant> getPartLookup() {
		if (partLookup.isEmpty() || idLookup.size() != partLookup.size()) {
			idLookup.clear();
			partLookup.clear();

			for (Participant p : participants) {
				idLookup.put(p.getId(), p.getUid());
				partLookup.put(p.getUid(), p);
			}
		}

		return partLookup;
	}

	public Map<Integer, String> getIdLookup() {
		if (idLookup.isEmpty() || idLookup.size() != partLookup.size()) {
			idLookup.clear();
			partLookup.clear();

			for (Participant p : participants) {
				idLookup.put(p.getId(), p.getUid());
				partLookup.put(p.getUid(), p);
			}
		}

		return idLookup;
	}

	public Participant getLookup(String id) {
		if (id == null) return null;
		else if (id.equals("BYE")) return Participant.BYE;

		return getPartLookup().get(id);
	}

	public Participant getLookup(Integer id) {
		if (id == null) return null;
		else if (id == -1) return Participant.BYE;

		return getLookup(getIdLookup().get(id));
	}

	public Set<String> getBench() {
		return bench;
	}

	public TreeSet<Participant> getRanking() {
		TreeSet<Participant> rank = new TreeSet<>(
				Comparator.comparingInt(Participant::getPoints).reversed()
						.thenComparingInt(Participant::getIndex)
		);
		rank.addAll(participants);

		return rank;
	}

	public Bracket getBracket() {
		return bracket;
	}

	public Phase getPhase(int phase) {
		return bracket.getPhases().get(phase);
	}

	public TournamentMatch generateMatch(int phase, String uid) {
		if (phase == getBracket().getPhases().size()) {
			List<Participant> tp = getTPMatch();
			return new TournamentMatch(id, phase, 0, tp.get(0).getUid(), 1, tp.get(1).getUid());
		}

		Phase p = getPhase(phase);

		List<Participant> parts = p.getParticipants(this);
		for (int i = 0; i < parts.size(); i++) {
			Participant part = parts.get(i);

			if (part.getUid().equals(uid)) {
				if (!p.isLast()) {
					Phase next = getPhase(phase + 1);
					if (next.getParticipants(this).get(i / 2) != null) return null;
				}

				int topIndex = i % 2 == 0 ? i : i - 1;
				int botIndex = i % 2 == 0 ? i + 1 : i;

				Participant top = parts.get(topIndex);
				Participant bot = parts.get(botIndex);

				TournamentMatch tm = new TournamentMatch(id, phase, topIndex, top.getUid(), botIndex, bot.getUid());
				if (Helper.notNull(top, bot)) {
					return tm;
				} else {
					return null;
				}
			}
		}

		return null;
	}

	public boolean isClosed() {
		return closed;
	}

	public void register(String id) {
		participants.add(new Participant(id));
	}

	public void leave(String id) {
		Participant p = null;
		for (Participant part : participants) {
			if (part.getUid().equals(id)) {
				p = part;
				break;
			}
		}
		if (p == null) return;

		participants.remove(p);
	}

	public void close() {
		closed = true;
		size = Math.max(8, Helper.roundToBit(participants.size()));
		bracket = new Bracket(size, this);
		bracket.populate(this, List.copyOf(participants));
		bench.addAll(
				Helper.removeIf(participants, p -> p.getIndex() == -1).stream()
						.map(Participant::getUid)
						.toList()
		);
	}

	public void setResult(int phase, int index) {
		Phase p = getPhase(phase);
		if (!p.isLast()) {
			Phase next = getPhase(phase + 1);
			if (next.getParticipants(this).get(index / 2) != null) return;
		}

		Participant winner = p.getParticipants(this).get(index);
		winner.addPoints((size / 2) >> phase);
		winner.setPhase(phase + 1);

		Phase next = getPhase(phase + 1);
		next.setMatch(index / 2, winner);
	}

	public void setTPResult(int index) {
		List<Participant> tp = getTPMatch();
		if (tp.size() != 2) return;

		Participant winner = tp.get(index);
		winner.addPoints(1);
		winner.setPhase(winner.getPhase() + 1);

		finished = true;
	}

	public Participant getFirstPlace() {
		return participants.stream()
				.filter(p -> p.getPoints() == size - 1)
				.findFirst()
				.orElse(null);
	}

	public Participant getSecondPlace() {
		return participants.stream()
				.filter(p -> p.getPoints() == size - 2)
				.findFirst()
				.orElse(null);
	}

	public Participant getThirdPlace() {
		return participants.stream()
				.filter(p -> p.getPoints() == size - 3)
				.findFirst()
				.orElse(null);
	}

	public List<Participant> getTPMatch() {
		return participants.stream()
				.filter(p -> p.getPoints() == size - 4)
				.sorted(Comparator.comparingInt(Participant::getIndex))
				.collect(Collectors.toList());
	}

	public BufferedImage view() {
		int phases = bracket.getPhases().size();

		BufferedImage bi = new BufferedImage((WIDTH + H_MARGIN) * phases + WIDTH - H_MARGIN + 10, Math.max((HEIGHT * 2 + V_MARGIN) * 5 - V_MARGIN + 10, (HEIGHT + V_MARGIN) * size - V_MARGIN + 10), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setColor(BG_COLOR);
		g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(new BasicStroke(8, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

		for (int i = 0; i < phases; i++) {
			int x = (WIDTH + H_MARGIN) * i + 5;

			Phase p = getPhase(i);
			if (p.isLast()) {
				int y = bi.getHeight() / 2 - HEIGHT;

				drawNameBox(g2d, x, y, Color.orange, true);
				drawNameBox(g2d, x, y + HEIGHT * 2 + V_MARGIN, Color.lightGray, true);
				drawNameBox(g2d, x, y + (HEIGHT * 2 + V_MARGIN) * 2, new Color(0x964B00), true);

				Participant first = getFirstPlace();
				if (first != null) {
					drawName(g2d, first, x, y, true, true);

					Participant second = getSecondPlace();
					if (second != null) {
						drawName(g2d, second, x, y + HEIGHT * 2 + V_MARGIN, true, true);

						Participant third = getThirdPlace();
						if (third != null) {
							drawName(g2d, third, x, y + (HEIGHT * 2 + V_MARGIN) * 2, true, true);
						}
					}
				}
			} else {
				List<Participant> ps = p.getParticipants(this);

				int pSize = ps.size();
				for (int k = 0; k < pSize; k++) {
					Participant part = ps.get(k);

					int y = (bi.getHeight() - 10) / pSize * k + 5;
					int offset = (bi.getHeight() - 10) / pSize / 2 - HEIGHT / 2;
					boolean winner = part != null && !part.isBye() && part.equals(getPhase(i + 1).getParticipants(this).get(k / 2));

					int y2 = (bi.getHeight() - 10) / (pSize / 2) * (k / 2);
					int offset2 = (bi.getHeight() - 10) / (pSize / 2) / 2 - HEIGHT / 2;

					g2d.setColor(winner ? Color.white : SECONDARY_COLOR);
					Composite comp = g2d.getComposite();
					g2d.setComposite(BlendComposite.Lighten);
					Helper.drawSquareLine(g2d, x + WIDTH, y + offset + HEIGHT / 2, (WIDTH + H_MARGIN) * (i + 1), y2 + offset2 + HEIGHT / 2);
					g2d.setComposite(comp);

					drawNameBox(g2d, x, y + offset, winner ? Color.white : PRIMARY_COLOR, false);
					drawName(g2d, part, x, y + offset, winner, false);

					if (i == 1 && k == 0) {
						g2d.setColor(Color.white);
						g2d.setFont(Fonts.DOREKING.deriveFont(Font.BOLD, bi.getHeight() / 10f));
						g2d.drawString(name, bi.getWidth() - g2d.getFontMetrics().stringWidth(name) - 10, y + g2d.getFontMetrics().getHeight());
					}
				}
			}
		}

		g2d.dispose();
		return bi;
	}

	private void drawNameBox(Graphics2D g2d, int x, int y, Color border, boolean bigger) {
		int mult = bigger ? 2 : 1;

		g2d.setColor(SECONDARY_COLOR);
		g2d.fillRoundRect(x, y, WIDTH * mult, HEIGHT * mult, 50, 50);
		g2d.setColor(border);
		g2d.drawRoundRect(x, y, WIDTH * mult, HEIGHT * mult, 50, 50);
	}

	private void drawName(Graphics2D g2d, Participant part, int x, int y, boolean winner, boolean bigger) {
		int mult = bigger ? 2 : 1;

		if (part != null) {
			String name;
			if (part.isBye()) {
				g2d.setColor(PRIMARY_COLOR);
				g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.ITALIC, 50 * mult));
				name = "BYE";
			} else {
				g2d.setColor(winner ? Color.white : PRIMARY_COLOR);
				g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.BOLD, 50 * mult));
				name = StringUtils.abbreviate(part.toString(), 13);
			}

			Helper.drawCenteredString(g2d, name, x, y, WIDTH * mult, (HEIGHT - 20) * mult);
		}
	}

	public boolean isFinished() {
		return finished;
	}
}
