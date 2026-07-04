package com.shopflow.user_service.service;


import com.shopflow.user_service.dto.LoginRequest;
import com.shopflow.user_service.dto.RegisterRequest;
import com.shopflow.user_service.dto.TokenResponse;
import com.shopflow.user_service.dto.UserResponse;
import com.shopflow.user_service.entity.User;
import com.shopflow.user_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("${keycloak.server-url}")
    private String serverUrl;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret}")
    private String clientSecret;
    @Value("${keycloak.realm}")
    private String realm;

    public UserService(UserRepository userRepository, Keycloak keycloak) {
        this.userRepository = userRepository;
        this.keycloak = keycloak;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // 1. Создаём юзера в Keycloak
        String keycloakId = createKeycloakUser(request);

        // 2. Сохраняем в нашу БД
        User user = User.builder()
                .keycloakId(keycloakId)
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(User.Role.CUSTOMER)
                .build();

        userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        return UserResponse.from(user);
    }

    public UserResponse getByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .map(UserResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public TokenResponse login(LoginRequest request) {
        try {
            Keycloak userKeycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .grantType(OAuth2Constants.PASSWORD)
                    .username(request.getEmail())
                    .password(request.getPassword())
                    .build();

            AccessTokenResponse token = userKeycloak.tokenManager().getAccessToken();

            return TokenResponse.builder()
                    .accessToken(token.getToken())
                    .refreshToken(token.getRefreshToken())
                    .expiresIn(token.getExpiresIn())
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid email or password");
        }
    }

    private String createKeycloakUser(RegisterRequest request) {
        UserRepresentation kcUser = getUserRepresentation(request);


        try (Response response = keycloak.realm(realm).users().create(kcUser)) {

            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatus());
            }

            String location = response.getHeaderString("Location");
            return location.substring(location.lastIndexOf("/") + 1);
        }

    }

    @NonNull
    private static UserRepresentation getUserRepresentation(RegisterRequest request) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(request.getEmail());
        kcUser.setEmail(request.getEmail());
        kcUser.setFirstName(request.getFirstName());
        kcUser.setLastName(request.getLastName());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);
        kcUser.setCredentials(List.of(credential));
        return kcUser;
    }
}
