package com.sfs.ui.controller;

import com.sfs.ui.view.ErrorViewModel;
import com.sfs.ui.view.NavigationItem;
import com.sfs.ui.view.PageViewModel;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SfsErrorController implements ErrorController {

    private static final Logger LOG = LoggerFactory.getLogger(SfsErrorController.class);

    private static final String VIEW_ERROR = "error";
    private static final String ATTR_PAGE = "page";
    private static final String ATTR_ERROR = "error";

    private static final int DEFAULT_STATUS = 500;

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        int status = resolveStatus(request);
        String path = resolvePath(request);

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
