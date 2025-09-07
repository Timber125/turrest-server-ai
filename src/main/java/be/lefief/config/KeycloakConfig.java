package be.lefief.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class KeycloakConfig {

    // Fake keycloak impl until real deployment
    @Bean
    FakeKeycloak fakeKeycloak(){
        return new FakeKeycloak();
    }

}
