package com.kuuhaku.interfaces.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.dunhun.*;
import com.kuuhaku.model.common.shoukan.RegDeg;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.dunhun.Boss;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.persistent.shoukan.CombatCardAttributes;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.kuuhaku.model.records.dunhun.CombatContext;
import com.kuuhaku.model.records.dunhun.RaceValues;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@MappedSuperclass
public abstract class Actor<T extends Actor<T>> extends DAO<T> {
	@Transient
	public final long SERIAL = ThreadLocalRandom.current().nextLong();

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	private transient final ActorModifiers modifiers = new ActorModifiers();
	private transient final ActorBinding binding = new ActorBinding();
	private transient final ActorCache cache = new ActorCache(this);
	private transient final RegDeg regDeg = new RegDeg(null);
	private transient int hp, ap;
	private transient boolean fleed;

	public Actor() {
	}

	public Actor(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return getName(binding.getLocale());
	}

	public abstract String getName(I18N locale);

	public abstract Race getRace();

	public abstract int getMaxHp();

	public abstract int getMaxAp();

	public abstract int getApCap();

	public abstract int getInitiative();

	public abstract double getCritical();

	public abstract int getAggroScore();

	public RarityClass getRarityClass() {
		if (this instanceof Hero || this instanceof Boss) {
			return RarityClass.UNIQUE;
		}

		return RarityClass.NORMAL;
	}

	public ActorModifiers getModifiers() {
		return modifiers;
	}

	public ActorBinding getBinding() {
		return binding;
	}

	public ActorCache getCache() {
		return cache;
	}

	public int getHp() {
		int max = getMaxHp();
		if (hp > max) hp = max;
		else if (hp < 0) hp = 0;

		return hp;
	}

	public void setHp(int value) {
		hp = Utils.clamp(value, 0, getMaxHp());
	}

	public int getAp() {
		int max = getMaxAp();
		if (ap > max) ap = max;
		else if (ap < 0) ap = 0;

		return ap;
	}

	public void setAp(int value) {
		ap = Utils.clamp(value, 0, getMaxAp());
	}

	public void consumeAp(int value) {
		setAp(getAp() - value);
	}

	public int heal(Actor<?> source, int value) {
		return modHp(source, Math.max(0, value), 0);
	}

	public int damage(Actor<?> source, int value) {
		return modHp(source, -Math.max(0, applyMitigation(value)), source.getCritical());
	}

	public int modHp(Actor<?> source, int value, double critChance) {
		boolean crit = Calc.chance(critChance);
		if (crit) value *= 2;

		AtomicInteger val = new AtomicInteger(value);
		Combat cbt = binding.getGame().getCombat();
		if (cbt != null) {
			if (hp > 0) {
				cbt.trigger(value < 0 ? Trigger.ON_DAMAGE : Trigger.ON_HEAL, source, this, val);
				if (hp + value <= 0) {
					cbt.trigger(Trigger.ON_GRAVEYARD, source, this, val);
				}
			} else if (hp + value > 0) {
				cbt.trigger(Trigger.ON_REVIVE, source, this, val);
			}

			I18N locale = cbt.getLocale();
			cbt.getHistory().add(locale.get(value < 0 ? "str/actor_damage" : "str/actor_heal",
					getName(), Math.abs(value), crit ? ("**(" + locale.get("str/critical_hit") + ")**") : ""
			));

			if (value < 0) {
				getSenshi().reduceSleep(999);
			}
		}

		value = val.get();
		setHp(getHp() + value);

		return value;
	}

	public int applyMitigation(int raw) {
		Dunhun game = binding.getGame();
		if (raw < 0 || game == null) return raw;

		Senshi sen = getSenshi();
		double fac = sen.isDefending() ? 2 : 1;
		if (game.isDuel()) {
			fac *= 25;
		}

		return (int) Math.max(raw / (5 * fac), ((5 / fac) * Math.pow(raw, 2)) / (sen.getDfs() + (5 / fac) * raw));
	}

