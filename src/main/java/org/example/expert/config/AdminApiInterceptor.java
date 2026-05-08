package org.example.expert.config;

import java.time.LocalDateTime;

import org.example.expert.domain.comment.controller.CommentAdminController;
import org.example.expert.domain.user.controller.UserAdminController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminApiInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(AdminApiInterceptor.class);

	@Override
	public boolean preHandle(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull Object handler
	) {
		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}

		if (!isTargetAdminApi(handlerMethod)) {
			return true;
		}

		Object userId = request.getAttribute("userId");
		Object userRole = request.getAttribute("userRole");

		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
		}

		if (!isAdmin(userRole)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 없습니다.");
		}

		log.info(
			"[ADMIN_ACCESS] requestedAt={}, userId={}, url={}",
			LocalDateTime.now(),
			userId,
			getFullUrl(request)
		);

		return true;
	}

	private boolean isTargetAdminApi(HandlerMethod handlerMethod) {
		Class<?> controllerClass = handlerMethod.getBeanType();
		String methodName = handlerMethod.getMethod().getName();

		return controllerClass.equals(CommentAdminController.class)
			&& methodName.equals("deleteComment")
			|| controllerClass.equals(UserAdminController.class)
			&& methodName.equals("changeUserRole");
	}

	private boolean isAdmin(Object userRole) {
		if (userRole == null) {
			return false;
		}

		String role = userRole.toString();
		return "ADMIN".equals(role) || "ROLE_ADMIN".equals(role);
	}

	private String getFullUrl(HttpServletRequest request) {
		String queryString = request.getQueryString();

		if (queryString == null || queryString.isBlank()) {
			return request.getRequestURI();
		}

		return request.getRequestURI() + "?" + queryString;
	}
}