package com.shop.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.UserRepository;
import com.shop.ecommerce.service.PasswordService;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EcommerceApplicationTests {

	@Autowired private MockMvc mvc;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private UserRepository userRepository;
	@Autowired private PasswordService passwordService;

	@Test
	void contextLoads() {
	}

	@Test
	void publicCatalogIsAvailableButProductMutationRequiresAnAdminToken() throws Exception {
		mvc.perform(get("/api/products"))
				.andExpect(status().isOk());

		mvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void customerTokenCanAccessOwnCartButCannotReadAnotherUsersCart() throws Exception {
		JsonNode firstUser = registerUser();
		JsonNode secondUser = registerUser();
		String token = firstUser.path("accessToken").asText();
		long ownId = firstUser.path("id").asLong();
		long otherId = secondUser.path("id").asLong();

		mvc.perform(get("/api/cart/user/{userId}", ownId)
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mvc.perform(get("/api/admin/users")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());

		mvc.perform(get("/api/cart/user/{userId}", otherId)
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminAuthorizationUsesSignedIdentityInsteadOfClientSuppliedAdminId() throws Exception {
		User admin = createUser("ADMIN");
		User customer = createUser("CUSTOMER");
		String adminToken = login(admin.getEmail());
		String customerToken = login(customer.getEmail());

		mvc.perform(get("/api/admin/users")
				.param("adminUserId", customer.getId().toString())
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		mvc.perform(get("/api/admin/users")
				.param("adminUserId", admin.getId().toString())
				.header("Authorization", "Bearer " + customerToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void legacySha256PasswordIsUpgradedToBcryptAfterSuccessfulLogin() throws Exception {
		String password = "legacy-password";
		User legacy = createUser("CUSTOMER");
		legacy.setPassword(Base64.getEncoder().encodeToString(
				MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8))));
		userRepository.save(legacy);

		mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(java.util.Map.of(
						"email", legacy.getEmail(),
						"password", password
				))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());

		org.junit.jupiter.api.Assertions.assertTrue(userRepository.findById(legacy.getId()).orElseThrow()
				.getPassword().startsWith("$2"));
	}

	private User createUser(String role) {
		String suffix = UUID.randomUUID().toString().replace("-", "");
		User user = new User();
		user.setName("Test " + role);
		user.setEmail(suffix + "@example.test");
		user.setMobile("8" + suffix.substring(0, 9));
		user.setPassword(passwordService.hash("test-password-123"));
		user.setRole(role);
		return userRepository.save(user);
	}

	private String login(String email) throws Exception {
		MvcResult result = mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(java.util.Map.of(
						"email", email,
						"password", "test-password-123"
				))))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
	}

	private JsonNode registerUser() throws Exception {
		String suffix = UUID.randomUUID().toString().replace("-", "");
		MvcResult result = mvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(java.util.Map.of(
						"name", "Test Customer",
						"email", suffix + "@example.test",
						"mobile", "9" + suffix.substring(0, 9),
						"password", "test-password-123"
				))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

}
