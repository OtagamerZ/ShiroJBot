package com.kuuhaku.model.aspects;

import com.kuuhaku.Constants;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.Level;

public aspect ExecTimeAspect {
	pointcut point(): @annotation(com.kuuhaku.interfaces.annotations.ExecTime) && execution(* *(..));

	Object around(): point() {
		StopWatch watch = new StopWatch();
		watch.start();

		try {
			return proceed();
		} finally {
			watch.stop();
			Constants.LOGGER.log(
					Level.getLevel("ASPECT"),
					"Method %s executed in %sms".formatted(
							thisJoinPoint.getSignature().getName(),
							watch.getTime()
					)
			);
		}
	}
}
