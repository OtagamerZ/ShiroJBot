package com.kuuhaku.model.records;

import com.kuuhaku.model.enums.I18N;

public record NotificationChannel(String guild, String channel, I18N locale) {
}
