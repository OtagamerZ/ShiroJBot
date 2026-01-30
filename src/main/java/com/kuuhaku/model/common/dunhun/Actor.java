package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.interfaces.dunhun.Usable;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.shoukan.CardExtra;
import com.kuuhaku.model.common.shoukan.FlatMod;
import com.kuuhaku.model.common.shoukan.MultMod;
import com.kuuhaku.model.common.shoukan.RegDeg;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.NodeType;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.dunhun.*;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.CombatCardAttributes;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.kuuhaku.model.records.dunhun.CombatContext;
import com.kuuhaku.model.records.dunhun.Loot;
import com.kuuhaku.model.records.dunhun.RaceValues;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import groovy.lang.Tuple2;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@MappedSuperclass
public abstract class Actor<T extends Actor<T>> extends DAO<T> {
	public static final int MAX_LEVEL = 100;

	@Transient
	public final long SERIAL = ThreadLocalRandom.current().nextLong();

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	private transient final ActorModifiers modifiers = new ActorModifiers(this);
	private transient final ActorBinding binding = new ActorBinding();
	private transient final ActorCache cache = new ActorCache(this);
	private transient final RegDeg regDeg = new RegDeg(null);
	private transient final Deque<MonsterBase<?>> minions = new ArrayDeque<>();
	private transient final Map<String, Object> props = new HashMap<>();
	private transient Actor<?> killer;
	private transient int hp = -1, ap;
	private transient int maxHp = -1;
	private transient boolean fleed;
	private transient boolean essential;

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

	public BufferedImage getImage() {
		return new Card(this).drawCardNoBorder();
	}

	public abstract int getLevel();

	public int getDropLevel() {
		if (this instanceof Hero) return getGame().getAreaLevel();

		return getGame().getAreaLevel() + switch (getRarityClass()) {
			case NORMAL -> 0;
			case MAGIC -> 1;
			case RARE -> 2;
			case UNIQUE -> 5;
		};
	}

	public abstract int getMaxHp();

	public abstract int getMaxAp();

	public int getReservedAp() {
		return getSkills().stream()
				.filter(Objects::nonNull)
				.filter(s -> s.getToggledEffect() != null)
				.map(Skill::getStats)
				.mapToInt(SkillStats::getReservation)
				.sum();
	}

	public int getUsableAp() {
		return getMaxAp() - getReservedAp();
	}

	public abstract int getApCap();

	public abstract int getInitiative();

	public abstract double getCritical();

	public abstract int getThreatScore();

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

	public Actor<?> getKiller() {
		return killer;
	}

	public void setKiller(Actor<?> killer) {
		this.killer = killer;
	}

	public int getHp() {
		int max = getMaxHp();
		if (maxHp != -1 && maxHp != max) {
			hp = max * hp / maxHp;
		}

		maxHp = max;
		if (hp > max || hp == -1) hp = max;
		else if (hp < 0) hp = 0;

		return hp;
	}

	public void setHp(int value) {
		hp = Utils.clamp(value, 0, getMaxHp());
	}

