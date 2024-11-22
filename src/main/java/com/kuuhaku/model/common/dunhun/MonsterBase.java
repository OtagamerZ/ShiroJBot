package com.kuuhaku.model.common.dunhun;

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.Delta;
import com.kuuhaku.model.common.shoukan.RegDeg;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.FrameSkin;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.dunhun.Monster;
import com.kuuhaku.model.persistent.dunhun.MonsterStats;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.persistent.localized.LocalizedMonster;
import com.kuuhaku.model.persistent.shoukan.CardAttributes;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.dunhun.CombatContext;
import com.kuuhaku.model.records.id.LocalizedId;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static jakarta.persistence.CascadeType.ALL;

@MappedSuperclass
public abstract class MonsterBase<T extends MonsterBase<T>> extends DAO<T> implements Actor {
	protected static final double[] hpTable = new double[1000];
	protected static final double[] statTable = new double[1000];

	static {
		for (int i = 0; i < 1000; i++) {
			hpTable[i] = 1 + i / 10d;
			statTable[i] = 1 + 15 * i / (i + 100d);
		}
	}

	@Transient
	public static final Deck DECK = Utils.with(new Deck(), d -> {
		d.getStyling().setFrame(FrameSkin.GLITCH);
	});
	@Transient
	public final long SERIAL = ThreadLocalRandom.current().nextLong();

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Embedded
	protected MonsterStats stats;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	@Fetch(FetchMode.SUBSELECT)
	protected Set<LocalizedMonster> infos = new HashSet<>();

	private transient final ActorModifiers modifiers = new ActorModifiers();
	private transient final Set<SelfEffect> effects = new HashSet<>();
	private transient final RegDeg regDeg = new RegDeg(null);
	private transient final Delta<Integer> hp = new Delta<>();
	protected transient List<Skill> skillCache;
	private transient Senshi senshiCache;
	private transient Dunhun game;
	private transient Team team;
	private transient int ap;
	private transient boolean flee;

	public MonsterBase() {
	}

	public MonsterBase(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	public MonsterStats getStats() {
		return stats;
	}

	public LocalizedMonster getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny().orElseThrow();
	}

	@Override
	public abstract String getName(I18N locale);

	@Override
	public Race getRace() {
		return stats.getRace();
	}

	@Override
	public int getHp() {
		int max = getMaxHp();

		if (hp.get() == null || hp.get() > max) hp.set(max);
		return hp.get();
	}

	@Override
	public int getHpDelta() {
		if (hp.previous() == null) return 0;

		return hp.get() - hp.previous();
	}

	@Override
	public void setHp(int value, boolean bypass) {
		hp.set(Calc.clamp(value, 0, getMaxHp()), bypass);
	}

	@Override
	public int getAp() {
		return ap;
	}

	@Override
	public int getMaxAp() {
		int bonus = 0;
		if (game.getPartySize() > 1 && team == Team.KEEPERS) {
			bonus = game.getPartySize() / 2;
		}

		return (int) Calc.clamp(getStats().getMaxAp() + modifiers.getMaxAp().get() + game.getAreaLevel() / 5d + bonus, 1, getApCap() + bonus);
	}

	public int getApCap() {
		return (int) (5 + getStats().getMaxAp() + modifiers.getMaxAp().get());
	}

	@Override
	public void modAp(int value) {
		ap = Calc.clamp(ap + value, 0, getMaxAp());
	}

	@Override
	public int getInitiative() {
		return game.getAreaLevel() / 3 + stats.getInitiative() + (int) modifiers.getInitiative().get();
	}

	@Override
	public double getCritical() {
		return 5 * (1 + modifiers.getCritical().get());
	}

	@Override
	public int getAggroScore() {
		int flat = getSenshi().getDmg() / 10 + getSenshi().getDfs() / 20 + getHp() / 150;
		double mult = switch (getRarityClass()) {
			case NORMAL -> 1;
			case MAGIC -> 1.5;
			case RARE -> 2.25;
			case UNIQUE -> 10;
		};

		return (int) Math.max(1, flat * (1 + modifiers.getAggroMult().get()) * (game.getAreaLevel() + 1) * mult);
	}

	@Override
	public ActorModifiers getModifiers() {
		return modifiers;
	}

	@Override
	public RegDeg getRegDeg() {
		return regDeg;
	}

	@Override
	public boolean hasFleed() {
		return flee;
	}

