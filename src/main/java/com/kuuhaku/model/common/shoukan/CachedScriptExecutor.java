package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.dunhun.context.ShoukanContext;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import org.intellij.lang.annotations.Language;

import java.util.Map;

public class CachedScriptExecutor {
	private final Drawable<?> owner;
	private final JSONObject context = new JSONObject();
	private final Props storedProps = new Props();

	@Language("Groovy")
	private final String script;

	public CachedScriptExecutor(Drawable<?> owner, JSONObject context, Props props, @Language("Groovy") String script) {
		this.owner = owner;
		this.context.putAll(context);
		this.storedProps.putAll(props);
		this.script = script;
	}

	public CachedScriptExecutor withVar(String key, Object value) {
		context.put(key, value);
		return this;
	}

	public void run() {
		Utils.exec(owner.toString(), script, context);
	}

	@SuppressWarnings("unchecked")
	public ShoukanContext toContext() {
		return new ShoukanContext(
				owner,
				context.get(Trigger.class, "trigger"),
				context.get(EffectParameters.class, "ep"),
				context.get(Shoukan.class, "game"),
				context.get(Senshi.class, "self"),
				context.get(Side.class, "side"),
				context.get(Map.class, "data"),
				storedProps
		);
	}
}