	public int getAp() {
		int max = getUsableAp();

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

	public Tuple2<Integer, Boolean> heal(int value) {
		return heal(null, null, value);
	}

	public Tuple2<Integer, Boolean> heal(Actor<?> source, Usable usable, int value) {
		double crit = 0;
		if (usable instanceof Skill s && s.getStats().isSpell()) {
			crit = source.getModifiers().getCritical(s.getStats().getCritical());
		}

		return modHp(source, usable, Math.max(0, value), crit);
	}

	public Tuple2<Integer, Boolean> damage(int value) {
		return damage(null, null, value);
	}

	public Tuple2<Integer, Boolean> damage(Actor<?> source, Usable usable, int value) {
		AtomicInteger val = new AtomicInteger(value);

		double crit = 0;
		if (usable instanceof Skill s && source != null) {
			if (s.getStats().isSpell()) {
				crit = source.getModifiers().getCritical(s.getStats().getCritical());
			} else {
				Combat cbt = source.getGame().getCombat();
				if (cbt != null) {
					cbt.trigger(Trigger.ON_ATTACK, source, this, usable, val);
				}

				crit = source.getCritical();
			}
		}

		return modHp(source, usable, -Math.max(0, applyMitigation(val.get())), crit);
	}

	public Tuple2<Integer, Boolean> modHp(Actor<?> source, Usable usable, int value, double critChance) {
		boolean isAttack = usable instanceof Skill s && !s.getStats().isSpell();
		boolean crit = Calc.chance(critChance);
		if (crit) value *= 2;

		value = (int) (value < 0 ? modifiers.getDamageTaken(value) : modifiers.getHealing(value));
		AtomicInteger val = new AtomicInteger(value);
		Combat cbt = binding.getGame().getCombat();
		if (cbt != null) {
			if (source != null) {
				if (hp > 0) {
					if (val.get() < 0) {
						cbt.trigger(Trigger.ON_DEFEND, this, source, usable, val);

						Senshi srcSen = source.getSenshi();
						Senshi tgtSen = getSenshi();
						if (source.getTeam() != getTeam()) {
							String outcome = null;
							int histIdx = cbt.getHistory().size();
							if (srcSen.isBlinded(true) && Calc.chance(50)) {
								cbt.trigger(Trigger.ON_MISS, source, this, usable);

								outcome = cbt.getLocale().get("str/actor_miss", source.getName());
							} else if (!srcSen.hasFlag(Flag.TRUE_STRIKE, true) && !tgtSen.isSleeping() && !tgtSen.isStasis()) {
								if (Calc.chance(tgtSen.getDodge())) {
									cbt.trigger(Trigger.ON_MISS, source, this, usable);
									cbt.trigger(Trigger.ON_DODGE, this, source, usable);

									outcome = cbt.getLocale().get("str/actor_dodge", this.getName());
								} else if (isAttack && Calc.chance(tgtSen.getParry())) {
									cbt.trigger(Trigger.ON_PARRY, this, source, usable);

									outcome = cbt.getLocale().get("str/actor_parry", this.getName());
									cbt.attack(this, source);
								}
							}

							if (outcome != null) {
								cbt.getHistory().add(histIdx, outcome);
								return Tuple2.tuple(0, false);
							}
						}

						cbt.trigger(Trigger.ON_HIT, source, this, usable);
						if (crit) {
							cbt.trigger(Trigger.ON_CRITICAL, source, this, usable);
						}
					}

					cbt.trigger(val.get() < 0 ? Trigger.ON_DAMAGE : Trigger.ON_HEAL, source, this, usable, val);
					if (hp + val.get() <= 0) {
						cbt.trigger(Trigger.ON_GRAVEYARD, this, this, usable);
						cbt.trigger(Trigger.ON_KILL, source, this, usable);

						Actor<?> killer = source;
						if (killer instanceof MonsterBase<?> m && m.isMinion()) {
							killer = m.getMaster();
						}
						setKiller(killer);

						if (this instanceof MonsterBase<?> m && !m.isMinion() && !m.didDropLoot()) {
							MonsterStats stats = m.getStats();
							Loot lt = m.generateLoot();
							lt.xp().addAndGet(m.getKillXp());

							double mf = killer.getModifiers().getMagicFind(1);
							double mult = mf
									* stats.getLootMultiplier(m)
									* Math.pow(1.2, cbt.getGame().getModifiers().size())
									* (cbt.getGame().getAreaType() == NodeType.DANGER ? 1.5 : 1);

							double dropFac = 30 * mult;
							while (Calc.chance(dropFac)) {
								Gear drop = Gear.getRandom(m);
								if (drop != null) {
									lt.gear().add(drop);
								}

								dropFac /= 2;
							}

							dropFac = 10 * mult;
							while (Calc.chance(dropFac)) {
								GlobalDrop drop = GlobalDrop.getRandom(cbt.getGame());
								if (drop == null) break;

								lt.items().add(drop.getItem());
								dropFac /= 2;
							}

							cbt.getLoot().add(lt);
							m.setDroppedLoot(true);
						}
					}
				} else if (hp + val.get() > 0) {
					cbt.trigger(Trigger.ON_REVIVE, this, this, usable);
					getSenshi().setAvailable(true);
				}
			}

			I18N locale = cbt.getLocale();
			if (crit) {
				cbt.getHistory().add("**" + locale.get("str/critical_hit") + "**");
			}

			if (val.get() != 0) {
				cbt.getHistory().add(locale.get(val.get() < 0 ? "str/actor_damage" : "str/actor_heal", getName(), Math.abs(val.get())));
			}

			if (val.get() < 0) {
				getSenshi().reduceSleep(999);
			}
		}

		setHp(getHp() + val.get());

		return Tuple2.tuple(val.get(), crit);
	}

	public int applyMitigation(int raw) {
		Dunhun game = binding.getGame();
		if (raw < 0 || game == null) return raw;

		Senshi sen = getSenshi();
		double fac = sen.isDefending() ? 3 : 1;
		double mit = 1 - Math.min(sen.getDfs() / (sen.getDfs() + 1000 / fac), 0.9);

		return (int) Math.ceil(raw * mit);
	}

	public boolean hasFleed() {
		if (this instanceof MonsterBase<?> m && m.isMinion()) {
			return m.getMaster().fleed;
		}

		return fleed;
	}

	public void setFleed(boolean fleed) {
		if (getGame() != null && !this.fleed && fleed) {
			getGame().getChannel().sendMessage(getGame().getString("str/actor_flee", getName()));
		}

		setAp(0);
		this.fleed = fleed;
	}

	public boolean isEssential() {
		return essential;
	}

	public void setEssential(boolean essential) {
		this.essential = essential;
	}

	public boolean isOutOfCombat() {
		if (!binding.isBound()) return true;
		else if (this instanceof MonsterBase<?> m && m.isMinion()) {
			if (m.getMaster().isOutOfCombat()) return true;
		}

		return hasFleed() || getHp() <= 0;
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

	public void applyRegDeg() {
		AtomicInteger val = new AtomicInteger(getRegDeg().next());

		Combat cbt = binding.getGame().getCombat();
		if (cbt != null) {
			cbt.trigger(val.get() < 0 ? Trigger.ON_DEGEN : Trigger.ON_REGEN, this, this, null, val);
		}

		setHp(getHp() + val.get());
	}

	public Deque<MonsterBase<?>> getMinions() {
		return minions;
	}

	public Map<String, Object> getProps() {
		return props;
	}

	public void addHpBar(I18N locale, XStringBuilder sb) {
		sb.appendNewLine("HP: " + Utils.shorten(locale, getHp()) + "/" + Utils.shorten(locale, getMaxHp()));

		int rd = getRegDeg().peek();
		if (rd != 0) {
			String icon = Utils.getEmoteString(rd > 0 ? "regen" : "degen");
			sb.append(" (" + icon + Utils.shorten(locale, Math.abs(rd)) + ")");
		}

		int max = getMaxHp();
		String bar = Utils.makeProgressBar(getHp(), max, Calc.clamp((int) Math.ceil(max / 100d), 1, 10), '▱', '▰');
		sb.appendNewLine("-# " + bar);
	}

	public void addApBar(XStringBuilder sb) {
		int max = getMaxAp();
		String bar = Utils.makeProgressBar(getAp(), max, max, '◇', '◈');

		int reserved = getReservedAp();
		if (reserved > 0) {
			bar = bar.substring(0, max - reserved) + "~~" + bar.substring(max - reserved) + "~~";
		}

		sb.appendNewLine("-# " + bar);
	}

	public void trigger(Trigger trigger, Actor<?> target, Usable usable, AtomicInteger value) {
		if (!binding.isBound()) return;

		CombatContext context = new CombatContext(getGame().getCombat(), trigger, this, target, usable, value);

		List<EffectBase> queue = new ArrayList<>();
		for (Gear g : getEquipment()) {
			queue.addAll(g.getEffects().stream()
					.filter(e -> !e.isClosed())
					.toList()
			);
		}

		queue.addAll(getModifiers().getEffects().stream()
				.map(EffectProperties::getEffect)
				.filter(Objects::nonNull)
				.filter(e -> !e.isClosed())
				.toList()
		);

		for (EffectBase e : queue) {
			if (e.isLocked()) continue;
			else if (e instanceof TriggeredEffect te) {
				if (!Utils.equalsAny(trigger, te.getTriggers())) continue;
				te.decLimit();
			}

			try {
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
		return cache.getSkills().stream()
				.filter(Objects::nonNull)
				.filter(s -> s.getId().equalsIgnoreCase(id))
				.findFirst().orElse(null);
	}

	public Senshi createSenshi() {
		Senshi senshi = new Senshi(this);
		cache.setSenshi(senshi);

		int dmg, def, ddg, pry;
		double pow;

		if (this instanceof Hero h) {
			RaceValues bonus = h.getStats().getRaceBonus();
			dmg = 150 + bonus.attack();
			def = 150 + bonus.defense();
			ddg = bonus.dodge();
			pry = bonus.parry();
			pow = (bonus.power() / 100d) + h.getAttributes().wis() * 0.05;
		} else {
			MonsterBase<?> m = (MonsterBase<?>) this;
			dmg = m.getStats().getAttack();
			def = m.getStats().getDefense();
			ddg = m.getStats().getDodge();
			pry = m.getStats().getParry();
			pow = switch (getRarityClass()) {
				case MAGIC -> 0.25;
				case RARE -> 1;
				default -> 0;
			} + getLevel() * 0.025;
		}

		Attributes total = this instanceof Hero h ? h.getAttributes() : new Attributes();
		Equipment equip = getEquipment();

		attrCheck:
		while (true) {
			for (Gear g : equip) {
				if (!total.has(g.getRequirements().attributes())) {
					equip.unequip(g);
					total = total.reduce(g.getAttributes());
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

		double mult;
		if (getGame() != null && this instanceof MonsterBase<?> m) {
			m.load();

			int level = m.getLevel();
			mult = switch (getRarityClass()) {
				case RARE -> {
					senshi.setStashRef(new StashedCard(senshi.getCard(), true));
					yield 2;
				}
				case MAGIC -> 1.25;
				default -> 1;
			} * MonsterBase.STAT_TABLE[level - 1] * (
					getTeam() == Team.KEEPERS
							? 0.7 + getGame().getPartySize() * 0.3
							: 1
			);
		} else {
			mult = 1;
		}

		int baseDmg = dmg + wDmg;
		int baseDef = (int) (def * (1 + total.str() * 0.01));
		int baseDdg = ddg + total.dex() / 2;
		int basePry = pry;

		CardExtra stats = senshi.getStats();
		stats.getAtk().set(new FlatMod(() -> modifiers.getDamage(baseDmg) * mult));
		stats.getDfs().set(new FlatMod(() -> modifiers.getDefense(baseDef) * mult * 0.75));
		stats.getDodge().set(new FlatMod(() -> modifiers.getDodge(baseDdg)));
		stats.getParry().set(new FlatMod(() -> modifiers.getParry(basePry)));
		stats.getPower().set(new MultMod(() -> modifiers.getPower(pow)));

		CombatCardAttributes base = senshi.getBase();
		int effCost = (int) Utils.regex(base.getEffect(), "%EFFECT%").results().count();
		base.setMana((int) (1 + (base.getAtk() + base.getDfs()) * senshi.getPower() / 750 + effCost));
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

	public abstract Actor<?> copy();

	public List<Actor<?>> getNearby() {
		Combat cbt = getGame().getCombat();
		if (cbt == null) return List.of();

		List<Actor<?>> actors = cbt.getActors(getTeam());

		int idx = actors.indexOf(this);
		if (idx > -1) {
			List<Actor<?>> nearby = new ArrayList<>(2);
			if (idx > 0) nearby.add(actors.get(idx - 1));
			if (idx < actors.size() - 1) nearby.add(actors.get(idx + 1));

			return nearby;
		}

		return List.of();
	}

	public void destroy() {
		if (this instanceof MonsterBase<?> m && m.isMinion()) {
			m.setMaster(null);
		}

		for (MonsterBase<?> m : getMinions()) {
			m.destroy();
		}

		Dunhun game = getGame();
		if (game != null) {
			Combat cbt = game.getCombat();
			if (cbt != null) {
				cbt.getActors(getTeam()).remove(this);
			}
		}

		binding.unbind();
	}

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
