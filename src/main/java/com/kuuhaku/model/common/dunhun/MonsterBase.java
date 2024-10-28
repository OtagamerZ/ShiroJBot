package com.kuuhaku.model.common.dunhun;

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
import com.kuuhaku.model.persistent.dunhun.MonsterStats;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.persistent.localized.LocalizedMonster;
import com.kuuhaku.model.persistent.shoukan.CardAttributes;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.dunhun.CombatContext;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import static jakarta.persistence.CascadeType.ALL;

@MappedSuperclass
public abstract class MonsterBase<T extends MonsterBase<T>> extends DAO<T> implements Actor {
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
	public void setHp(int value) {
		hp.set(Calc.clamp(value, 0, getMaxHp()));
	}

	@Override
	public int getAp() {
		return ap;
	}

	@Override
	public void modAp(int value) {
		ap = Calc.clamp(ap + value, 0, getMaxAp());
	}

	@Override
	public int getInitiative() {
		return game.getAreaLevel() / 3 + (int) modifiers.getInitiative().get();
	}

	@Override
	public double getCritical() {
		return (int) (5 * (1 + modifiers.getCritical().get()));
	}

	@Override
	public int getAggroScore() {
		int aggro = 1;
		if (senshiCache != null) {
			aggro = senshiCache.getDmg() / 10 + senshiCache.getDfs() / 20 + getHp() / 150;
		}

		return (int) Math.max(1, aggro * (1 + modifiers.getAggroMult().get()) * game.getAreaLevel() / 2);
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

	public void addEffect(BiConsumer<EffectBase, CombatContext> effect, Trigger... triggers) {
		effects.add(new SelfEffect(this, effect, triggers));
	}

	@Override
	public void trigger(Trigger trigger, Actor target) {
		for (SelfEffect e : effects) {
			if (!Utils.equalsAny(trigger, e.getTriggers())) continue;

			try {
				e.lock();
				e.getEffect().accept(e, new CombatContext(this, target));
			} finally {
				e.unlock();
			}
		}
	}

	public int getKillXp() {
		double mult = switch (getRarityClass()) {
			case NORMAL -> 1;
			case MAGIC -> 1.5;
			case RARE -> 2.25;
			case UNIQUE -> 10;
		};

		double xp = getStats().getBaseHp() / 500d + getStats().getAttack() / 175d + getStats().getDefense() / 250d;
		if (game != null) {
			xp *= 1 + game.getAreaLevel() * 0.1;
		}

		return (int) (xp * mult);
	}

	@Override
	public List<Skill> getSkills() {
		if (skillCache != null) return skillCache;

		return skillCache = DAO.queryAll(Skill.class, "SELECT s FROM Skill s WHERE s.id IN ?1", stats.getSkills());
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
			} * (1 + game.getAreaLevel() * 0.2);

			base.setAtk((int) (stats.getAttack() * mult));
			base.setDfs((int) (stats.getDefense() * mult));
			base.setDodge(stats.getDodge());
			base.setParry(stats.getParry());
		}

		base.getTags().add("MONSTER");

		return senshiCache;
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
}