	public boolean hasFleed() {
		return fleed;
	}

	public boolean isFlee() {
		return fleed;
	}

	public void setFleed(boolean fleed) {
		if (getGame() != null && !this.fleed && fleed) {
			getGame().getChannel().sendMessage(getGame().getLocale().get("str/actor_flee", getName()));
		}

		setAp(0);
		this.fleed = fleed;
	}

	public boolean isOutOfCombat() {
		return fleed || getHp() <= 0;
	}

	public Dunhun getGame() {
		return binding.getGame();
	}

	public Team getTeam() {
		return binding.getTeam();
	}

	public RegDeg getRegDeg() {
		return regDeg;
	}

	public int applyRegDeg() {
		AtomicInteger val = new AtomicInteger(getRegDeg().next());

		Combat cbt = binding.getGame().getCombat();
		if (cbt != null) {
			cbt.trigger(val.get() < 0 ? Trigger.ON_DEGEN : Trigger.ON_REGEN, null, this, val);
		}

		setHp(getHp() + val.get());

		return 0;
	}

	public void addHpBar(XStringBuilder sb) {
		int[] blocks = {1000, 2500, 5000};

		int part = 0;
		int maxHp = getMaxHp();
		for (int i = 0, mult = 1; i < blocks.length; i++) {
			part = blocks[i] * mult;
			if (maxHp <= part) {
				part /= 20;
				break;
			}

			if (i == blocks.length - 1) {
				mult *= 10;
				i = -1;
			}
		}

		sb.appendNewLine("HP: " + Utils.shorten(getHp()) + "/" + Utils.shorten(getMaxHp()));
		sb.nextLine();

		boolean rdClosed = true;
		int rd = -getRegDeg().peek();
		if (rd >= part) {
			sb.append("__");
			rdClosed = false;
		}

		int steps = maxHp / part;
		for (int i = 0; i < steps; i++) {
			if (i > 0 && i % 10 == 0) sb.nextLine();
			int threshold = i * part;

			if (!rdClosed && threshold > rd) {
				sb.append("__");
				rdClosed = true;
			}

			if (getHp() > 0 && getHp() >= threshold) sb.append('▰');
			else sb.append('▱');
		}

		if (rd >= maxHp && !rdClosed) {
			sb.append("__");
		}
	}

	public void addApBar(XStringBuilder sb) {
		sb.appendNewLine(Utils.makeProgressBar(getAp(), getMaxAp(), getMaxAp(), '◇', '◈'));
	}

	public void trigger(Trigger trigger, Actor<?> target, AtomicInteger value) {
		CombatContext context = new CombatContext(trigger, this, target, value);

		List<EffectBase> queue = new ArrayList<>();
		for (Gear g : getEquipment()) {
			Set<EffectBase> effects = g.getEffects();
			if (effects.isEmpty()) continue;

			effects.removeIf(EffectBase::isClosed);
			queue.addAll(effects);
		}

		Set<EffectBase> effects = getModifiers().getEffects();
		if (!effects.isEmpty()) {
			effects.removeIf(EffectBase::isClosed);
			queue.addAll(effects);
		}

		for (EffectBase e : queue) {
			try {
				if (e instanceof TriggeredEffect te) {
					if (!Utils.equalsAny(trigger, te.getTriggers())) continue;
					te.decLimit();
				}

				e.lock();
				e.getEffect().accept(e, context);
			} finally {
				e.unlock();
			}
		}
	}

	public Senshi getSenshi() {
		return cache.getSenshi();
	}

	public Equipment getEquipment() {
		return cache.getEquipment();
	}

	public List<Skill> getSkills() {
		return cache.getSkills();
	}

	public Skill getSkill(String id) {
		return cache.getSkills().parallelStream()
				.filter(s -> s.getId().equalsIgnoreCase(id))
				.findFirst().orElse(null);
	}

