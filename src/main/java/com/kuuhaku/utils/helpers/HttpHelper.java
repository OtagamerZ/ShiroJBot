package com.kuuhaku.utils.helpers;

import com.kuuhaku.Main;
import com.kuuhaku.model.common.Extensions;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.json.JSONObject;
import com.squareup.moshi.JsonDataException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class HttpHelper {
	public static boolean findURL(String text) {
		Map<String, String> leetSpeak = Map.of(
				"(1|!)", "i",
				"3", "e",
				"4", "a",
				"5", "s",
				"7", "t",
				"0", "o",
				"(@|#|$|%|&|*)", "."
		);

		final Pattern urlPattern = Pattern.compile(
				"(((ht|f)tp|ws)(s?)://|www\\.)([\\w\\-]+\\.)+?([\\w\\-.~]+/?)*[\\w.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*?",
				Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
		text = StringUtils.deleteWhitespace(text);
		text = MiscHelper.replaceWith(text, leetSpeak);

		final Matcher msg = urlPattern.matcher(text.toLowerCase(Locale.ROOT));
		return msg.find() && Extensions.checkExtension(text);
	}

	public static boolean isUrl(String text) {
		return new UrlValidator().isValid(text);
	}


	public static JSONObject post(String endpoint, JSONObject payload, String token) {
		try {
			HttpPost req = new HttpPost(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			req.setEntity(new StringEntity(payload.toString()));

			URI uri = ub.build();

			req.setHeaders(new Header[]{
					new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8"),
					new BasicHeader(HttpHeaders.AUTHORIZATION, token)
			});
			req.setURI(uri);

			try (CloseableHttpResponse res = Constants.DEFAULT_HTTP.execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject post(String endpoint, JSONObject payload, Map<String, String> headers) {
		try {
			HttpPost req = new HttpPost(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			req.setEntity(new StringEntity(payload.toString()));

			URI uri = ub.build();

			req.setHeaders(headers.entrySet().parallelStream()
					.map(e -> new BasicHeader(e.getKey(), e.getValue()))
					.toArray(Header[]::new)
			);
			req.setURI(uri);

			try (CloseableHttpResponse res = Constants.DEFAULT_HTTP.execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject post(String endpoint, JSONObject payload, Map<String, String> headers, String token) {
		try {
			HttpPost req = new HttpPost(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			req.setEntity(new StringEntity(payload.toString()));

			URI uri = ub.build();

			req.setHeaders(headers.entrySet().parallelStream()
					.map(e -> new BasicHeader(e.getKey(), e.getValue()))
					.toArray(Header[]::new)
			);
			req.setHeader(HttpHeaders.AUTHORIZATION, token);
			req.setURI(uri);

			try (CloseableHttpResponse res = Constants.DEFAULT_HTTP.execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject post(String endpoint, String payload, Map<String, String> headers, String token) {
		try {
			HttpPost req = new HttpPost(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			req.setEntity(new StringEntity(payload));

			URI uri = ub.build();

			req.setHeaders(headers.entrySet().parallelStream()
					.map(e -> new BasicHeader(e.getKey(), e.getValue()))
					.toArray(Header[]::new)
			);
			req.setHeader(HttpHeaders.AUTHORIZATION, token);
			req.setURI(uri);

			try (CloseableHttpResponse res = Constants.DEFAULT_HTTP.execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject get(String endpoint, JSONObject payload) {
		try {
			HttpGet req = new HttpGet(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			for (Map.Entry<String, Object> params : payload.entrySet()) {
				ub.setParameter(params.getKey(), String.valueOf(params.getValue()));
			}

			URI uri = ub.build();

			req.setHeaders(new Header[]{
					new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8")
			});
			req.setURI(uri);

			try (CloseableHttpResponse res = Constants.DEFAULT_HTTP.execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject get(String endpoint, JSONObject payload, String token) {
		try {
			HttpGet req = new HttpGet(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			for (Map.Entry<String, Object> params : payload.entrySet()) {
				ub.setParameter(params.getKey(), String.valueOf(params.getValue()));
			}

			URI uri = ub.build();

			req.setHeaders(new Header[]{
					new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT, "application/json"),
					new BasicHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8"),
					new BasicHeader(HttpHeaders.AUTHORIZATION, token)
			});
			req.setURI(uri);

			try (CloseableHttpResponse res = Constants.DEFAULT_HTTP.execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static JSONObject get(String endpoint, JSONObject payload, Map<String, String> headers, String token) {
		try {
			HttpGet req = new HttpGet(endpoint);
			URIBuilder ub = new URIBuilder(req.getURI());

			for (Map.Entry<String, Object> params : payload.entrySet()) {
				ub.setParameter(params.getKey(), String.valueOf(params.getValue()));
			}

			URI uri = ub.build();

			req.setHeaders(headers.entrySet().parallelStream()
					.map(e -> new BasicHeader(e.getKey(), e.getValue()))
					.toArray(Header[]::new)
			);
			req.addHeader(HttpHeaders.AUTHORIZATION, token);
			req.setURI(uri);

			try (CloseableHttpResponse res = Constants.DEFAULT_HTTP.execute(req)) {
				HttpEntity ent = res.getEntity();

				if (ent != null)
					return new JSONObject(EntityUtils.toString(ent));
				else
					return new JSONObject();
			}
		} catch (JsonDataException | IllegalStateException | URISyntaxException | IOException e) {
			return new JSONObject();
		}
	}

	public static String urlEncode(JSONObject payload) {
		String[] params = payload.entrySet().stream()
				.map(e -> e.getKey() + "=" + e.getValue())
				.toArray(String[]::new);

		return String.join("&", params);
	}

	public static String getFileType(String url) {
		try {
			HttpHead req = new HttpHead(url);

			try (CloseableHttpResponse res = Constants.DEFAULT_HTTP.execute(req)) {
				return res.getFirstHeader("Content-Type").getValue();
			}
		} catch (JsonDataException | IllegalStateException | IOException e) {
			return null;
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static String serveImage(byte[] bytes) {
		String hash = StringHelper.hash(System.currentTimeMillis() + StringHelper.hash(bytes, "MD5"), "MD5");
		File f = new File(Main.getInfo().getTemporaryFolder(), hash);

		try {
			f.createNewFile();
			FileUtils.writeByteArrayToFile(f, bytes);
			return Constants.IMAGE_ENDPOINT.formatted(hash);
		} catch (IOException e) {
			return null;
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static String serveImage(byte[] bytes, String hash) {
		File f = new File(Main.getInfo().getTemporaryFolder(), hash);

		try {
			f.createNewFile();
			FileUtils.writeByteArrayToFile(f, bytes);
			return Constants.IMAGE_ENDPOINT.formatted(hash);
		} catch (IOException e) {
			return null;
		}
	}
}