	@Override
	public void setFleed(boolean flee) {
		this.flee = flee;

		if (game != null && flee) {
			game.getChannel().sendMessage(game.getLocale().get("str/actor_flee", getName(game.getLocale())));
		}
	}

	public Set<SelfEffect> getEffects() {
		return effects;
	}

	public void addEffect(ThrowingBiConsumer<EffectBase, CombatContext> effect, Trigger... triggers) {
		effects.add(new SelfEffect(this, effect, triggers));
	}

	@Override
	public void trigger(Trigger trigger, Actor target) {
		for (SelfEffect e : effects) {
			if (!Utils.equalsAny(trigger, e.getTriggers())) continue;

			try {
				e.lock();
				e.getEffect().accept(e, new CombatContext(trigger, this, target));
			} finally {
				e.unlock();
			}
		}
	}

	@Override
	public void shiftInto(Actor a) {
		if (a == null) {
			senshiCache = null;
			skillCache = null;
		} else {
			senshiCache = a.getSenshi().copy();
			skillCache = a.getSkills().stream()
					.map(Skill::clone)
					.collect(Collectors.toCollection(ArrayList::new));
		}
	}

	public int getKillXp() {
		if (stats.isMinion()) return 0;

		double mult = switch (getRarityClass()) {
			case NORMAL -> 1;
			case MAGIC -> 1.5;
			case RARE -> 2.25;
			case UNIQUE -> 10;
		};

		double xp = getMaxHp() / 500d + getSenshi().getDmg() / 175d + getSenshi().getDfs() / 250d;
		if (game != null) {
			xp *= 1 + game.getAreaLevel() * 0.1;
		}

		return (int) (xp * mult);
	}

	@Override
	public List<Skill> getSkills() {
		if (skillCache != null) return skillCache;

		return skillCache = DAO.queryAll(Skill.class, "SELECT s FROM Skill s WHERE s.id IN ?1 OR s.reqRace = ?2", stats.getSkills(), getRace());
	}

	@Override
	public Team getTeam() {
		return team;
	}

	@Override
	public void setTeam(Team team) {
		this.team = team;
	}

	@Override
	public Dunhun getGame() {
		return game;
	}

	@Override
	public void setGame(Dunhun game) {
		this.game = game;
	}

	protected void load(I18N locale) {

	}

	@Override
	public Senshi asSenshi(I18N locale) {
		if (senshiCache != null) return senshiCache;

		senshiCache = new Senshi(this, locale);

		CardAttributes base = senshiCache.getBase();
		load(locale);

		if (!id.equalsIgnoreCase("DUMMY")) {
			double mult = switch (getRarityClass()) {
				case RARE -> {
					senshiCache.setHueOffset(Calc.rng(90, 270, SERIAL));
					yield 2;
				}
				case MAGIC -> 1.25;
				default -> 1;
			} * statTable[game.getAreaLevel()];

			if (game.getPartySize() > 1 && team == Team.KEEPERS) {
				mult *= 1 + game.getPartySize() * 0.3;
			}

			base.setAtk((int) (stats.getAttack() * mult));
			base.setDfs((int) (stats.getDefense() * mult / 2));
			base.setDodge(stats.getDodge());
			base.setParry(stats.getParry());
		}

		base.getTags().add("MONSTER");

		return senshiCache;
	}

	@Override
	public Senshi getSenshi() {
		if (senshiCache != null) return senshiCache;
		return asSenshi(game.getLocale());
	}

	@Override
	public BufferedImage render(I18N locale) {
		return asSenshi(locale).render(locale, DECK);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MonsterBase<?> monster = (MonsterBase<?>) o;
		return SERIAL == monster.SERIAL && Objects.equals(id, monster.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(SERIAL, id);
	}

	public static Monster dummy(Actor of) {
		MonsterBase<?> dummy = new Monster("DUMMY");
		dummy.game = of.getGame();

		for (I18N loc : I18N.validValues()) {
			dummy.infos.add(new LocalizedMonster(new LocalizedId("DUMMY", loc), "Dummy"));
		}

		Senshi sof = of.getSenshi();
		dummy.stats = new MonsterStats(
				of.getMaxHp(), of.getRace(),
				sof.getDmg(), sof.getDfs(), sof.getDodge(), sof.getParry(),
				of.getMaxAp(), of.getInitiative()
		);

		return (Monster) dummy;
	}
}
