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
                        .ignoringRequestMatchers("/api/**") // Disable CSRF for API endpoints
                )
                .authorizeHttpRequests(authorize -> authorize

                        // ========================================
                        // ✅ LICENSE ENDPOINTS (ADD THIS SECTION)
                        // ========================================
                        .requestMatchers("/license-activation", "/license-activation/**").permitAll()
                        .requestMatchers("/api/license/**").permitAll()

                        // ========================================
                        // PUBLIC ENDPOINTS (MOVED UP FOR PRIORITY)
                        // ========================================
                        .requestMatchers("/register/**").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()

                        // ========================================
                        // ROLE-BASED PERMISSIONS
                        // ========================================

                        // Bulk upload - Admin only
                        .requestMatchers("/bulk-upload").hasAuthority("DOCUMENT_BULK_UPLOAD")  // ← ADD THIS LINE
                        // Upload endpoint
                        .requestMatchers("/upload").hasAuthority("DOCUMENT_UPLOAD")

                        // Document API
                        .requestMatchers(HttpMethod.GET, "/api/documents/**")
                        .hasAuthority("DOCUMENT_VIEW")

                        // User API
                        .requestMatchers(HttpMethod.GET, "/api/users/**")
                        .hasAuthority("USER_VIEW")

                        // Access Groups API
                        .requestMatchers(HttpMethod.GET, "/api/access-groups/**")
                        .hasAuthority("GROUP_VIEW")
                        .requestMatchers("/api/access-groups/**")
                        .hasAuthority("GROUP_UPDATE")

                        // Documents pages
                        .requestMatchers("/documents").hasAuthority("DOCUMENT_VIEW")
                        .requestMatchers("/documents/**").hasAuthority("DOCUMENT_VIEW")
                        .requestMatchers("/my-bookmarks").hasAuthority("BOOKMARK_VIEW")
                        .requestMatchers("/DocViewer").hasAuthority("DOCUMENT_VIEW")

                        // Activity logs - Admin only
                        .requestMatchers("/activity-logs").hasAuthority("ACTIVITY_LOG_VIEW")

                        // Tags and Classifications management
                        .requestMatchers("/tags-management").hasAuthority("TAG_VIEW")
                        .requestMatchers("/classifications-management").hasAuthority("CLASSIFICATION_VIEW")
                        .requestMatchers("/api/tags/**").hasAuthority("TAG_VIEW")
                        .requestMatchers("/api/classifications/**").hasAuthority("CLASSIFICATION_VIEW")

                        // Admin only endpoints
                        .requestMatchers("/users", "/users/**").hasAuthority("USER_VIEW")
                        .requestMatchers("/add", "/add/**").hasAuthority("USER_CREATE")
                        .requestMatchers("/edit/**").hasAuthority("USER_UPDATE")
                        .requestMatchers("/delete/**").hasAuthority("USER_DELETE")

                        // Manager endpoints
                        .requestMatchers("/manager", "/manager/**").hasAuthority("DOCUMENT_UPDATE")
                        .requestMatchers("/manager/documents/**").hasAuthority("DOCUMENT_UPDATE")

                        // Viewer endpoints
                        .requestMatchers("/viewer", "/viewer/**").hasAuthority("DOCUMENT_VIEW")

                        // Profile accessible by all authenticated users
                        .requestMatchers("/profile", "/profile/**").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(customSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        // ✅ SESSION MANAGEMENT - Dynamic timeout from database + concurrent session control
        http.sessionManagement(session -> session
                .sessionFixation().migrateSession()  // Prevent session fixation attacks
                .invalidSessionUrl("/login?expired")  // Redirect when session is invalid
                .maximumSessions(1)  // Allow only 1 session per user
                .maxSessionsPreventsLogin(false)  // New login kicks out old session
                .expiredUrl("/login?expired")  // Redirect when session expires
        );

        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }
}