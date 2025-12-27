package be.lefief.controller;

import be.lefief.config.FakeKeycloak;
import be.lefief.controller.dto.AuthenticationDTO;
import be.lefief.controller.dto.AuthenticationSuccessDTO;
import be.lefief.repository.UserData;
import be.lefief.service.userprofile.UserProfileService;
import org.apache.catalina.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class AuthenticationController {

    private final UserProfileService userProfileService;
    private final FakeKeycloak fakeKeycloak;

    public AuthenticationController(
            UserProfileService userProfileService,
            FakeKeycloak fakeKeycloak) {
        this.userProfileService = userProfileService;
        this.fakeKeycloak = fakeKeycloak;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationSuccessDTO> login(
            @RequestBody AuthenticationDTO authenticationDTO) {
        Optional<UserData> authenticatedUser = userProfileService.login(authenticationDTO.username(),
                authenticationDTO.password());
        if (authenticatedUser.isEmpty())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserData user = authenticatedUser.get();
        return ResponseEntity.ok(new AuthenticationSuccessDTO(user.getName(), user.getId(),
                fakeKeycloak.createAccessToken(user.getId())));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationSuccessDTO> register(
            @RequestBody AuthenticationDTO authenticationDTO) {
        Optional<UserData> registeredUser = userProfileService.save(authenticationDTO);
        if (registeredUser.isEmpty())
            return ResponseEntity.badRequest().build();
        UserData user = registeredUser.get();
        return ResponseEntity.ok(new AuthenticationSuccessDTO(user.getName(), user.getId(),
                fakeKeycloak.createAccessToken(user.getId())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationSuccessDTO> refresh(
            @RequestBody be.lefief.controller.dto.RefreshRequestDTO refreshRequest) {
        Optional<UserData> user = userProfileService.findByID(refreshRequest.userId());
        if (user.isEmpty())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(new AuthenticationSuccessDTO(user.get().getName(), user.get().getId(),
                fakeKeycloak.createAccessToken(user.get().getId())));
    }
}
