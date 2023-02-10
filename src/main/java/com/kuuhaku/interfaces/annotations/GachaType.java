package com.kuuhaku.interfaces.annotations;

import com.kuuhaku.model.enums.Currency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Month;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GachaType {
	String value();
	int price();
	Currency currency();
	int prizes() default 3;
	Month[] months() default {};
}
