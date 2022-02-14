package com.kuuhaku.utils.converters;

import javax.persistence.Converter;

@Converter
public class IntListConverter extends ListConverter<Integer> {
    public IntListConverter() {
        super(Integer.class, o -> ((Number) o).intValue());
    }
}