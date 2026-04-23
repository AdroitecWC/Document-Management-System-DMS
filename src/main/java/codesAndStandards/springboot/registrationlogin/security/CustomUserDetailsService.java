package codesAndStandards.springboot.registrationlogin.security;

import codesAndStandards.springboot.registrationlogin.entity.Permission;
import codesAndStandards.springboot.registrationlogin.entity.User;
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
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    public CustomUserDetailsService(UserRepository userRepository, PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("Invalid username or password.");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add the role with "ROLE_" prefix as per Spring Security convention
        String roleName = user.getRole().getRoleName().trim();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName)); // Removed toUpperCase()
        System.out.println("User: " + username + " has role: ROLE_" + roleName); // debug

        // Fetch and add permissions (actions) for the user's role
        List<Permission> permissions = permissionRepository.findByRole(user.getRole());
        for (Permission permission : permissions) {
            String actionName = permission.getAction().getActionName().trim();
            authorities.add(new SimpleGrantedAuthority(actionName));
            System.out.println("User: " + username + " has action permission: " + actionName); // debug
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }

}
