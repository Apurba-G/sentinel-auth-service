package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test.repo@example.com")
                .username("testrepo")
                .password("encoded_pass")
                .firstName("Test")
                .lastName("User")
                .accountStatus("ACTIVE")
                .emailVerified(true)
                .build();

        entityManager.persistAndFlush(testUser);
    }

    @Test
    void testFindByEmailSuccess() {
        Optional<User> found = userRepository.findByEmail("test.repo@example.com");

        assertTrue(found.isPresent());
        assertEquals("test.repo@example.com", found.get().getEmail());
    }

    @Test
    void testExistsByEmailReturnsTrue() {
        Boolean exists = userRepository.existsByEmail("test.repo@example.com");

        assertTrue(exists);
    }

    @Test
    void testExistsByEmailReturnsFalse() {
        Boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertFalse(exists);
    }
}
