package com.kuuhaku.utils;

import com.kuuhaku.model.enums.I18n;
import io.github.furstenheim.CopyDown;
import net.dv8tion.jda.api.Permission;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.File;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.Permission.*;

public abstract class Constants {
	public static final long START_TIME = System.currentTimeMillis();
	public static final boolean USE_BUTTONS = true;
	public static final String DEFAULT_PREFIX = "s!";

	public static final String RESOURCES_URL = "https://raw.githubusercontent.com/OtagamerZ/ShiroJBot/master/src/main/resources";
	public static final String USATAN_AVATAR = RESOURCES_URL + "/avatar/usa-tan/%s.png";
	public static final String NERO_AVATAR = RESOURCES_URL + "/avatar/nero/%s.png";
	public static final String STEPHANIE_AVATAR = RESOURCES_URL + "/avatar/stephanie/%s.png";
	public static final String TET_AVATAR = RESOURCES_URL + "/avatar/tet/%s.png";
	public static final String JIBRIL_AVATAR = RESOURCES_URL + "/avatar/jibril/%s.png";
	public static final String SHIRO_AVATAR = RESOURCES_URL + "/avatar/shiro/%s.png";
	public static final String GIFS_URL = "https://raw.githubusercontent.com/OtagamerZ/ShoukanAssets/master/gifs";
	public static final String SITE_ROOT = "https://" + System.getenv("SERVER_URL");
	public static final String API_ROOT = "https://api." + System.getenv("SERVER_URL");
	public static final String COLLECTION_ENDPOINT = API_ROOT + "/collection.jpg?id=%s";
	public static final String IMAGE_ENDPOINT = API_ROOT + "/image?id=%s";
	public static final String SOCKET_ROOT = "wss://socket." + System.getenv("SERVER_URL");

	public static final String VOID = "\u200B";
	public static final String CANCEL = "❎";
	public static final String ACCEPT = "✅";
	public static final String ANTICOPY = "\uFFF8"; //or U+034F

	public static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;
	public static final int CANVAS_SIZE = 2049;
	public static final CopyDown HTML_CONVERTER = new CopyDown();

	public static final int BASE_CARD_PRICE = 300;
	public static final int BASE_EQUIPMENT_PRICE = 2250;
	public static final int BASE_FIELD_PRICE = 35000;

	public static final String TIMESTAMP = "<t:%s:R>";
	public static final DateTimeFormatter FULL_DATE_FORMAT = DateTimeFormatter.ofPattern(I18n.getString("full-date-format"));
	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(I18n.getString("date-format"));

	public static final long MILLIS_IN_DAY = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
	public static final long MILLIS_IN_HOUR = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
	public static final long MILLIS_IN_MINUTE = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
	public static final long MILLIS_IN_SECOND = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);
	public static final long ALL_MUTE_PERMISSIONS = Permission.getRaw(
			MESSAGE_ADD_REACTION, MESSAGE_WRITE, MESSAGE_TTS,
			MESSAGE_MANAGE, MESSAGE_EMBED_LINKS, MESSAGE_ATTACH_FILES,
			MESSAGE_MENTION_EVERYONE, USE_SLASH_COMMANDS
	);

	public static final Random DEFAULT_RNG = new Random();
	public static final Random DEFAULT_SECURE_RNG = new SecureRandom();

	public static final File COLLECTIONS_FOLDER = new File(System.getenv("COLLECTIONS_PATH"));
	public static final File TEMPORARY_FOLDER = new File(System.getenv("TEMPORARY_PATH"));

	public static final CloseableHttpClient DEFAULT_HTTP = HttpClients.custom().setDefaultHeaders(List.of(
			new BasicHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
	)).build();
}
