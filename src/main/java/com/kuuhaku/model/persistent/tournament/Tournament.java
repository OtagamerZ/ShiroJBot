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
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
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

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int seed = new Random().nextInt();

	@LazyCollection(LazyCollectionOption.FALSE)
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "tournament")
	private List<Participant> participants = new ArrayList<>();

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	private Bracket bracket;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean closed = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean finished = false;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int size = 0;

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

	public List<Participant> getParticipants() {
		return List.copyOf(participants);
	}

	public Map<String, Participant> getPartLookup() {
		if (partLookup.isEmpty()) {
			for (Participant p : participants) {
				partLookup.put(p.getUid(), p);
			}
		}

		return partLookup;
	}

	public Participant getLookup(String id) {
		return getPartLookup().get(id);
	}

	public TreeSet<Participant> getRanking() {
		TreeSet<Participant> rank = new TreeSet<>(
				Comparator.comparingInt(Participant::getPoints).reversed()
						.thenComparingInt(Participant::getIndex)
						.thenComparing(Participant::isThird).reversed()
		);
		rank.addAll(participants);

		return rank;
	}

	public Bracket getBracket() {
		return bracket;
	}

	public boolean isClosed() {
		return closed;
	}

	public void register(String id) {
		participants.add(new Participant(id));
	}

	public void close() {
		closed = true;
		size = Helper.roundToBit(participants.size());
		bracket = new Bracket(size);
		bracket.populate(this, List.copyOf(participants));
		participants.removeIf(p -> p.getIndex() == -1);
	}

	public void setResult(int phase, int index) {
		Phase p = bracket.getPhases().get(phase);
		Participant winner = getLookup(p.getParticipants().get(index));
		winner.addPoints((size / 2) >> phase);

		Phase next = bracket.getPhases().get(phase + 1);
		next.setMatch(index / 2, winner);
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

	public BufferedImage view() {
		int phases = bracket.getPhases().size();

		BufferedImage bi = new BufferedImage((WIDTH + H_MARGIN) * phases + WIDTH - H_MARGIN + 10, Math.max((HEIGHT + V_MARGIN) * 6 - V_MARGIN + 2, (HEIGHT + V_MARGIN) * size - V_MARGIN + 10), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setBackground(BG_COLOR);
		g2d.clearRect(0, 0, bi.getWidth(), bi.getHeight());
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(new BasicStroke(8, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

		g2d.setColor(Color.white);
		g2d.setFont(Fonts.DOREKING.deriveFont(Font.BOLD, 200));
		Helper.drawCenteredString(g2d, name, (WIDTH + H_MARGIN) * 3 + 5, 5, bi.getWidth() - (WIDTH + H_MARGIN) * 3, 400);

		for (int i = 0; i < phases; i++) {
			int x = (WIDTH + H_MARGIN) * i + 5;

			Phase p = bracket.getPhases().get(i);
			if (p.isLast()) {
				int y = bi.getHeight() / 2 - HEIGHT;

				drawNameBox(g2d, x, y, Color.orange, true);
				drawNameBox(g2d, x, y + (HEIGHT + V_MARGIN) * 2, Color.lightGray, true);
				drawNameBox(g2d, x, y + (HEIGHT + V_MARGIN) * 4, new Color(0x964B00), true);

				Participant first = getFirstPlace();
				if (first != null) {
					drawName(g2d, first, x, y, true, true);

					Participant second = getSecondPlace();
					if (second != null) {
						drawName(g2d, second, x, y + (HEIGHT + V_MARGIN) * 2, true, true);

						Participant third = getThirdPlace();
						if (third != null) {
							drawName(g2d, third, x, y + (HEIGHT + V_MARGIN) * 4, true, true);
						}
					}
				}
			} else {
				List<Participant> ps = p.getParticipants().stream()
						.map(this::getLookup)
						.collect(Collectors.toList());

				int pSize = ps.size();
				for (int k = 0; k < pSize; k++) {
					Participant part = ps.get(k);

					int y = (bi.getHeight() - 10) / pSize * k + 5;
					int offset = (bi.getHeight() - 10) / pSize / 2 - HEIGHT / 2;
					boolean winner = part != null && !part.isBye() && part.getUid().equals(bracket.getPhases().get(i + 1).getParticipants().get(k / 2));

					int y2 = (bi.getHeight() - 10) / (pSize / 2) * (k / 2);
					int offset2 = (bi.getHeight() - 10) / (pSize / 2) / 2 - HEIGHT / 2;

					g2d.setColor(winner ? Color.white : SECONDARY_COLOR);
					Composite comp = g2d.getComposite();
					g2d.setComposite(BlendComposite.Lighten);
					Helper.drawSquareLine(g2d, x + WIDTH, y + offset + HEIGHT / 2, (WIDTH + H_MARGIN) * (i + 1), y2 + offset2 + HEIGHT / 2);
					g2d.setComposite(comp);

					drawNameBox(g2d, x, y + offset, winner ? Color.white : PRIMARY_COLOR, false);
					drawName(g2d, part, x, y + offset, winner, false);
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

	public void setFinished(boolean finished) {
		this.finished = finished;
	}
}
