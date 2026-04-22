package codesAndStandards.springboot.registrationlogin.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Set<String> authorities = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        // Now we use the PBAC action to determine if they can view documents.
        // As long as they have DOCUMENT_VIEW, they can log in.
        // We still keep the admin check as a fallback.
        if (authorities.contains("Admin") || authorities.contains("DOCUMENT_VIEW")) {
            response.sendRedirect("/documents");
        } else if (authorities.contains("Manager")) {
            response.sendRedirect("/documents");
        } else if (authorities.contains("Viewer")) {
            response.sendRedirect("/documents");
        }
        else {
            response.sendRedirect("/login?error=unauthorized");
        }
    }
}
