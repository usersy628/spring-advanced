package org.example.expert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminApiLoggingAspect {

	private static final Logger log = LoggerFactory.getLogger(AdminApiLoggingAspect.class);

	private final ObjectMapper objectMapper;

	@Pointcut(
		"execution(* org.example.expert.domain.comment.controller.CommentAdminController.deleteComment(..))"
			+ " || execution(* org.example.expert.domain.user.controller.UserAdminController.changeUserRole(..))"
	)
	public void adminApiPointcut() {
	}

	@Around("adminApiPointcut()")
	public Object logAdminApi(ProceedingJoinPoint joinPoint) throws Throwable {
		HttpServletRequest request = getCurrentRequest();

		Object userId = request.getAttribute("userId");
		String url = getFullUrl(request);
		LocalDateTime requestedAt = LocalDateTime.now();

		Object requestBody = extractRequestBody(joinPoint);

		log.info(
			"[ADMIN_API_REQUEST] userId={}, requestedAt={}, url={}, requestBody={}",
			userId,
			requestedAt,
			url,
			toJson(requestBody)
		);

		try {
			Object result = joinPoint.proceed();

			log.info(
				"[ADMIN_API_RESPONSE] userId={}, requestedAt={}, url={}, responseBody={}",
				userId,
				requestedAt,
				url,
				toJson(extractResponseBody(result))
			);

			return result;
		} catch (Throwable e) {
			log.warn(
				"[ADMIN_API_ERROR] userId={}, requestedAt={}, url={}, requestBody={}, error={}",
				userId,
				requestedAt,
				url,
				toJson(requestBody),
				e.getMessage(),
				e
			);

			throw e;
		}
	}

	private HttpServletRequest getCurrentRequest() {
		ServletRequestAttributes attributes =
			(ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

		return attributes.getRequest();
	}

	private Object extractRequestBody(ProceedingJoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();

		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		Object[] args = joinPoint.getArgs();

		for (int i = 0; i < parameterAnnotations.length; i++) {
			for (Annotation annotation : parameterAnnotations[i]) {
				if (annotation instanceof RequestBody) {
					return args[i];
				}
			}
		}

		return null;
	}

	private Object extractResponseBody(Object result) {
		if (result instanceof ResponseEntity<?> responseEntity) {
			return responseEntity.getBody();
		}

		return result;
	}

	private String toJson(Object value) {
		if (value == null) {
			return "null";
		}

		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String getFullUrl(HttpServletRequest request) {
		String queryString = request.getQueryString();

		if (queryString == null || queryString.isBlank()) {
			return request.getRequestURI();
		}

		return request.getRequestURI() + "?" + queryString;
	}
}