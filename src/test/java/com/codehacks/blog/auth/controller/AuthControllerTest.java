package com.codehacks.blog.auth.controller;

import com.codehacks.blog.auth.dto.LoginRequest;
import com.codehacks.blog.auth.dto.PasswordChangeRequest;
import com.codehacks.blog.auth.dto.RegisterRequest;
import com.codehacks.blog.auth.dto.RoleChangeRequest;
import com.codehacks.blog.auth.dto.UserDTO;
import com.codehacks.blog.auth.model.CustomUserDetails;
import com.codehacks.blog.auth.model.Role;
import com.codehacks.blog.auth.model.User;
import com.codehacks.blog.auth.service.AuthService;
import com.codehacks.blog.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("testUser", "Test@123", "user@example.com", "USER");
        UserDTO responseDTO = new UserDTO("user@example.com", "testUser", Role.USER.name());

        when(authService.registerUser(any(), anyString())).thenReturn(responseDTO);

        mockMvc.perform(post(Constants.AUTH_PATH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "Test@123");

        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userDetails.getEmail()).thenReturn("user@example.com");
        when(userDetails.getRole()).thenReturn(Role.ADMIN);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authService.authenticate(userDetails)).thenReturn("mockToken");

        mockMvc.perform(post(Constants.AUTH_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("mockToken"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "SUBSCRIBER")
    void testChangePassword_Success() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest("testUser", "OldPass@123", "NewPass@456");

        doNothing().when(authService).changePassword(anyString(), anyString(), anyString());

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testChangeUserRole_Success() throws Exception {
        RoleChangeRequest request = new RoleChangeRequest("testUser", Role.SUBSCRIBER);

        User mockUser = new User();
        mockUser.setUsername("testUser");
        mockUser.setRole(Role.SUBSCRIBER);

        when(authService.changeUserRole(request.username(), request.userRole())).thenReturn(mockUser);

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("SUBSCRIBER"))
                .andExpect(jsonPath("$.message").value("Role changed successfully"))
                .andDo(print());

        verify(authService).changeUserRole(request.username(), request.userRole());
    }

//    @Test
//    @WithMockUser(username = "subscriberUser", roles = "SUBSCRIBER")
//    void testChangeUserRole_Forbidden() throws Exception {
//        CustomUserDetails userDetails = new CustomUserDetails(
//                "subscriberUser",
//                "Password&123",
//                "subscriber@example.com",
//                Role.SUBSCRIBER,
//                List.of(new SimpleGrantedAuthority("ROLE_SUBSCRIBER")),
//                true);
//
//        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//        SecurityContextHolder.getContext().setAuthentication(auth);
//
//        RoleChangeRequest request = new RoleChangeRequest("testUser", Role.USER);

//        when(authService.changeUserRole(anyString(), any()))
//                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access Denied"));

//        mockMvc.perform(put(Constants.AUTH_PATH + "/change-role")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andDo(print())
//                .andExpect(status().isForbidden());
//
//        verify(authService, never()).changeUserRole(anyString(), any());
//    }

    @Test
    @WithMockUser(roles = "USER")
    void testDeleteAccount_Failure_WhenUserRoleDeletesOwnAccount() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(
                "testUser",
                "Password123",
                "test@example.com",
                Role.USER,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                true);

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(delete(Constants.AUTH_PATH + "/delete-account")
                        .param("username", "testUser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Not authorized to delete this account"));

        verify(authService, never()).deleteUserAccount("testUser");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteAccount_Success_WhenAdminRoleDeletesOwnAccount() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(
                "testUser",
                "Password123",
                "test@example.com",
                Role.ADMIN,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                true);

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(delete(Constants.AUTH_PATH + "/delete-account")
                        .param("username", "testUser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account deleted successfully"));

        verify(authService).deleteUserAccount("testUser");
    }

    @Test
    void testLogout_Success() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(
                "testUser",
                "Test&123g",
                "user@example.com",
                Role.USER,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                true);

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post(Constants.AUTH_PATH + "/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"))
                .andDo(print());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminEndpoint_Success() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(
                "adminUser",
                "Admin&123",
                "admin@example.com",
                Role.ADMIN,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                true);

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        doNothing().when(authService).logAdminAccess(anyString(), anyString());

        mockMvc.perform(post(Constants.AUTH_PATH + "/admin-only")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Admin access granted"));

        verify(authService).logAdminAccess(anyString(), anyString());
    }

    @Test
    void testAdminEndpoint_Unauthenticated() throws Exception {
        mockMvc.perform(post(Constants.AUTH_PATH + "/admin-only"))
                .andExpect(status().isUnauthorized());
    }
}