	public Senshi createSenshi() {
		Senshi senshi = new Senshi(this);
		cache.setSenshi(senshi);

		modifiers.clear(this);
		int dmg = this instanceof MonsterBase<?> m ? m.getStats().getAttack() : 100;
		int def = this instanceof MonsterBase<?> m ? m.getStats().getDefense() : 100;
		int ddg = this instanceof MonsterBase<?> m ? m.getStats().getDodge() : 0;
		int pry = this instanceof MonsterBase<?> m ? m.getStats().getParry() : 0;
		double pow = 0;

		if (this instanceof Hero h) {
			RaceValues bonus = h.getStats().getRaceBonus();
			dmg += bonus.attack();
			def += bonus.defense();
			ddg += bonus.dodge();
			pry += bonus.parry();
			pow += (bonus.power() / 100d) + h.getAttributes().wis() * 0.05;
		} else {
			pow = switch (getRarityClass()) {
				case MAGIC -> 0.25;
				case RARE -> 1;
				default -> 0;
			};
		}

		Attributes total = this instanceof Hero h ? h.getAttributes() : new Attributes();
		Equipment equip = getEquipment();

		attrCheck:
		while (true) {
			for (Gear g : equip) {
				if (!total.has(g.getBasetype().getStats().requirements().attributes())) {
					equip.unequip(g);
					total = total.reduce(g.getAttributes(this));
					continue attrCheck;
				}
			}

			break;
		}

		int wDmg = 0;
		for (Gear g : equip) {
			if (g == null) continue;
			g.load(this);

			if (!g.isWeapon()) {
				dmg += g.getDmg();
			} else {
				double mult = 1;
				if (g.getTags().contains("LIGHT")) {
					mult *= 1 + total.dex() * 0.05f;
				}

				if (g.getTags().contains("HEAVY")) {
					mult *= 1 + total.str() * 0.05f;
				}

				if (g.getTags().contains("OFFHAND")) {
					dmg += (int) (g.getDmg() * mult);
				} else {
					int d = (int) (g.getDmg() * mult);
					if (wDmg > 0) {
						wDmg = (int) ((wDmg + d) * 0.6);
					} else {
						wDmg += d;
					}
				}

			}

			def += g.getDfs();
		}

		double mult = 1;
		if (getGame() != null && this instanceof MonsterBase<?> m) {
			m.load();

			int level = getGame().getAreaLevel();
			mult = switch (getRarityClass()) {
				case RARE -> {
					cache.getSenshi().setHueOffset(Calc.rng(90, 270, SERIAL));
					yield 2;
				}
				case MAGIC -> 1.25;
				default -> 1;
			} * MonsterBase.statTable[level];

			if (getGame().getPartySize() > 1 && getTeam() == Team.KEEPERS) {
				mult *= 1 + getGame().getPartySize() * 0.3;
			}
		}

		CombatCardAttributes base = senshi.getBase();
		base.setAtk((int) ((dmg + wDmg) * mult));
		base.setDfs((int) ((def * (1 + total.str() * 0.01)) * mult));
		base.setDodge(ddg + total.dex() / 2);
		base.setParry(pry);
		senshi.getStats().getPower().set(pow);

		int effCost = (int) Utils.regex(base.getEffect(), "%EFFECT%").results().count();
		base.setMana(1 + (base.getAtk() + base.getDfs()) / 750 + effCost);
		base.setSacrifices((base.getAtk() + base.getDfs()) / 3000);

		if (this instanceof Hero) {
			base.getTags().add("HERO");
		} else {
			base.getTags().add("MONSTER");
		}

		return senshi;
	}

	public BufferedImage render() {
		return render(binding.getLocale());
	}

	public BufferedImage render(I18N locale) {
		return getSenshi().render(locale, cache.getDeck());
	}

	public abstract Actor<?> fork();

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Actor<?> actor = (Actor<?>) o;
		return SERIAL == actor.SERIAL && Objects.equals(id, actor.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(SERIAL, id);
	}
}
