package com.example.vargoBackend.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final RestTemplate template = new RestTemplate();
    // TODO: add JWTs

    @GetMapping("/steam")
    public void redirectToSteam(HttpServletResponse response) throws IOException {
        String steamRedirectUrl = "https://steamcommunity.com/openid/login"
                + "?openid.ns=http://specs.openid.net/auth/2.0"
                + "&openid.mode=checkid_setup"
                + "&openid.return_to=http://74.135.5.230:8080/auth/steam/return"
                + "&openid.realm=http://74.135.5.230:8080"
                + "&openid.identity=http://specs.openid.net/auth/2.0/identifier_select"
                + "&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select";

        response.sendRedirect(steamRedirectUrl);
    }

    @GetMapping("/steam/return")
    public ResponseEntity<?> handleSteamReturn(@RequestParam Map<String, String> params, HttpServletResponse res) throws IOException {

        MultiValueMap<String, String> verify = new LinkedMultiValueMap<>();
        verify.setAll(params);
        verify.set("openid.mode", "check_authentication");

        //verify w/ steam
        String resp = template.postForObject(
                "https://steamcommunity.com/openid/login",
                verify,
                String.class);

        boolean ok = resp != null && resp.contains("is_valid:true");
        if (!ok) {
            res.sendError(401, "Steam signature invalid");
            return ResponseEntity.status(400).body("Invalid request.");
        }

        String claimed = params.get("openid.claimed_id");
        String steamId = claimed.substring(claimed.lastIndexOf('/') + 1);

        System.out.println("Steam returned: " + params);
        return ResponseEntity.ok("Steam login successful!");
    }

}
