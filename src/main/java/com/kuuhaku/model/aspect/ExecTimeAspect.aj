package com.kuuhaku.model.aspect;

import com.kuuhaku.utils.helpers.MiscHelper;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.Level;

public aspect ExecTimeAspect {
	pointcut point(): @annotation(com.kuuhaku.model.annotations.ExecTime) && execution(* *(..));

	Object around(): point() {
		StopWatch watch = new StopWatch();
		watch.start();

		try {
			return proceed();
		} finally {
			watch.stop();
			MiscHelper.logger(thisJoinPoint.getTarget().getClass()).log(
					Level.getLevel("ASPECT"),
					"Method %s executed in %sms".formatted(
							thisJoinPoint.getSignature().getName(),
							watch.getTime()
					)
			);
		}
	}
}
