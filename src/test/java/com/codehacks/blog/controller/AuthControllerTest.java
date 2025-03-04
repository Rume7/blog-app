package com.codehacks.blog.controller;

import com.codehacks.blog.config.SecurityConfig;
import com.codehacks.blog.dto.PasswordChangeRequest;
import com.codehacks.blog.dto.RegisterRequest;
import com.codehacks.blog.dto.RoleChangeRequest;
import com.codehacks.blog.dto.UserDTO;
import com.codehacks.blog.exception.UserAccountException;
import com.codehacks.blog.model.CustomUserDetails;
import com.codehacks.blog.model.Role;
import com.codehacks.blog.model.User;
import com.codehacks.blog.repository.UserRepository;
import com.codehacks.blog.service.AuthService;
import com.codehacks.blog.service.TokenService;
import com.codehacks.blog.util.Constants;
import com.codehacks.blog.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableAspectJAutoProxy
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

    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @ParameterizedTest
    @MethodSource("provideRolesAndExpectedStatusForUsers")
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

    private static Stream<Arguments> provideRolesAndExpectedStatusForUsers() {
        return Stream.of(
                Arguments.of(Role.USER, HttpStatus.OK),
                Arguments.of(Role.SUBSCRIBER, HttpStatus.OK),
                Arguments.of(Role.ADMIN, HttpStatus.OK)
        );
    }

    @ParameterizedTest
    @MethodSource("provideRolesAndExpectedStatusForChangePassword")
    void changePassword_WithDifferentRoles(Role role, HttpStatus expectedStatus) throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest("testUser", "oldPassword123", "newPassword&12");

        ResultActions resultActions = mockMvc.perform(put(Constants.AUTH_PATH + "/change-password")
                .with(user("testUser").roles(role.name()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        if (expectedStatus == HttpStatus.OK) {
            resultActions.andExpect(status().isOk())
                    .andExpect(content().string("Password changed successfully"));
            verify(authService).changePassword(request.username(), request.currentPassword(), request.newPassword());
        } else {
            resultActions.andExpect(status().isForbidden());
            verify(authService, never()).changePassword(anyString(), anyString(), anyString());
        }
    }

    private static Stream<Arguments> provideRolesAndExpectedStatusForChangePassword() {
        return Stream.of(
                Arguments.of(Role.USER, HttpStatus.FORBIDDEN),
                Arguments.of(Role.SUBSCRIBER, HttpStatus.OK),
                Arguments.of(Role.ADMIN, HttpStatus.OK)
        );
    }

    @Test
    void changePassword_WhenUnauthenticated_Unauthorized() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest("testUser", "oldPass123", "newPassword&12");

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andDo(print());

        verify(authService, never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "SUBSCRIBER")
    void changePassword_WithInvalidRequest_BadRequest() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest("", "", "");

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(print());

        verify(authService, never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "SUBSCRIBER")
    void changePassword_UserNotFound_NotFound() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest("nonexistentUser", "oldPass123", "newPassword&12");

        doThrow(new UserAccountException("User not found"))
                .when(authService).changePassword(request.username(), request.currentPassword(), request.newPassword());

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"))
                .andDo(print());

        verify(authService).changePassword(request.username(), request.currentPassword(), request.newPassword());
    }

    @Test
    @WithMockUser(roles = "SUBSCRIBER")
    void changePassword_WithSamePassword_BadRequest() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest("testUser", "PassLove&123", "PassLove&123");

        doThrow(new UserAccountException("New password must be different from current password"))
                .when(authService).changePassword(request.username(), request.currentPassword(), request.newPassword());

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("New password must be different from current password"))
                .andDo(print());

        verify(authService).changePassword(request.username(), request.currentPassword(), request.newPassword());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeUserRole_AsAdmin_Success() throws Exception {
        RoleChangeRequest request = new RoleChangeRequest("testUser", Role.SUBSCRIBER);
        User updatedUser = new User();
        updatedUser.setUsername("testUser");
        updatedUser.setRole(Role.SUBSCRIBER);

        when(authService.changeUserRole(request.username(), request.userRole())).thenReturn(updatedUser);

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Role changed successfully"))
                .andDo(print());

        verify(authService).changeUserRole(request.username(), request.userRole());
    }

    @Test
    @WithMockUser(roles = "SUBSCRIBER")
    void changeUserRole_AsSubscriber_Forbidden() throws Exception {
        RoleChangeRequest request = new RoleChangeRequest("testUser", Role.SUBSCRIBER);

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andDo(print());

        verify(authService, never()).changeUserRole(anyString(), any(Role.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void changeUserRole_AsUser_Forbidden() throws Exception {
        RoleChangeRequest request = new RoleChangeRequest("testUser", Role.SUBSCRIBER);

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andDo(print());

        verify(authService, never()).changeUserRole(anyString(), any(Role.class));
    }

    @Test
    void changeUserRole_Unauthenticated_Unauthorized() throws Exception {
        RoleChangeRequest request = new RoleChangeRequest("testUser", Role.SUBSCRIBER);

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andDo(print());

        verify(authService, never()).changeUserRole(anyString(), any(Role.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeUserRole_InvalidRequest_BadRequest() throws Exception {
        RoleChangeRequest request = new RoleChangeRequest("", null);

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(print());

        verify(authService, never()).changeUserRole(anyString(), any(Role.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeUserRole_UserNotFound_NotFound() throws Exception {
        RoleChangeRequest request = new RoleChangeRequest("nonexistentUser", Role.SUBSCRIBER);

        when(authService.changeUserRole(request.username(), request.userRole()))
                .thenThrow(new UserAccountException("User not found"));

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"))
                .andDo(print());

        verify(authService).changeUserRole(request.username(), request.userRole());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeUserRole_ToAdmin_Success() throws Exception {
        RoleChangeRequest request = new RoleChangeRequest("testUser", Role.ADMIN);
        User updatedUser = new User();
        updatedUser.setUsername("testUser");
        updatedUser.setRole(Role.ADMIN);

        when(authService.changeUserRole(request.username(), request.userRole())).thenReturn(updatedUser);

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Role changed successfully"))
                .andDo(print());

        verify(authService).changeUserRole(request.username(), request.userRole());
    }

    @ParameterizedTest
    @MethodSource("provideRolesAndExpectedStatusForDeleteUserNotFound")
    void testDeleteUserNotFoundWithDifferentRoles(Role role, HttpStatus expectedStatus) throws Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, role);

        // Only set up mocks for ADMIN role
        if (role != Role.USER && role != Role.SUBSCRIBER) {
            when(authService.canUserDeleteAccount(username, userDetails)).thenReturn(role == Role.ADMIN);
            doThrow(new UserAccountException("User account not found")).when(authService).deleteUserAccount(username);
        }

        ResultActions resultActions = mockMvc.perform(delete(Constants.AUTH_PATH + "/delete-account")
                        .with(user(userDetails))
                        .param("username", username)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        if (role == Role.USER || role == Role.SUBSCRIBER) {
            resultActions.andExpect(status().isForbidden());
            verify(authService, never()).deleteUserAccount(username);
            verify(authService, never()).canUserDeleteAccount(any(), any());
        } else if (expectedStatus == HttpStatus.BAD_REQUEST) {
            resultActions.andExpect(status().isBadRequest());
            verify(authService, times(1)).deleteUserAccount(username);
            verify(authService).canUserDeleteAccount(username, userDetails);
        }
    }

    private static Stream<Arguments> provideRolesAndExpectedStatusForDeleteUserNotFound() {
        return Stream.of(
                Arguments.of(Role.USER, HttpStatus.FORBIDDEN),
                Arguments.of(Role.SUBSCRIBER, HttpStatus.FORBIDDEN),
                Arguments.of(Role.ADMIN, HttpStatus.BAD_REQUEST)       // ADMIN gets BAD_REQUEST when account not found
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

        if (role != Role.USER) {
            when(authService.canUserDeleteAccount(targetUsername, userDetails))
                    .thenReturn(role == Role.ADMIN);
        }

        ResultActions resultActions = mockMvc.perform(delete(Constants.AUTH_PATH + "/delete-account")
                        .with(user(userDetails))
                        .param("username", targetUsername)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print());

        if (expectedStatus == HttpStatus.NO_CONTENT) {
            resultActions.andExpect(status().isNoContent());
            verify(authService).deleteUserAccount(targetUsername);
            verify(authService).canUserDeleteAccount(targetUsername, userDetails);
        } else {
            resultActions.andExpect(status().isForbidden());
            verify(authService, never()).deleteUserAccount(targetUsername);
            if (role == Role.USER) {
                verify(authService, never()).canUserDeleteAccount(any(), any());
            }
        }
    }

    private static class RoleAndUsernameArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    // SUBSCRIBER cannot delete their own account
                    Arguments.of(Role.SUBSCRIBER, "subscriber1", "subscriber1", HttpStatus.FORBIDDEN),
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