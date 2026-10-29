package com.atithi.aavas.models;

import com.atithi.aavas.enums.HostelStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HostelTest {

    @Test
    @DisplayName("prePersist should generate a random token if token is null or blank")
    void testPrePersistGeneratesTokenWhenAbsent() {
        Hostel hostel = Hostel.builder()
                .name("Green PG")
                .status(HostelStatus.ACTIVE)
                .build();

        assertNull(hostel.getToken());

        hostel.prePersist();

        assertNotNull(hostel.getToken());
        assertEquals(32, hostel.getToken().length());
        assertTrue(hostel.getToken().matches("^[0-9a-f]{32}$"));
    }

    @Test
    @DisplayName("prePersist should preserve existing token if already set")
    void testPrePersistPreservesExistingToken() {
        String existingToken = "customtoken1234567890abcdef12345";
        Hostel hostel = Hostel.builder()
                .name("Green PG")
                .token(existingToken)
                .status(HostelStatus.ACTIVE)
                .build();

        hostel.prePersist();

        assertEquals(existingToken, hostel.getToken());
    }
}
