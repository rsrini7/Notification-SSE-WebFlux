package com.example.notification.repository;

import com.example.notification.model.UserPreferences;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserPreferencesRepositoryTest {

    @Autowired
    private UserPreferencesRepository repository;

    @Test
    void testSaveAndRetrieveUserPreferencesWithMutedTypes() {
        // Create a UserPreferences object
        UserPreferences preferences = new UserPreferences();
        preferences.setUserId("testUser123");
        preferences.setEmailEnabled(true);
        preferences.setMutedNotificationTypes(Set.of("ORDER_CONFIRMATION", "SHIPPING_UPDATE"));

        // Save the entity
        repository.save(preferences);
        repository.flush(); // Ensure data hits the DB

        // Retrieve the entity
        Optional<UserPreferences> retrievedPreferencesOptional = repository.findByUserId("testUser123");

        // Assert that the Optional is present
        assertThat(retrievedPreferencesOptional).isPresent();

        // Assert properties of the retrieved UserPreferences object
        UserPreferences retrievedPreferences = retrievedPreferencesOptional.get();
        assertThat(retrievedPreferences.getUserId()).isEqualTo("testUser123");
        assertThat(retrievedPreferences.isEmailEnabled()).isTrue();
        assertThat(retrievedPreferences.getMutedNotificationTypes())
                .isNotNull()
                .hasSize(2)
                .containsExactlyInAnyOrder("ORDER_CONFIRMATION", "SHIPPING_UPDATE");
    }
}
