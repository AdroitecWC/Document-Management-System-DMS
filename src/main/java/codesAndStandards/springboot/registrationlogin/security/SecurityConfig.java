package codesAndStandards.springboot.registrationlogin.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private CustomSuccessHandler customSuccessHandler;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )

                .authorizeHttpRequests(authorize -> authorize

                        // ========================================
                        // LICENSE ENDPOINTS
                        // ========================================
                        .requestMatchers("/license-activation", "/license-activation/**").permitAll()
                        .requestMatchers("/api/license/**").permitAll()

                        // ========================================
                        // PUBLIC ENDPOINTS
                        // ========================================
                        .requestMatchers("/register/**").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                        .requestMatchers("/error/403").permitAll()

                        // ========================================
                        // ROLE / AUTHORITY BASED ACCESS
                        // ========================================

                        .requestMatchers("/bulk-upload").hasAnyAuthority("superadmin", "DOCUMENT_BULK_UPLOAD")
                        .requestMatchers("/upload").hasAnyAuthority("superadmin", "DOCUMENT_UPLOAD")

                        // Document API
                        .requestMatchers(HttpMethod.GET, "/api/documents/**")
                        .hasAnyAuthority("superadmin", "DOCUMENT_VIEW")

                        // User API
                        .requestMatchers(HttpMethod.GET, "/api/users/**")
                        .hasAnyAuthority("superadmin", "USER_VIEW")

                        // Access Groups API
                        .requestMatchers(HttpMethod.GET, "/api/access-groups/**")
                        .hasAnyAuthority("superadmin", "GROUP_VIEW")
                        .requestMatchers("/api/access-groups/**")
                        .hasAnyAuthority("superadmin", "GROUP_UPDATE")

                        // Documents pages
                        .requestMatchers("/documents", "/documents/**")
                        .hasAnyAuthority("superadmin", "DOCUMENT_VIEW")

                        .requestMatchers("/my-bookmarks")
                        .hasAnyAuthority("superadmin", "BOOKMARK_VIEW")

                        .requestMatchers("/DocViewer")
                        .hasAnyAuthority("superadmin", "DOCUMENT_VIEW")

                        // Activity logs
                        .requestMatchers("/activity-logs")
                        .hasAnyAuthority("superadmin", "ACTIVITY_LOG_VIEW")

                        // Tags & classifications
                        .requestMatchers("/tags-management")
                        .hasAnyAuthority("superadmin", "TAG_VIEW")

                        .requestMatchers("/classifications-management")
                        .hasAnyAuthority("superadmin", "CLASSIFICATION_VIEW")

                        .requestMatchers("/api/tags/**")
                        .hasAnyAuthority("superadmin", "TAG_VIEW")

                        .requestMatchers("/api/classifications/**")
                        .hasAnyAuthority("superadmin", "CLASSIFICATION_VIEW")

                        // Admin endpoints
                        .requestMatchers("/users", "/users/**")
                        .hasAnyAuthority("superadmin", "USER_VIEW")

                        .requestMatchers("/add", "/add/**")
                        .hasAnyAuthority("superadmin", "USER_CREATE")

                        .requestMatchers("/edit/**")
                        .hasAnyAuthority("superadmin", "USER_UPDATE")

                        .requestMatchers("/delete/**")
                        .hasAnyAuthority("superadmin", "USER_DELETE")


                        .requestMatchers("/users/*/toggle-status")
                        .hasAnyAuthority("superadmin", "USER_SUSPEND")
                        // Manager
                        .requestMatchers("/manager", "/manager/**")
                        .hasAnyAuthority("superadmin", "DOCUMENT_UPDATE")

                        .requestMatchers("/manager/documents/**")
                        .hasAnyAuthority("superadmin", "DOCUMENT_UPDATE")

                        // Viewer
                        .requestMatchers("/viewer", "/viewer/**")
                        .hasAnyAuthority("superadmin", "DOCUMENT_VIEW")

                        // Profile
                        .requestMatchers("/profile", "/profile/**")
                        .authenticated()

                        // Everything else
                        .anyRequest().authenticated()
                )

                // =========================
                // LOGIN
                // =========================
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(customSuccessHandler)
                        .permitAll()
                )

                // =========================
                // LOGOUT
                // =========================
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                )

                // =========================
                // SESSION MANAGEMENT
                // =========================
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .invalidSessionUrl("/login?expired")
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired")
                );

        return http.build();
    }

    // =========================
    // AUTH CONFIG
    // =========================
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }
}