package com.kaii.dentix.global.common.error;

import com.kaii.dentix.global.common.error.exception.*;
import com.kaii.dentix.global.common.response.ResponseMessage;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 잘못된 요청
     * HttpStatus 400
     */
    @ExceptionHandler(BadRequestApiException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse BadRequestApiException(HttpServletRequest request, BadRequestApiException e) {
        log.info("error : ", e);
        if (e.getMessage().isEmpty()) {
            return ErrorResponse.of(HttpStatus.BAD_REQUEST, ResponseMessage.BAD_REQUEST_MSG);
        } else {
            return ErrorResponse.of(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    
    /**
     * 인증 실패
     * HttpStatus 401
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse UnauthorizedException(HttpServletRequest request, UnauthorizedException e) {
        //파일 다운로드 요청 중 예외 발생 시 JSON으로 응답하지 않음
        if (request.getRequestURI().contains("/bulk-upload/template")) {
            return null;
        }
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.UNAUTHORIZED, StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : ResponseMessage.UNAUTHORIZED_MSG);
    }
    
    /**
     * 권한 없음
     * HttpStatus 403
     */
    @ExceptionHandler(TokenExpiredException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse TokenExpiredException(HttpServletRequest request, TokenExpiredException e) {
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.FORBIDDEN, ResponseMessage.FORBIDDEN_MSG);
    }
    
    /**
     * 허용되지 않는 방법(Request Method - GET, POST, PUT, DELETE)
     * HttpStatus 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse HttpRequestMethodNotSupportedException(HttpServletRequest request, HttpRequestMethodNotSupportedException e) {
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED, ResponseMessage.METHOD_NOT_ALLOWED_MSG);
    }

    /**
     * Input Request 실패(HttpMessageNotReadable)
     * HttpStatus 405
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse HttpMessageNotReadableException(HttpServletRequest request, HttpMessageNotReadableException e) {
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED, ResponseMessage.HTTP_MESSAGE_NOT_READABLE_MSG.replace("{msg}", Objects.requireNonNull(e.getMessage())));
    }

    /**
     * 버전 정보 오류
     * HttpStatus 406
     */
    @ExceptionHandler(RequiredVersionInfoException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse RequiredVersionInfoException(HttpServletRequest request, RequiredVersionInfoException e) {
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.NOT_ACCEPTABLE, ResponseMessage.REQUIRED_VERSION_INFO_MSG);
    }

    /**
     * 데이터 조회 실패 (데이터 조회 실패로 인한 처리 불가, 저장/수정/삭제 실패)
     * HttpStatus 416
     */
    @ExceptionHandler(NotFoundDataException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse NotFoundDataException(HttpServletRequest request, NotFoundDataException e) {
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, e.getMessage());
    }
    
    /**
     * Validation 실패 (RequestBody)
     * HttpStatus 417
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse MethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e) {
        log.info("error : ", e);

        BindingResult bindingResult = e.getBindingResult();

        FieldError fieldError = null;
        // @NotBlank, @NotNull, @Size, @Pattern 어노테이션 순으로 유효성 검사 체크
        List<String> errorCase = List.of("NotBlank", "NotNull", "Size", "Pattern");

        for (String errorCode : errorCase) {
            fieldError = bindingResult.getFieldErrors().stream()
                    .filter(error -> Objects.equals(error.getCode(), errorCode))
                    .findFirst()
                    .orElse(null);

            if (fieldError != null) {
                break;
            }
        }

        String message = (fieldError != null) ? fieldError.getDefaultMessage() : bindingResult.getFieldError().getField() + "의 형식이 올바르지 않습니다.";

        return ErrorResponse.of(HttpStatus.EXPECTATION_FAILED, message);
    }
    
    /**
     * Validation 실패 (RequestParam)
     * HttpStatus 417
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse MissingServletRequestParameterException(HttpServletRequest request, MissingServletRequestParameterException e) {
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.EXPECTATION_FAILED, ResponseMessage.EXPECTATION_FAILED_MSG.replace("{FieldName}", e.getParameterName()));
    }

    /**
     * Validation 실패 (multipart/form-data)
     * HttpStatus 417
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse MissingServletRequestPartException(HttpServletRequest request, MissingServletRequestPartException e) {
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.EXPECTATION_FAILED, ResponseMessage.EXPECTATION_FAILED_MSG.replace("{FieldName}", e.getRequestPartName()));
    }
    
    /**
     * Validation 실패 (ModelAttribute)
     * HttpStatus 417
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse BindException(HttpServletRequest request, BindException e) {
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.EXPECTATION_FAILED, ResponseMessage.EXPECTATION_FAILED_MSG.replace("{FieldName}", e.getFieldError().getField()));
    }
    
    /**
     * Validation 실패 (formValidation)
     * HttpStatus 417
     */
    @ExceptionHandler(FormValidationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse FormValidationException(HttpServletRequest request, FormValidationException e) {
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.EXPECTATION_FAILED, e.getMessage());
    }

    /**
     * Validation 실패 (formValidation)
     * HttpStatus 417
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse ValidationException(HttpServletRequest request, ValidationException e) {
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.EXPECTATION_FAILED, e.getMessage());
    }

    /**
     * Validation 실패
     * HttpStatus 417
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse ConstraintViolationException(HttpServletRequest request, ConstraintViolationException e) {
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.EXPECTATION_FAILED, e.getMessage());
    }
    
    /**
     * 처리할 수 없는 엔티티 (이미 존재하는 데이터로 인해 처리 불가, 저장/수정/삭제 실패)
     * HttpStatus 422
     */
    @ExceptionHandler(AlreadyDataException.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse AlreadyDataException(HttpServletRequest request, AlreadyDataException e) {
        log.info("error : ", e);
        if (e.getMessage().isEmpty()) {
            return ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, ResponseMessage.ALREADY_DATA_MSG);
        } else {
            return ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }
    
    /**
     * 알수 없는 오류(내부 서버 오류)
     * httpStatus 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse Exception(HttpServletRequest request, Exception e) {
        log.error("error : " + e);
        log.info("error : ", e);
        return ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, ResponseMessage.INTERNAL_SERVER_ERROR_MSG);
    }
}
