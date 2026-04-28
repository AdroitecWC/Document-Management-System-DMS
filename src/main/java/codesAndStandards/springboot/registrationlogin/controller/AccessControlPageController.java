package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.entity.User;
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AccessControlPageController {

    private final UserRepository userRepository;

    /**
     * Display Access Control Management page
     */
    @GetMapping("/access-control")
    @PreAuthorize("hasAuthority('GROUP_VIEW')")
    public String accessControlPage(Model model, Principal principal) {
        log.info("Displaying Access Control Management page");
        User user = userRepository.findByUsername(principal.getName());
        model.addAttribute("userRole", user.getRole().getRoleName());
        return "access-control";
    }
}
