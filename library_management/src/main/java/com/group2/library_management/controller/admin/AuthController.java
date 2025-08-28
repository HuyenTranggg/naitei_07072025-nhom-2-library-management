package com.group2.library_management.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.group2.library_management.common.constants.Endpoints;

@Controller("adminAuthController")
@RequestMapping(Endpoints.Admin.BASE)
public class AuthController {

    @GetMapping(Endpoints.Admin.Login.LOGIN_ACTION)
    public String login() {
        return Endpoints.Admin.Login.LOGIN_STATIC;
    }
    
    // access-denied
    @GetMapping(Endpoints.Admin.Login.ACCESS_DENIED_ACTION)
    public String accessDenied() {
        return Endpoints.Admin.Login.ACCESS_DENIED_STATIC;
    }
}
