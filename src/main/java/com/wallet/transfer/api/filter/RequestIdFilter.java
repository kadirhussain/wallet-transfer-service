package com.wallet.transfer.api.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class RequestIdFilter  implements Filter {

    private static final String HEADER ="X-Request-ID";
    private static final String MDC_KEY = "requestId";


    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest hReq = (HttpServletRequest)  req;
        HttpServletResponse hRes = (HttpServletResponse) res;

        String rid = hReq.getHeader(HEADER);

        if (rid == null || rid.isBlank())
            rid = UUID.randomUUID().toString();

        MDC.put(MDC_KEY, rid);

        hRes.setHeader(HEADER, rid);

        try {
            chain.doFilter(req, res);
        }
        finally {
            MDC.remove(MDC_KEY);
        }

    }

}
