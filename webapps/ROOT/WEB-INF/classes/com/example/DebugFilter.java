package com.example;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class DebugFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("[DebugFilter] init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        System.out.println("[DebugFilter] before chain, uri=" + request.getRemoteAddr());
        // 这里放断点即可，调试时会停在这里。
        chain.doFilter(request, response);
        System.out.println("[DebugFilter] after chain");
    }

    @Override
    public void destroy() {
        System.out.println("[DebugFilter] destroy");
    }
}
