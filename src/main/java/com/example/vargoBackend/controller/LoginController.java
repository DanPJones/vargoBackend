package com.example.vargoBackend.controller;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.vargoBackend.auth.SessionStore;
import com.example.vargoBackend.model.User;
import com.example.vargoBackend.repo.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final RestTemplate template = new RestTemplate();
    private final UserRepository userRepo;
    private final SessionStore sessionStore;
    private final HttpSession httpSession;

    public LoginController(UserRepository userRepo, SessionStore sessionStore, HttpSession httpSession) {
        this.userRepo = userRepo;
        this.sessionStore = sessionStore;
        this.httpSession = httpSession;
    }

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
        if (claimed == null || !claimed.contains("/id/")) {
            return ResponseEntity.badRequest().body("Missing claimed_id.");
        }
        String steamId = claimed.substring(claimed.lastIndexOf('/') + 1);
        httpSession.setAttribute("steamId", steamId);

        User user = userRepo.findBySteamId(steamId)
                .orElseGet(() -> userRepo.saveNewUser(steamId, steamId));

        String token = UUID.randomUUID().toString();
        sessionStore.put(token, user.id());

           ResponseCookie cookie = ResponseCookie.from("session", token)
            .httpOnly(true)
            .secure(false)      // change to true when using HTTPS
            .path("/")
            .maxAge(java.time.Duration.ofDays(1))
            .sameSite("Lax")    // OK since frontend + backend same site (same host IP)
            .build();

           return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .header(HttpHeaders.LOCATION, "http://74.135.5.230:8080/")
            .build();
    }

}
