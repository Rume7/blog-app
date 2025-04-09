package com.codehacks.blog.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

//@SpringBootTest
//@AutoConfigureMockMvc
//@Testcontainers
//@TestPropertySource(locations = "classpath:application-test.properties")
class SecurityConfigTest {

//    @TestConfiguration
//    static class TestConfig {
//        @Bean
//        public BCryptPasswordEncoder passwordEncoder() {
//            return new BCryptPasswordEncoder();
//        }
//    }
//
//    @Container
//    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
//            .withDatabaseName("blog_test_db")
//            .withUsername("test")
//            .withPassword("test")
//            .withStartupTimeoutSeconds(180);
//
//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        boolean isCiEnvironment = Boolean.parseBoolean(System.getenv("CI")); // More reliable check
//        String dbUrl = isCiEnvironment
//                ? "jdbc:postgresql://localhost:5432/blog_test_db"  // Use GitHub Actions service
//                : postgres.getJdbcUrl();                    // Use Testcontainers locally
//
//        registry.add("spring.datasource.url", () -> dbUrl);
//        registry.add("spring.datasource.username", () -> "test");
//        registry.add("spring.datasource.password", () -> "test");
//
//        System.out.println("⚡ CI Environment: " + isCiEnvironment);
//        System.out.println("⚡ Using Database URL: " + dbUrl);
//    }
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private BCryptPasswordEncoder passwordEncoder;
//
//    private static final String AUTH_PATH = "/api/v1/auth";
//
//    @BeforeEach
//    void setUp() {
//        System.out.println("🔄 Clearing users before tests...");
//
//        userRepository.deleteAllInBatch();
//
//        User testUser = new User();
//        testUser.setUsername("testUser");
//        testUser.setEmail("testuser@example.com");
//        testUser.setPassword(passwordEncoder.encode("password"));
//        testUser.setRole(Role.USER);
//        userRepository.save(testUser);
//
//        User adminUser = new User();
//        adminUser.setUsername("admin");
//        adminUser.setEmail("admin@example.com");
//        adminUser.setPassword(passwordEncoder.encode("adminPassword"));
//        adminUser.setRole(Role.ADMIN);
//        userRepository.save(adminUser);
//
//        System.out.println("✅ Users added to the database successfully!");
//    }
//
//    @Test
//    @WithMockUser(username = "testUser", roles = "USER")
//    void givenUserWithRoleUser_whenAccessRestrictedEndpoint_thenForbidden() throws Exception {
//        mockMvc.perform(delete(AUTH_PATH + "/delete-account")
//                        .with(csrf().asHeader())
//                        .with(user("testUser").roles("USER")))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @WithMockUser(username = "admin", roles = "ADMIN")
//    void givenUserWithAdminRole_whenAccessAdminEndpoint_thenOk() throws Exception {
//        RoleChangeRequest roleChangeRequest = new RoleChangeRequest("testUser", Role.SUBSCRIBER);
//
//        mockMvc.perform(put(AUTH_PATH + "/change-role")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(new ObjectMapper().writeValueAsString(roleChangeRequest)))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void givenNoAuth_whenAccessRestrictedEndpoint_thenUnauthorized() throws Exception {
//        mockMvc.perform(put(AUTH_PATH + "/change-password")
//                        .with(csrf()))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    void givenValidCorsConfig_whenMakingCorsRequest_thenPass() throws Exception {
//        mockMvc.perform(options(AUTH_PATH + "/login")
//                        .header("Origin", "http://localhost:4200")
//                        .header("Access-Control-Request-Method", "POST"))
//                .andExpect(status().isOk())
//                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
//                .andExpect(header().exists("Access-Control-Allow-Methods"));
//    }
//
//    @Test
//    void givenInvalidCorsConfig_whenMakingCorsRequest_thenFail() throws Exception {
//        mockMvc.perform(options(AUTH_PATH + "/login")
//                        .header("Origin", "https://untrusted-domain.com")
//                        .header("Access-Control-Request-Method", "POST"))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @WithMockUser(username = "testUser", roles = "USER")
//    void testSecurityConfigurationWithUserRole() throws Exception {
//        PasswordChangeRequest passwordChangeRequest = PasswordChangeRequest.builder()
//                .username("testUser")
//                .currentPassword("password")
//                .newPassword("newPass@123X")
//                .build();
//
//        mockMvc.perform(put(AUTH_PATH + "/change-password")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(new ObjectMapper().writeValueAsString(passwordChangeRequest)))
//                .andExpect(status().isOk());
//    }
}