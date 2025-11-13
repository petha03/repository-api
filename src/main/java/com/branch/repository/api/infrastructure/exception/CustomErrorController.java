package com.branch.repository.api.infrastructure.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Custom error controller that handles requests that cannot be mapped to any controller endpoint.
 * Returns RFC 7807 Problem Detail responses with GMT timestamps.
 *
 * <p>This replaces the deprecated `spring.mvc.throw-exception-if-no-handler-found` property.
 * Spring Boot 3.x recommends implementing ErrorController instead of using the deprecated property.
 */
@Controller
public class CustomErrorController implements ErrorController {

    private static final String ERROR_PATH = "/error";

    @RequestMapping(ERROR_PATH)
    @ResponseBody
    public ProblemDetail handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int statusCode = status != null ? Integer.parseInt(status.toString()) : 500;
        HttpStatus httpStatus = HttpStatus.resolve(statusCode);
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String detail = getDetailMessage(httpStatus, errorMessage, requestUri);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(httpStatus, detail);
        problemDetail.setTitle(httpStatus.getReasonPhrase());
        problemDetail.setProperty("timestamp", ZonedDateTime.now(ZoneId.of("GMT")));

        return problemDetail;
    }

    private String getDetailMessage(HttpStatus httpStatus, Object errorMessage, Object requestUri) {
        return switch (httpStatus) {
            case NOT_FOUND -> "The requested resource was not found" +
                    (requestUri != null ? ": " + requestUri : "");
            case METHOD_NOT_ALLOWED -> "HTTP method not allowed for this endpoint";
            case UNSUPPORTED_MEDIA_TYPE -> "Unsupported media type";
            case NOT_ACCEPTABLE -> "Requested media type not acceptable";
            default -> errorMessage != null ? errorMessage.toString() : "An error occurred";
        };
    }
}