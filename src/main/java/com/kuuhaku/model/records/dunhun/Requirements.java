package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public record Requirements(
		@Column(name = "req_level", nullable = false)
		int level,
		@Embedded
		@AttributeOverride(name = "attributes", column = @Column(name = "req_attributes", nullable = false))
		Attributes attributes,
		@JdbcTypeCode(SqlTypes.JSON)
		@Column(name = "req_tags", nullable = false, columnDefinition = "JSONB")
		@Convert(converter = JSONArrayConverter.class)
		JSONArray tags
) {
}
