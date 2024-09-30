package com.kuuhaku.model.records.dunhun;

import java.util.List;

public record EventDescription(String description, List<EventAction> actions) {
}
