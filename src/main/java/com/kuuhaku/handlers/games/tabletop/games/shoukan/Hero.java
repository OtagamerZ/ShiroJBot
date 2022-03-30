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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Perk;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.model.persistent.id.CompositeHeroId;
import com.kuuhaku.model.records.BountyInfo;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.ImageHelper;
import com.kuuhaku.utils.helpers.MathHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import net.dv8tion.jda.api.entities.User;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Entity
@Table(name = "hero")
@IdClass(CompositeHeroId.class)
public class Hero extends DAO implements Cloneable {
    @Id
    @Column(columnDefinition = "INT NOT NULL DEFAULT 0")
    private int id;

    @Id
    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String uid;

    @Column(columnDefinition = "VARCHAR(25) NOT NULL")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String image = null;

    @Embedded
    private Attributes stats;

    @Enumerated(EnumType.STRING)
    private Race race;

    @Column(columnDefinition = "INT NOT NULL DEFAULT 0")
    private int energy = 0;

    @Column(columnDefinition = "INT NOT NULL DEFAULT 0")
    private int xp = 0;

    @Column(columnDefinition = "INT NOT NULL DEFAULT 0")
    private int effect = 0;

    @Column(columnDefinition = "INT NOT NULL DEFAULT 0")
    private int bonusPoints = 0;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "hero_id")
    private Set<Perk> perks = EnumSet.noneOf(Perk.class);

    @LazyCollection(LazyCollectionOption.FALSE)
    @ManyToMany
    private Set<Evogear> inventory = new HashSet<>();

    @Column(columnDefinition = "VARCHAR(255)")
    private String quest;

    @Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
    private long questEnd = 0;

    @Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
    private long questSeed = 0;

    @Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
    private long seed = 0;

    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean resting = false;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumns({
            @JoinColumn(nullable = false, name = "hero_id", referencedColumnName = "id"),
            @JoinColumn(nullable = false, name = "hero_uid", referencedColumnName = "uid")
    })
    private Set<AppliedDebuff> debuffs = new HashSet<>();

    private transient int hitpoints = -1;
    private transient boolean died = false;

    public Hero() {
    }

    public Hero(User user, String name, Race race, BufferedImage image) {
        Set<Integer> ids = KawaiponDAO.getHeroes(user.getId()).stream()
                .map(Hero::getId)
                .collect(Collectors.toSet());

        int i = 0;
        this.id = -1;
        while (id == -1) {
            if (!ids.contains(i)) {
                this.id = i;
            }

            i++;
        }

        this.uid = user.getId();
        this.name = name;
        this.stats = new Attributes(race.getStartingStats());
        this.race = race;
        this.image = ImageHelper.atob(ImageHelper.scaleAndCenterImage(ImageHelper.removeAlpha(image), 225, 350), "jpg");
        this.energy = getMaxEnergy();
    }

    public int getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public BufferedImage getImage() {
        return image == null ? null : ImageHelper.btoa(image);
    }

    public void setImage(BufferedImage image) {
        if (image.getWidth() == 225 && image.getHeight() == 350) {
            this.image = ImageHelper.atob(ImageHelper.removeAlpha(image), "jpg");
        } else {
            this.image = ImageHelper.atob(ImageHelper.scaleAndCenterImage(ImageHelper.removeAlpha(image), 225, 350), "jpg");
        }
    }

    public Attributes getRawStats() {
        return stats;
    }

    public Attributes getEquipStats() {
        Integer[] out = new Integer[]{0, 0, 0, 0, 0};
        if (perks.contains(Perk.TABULA_RASA)) return new Attributes(out);

        for (Evogear e : inventory) {
            out[0] += e.getAtk() / (e.getCharms().isEmpty() ? 100 : 125);
            out[1] += e.getDef() / (e.getCharms().isEmpty() ? 100 : 125);

            if (!e.getCharms().isEmpty()) {
                int div = e.getCharms().size();

                for (Charm charm : e.getCharms()) {
                    switch (charm) {
                        case PIERCING, DRAIN, BLEEDING -> out[0] += e.getTier() / div;
                        case SHIELD, MIRROR -> out[1] += e.getTier() / div;
                        case AGILITY -> out[2] += e.getTier() * 2 / div;
                        case TIMEWARP, DOUBLETAP -> out[3] += e.getTier() * 2 / div;
                        case CLONE, LINK -> out[4] += e.getTier() * 2 / div;
                    }
                }
            }
        }

        return new Attributes(out);
    }

    public Attributes getStats() {
        return new Attributes(CollectionHelper.mergeArrays(stats.getStats(), getEquipStats().getStats()));
    }

    public void resetStats() {
        stats = new Attributes(race.getStartingStats());
    }

    public Race getRace() {
        return race;
    }

    public int getLevel() {
        return MathHelper.revFibonacci(xp / 5) - 1;
    }

    public int getXp() {
        return xp;
    }

    public void addXp(int xp) {
        this.xp += xp;
    }

    public void removeXp(int xp) {
        this.xp = Math.max(0, this.xp - xp);
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public int getXpToNext() {
            return (int) MathHelper.getFibonacci(getLevel() + 2) * 5;
    }

    public int getBonusPoints() {
        return bonusPoints;
    }

    public void addBonusPoints(int bonusPoints) {
        this.bonusPoints += bonusPoints;
    }

    public int getMaxStatPoints() {
        return 5 + getLevel() * 5 + bonusPoints;
    }

    public int getAvailableStatPoints() {
        return Math.max(0, getMaxStatPoints() - stats.getUsedPoints());
    }

    public Set<Perk> getPerks() {
        return perks;
    }

    public int getMaxPerks() {
        int extra = 0;
        if (perks.contains(Perk.SCHOLAR)) extra += 2;

        return getLevel() / 5 + extra;
    }

    public int getAvailablePerks() {
        return Math.max(0, getMaxPerks() - perks.size());
    }

    public Set<Evogear> getInventory() {
        return inventory;
    }

    public Set<String> getInventoryNames() {
        return inventory.stream()
                .map(Evogear::getCard)
                .map(Card::getName)
                .collect(Collectors.toSet());
    }

    public int getInventoryCap() {
        if (perks.contains(Perk.SCHOLAR)) return 0;

        return Math.max(0, getStats().calcInventoryCap() - inventory.size());
    }

    public BountyInfo getQuest() {
        if (quest == null) return null;

        return BountyQuest.find(BountyQuest.class, quest).getInfo(this, questSeed);
    }

    public void setQuest(BountyQuest quest, long seed) {
        this.quest = quest.getId();
        this.questSeed = seed;

        double timeModif = 1;
        for (Perk perk : perks) {
            timeModif *= switch (perk) {
                case NIMBLE -> 0.9;
                default -> 1;
            };
        }

        debuffs.removeIf(AppliedDebuff::expired);
        for (AppliedDebuff d : debuffs) {
            Debuff debuff = d.getDebuff();
            timeModif *= switch (debuff.getId()) {
                case "D_TIREDNESS" -> 1.5;
                default -> 1;
            };
        }

        this.questEnd = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(Math.round(getQuest().time() * timeModif), TimeUnit.MINUTES);
    }

    public long getQuestEnd() {
        return questEnd;
    }

    public boolean isUnavailable() {
        return isQuesting() || resting;
    }

    public boolean isQuesting() {
        return quest != null;
    }

    public boolean hasArrived() {
        return System.currentTimeMillis() >= questEnd;
    }

    public void arrive() {
        setEnergy(this.energy - 1);
        this.quest = null;
        this.questSeed = 0;
        this.questEnd = 0;
    }

    public boolean isResting() {
        return resting;
    }

    public void setResting(boolean sleeping) {
        this.resting = sleeping;
    }

    public void toggleResting() {
        this.resting = !this.resting;
    }

    public Set<AppliedDebuff> getDebuffs() {
        debuffs.removeIf(AppliedDebuff::expired);

        return debuffs;
    }

    public void setDebuffs(Set<AppliedDebuff> debuffs) {
        debuffs.removeIf(AppliedDebuff::expired);

        this.debuffs = debuffs;
    }

    public void addDebuff(Debuff debuff) {
        debuffs.removeIf(AppliedDebuff::expired);

        this.debuffs.add(new AppliedDebuff(debuff, debuff.getDuration()));
    }

    public long getSeed() {
        return seed;
    }

    public void randomizeSeed() {
        this.seed = (long) MathHelper.rng(System.currentTimeMillis());
    }

    public String getDescription() {
        Champion ref = Champion.find(Champion.class, effect);
        return ref == null ? "Lendário herói " + race.toString().toLowerCase(Locale.ROOT) + " invocado por " + MiscHelper.getUsername(uid) : ref.getDescription();
    }

    public void setReferenceChampion(int id) {
        this.effect = id;
    }

    public Champion getReferenceChampion() {
        return Champion.find(Champion.class, effect);
    }

    public int getMaxHp() {
        return getStats().calcMaxHp(perks);
    }

    public int getHitpoints() {
        if (hitpoints == -1) hitpoints = getMaxHp();
        return hitpoints;
    }

    public void setHitpoints(int hitpoints) {
        this.hitpoints = MathHelper.clamp(hitpoints, 0, getMaxHp());
    }

    public int bufferDamage(int dmg) {
        int aux = dmg - getHitpoints();
        hitpoints = -Math.min(aux, 0);
        return Math.max(0, aux);
    }

    public int getMaxEnergy() {
        return getStats().calcMaxEnergy();
    }

    public int getEnergy() {
        return MathHelper.clamp(energy, 0, getMaxEnergy());
    }

    public void setEnergy(int energy) {
        this.energy = MathHelper.clamp(energy, 0, getMaxEnergy());
    }

    public void rest() {
        setEnergy(energy + 1);
    }

    public void rest(int val) {
        setEnergy(energy + val);
    }

    public void removeEnergy(int val) {
        setEnergy(energy - val);
    }

    public int getMana() {
        double mpModif = 1;
        for (Perk perk : perks) {
            mpModif *= switch (perk) {
                case BLOODLUST -> 0.5;
                case MANALESS -> 0;
                case MINDSHIELD -> 2;
                default -> 1;
            };
        }

        return (int) Math.ceil(Math.max(perks.contains(Perk.MANALESS) ? 0 : 1, stats.calcMp(this, getReferenceChampion()) * mpModif));
    }

    public int getBlood() {
        int blood = 0;
        for (Perk perk : perks) {
            double bldModif = 1;
            for (Perk p : perks) {
                if (!p.equals(perk))
                    bldModif *= switch (p) {
                        case BLOODLUST -> 0.5;
                        case MANALESS -> 0;
                        case MINDSHIELD -> 2;
                        default -> 1;
                    };
            }

            blood += switch (perk) {
                case BLOODLUST -> stats.calcMp(this, getReferenceChampion()) * bldModif / 2 * 100;
                default -> 0;
            };
        }

        return Math.max(0, blood);
    }

    public int getAtk() {
        double atkModif = 1;
        for (Perk perk : perks) {
            atkModif *= switch (perk) {
                case VANGUARD -> 0.75;
                case CARELESS -> 1.25;
                case MANALESS -> 0.5;
                default -> 1;
            };
        }

        return (int) Math.max(0, MiscHelper.roundTrunc(getStats().calcAtk() * atkModif, 25));
    }

    public int getDef() {
        double defModif = 1;
        for (Perk perk : perks) {
            defModif *= switch (perk) {
                case VANGUARD -> 1.15;
                case CARELESS -> 0.66;
                case MANALESS -> 0.5;
                default -> 1;
            };
        }

        return (int) Math.max(0, MiscHelper.roundTrunc(getStats().calcDef() * defModif, 25));
    }

    public int getDodge() {
        double ddgModif = 1;
        for (Perk perk : perks) {
            ddgModif *= switch (perk) {
                case NIMBLE -> 1.25;
                case ARMORED -> 0;
                case MANALESS -> 0.5;
                default -> 1;
            };
        }

        return (int) Math.round(getStats().calcDodge() * ddgModif);
    }

    public int getBlock() {
        int block = 0;
        for (Perk perk : perks) {
            block += switch (perk) {
                case ARMORED -> getStats().calcDodge() / 2;
                default -> 0;
            };
        }

        return Math.max(0, block);
    }

    public Champion toChampion() {
        Champion ref = Champion.find(Champion.class, effect);
        Champion c = new Champion(
                new Card(uid, name, new AddedAnime("HERO", true), KawaiponRarity.ULTIMATE, image),
                race, getMana(), getBlood(), getAtk(), getDef(), getDescription(), ref == null ? null : ref.getRawEffect()
        );
        c.setAcc(Account.find(Account.class, uid));
        c.setHero(this);

        return c;
    }

    public boolean hasDied() {
        return died;
    }

    public void setDied(boolean died) {
        this.died = died;
    }

    @Override
    public Hero clone() {
        try {
            Hero h = (Hero) super.clone();
            h.stats = new Attributes(stats.getStats());
            h.perks = new HashSet<>(perks);
            h.inventory = new HashSet<>(inventory);
            return h;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hero hero = (Hero) o;
        return id == hero.id && Objects.equals(uid, hero.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uid);
    }
}
