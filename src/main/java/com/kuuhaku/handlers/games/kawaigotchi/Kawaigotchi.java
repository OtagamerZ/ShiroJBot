package com.kuuhaku.handlers.games.kawaigotchi;

import com.kuuhaku.controller.sqlite.KGotchiDAO;
import com.kuuhaku.handlers.games.kawaigotchi.enums.Action;
import com.kuuhaku.handlers.games.kawaigotchi.enums.*;
import com.kuuhaku.handlers.games.kawaigotchi.exceptions.EmptyStockException;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.persistence.*;
import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Random;

@Entity
@Table(name = "kawaigotchi")
public class Kawaigotchi {
	//FOOD | ENERGY | MOOD
	private enum rate {
		FOOD(0.008f),
		ENERGY(0.01f),
		MOOD(0.02f),
		HEALTH(0.01f);

		private final float fac;

		rate(float fac) {
			this.fac = fac;
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String userId;

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String name;

	@Enumerated(EnumType.ORDINAL)
	private Nature nature = Nature.randNature();

	@Column(columnDefinition = "INT")
	private int skin = 1 + Helper.rng(5);

	@Enumerated(EnumType.ORDINAL)
	private Stance stance = Stance.IDLE;

	@Enumerated(EnumType.ORDINAL)
	private Race race;

	@Enumerated(EnumType.ORDINAL)
	private Tier tier = Tier.CHILD;

	@Column(columnDefinition = "FLOAT DEFAULT 100")
	private float hunger = 100;

	@Column(columnDefinition = "FLOAT DEFAULT 100")
	private float health = 100;

	@Column(columnDefinition = "FLOAT")
	private float mood = (int) (50 * nature.getKindness());

	@Column(columnDefinition = "FLOAT DEFAULT 100")
	private float energy = 100;

	@Column(columnDefinition = "FLOAT DEFAULT 0")
	private float xp = 0;

	@Column(columnDefinition = "INT")
	private int pos = new Random().nextInt(1024);

	@Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
	private boolean alive = true;

	@Column(columnDefinition = "TEXT")
	private String bag = "{\"almondega\":5}";

	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean alerted;

	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean warned;

	private transient int lastRoll;

	public Kawaigotchi() {

	}

	public Kawaigotchi(String userId, String name, Race race) {
		this.userId = userId;
		this.name = name;
		this.race = race;
	}

	public void update(Member m) {
		if (m.getOnlineStatus() == OnlineStatus.OFFLINE || m.getOnlineStatus() == OnlineStatus.UNKNOWN) return;

		//CLAMPS
		health = Helper.clamp(health, 0, 100);
		hunger = Helper.clamp(hunger, 0, 100);
		mood = Helper.clamp(mood, 0, 100);
		energy = Helper.clamp(energy, 0, 100);

		if (!alive) stance = Stance.DEAD;

		if (health <= 0) {
			alive = false;
			KGotchiDAO.saveKawaigotchi(this);
			return;
		} else if (hunger <= 0) {
			health -= rate.HEALTH.fac;
			KGotchiDAO.saveKawaigotchi(this);
			return;
		}

		tier = Tier.tierByXp(xp);

		int currTime = OffsetDateTime.now(ZoneId.of("GMT-3")).getHour();

		if (stance.isResting()) {
			if (!Time.inRange(Time.NIGHT, currTime) && energy >= 100) stance = Stance.IDLE;
			energy += rate.ENERGY.fac * 2 * nature.getEnergy();
			health += rate.HEALTH.fac * (hunger / 50);
			KGotchiDAO.saveKawaigotchi(this);
			return;
		} else if (Time.inRange(Time.NIGHT, currTime) || energy < 5) {
			stance = Stance.SLEEPING;
			KGotchiDAO.saveKawaigotchi(this);
			return;
		}

		if (hunger < 25 || health < 25) {
			if (!warned) {
				try {
					m.getUser().openPrivateChannel().complete().sendMessage("Seu Kawaigotchi " + name + " está muito triste, corra ver o porquê!").queue();
				} catch (RuntimeException ignore) {
				}
				warned = true;
			}
		} else if (hunger < 50 || health < 50) {
			stance = Stance.SAD;
			if (!alerted) {
				try {
					m.getUser().openPrivateChannel().complete().sendMessage("Seu Kawaigotchi " + name + " está triste, vá ver o porquê!").queue();
				} catch (RuntimeException ignore) {
				}
				alerted = true;
			}
		} else if (mood > 66) {
			stance = Stance.HAPPY;
			alerted = false;
			warned = false;
		} else if (mood < 33) {
			stance = Stance.ANGRY;
			alerted = false;
			warned = false;
		} else {
			stance = Stance.IDLE;
			alerted = false;
			warned = false;
		}

		if (hunger > 60 && mood < 80) mood += (rate.MOOD.fac + ((100 - hunger) * 0.01f / 20)) * nature.getKindness();
		else mood -= rate.MOOD.fac / nature.getKindness();

		hunger -= rate.FOOD.fac;
		energy -= rate.ENERGY.fac / nature.getEnergy();

		xp += 0.1f * tier.getTrainability();
		KGotchiDAO.saveKawaigotchi(this);
	}

	public void resurrect() {
		alive = true;
		health = 100;
		hunger = 100;
		mood = (int) (50 * nature.getKindness());
		energy = 100;
		xp /= 2;
		KGotchiDAO.saveKawaigotchi(this);
	}

	public Action feed(Food food) {
		if (food.getType() == FoodType.SPECIAL) {
			return useFood(food);
		} else if (stance.canEat()) {
			return useFood(food);
		} else return Action.UNABLE;
	}

	@NotNull
	public Action useFood(Food food) {
		try {
			useFromBag(food);
			hunger += food.getNutrition();
			health += food.getHealthiness();
			mood += food.getMoodBoost();
			if (food.getType() == FoodType.SPECIAL) food.getSpecial().accept(this);
			KGotchiDAO.saveKawaigotchi(this);
			return Action.SUCCESS;
		} catch (EmptyStockException e) {
			return Action.FAILED;
		}
	}

	public Action play() {
		if (stance.canPlay()) {
			int threshold = (int) ((Helper.clamp(100 - (int) health, 10, 40)) / nature.getKindness());
			lastRoll = Helper.rng(100);

			if (lastRoll >= threshold) {
				mood += Helper.clamp(Helper.rng(10), 3, 10) * nature.getKindness();
				energy -= Helper.clamp(Helper.rng(6), 1, 6);
				hunger -= Helper.clamp(Helper.rng(6), 1, 6);

				KGotchiDAO.saveKawaigotchi(this);
				return Action.SUCCESS;
			} else {
				energy -= Helper.clamp(Helper.rng(6), 1, 6);
				hunger -= Helper.clamp(Helper.rng(6), 1, 6);

				KGotchiDAO.saveKawaigotchi(this);
				return Action.FAILED;
			}
		} else return Action.UNABLE;
	}

	public Action train() {
		if (stance.canTrain()) {
			int threshold = (int) ((Helper.clamp(100 - (int) mood, 10, 40)) / nature.getTrainability());
			lastRoll = Helper.rng(100);

			if (lastRoll >= threshold) {
				xp += Helper.clamp(Helper.rng(6), 1, 6) * tier.getTrainability();
				energy -= Helper.clamp(Helper.rng(10), 3, 10);
				hunger -= Helper.clamp(Helper.rng(10), 3, 10);

				KGotchiDAO.saveKawaigotchi(this);
				return Action.SUCCESS;
			} else {
				energy -= Helper.clamp(Helper.rng(10), 3, 10);
				hunger -= Helper.clamp(Helper.rng(10), 3, 10);

				KGotchiDAO.saveKawaigotchi(this);
				return Action.FAILED;
			}
		} else return Action.UNABLE;
	}

	public MessageAction view(TextChannel channel) throws IOException {
		BufferedImage scn = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
		BufferedImage pet = race.extract(stance, skin);
		int currTime = OffsetDateTime.now(ZoneId.of("GMT-3")).getHour();
		int dir = (Math.random() > 0.5 ? 1 : -1);

		if (stance != Stance.SLEEPING && stance != Stance.DEAD)
			pos = pet.getWidth() + new Random().nextInt(1280 - pet.getWidth() * 2);
		else {
			pos = 256;
			dir = 1;
		}

		Graphics2D g2d = scn.createGraphics();

		g2d.drawImage(Time.getParallax()[0], 0, 0, null);
		g2d.drawImage(Time.getParallax()[1], 0, 0, null);
		g2d.drawImage(Time.getParallax()[2], 0, 0, null);

		String desc = name + " | " + tier.toString() + " " + race.toString().toLowerCase() + " " + nature.toString().toLowerCase();

		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 30));

