package codesAndStandards.springboot.registrationlogin.security;

import codesAndStandards.springboot.registrationlogin.entity.Action;
import codesAndStandards.springboot.registrationlogin.entity.Permission;
import codesAndStandards.springboot.registrationlogin.entity.User;
import codesAndStandards.springboot.registrationlogin.repository.ActionRepository;
import codesAndStandards.springboot.registrationlogin.repository.PermissionRepository;
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final ActionRepository actionRepository;
    private final SuperAdminConfig superAdminConfig;

    public CustomUserDetailsService(UserRepository userRepository,
                                    PermissionRepository permissionRepository,
                                    ActionRepository actionRepository,
                                    SuperAdminConfig superAdminConfig) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.actionRepository = actionRepository;
        this.superAdminConfig = superAdminConfig;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Superadmin — credentials from application.properties, not from DB
        if (username.equals(superAdminConfig.getUsername())) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("superadmin"));
            // Grant every action so URL-level hasAuthority() checks also pass
            for (Action action : actionRepository.findAll()) {
                authorities.add(new SimpleGrantedAuthority(action.getActionName().trim()));
            }
            return org.springframework.security.core.userdetails.User
                    .withUsername(superAdminConfig.getUsername())
                    .password(superAdminConfig.getEncodedPassword())
                    .authorities(authorities)
                    .build();
        }

        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("Invalid username or password.");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add the role with "ROLE_" prefix as per Spring Security convention
        String roleName = user.getRole().getRoleName().trim();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
        System.out.println("User: " + username + " has role: ROLE_" + roleName);

        // Fetch and add permissions (actions) for the user's role
        List<Permission> permissions = permissionRepository.findByRole(user.getRole());
        for (Permission permission : permissions) {
            String actionName = permission.getAction().getActionName().trim();
            authorities.add(new SimpleGrantedAuthority(actionName));
            System.out.println("User: " + username + " has action permission: " + actionName);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }

}