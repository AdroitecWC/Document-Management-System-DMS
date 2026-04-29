package codesAndStandards.springboot.registrationlogin.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminConfig {

    @Value("${superadmin.username}")
    private String username;

    @Value("${superadmin.password}")
    private String rawPassword;

    private String encodedPassword;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        this.encodedPassword = passwordEncoder.encode(rawPassword);
    }

    public String getUsername() {
        return username;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }
}