		g2d.setColor(new Color(89, 44, 27, 150));

		g2d.fillRoundRect(5, 15, g2d.getFontMetrics().stringWidth(desc) + 15, 50, 25, 25);
		assert stance.toString() != null;
		g2d.fillRoundRect(5, 70, g2d.getFontMetrics().stringWidth(stance.toString()) + 15, 50, 25, 25);
		g2d.fillRoundRect(g2d.getFontMetrics().stringWidth(stance.toString()) + 25, 70, g2d.getFontMetrics().stringWidth(currTime + "h") + 15, 50, 25, 25);

		g2d.setColor(Color.red);
		g2d.fillRoundRect((int) (1070 + (150 - (150 * health / 100))), 30, (int) (150 * health / 100), 20, 15, 15);

		g2d.setColor(new Color(127, 62, 39));
		g2d.fillRoundRect((int) (1070 + (150 - (150 * hunger / 100))), 85, (int) (150 * hunger / 100), 20, 15, 15);

		g2d.setColor(Color.green);
		g2d.fillRoundRect((int) (1070 + (150 - (150 * energy / 100))), 140, (int) (150 * energy / 100), 20, 15, 15);

		g2d.setColor(Color.yellow);
		g2d.fillRoundRect((int) (1070 + (150 - (150 * mood / 100))), 195, (int) (150 * mood / 100), 20, 15, 15);

