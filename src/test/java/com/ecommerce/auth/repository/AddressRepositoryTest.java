package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.Address;
import com.ecommerce.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AddressRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AddressRepository addressRepository;

    private User user;
    private Address defaultAddress;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("address.user@example.com")
                .username("addressuser")
                .password("password")
                .accountStatus("ACTIVE")
                .build();

        entityManager.persist(user);

        defaultAddress = Address.builder()
                .user(user)
                .addressType("HOME")
                .fullName("John Address")
                .phone("+919876543210")
                .addressLine1("123 Test St")
                .city("Mumbai")
                .state("Maharashtra")
                .country("India")
                .isDefault(true)
                .build();

        entityManager.persistAndFlush(defaultAddress);
    }

    @Test
    void testFindByUserReturnsAddressList() {
        List<Address> addresses = addressRepository.findByUser(user);

        assertEquals(1, addresses.size());
        assertEquals("Mumbai", addresses.get(0).getCity());
    }

    @Test
    void testFindByUserAndIsDefaultTrueSuccess() {
        Optional<Address> found = addressRepository.findByUserAndIsDefaultTrue(user);

        assertTrue(found.isPresent());
        assertTrue(found.get().getIsDefault());
    }
}
