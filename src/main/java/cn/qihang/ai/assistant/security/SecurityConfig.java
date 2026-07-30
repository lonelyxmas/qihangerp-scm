package cn.qihang.ai.assistant.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthenticationExceptionHandler invalidAuthenticationEntryPoint;

    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    @Autowired
    private TokenService tokenService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(invalidAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        // 静态资源
                        .requestMatchers("/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                        // 公开 API（登录、注册、公开沙箱）
                        .requestMatchers("/api/sys-api/login", "/api/sys-api/captchaImage", "/api/sys-api/register").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        // 页面路由（所有页面均可访问，数据层根据登录状态控制可见性）
                        .requestMatchers("/", "/login", "/login.html", "/config").permitAll()
                        .requestMatchers("/chat", "/help", "/log", "/tools", "/planner", "/notes").permitAll()
                        .requestMatchers("/ai-guide", "/insights", "/health", "/activity", "/notifications", "/approvals", "/automation").permitAll()
                        .requestMatchers("/data/**", "/kb/**", "/image/**", "/coding/**", "/v1/**", "/v3/**", "/admin/**").permitAll()
                        // API 路由：除公开 API 外需要登录（未登录时返回受限数据而非 401）
                        .requestMatchers("/api/system/**").authenticated()
                        .anyRequest().permitAll())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(new JwtAuthenticationTokenFilter(tokenService), UsernamePasswordAuthenticationFilter.class);
        http.logout(logout -> logout.logoutUrl("/api/sys-api/logout").logoutSuccessHandler(logoutSuccessHandler));
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userDetailsService.loadUserByUsername(username);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}