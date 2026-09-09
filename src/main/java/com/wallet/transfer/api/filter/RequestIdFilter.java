package com.wallet.transfer.api.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
public class RequestIdFilter  implements Filter {

    private static final String HEADER ="X-Request-ID";
    private static final String MDC_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest)  servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        log.info("HTTP Method: {}", httpServletRequest.getMethod());
        log.info("Request URI: {}", httpServletRequest.getRequestURI());
        log.info("Query String: {}", httpServletRequest.getQueryString());
        log.info("Header: {} ", httpServletRequest.getHeader(HEADER));

        String rid = httpServletRequest.getHeader(HEADER);

        if (rid == null || rid.isBlank())
            rid = UUID.randomUUID().toString();

        MDC.put(MDC_KEY, rid);

        log.info("rid: {} ", rid);

        httpServletResponse.setHeader(HEADER, rid);
        log.info("Header 2: {} ", httpServletRequest.getHeader(HEADER));
        String clientIp = httpServletRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp= httpServletRequest.getRemoteAddr();
        }

        log.info("Client IP: {}", clientIp);
        log.info("User-Agent: {}", httpServletRequest.getHeader("User-Agent"));



        try {
            filterChain.doFilter(servletRequest, servletResponse);
        }
        finally {
            MDC.remove(MDC_KEY);
        }

    }

}
