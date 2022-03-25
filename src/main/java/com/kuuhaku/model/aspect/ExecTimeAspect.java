package com.kuuhaku.model.aspect;

import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.Level;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class ExecTimeAspect {
	@Around("@annotation(com.kuuhaku.model.annotations.ExecTime) && execution(* *(..))")
	public Object measureExecTime(ProceedingJoinPoint pjp) throws Throwable {
		StopWatch watch = new StopWatch();
		watch.start();

		try {
			return pjp.proceed();
		} finally {
			watch.stop();
			Helper.logger(pjp.getTarget().getClass()).log(
					Level.forName("ASPECT", 400),
					"Method %s executed in %sms".formatted(
							pjp.getSignature().getName(),
							watch.getTime()
					)
			);
		}
	}
}
