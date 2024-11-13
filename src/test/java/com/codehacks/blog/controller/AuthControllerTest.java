package com.codehacks.blog.controller;

import com.codehacks.blog.config.SecurityConfig;
import com.codehacks.blog.dto.RegisterRequest;
import com.codehacks.blog.dto.UserDTO;
import com.codehacks.blog.exception.UserAccountException;
import com.codehacks.blog.model.CustomUserDetails;
import com.codehacks.blog.model.Role;
import com.codehacks.blog.model.User;
import com.codehacks.blog.service.AuthService;
import com.codehacks.blog.service.TokenService;
import com.codehacks.blog.util.Constants;
import com.codehacks.blog.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthService.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @ParameterizedTest
    @MethodSource("provideRolesAndExpectedStatusForAddUser")
    void testAddUserWithDifferentRoles(Role role, HttpStatus expectedStatus) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("testUser",
                "Register123Password", "user@example.com");

        UserDTO userDTO = new UserDTO("user@example.com", "testUser");

        when(authService.registerUser(any(User.class))).thenReturn(userDTO);

        mockMvc.perform(post(Constants.AUTH_PATH + "/register")
                .with(user("testUser").roles(role.name().replace("ROLE_", "")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().is(expectedStatus.value()));
    }

    private static Stream<Arguments> provideRolesAndExpectedStatusForAddUser() {
        return Stream.of(
                Arguments.of(Role.USER, HttpStatus.OK),
                Arguments.of(Role.SUBSCRIBER, HttpStatus.OK),
                Arguments.of(Role.ADMIN, HttpStatus.OK)
        );
    }

    @ParameterizedTest
    @MethodSource("provideRolesAndExpectedStatusForDeleteUserNotFound")
    void testDeleteUserNotFoundWithDifferentRoles(Role role, HttpStatus expectedStatus) throws Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, role);

        when(authService.canUserDeleteAccount(username, userDetails))
                .thenReturn(role == Role.ADMIN || (role == Role.SUBSCRIBER && username.equals(userDetails.getUsername())));

        doThrow(new UserAccountException("User account not found")).when(authService).deleteUserAccount(username);

        ResultActions resultActions = mockMvc.perform(delete(Constants.AUTH_PATH + "/delete-account")
                .with(user(userDetails))
                        .param("username", username)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        if (expectedStatus == HttpStatus.BAD_REQUEST) {
            resultActions.andExpect(status().isBadRequest());
            verify(authService, times(1)).deleteUserAccount(username);
        } else {
            resultActions.andExpect(status().isForbidden());
            verify(authService, never()).deleteUserAccount(username);
        }
    }

    private static Stream<Arguments> provideRolesAndExpectedStatusForDeleteUserNotFound() {
        return Stream.of(
                Arguments.of(Role.USER, HttpStatus.FORBIDDEN),
                Arguments.of(Role.SUBSCRIBER, HttpStatus.BAD_REQUEST),
                Arguments.of(Role.ADMIN, HttpStatus.BAD_REQUEST)
        );
    }

    private UserDetails createUserDetails(String username, Role role) {
        return new CustomUserDetails(username, "Register123Password", "user@example.com",
                role, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name())), true);
    }

    @ParameterizedTest
    @ArgumentsSource(RoleAndUsernameArgumentsProvider.class)
    void testDeleteAccountWithDifferentRolesAndUsername(Role role, String username, String targetUsername, HttpStatus expectedStatus) throws Exception {
        UserDetails userDetails = createUserDetails(username, role);

        when(authService.canUserDeleteAccount(targetUsername, userDetails))
                .thenReturn(role == Role.ADMIN || (role == Role.SUBSCRIBER && username.equals(targetUsername)));

        ResultActions resultActions = mockMvc.perform(delete(Constants.AUTH_PATH + "/delete-account")
                        .with(user(userDetails))
                        .param("username", targetUsername)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        if (expectedStatus == HttpStatus.NO_CONTENT) {
            resultActions.andExpect(status().isNoContent());
            verify(authService).deleteUserAccount(targetUsername);
        } else {
            resultActions.andExpect(status().isForbidden());
            verify(authService, never()).deleteUserAccount(targetUsername);
        }
        if (role != Role.USER) {
            verify(authService).canUserDeleteAccount(targetUsername, userDetails);
        }
    }

    static class RoleAndUsernameArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    // SUBSCRIBER can delete their own account
                    Arguments.of(Role.SUBSCRIBER, "subscriber1", "subscriber1", HttpStatus.NO_CONTENT),
                    // SUBSCRIBER cannot delete other accounts
                    Arguments.of(Role.SUBSCRIBER, "subscriber1", "subscriber2", HttpStatus.FORBIDDEN),
                    // ADMIN can delete any account
                    Arguments.of(Role.ADMIN, "admin1", "subscriber1", HttpStatus.NO_CONTENT),
                    Arguments.of(Role.ADMIN, "admin1", "admin1", HttpStatus.NO_CONTENT),
                    // USER role cannot delete any account
                    Arguments.of(Role.USER, "user1", "user1", HttpStatus.FORBIDDEN),
                    Arguments.of(Role.USER, "user1", "subscriber1", HttpStatus.FORBIDDEN)
            );
        }
    }
}