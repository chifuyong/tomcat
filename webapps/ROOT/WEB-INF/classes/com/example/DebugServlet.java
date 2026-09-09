package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DebugServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        System.out.println("Hello from DebugServlet");
        // 核心修改：使用 while 循环强制连续读取，直到流结束
//        InputStream is = request.getInputStream();
//        int b;
//        while ((b = is.read()) != -1) {
//            System.out.println("Servlet 消费了一个字节: " + b + " (字符: " + (char)b + ")");
//        }
//        byte[] buffer = new byte[4096]; // 4KB 缓冲区
//        int bytesRead;
//        while ((bytesRead = is.read(buffer)) != -1) {
//            System.out.print("DebugServlet read: " + new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
//        }
        out.println("requestURI=" + request.getRequestURI());
        out.println("queryString=" + request.getQueryString());
        out.flush();
    }
}