		g2d.setColor(Color.black);
		g2d.setStroke(new BasicStroke(2));

		g2d.drawRoundRect(5, 15, g2d.getFontMetrics().stringWidth(desc) + 15, 50, 25, 25);
		g2d.drawRoundRect(5, 70, g2d.getFontMetrics().stringWidth(stance.toString()) + 15, 50, 25, 25);
		g2d.drawRoundRect(g2d.getFontMetrics().stringWidth(stance.toString()) + 25, 70, g2d.getFontMetrics().stringWidth(currTime + "h") + 15, 50, 25, 25);

		g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("health.png"))).getImage(), 1225, 15, null);
		g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("food.png"))).getImage(), 1225, 70, null);
		g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("energy.png"))).getImage(), 1225, 125, null);
		g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mood.png"))).getImage(), 1225, 180, null);

		g2d.drawRoundRect(1070, 30, 150, 20, 15, 15);
		g2d.drawRoundRect(1070, 85, 150, 20, 15, 15);
		g2d.drawRoundRect(1070, 140, 150, 20, 15, 15);
		g2d.drawRoundRect(1070, 195, 150, 20, 15, 15);

		drawOutlinedText(desc, 13, 50, g2d);
		drawOutlinedText(stance.toString(), 13, 105, g2d);
		drawOutlinedText(currTime + "h", g2d.getFontMetrics().stringWidth(stance.toString()) + 33, 105, g2d);

		//GROUND = 108
		g2d.drawImage(pet, pos, scn.getHeight() - (tier == Tier.CHILD ? pet.getHeight(null) / 3 : tier == Tier.TEEN ? pet.getHeight(null) / 2 : pet.getHeight(null)) - 108, dir * (tier == Tier.CHILD ? pet.getWidth(null) / 3 : tier == Tier.TEEN ? pet.getWidth(null) / 2 : pet.getWidth(null)), (tier == Tier.CHILD ? pet.getHeight(null) / 3 : tier == Tier.TEEN ? pet.getHeight(null) / 2 : pet.getHeight(null)), null);

		g2d.dispose();

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(scn, "png", baos);
			baos.flush();
			return channel.sendFile(baos.toByteArray(), "file.png");
		}
	}

	private static void drawOutlinedText(String s, int x, int y, Graphics2D g2d) {
		AffineTransform transform = g2d.getTransform();
		transform.translate(x, y);
		g2d.transform(transform);
		makeOutline(s, g2d);
		transform.translate(-x, -y);
		g2d.setTransform(transform);
	}

	private static void makeOutline(String s, Graphics2D g2d) {
		g2d.setColor(Color.black);
		FontRenderContext frc = g2d.getFontRenderContext();
		TextLayout tl = new TextLayout(s, g2d.getFont(), frc);
		Shape shape = tl.getOutline(null);
		g2d.setStroke(new BasicStroke(4));
		g2d.draw(shape);
		g2d.setColor(Color.white);
		g2d.fill(shape);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Nature getNature() {
		return nature;
	}

	public void setNature(Nature nature) {
		this.nature = nature;
	}

	public int getSkin() {
		return skin;
	}

	public void setSkin(int skin) {
		this.skin = skin;
	}

	public Stance getStance() {
		return stance;
	}

	public void setStance(Stance stance) {
		this.stance = stance;
	}

	public Race getRace() {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	public Tier getTier() {
		return tier;
	}

	public void setTier(Tier tier) {
		this.tier = tier;
	}

	public float getHunger() {
		return hunger;
	}

	public void setHunger(float hunger) {
		this.hunger = hunger;
	}

	public float getHealth() {
		return health;
	}

	public void setHealth(float health) {
		this.health = health;
	}

	public float getMood() {
		return mood;
	}

	public void setMood(float mood) {
		this.mood = mood;
	}

	public float getEnergy() {
		return energy;
	}

	public void setEnergy(float energy) {
		this.energy = energy;
	}

	public float getXp() {
		return xp;
	}

	public void setXp(float xp) {
		this.xp = xp;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	public String getBag() {
		return bag;
	}

	public void setBag(String bag) {
		this.bag = bag;
	}

	public boolean isAlerted() {
		return alerted;
	}

	public void setAlerted(boolean alerted) {
		this.alerted = alerted;
	}

	public int getLastRoll() {
		return lastRoll;
	}

	public void setLastRoll(int lastRoll) {
		this.lastRoll = lastRoll;
	}

	public void addToBag(Food f) {
		JSONObject jo = new JSONObject(bag);

		int qtd = jo.has(f.getIdentifier()) ? jo.getInt(f.getIdentifier()) : 0;

		jo.put(f.getIdentifier(), qtd + 1);

		bag = jo.toString();
	}

	public void addToBag(Food f, int qtd) {
		JSONObject jo = new JSONObject(bag);

		int qtdAtual = jo.has(f.getIdentifier()) ? jo.getInt(f.getIdentifier()) : 0;

		jo.put(f.getIdentifier(), qtdAtual + qtd);

		bag = jo.toString();
	}

	private void useFromBag(Food f) {
		JSONObject jo = new JSONObject(bag);

		if (!jo.has(f.getIdentifier()) || jo.getInt(f.getIdentifier()) == 0) throw new EmptyStockException();

		jo.put(f.getIdentifier(), jo.getInt(f.getIdentifier()) - 1);

		bag = jo.toString();
	}

	public int getLastMoodRoll() {
		try {
			return (int) (Helper.clamp(lastRoll * 100 / 10, 3, 10) * nature.getKindness());
		} finally {
			lastRoll = 0;
		}
	}

	public int getLastResourceRoll(boolean train) {
		try {
			if (train) return (int) (Helper.clamp(lastRoll * 100 / 6, 1, 5) / 3f);
			else return (int) (Helper.clamp(lastRoll * 100 / 6, 1, 5) / 2f);
		} finally {
			lastRoll = 0;
		}
	}

	public int getLastXpRoll() {
		try {
			return (int) (Helper.clamp(lastRoll * 100 / 6, 1, 5) * tier.getTrainability());
		} finally {
			lastRoll = 0;
		}
	}

	public void doNothing() {
	}
}
