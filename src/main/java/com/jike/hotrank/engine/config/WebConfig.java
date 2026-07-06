package com.jike.hotrank.engine.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * SPA 前端路由回退配置。
 * <p>
 * Vue Router 使用 History 模式，路径如 /overview、/global 等
 * 需要由前端 index.html 处理，而非 Spring Boot 返回 404。
 * <p>
 * 通过 Filter 在请求进入 DispatcherServlet 之前，
 * 将非 API、非静态资源的前端路由请求转发到 /index.html。
 *
 * @author JikeHotRank Team
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Set<String> SPA_ROUTES = Set.of(
        "/overview", "/global", "/circle",
        "/newcomer", "/surging", "/anti-spam"
    );

    @Bean
    public FilterRegistrationBean<Filter> spaForwardFilter() {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest request = (HttpServletRequest) req;
                HttpServletResponse response = (HttpServletResponse) res;
                String path = request.getRequestURI();

                if (SPA_ROUTES.contains(path)) {
                    request.getRequestDispatcher("/index.html").forward(request, response);
                    return;
                }

                chain.doFilter(req, res);
            }
        });
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.addUrlPatterns("/*");
        return bean;
    }
}
