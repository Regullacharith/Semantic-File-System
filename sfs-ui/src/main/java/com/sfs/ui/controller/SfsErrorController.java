package com.sfs.ui.controller;

import com.sfs.app.api.error.ApiErrorCode;
import com.sfs.app.api.error.ApiErrorResponse;
import com.sfs.ui.view.ErrorViewModel;
import com.sfs.ui.view.NavigationItem;
import com.sfs.ui.view.PageViewModel;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SfsErrorController implements ErrorController {

    private static final Logger LOG = LoggerFactory.getLogger(SfsErrorController.class);

    private static final String VIEW_ERROR = "error";
    private static final String ATTR_PAGE = "page";
    private static final String ATTR_ERROR = "error";

    private static final int DEFAULT_STATUS = 500;

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, Model model) {
        int status = resolveStatus(request);
        String path = resolvePath(request);

        if (path != null && path.startsWith("/api/")) {
            return apiError(status, path);
        }

        ErrorViewModel error = ErrorViewModel.forStatus(status, path);

        if (error.isServerError()) {
            LOG.error("Request failed with status {} for path {}", status, path,
                    (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION));
        } else {
            LOG.warn("Request refused with status {} for path {}", status, path);
        }

        model.addAttribute(ATTR_PAGE, PageViewModel.of(error.title(), NavigationItem.HOME));
        model.addAttribute(ATTR_ERROR, error);

        return VIEW_ERROR;
    }

    @ResponseBody
    private ResponseEntity<ApiErrorResponse> apiError(int status, String path) {
        ApiErrorCode code = switch (status) {
            case 400 -> ApiErrorCode.REQUEST_MALFORMED;
            case 404 -> ApiErrorCode.FILE_NOT_FOUND;
            case 405 -> ApiErrorCode.METHOD_NOT_ALLOWED;
            case 413 -> ApiErrorCode.PAYLOAD_TOO_LARGE;
            case 415 -> ApiErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> status >= 500 ? ApiErrorCode.INTERNAL_ERROR : ApiErrorCode.VALIDATION_FAILED;
        };

        String message = switch (status) {
            case 404 -> "No such API operation.";
            case 405 -> "That method is not allowed for this operation.";
            default -> "The request could not be completed.";
        };

        LOG.warn("API request failed with status {} for path {}", status, path);

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiErrorResponse.of(code, message, path));
    }

    private int resolveStatus(HttpServletRequest request) {
        Object raw = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (raw instanceof Integer code && code >= 400 && code <= 599) {
            return code;
        }
        return DEFAULT_STATUS;
    }

    private String resolvePath(HttpServletRequest request) {
        Object raw = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return raw instanceof String uri ? uri : null;
    }
}
