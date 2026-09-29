package com.example.workflow.auth;
import com.example.workflow.common.ApiException;
import com.example.workflow.user.*;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthServiceTest {
    private final UserRepository users=mock(UserRepository.class);
    private final PasswordEncoder passwords=mock(PasswordEncoder.class);
    private final RefreshTokenService tokens=mock(RefreshTokenService.class);
    private AuthService service;
    @BeforeEach void setup() { when(passwords.encode(anyString())).thenReturn("hashed"); service=new AuthService(users,passwords,tokens); }
    @Test void registrationNormalizesEmailAndHashesPassword() {
        service.register(new AuthDtos.Register("ALOK@Example.com","long-password-123","Alok"));
        verify(users).saveAndFlush(argThat(u -> u.getEmail().equals("alok@example.com") && u.getPasswordHash().equals("hashed")));
        verify(tokens).create(any(AppUser.class));
    }
    @Test void duplicateRegistrationDoesNotIssueSession() {
        when(users.existsByEmail("a@example.com")).thenReturn(true);
        assertThrows(ApiException.class,() -> service.register(new AuthDtos.Register("a@example.com","long-password-123","A")));
        verifyNoInteractions(tokens);
    }
    @Test void unknownUserStillPerformsPasswordCheck() {
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(ApiException.class,() -> service.login(new AuthDtos.Login("a@example.com","wrong")));
        verify(passwords).matches("wrong","hashed"); verifyNoInteractions(tokens);
    }
    @Test void rejectsPasswordsBeyondBcryptByteLimit() {
        assertThrows(ApiException.class,() -> service.register(new AuthDtos.Register("a@example.com","é".repeat(40),"A")));
        verifyNoInteractions(users,tokens);
    }
}
