package com.example.hotel_restapi;

import com.example.hotel_restapi.configuration.InitConfiguration;
import com.example.hotel_restapi.configuration.SecurityConfiguration;
import com.example.hotel_restapi.entity.User;
import com.example.hotel_restapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationSecurityTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    Environment environment;

    // --- InitConfiguration Tests ---

    @Test
    void testInitConfiguration_whenDisabled_doesNotCreateAccounts() throws Exception {
        InitConfiguration config = new InitConfiguration(passwordEncoder);
        ReflectionTestUtils.setField(config, "environment", environment);
        ApplicationRunner runner = config.applicationRunner(userRepository, false, "admin", "user");
        runner.run(mock(ApplicationArguments.class));

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void testInitConfiguration_underDefaultProfile_seedsDefaultAccounts() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        InitConfiguration config = new InitConfiguration(passwordEncoder);
        ReflectionTestUtils.setField(config, "environment", environment);
        ApplicationRunner runner = config.applicationRunner(userRepository, true, "admin", "user");
        runner.run(mock(ApplicationArguments.class));

        verify(userRepository, times(2)).save(any());
        verify(passwordEncoder).encode("admin");
        verify(passwordEncoder).encode("user");
    }

    @Test
    void testInitConfiguration_underProdProfile_failsWhenAdminPasswordDefault() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        InitConfiguration config = new InitConfiguration(passwordEncoder);
        ReflectionTestUtils.setField(config, "environment", environment);
        ApplicationRunner runner = config.applicationRunner(userRepository, true, "admin", "customUserPass123");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run(mock(ApplicationArguments.class)));
        assertTrue(ex.getMessage().contains("APP_INIT_ADMIN_PASSWORD"));
    }

    @Test
    void testInitConfiguration_underProdProfile_failsWhenAdminPasswordBlank() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        InitConfiguration config = new InitConfiguration(passwordEncoder);
        ReflectionTestUtils.setField(config, "environment", environment);
        ApplicationRunner runner = config.applicationRunner(userRepository, true, "   ", "customUserPass123");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run(mock(ApplicationArguments.class)));
        assertTrue(ex.getMessage().contains("APP_INIT_ADMIN_PASSWORD"));
    }

    @Test
    void testInitConfiguration_underProdProfile_failsWhenUserPasswordDefault() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        InitConfiguration config = new InitConfiguration(passwordEncoder);
        ReflectionTestUtils.setField(config, "environment", environment);
        ApplicationRunner runner = config.applicationRunner(userRepository, true, "customAdminPass123", "user");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run(mock(ApplicationArguments.class)));
        assertTrue(ex.getMessage().contains("APP_INIT_USER_PASSWORD"));
    }

    @Test
    void testInitConfiguration_underProdProfile_failsWhenUserPasswordBlank() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        InitConfiguration config = new InitConfiguration(passwordEncoder);
        ReflectionTestUtils.setField(config, "environment", environment);
        ApplicationRunner runner = config.applicationRunner(userRepository, true, "customAdminPass123", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run(mock(ApplicationArguments.class)));
        assertTrue(ex.getMessage().contains("APP_INIT_USER_PASSWORD"));
    }

    @Test
    void testInitConfiguration_underProdProfile_succeedsWithCustomPasswords() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        InitConfiguration config = new InitConfiguration(passwordEncoder);
        ReflectionTestUtils.setField(config, "environment", environment);
        ApplicationRunner runner = config.applicationRunner(userRepository, true, "strongAdminPass_2026!", "strongUserPass_2026!");
        runner.run(mock(ApplicationArguments.class));

        verify(userRepository, times(2)).save(any());
        verify(passwordEncoder).encode("strongAdminPass_2026!");
        verify(passwordEncoder).encode("strongUserPass_2026!");
    }

    @Test
    void testInitConfiguration_whenAccountsAlreadyExist_doesNotOverwrite() throws Exception {
        User existingAdmin = User.builder()
                .username("admin")
                .password("preExistingHashedAdmin")
                .build();
        User existingUser = User.builder()
                .username("user")
                .password("preExistingHashedUser")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(existingUser));

        InitConfiguration config = new InitConfiguration(passwordEncoder);
        ReflectionTestUtils.setField(config, "environment", environment);
        ApplicationRunner runner = config.applicationRunner(userRepository, true, "admin", "user");
        runner.run(mock(ApplicationArguments.class));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    // --- SecurityConfiguration Tests ---

    @Test
    void testSecurityConfiguration_underProdProfile_failsWhenKeyNull() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SecurityConfiguration secConfig = new SecurityConfiguration();
        ReflectionTestUtils.setField(secConfig, "environment", environment);
        ReflectionTestUtils.setField(secConfig, "signerKey", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, secConfig::validateJwtSignerKey);
        assertTrue(ex.getMessage().contains("JWT_SIGNER_KEY"));
    }

    @Test
    void testSecurityConfiguration_underProdProfile_failsWhenKeyBlank() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SecurityConfiguration secConfig = new SecurityConfiguration();
        ReflectionTestUtils.setField(secConfig, "environment", environment);
        ReflectionTestUtils.setField(secConfig, "signerKey", "   ");

        IllegalStateException ex = assertThrows(IllegalStateException.class, secConfig::validateJwtSignerKey);
        assertTrue(ex.getMessage().contains("JWT_SIGNER_KEY"));
    }

    @Test
    void testSecurityConfiguration_underProdProfile_failsWhenKeyLessThan32Bytes() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SecurityConfiguration secConfig = new SecurityConfiguration();
        ReflectionTestUtils.setField(secConfig, "environment", environment);
        // 31 ASCII characters = 31 bytes (< 32 bytes / 256 bits)
        ReflectionTestUtils.setField(secConfig, "signerKey", "1234567890123456789012345678901");

        IllegalStateException ex = assertThrows(IllegalStateException.class, secConfig::validateJwtSignerKey);
        assertTrue(ex.getMessage().contains("JWT_SIGNER_KEY"));
        assertTrue(ex.getMessage().contains("256 bits"));
    }

    @Test
    void testSecurityConfiguration_underProdProfile_passesWithValidKey() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SecurityConfiguration secConfig = new SecurityConfiguration();
        ReflectionTestUtils.setField(secConfig, "environment", environment);
        // 32 ASCII characters = 32 bytes (256 bits)
        ReflectionTestUtils.setField(secConfig, "signerKey", "12345678901234567890123456789012");

        assertDoesNotThrow(secConfig::validateJwtSignerKey);
    }

    @Test
    void testSecurityConfiguration_underDefaultProfile_allowsAnyKey() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{});

        SecurityConfiguration secConfig = new SecurityConfiguration();
        ReflectionTestUtils.setField(secConfig, "environment", environment);
        ReflectionTestUtils.setField(secConfig, "signerKey", "local-dev-key");

        assertDoesNotThrow(secConfig::validateJwtSignerKey);
    }

    @Test
    void testSecurityConfiguration_exceptionMessageNeverRevealsSignerKey() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        SecurityConfiguration secConfig = new SecurityConfiguration();
        ReflectionTestUtils.setField(secConfig, "environment", environment);
        String secretKey = "shortSecretKey";
        ReflectionTestUtils.setField(secConfig, "signerKey", secretKey);

        IllegalStateException ex = assertThrows(IllegalStateException.class, secConfig::validateJwtSignerKey);
        assertFalse(ex.getMessage().contains(secretKey));
    }

    // --- Property Configuration Tests ---

    @Test
    void testProdProperties_requiresExplicitJwtSignerKeyWithoutFallback() throws Exception {
        Properties prodProps = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application-prod.properties")) {
            assertNotNull(in, "application-prod.properties must be on the classpath");
            prodProps.load(in);
        }
        assertEquals("${JWT_SIGNER_KEY}", prodProps.getProperty("jwt.signerKey"));
        assertEquals("${DB_URL}", prodProps.getProperty("spring.datasource.url"));
        assertEquals("${DB_USERNAME}", prodProps.getProperty("spring.datasource.username"));
        assertEquals("${DB_PASSWORD}", prodProps.getProperty("spring.datasource.password"));
        assertEquals("${APP_INIT_ENABLED:false}", prodProps.getProperty("app.init.enabled"));
        assertEquals("${APP_INIT_ADMIN_PASSWORD:}", prodProps.getProperty("app.init.admin-password"));
        assertEquals("${APP_INIT_USER_PASSWORD:}", prodProps.getProperty("app.init.user-password"));
    }

    @Test
    void testDefaultProperties_containsDefaultFallbacksForLocalDev() throws Exception {
        Properties defaultProps = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(in, "application.properties must be on the classpath");
            defaultProps.load(in);
        }
        assertTrue(defaultProps.getProperty("jwt.signerKey").startsWith("${JWT_SIGNER_KEY:"));
        assertTrue(defaultProps.getProperty("spring.datasource.url").startsWith("${DB_URL:"));
        assertTrue(defaultProps.getProperty("spring.datasource.username").startsWith("${DB_USERNAME:"));
        assertTrue(defaultProps.getProperty("spring.datasource.password").startsWith("${DB_PASSWORD:"));
        assertEquals("${APP_INIT_ENABLED:true}", defaultProps.getProperty("app.init.enabled"));
        assertEquals("${APP_INIT_ADMIN_PASSWORD:admin}", defaultProps.getProperty("app.init.admin-password"));
        assertEquals("${APP_INIT_USER_PASSWORD:user}", defaultProps.getProperty("app.init.user-password"));
    }
}
