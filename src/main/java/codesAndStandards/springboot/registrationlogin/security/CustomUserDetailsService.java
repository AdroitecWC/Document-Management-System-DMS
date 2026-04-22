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

        // Trim whitespace and use exact role name from DB
        String roleName = user.getRole().getRoleName().trim();

        System.out.println("User: " + username + " has role: " + roleName); // debug

        List<GrantedAuthority> authorities = new ArrayList<>();
        // Add the role itself as an authority, maybe prefixed with ROLE_ if needed, but keeping existing logic:
        authorities.add(new SimpleGrantedAuthority(roleName));

        // Fetch permissions for the user's role
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
