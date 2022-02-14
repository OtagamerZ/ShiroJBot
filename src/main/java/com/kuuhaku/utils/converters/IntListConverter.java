package com.kuuhaku.utils.converters;

import javax.persistence.Converter;

@Converter
public class IntListConverter extends ListConverter<Integer> {
    public IntListConverter() {
        super(Integer.class, o -> o == null ? null : ((Number) o).intValue());
    }
}