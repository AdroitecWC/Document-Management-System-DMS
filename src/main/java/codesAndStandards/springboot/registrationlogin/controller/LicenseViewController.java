package codesAndStandards.springboot.registrationlogin.controller;



import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LicenseViewController {

    /**
     * Show license activation page
     */
    @GetMapping("/license-activation")
    public String showLicenseActivation() {
        return "license-activation";
    }
}
