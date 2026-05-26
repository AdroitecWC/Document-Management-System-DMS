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

    /**
     * Sidebar-ordered list of (permission -> redirect URL).
     * The FIRST entry whose permission the user has wins.
     * superadmin always goes to /documents (checked before this list).
     */
    private static final String[][] PAGE_PRIORITY = {
            // LIBRARY
            { "DOCUMENT_VIEW",          "/documents"                },
            { "BOOKMARK_VIEW",          "/my-bookmarks"             },
            // MANAGEMENT
            { "DOCUMENT_UPLOAD",        "/upload"                   },
            { "TAG_VIEW",               "/tags-management"          },
            { "CLASSIFICATION_VIEW",    "/classifications-management" },
            // ADMINISTRATION
            { "DOCUMENT_BULK_UPLOAD",   "/bulk-upload"              },
            { "USER_VIEW",              "/users"                    },
            { "GROUP_VIEW",             "/access-control"           },
            { "SETTINGS_VIEW",          "/settings"                 },
            { "ACTIVITY_LOG_VIEW",      "/activity-logs"            },
    };

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        Set<String> authorities =
                AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        // superadmin always lands on Document Library
        if (authorities.contains("superadmin")) {
            response.sendRedirect("/documents");
            return;
        }

        // Walk the priority list — redirect to the first page the user can access
        for (String[] entry : PAGE_PRIORITY) {
            String permission = entry[0];
            String url        = entry[1];
            if (authorities.contains(permission)) {
                response.sendRedirect(url);
                return;
            }
        }

        // User is authenticated but has no recognised permissions at all
        response.sendRedirect("/login?error=unauthorized");
    }
}