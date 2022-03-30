package com.kuuhaku.utils.converters;

import com.kuuhaku.utils.json.JSONUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Converter
public abstract class ListConverter<T> implements AttributeConverter<List<T>, String> {
    private final Class<T> klass;
    private final Function<Object, T> converter;

    public ListConverter(Class<T> klass) {
        this.klass = klass;
        this.converter = null;
    }

    public ListConverter(Class<T> klass, Function<Object, T> converter) {
        this.klass = klass;
        this.converter = converter;
    }

    @Override
    public String convertToDatabaseColumn(List<T> list) {
        return JSONUtils.toJSON(list);
    }

    @Override
    public List<T> convertToEntityAttribute(String string) {
        return JSONUtils.toList(string).stream()
                .map(Objects.requireNonNullElseGet(converter, () -> klass::cast))
                .collect(Collectors.toList());
    }
}