package com.kaii.dentix.global.common.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaii.dentix.domain.errorLog.dao.ErrorLogRepository;
import com.kaii.dentix.domain.errorLog.domain.ErrorLog;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.systemLog.dao.SystemLogRepository;
import com.kaii.dentix.domain.systemLog.domain.SystemLog;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.global.common.aop.dto.LoggerDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@Aspect // AOP 사용
@Component // Bean 으로 사용
@RequiredArgsConstructor
public class LoggerAspect {

	private final ObjectMapper objectMapper;
	private final JwtTokenUtil jwtTokenUtil;
	private final SystemLogRepository systemLogRepository;
	private final ErrorLogRepository errorLogRepository;

	// domain 패키지 하위에 마지막 패키지가 controller인 클래스의 모든 메소드
	@Pointcut("execution(* com.kaii.dentix.domain..controller..*(..))")
	private void cut() {}

	@AfterReturning(value = "cut()", returning = "returnObj")
	public void writeSuccessLog(JoinPoint joinPoint, Object returnObj) throws JsonProcessingException {
		log.info("::: AOP writeSuccessLog Start :::");

		LoggerDTO loggerDTO = getLog(joinPoint);

		systemLogRepository.save(SystemLog.builder()
			.tokenUserId(loggerDTO.getTokenUserId())
			.tokenUserRole(loggerDTO.getTokenUserRole())
			.requestName(loggerDTO.getRequestName())
			.requestUrl(loggerDTO.getRequestUrl())
			.header(loggerDTO.getHeader())
			.requestBody(loggerDTO.getRequestBody())
			.responseBody(serializeForLog(returnObj))
			.build());

		log.info("::: AOP writeSuccessLog End :::");
	}

	@AfterThrowing(value = "cut()", throwing = "exception")
	public void writeFailLog(JoinPoint joinPoint, Exception exception) throws JsonProcessingException {
		log.info("::: AOP writeFailLog Start :::");

		LoggerDTO loggerDTO = getLog(joinPoint);

		errorLogRepository.save(ErrorLog.builder()
			.tokenUserId(loggerDTO.getTokenUserId())
			.tokenUserRole(loggerDTO.getTokenUserRole())
			.requestName(loggerDTO.getRequestName())
			.requestUrl(loggerDTO.getRequestUrl())
			.header(loggerDTO.getHeader())
			.requestBody(loggerDTO.getRequestBody())
			.errorLogMessage(exception.getClass().getSimpleName())
			.build());

		log.info("::: AOP writeFailLog End :::");
	}

	public LoggerDTO getLog(JoinPoint joinPoint) throws JsonProcessingException {

		//  request 정보 추출
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

		// 헤더 정보 추출
		Map<String, String> headers = new HashMap<>();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			headers.put(headerName, request.getHeader(headerName));
		}

		// 토큰 정보 추출
		Long tokenUserId = null;
		UserRole tokenUserRole = null;

		try {
			String header = jwtTokenUtil.getAccessToken(request);
			if (header != null) {
				tokenUserId = jwtTokenUtil.getUserId(header, TokenType.AccessToken);
				tokenUserRole = jwtTokenUtil.getRoles(header, TokenType.AccessToken);
			} else {
				String refreshToken = jwtTokenUtil.getRefreshToken(request);
				if (refreshToken != null && !refreshToken.isBlank()) {
					tokenUserId = jwtTokenUtil.getUserId(refreshToken, TokenType.RefreshToken);
					tokenUserRole = jwtTokenUtil.getRoles(refreshToken, TokenType.RefreshToken);
				}
			}
		} catch (Exception ignored) {}

		// 메소드 Mapping 어노테이션에 걸린 name 값 추출
		String requestName = null;

		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		for (Annotation annotation : methodSignature.getMethod().getAnnotations()) {
			if (annotation instanceof GetMapping) {
				requestName = ((GetMapping) annotation).name(); break;
			} else if (annotation instanceof PostMapping) {
				requestName = ((PostMapping) annotation).name(); break;
			} else if (annotation instanceof PutMapping) {
				requestName = ((PutMapping) annotation).name(); break;
			} else if (annotation instanceof DeleteMapping) {
				requestName = ((DeleteMapping) annotation).name(); break;
			} else if (annotation instanceof PatchMapping) {
				requestName = ((PatchMapping) annotation).name(); break;
			} else if (annotation instanceof RequestMapping) {
				requestName = ((RequestMapping) annotation).name(); break;
			}
		}

		// requestBody 추출
		CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
		String[] parameterNames = codeSignature.getParameterNames();
		Object[] args = joinPoint.getArgs();
		Map<String, Object> requestBody = new HashMap<>();
		for (int i = 0; i < parameterNames.length; i++) {
			if (codeSignature.getParameterTypes()[i].getSimpleName().equals("HttpServletRequest")) continue;

			if (args[i] != null) {
				String str = args[i].toString();
				if (str.contains("org.springframework.web.multipart.support.StandardMultipartHttpServletRequest$StandardMultipartFile")) {
					requestBody.put(parameterNames[i], str.replace("org.springframework.web.multipart.support.StandardMultipartHttpServletRequest$Standard", ""));
					continue;
				}
			}

			requestBody.put(parameterNames[i], args[i]);
		}

		return LoggerDTO.builder()
			.tokenUserId(tokenUserId)
			.tokenUserRole(tokenUserRole)
			.requestName(requestName)
			.requestUrl(request.getRequestURL().toString())
			.header(serializeForLog(headers))
			.requestBody(serializeForLog(requestBody))
			.build();
	}

	private String serializeForLog(Object value) throws JsonProcessingException {
		JsonNode node = objectMapper.valueToTree(value);
		maskSensitiveFields(node);
		return objectMapper.writeValueAsString(node);
	}

	private void maskSensitiveFields(JsonNode node) {
		if (node instanceof ObjectNode objectNode) {
			Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				if (isSensitiveKey(field.getKey())) {
					objectNode.put(field.getKey(), "********");
				} else {
					maskSensitiveFields(field.getValue());
				}
			}
		} else if (node instanceof ArrayNode arrayNode) {
			arrayNode.forEach(this::maskSensitiveFields);
		}
	}

	private boolean isSensitiveKey(String key) {
		String normalized = key == null ? "" : key.replace("_", "").toLowerCase();
		return normalized.equals("authorization")
			|| normalized.equals("credential")
			|| normalized.equals("encrypteddata")
			|| normalized.contains("password")
			|| normalized.endsWith("privatekey")
			|| normalized.endsWith("secret")
			|| normalized.endsWith("token");
	}
}
