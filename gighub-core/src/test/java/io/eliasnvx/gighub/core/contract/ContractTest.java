package io.eliasnvx.gighub.core.contract;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import io.eliasnvx.gighub.core.model.Contract;
import io.eliasnvx.gighub.core.model.ContractType;
import io.eliasnvx.gighub.core.model.ContractStatus;

/**
 * Basic tests for Contract functionality
 */
public class ContractTest {
    
    private UUID testPlayerId;
    private Contract testContract;
    
    @BeforeEach
    void setUp() {
        testPlayerId = UUID.randomUUID();
        testContract = Contract.builder()
            .id(UUID.randomUUID())
            .ownerUuid(testPlayerId)
            .type(ContractType.MINING)
            .title("Test Contract")
            .description("Test Description")
            .reward(1000.0)
            .deadline(7L)
            .status(ContractStatus.OPEN)
            .build();
    }
    
    @Test
    void testContractCreation() {
        assertNotNull(testContract);
        assertEquals(testPlayerId, testContract.getOwnerUuid());
        assertEquals(ContractType.MINING, testContract.getType());
        assertEquals("Test Contract", testContract.getTitle());
        assertEquals(1000.0, testContract.getReward());
        assertEquals(ContractStatus.OPEN, testContract.getStatus());
    }
    
    @Test
    void testContractBuilder() {
        Contract builtContract = Contract.builder()
            .ownerUuid(testPlayerId)
            .type(ContractType.BUILDING)
            .title("Building Contract")
            .description("Build something")
            .reward(5000.0)
            .deadline(14L)
            .build();
            
        assertNotNull(builtContract);
        assertEquals(ContractType.BUILDING, builtContract.getType());
        assertEquals("Building Contract", builtContract.getTitle());
        assertEquals(5000.0, builtContract.getReward());
    }
    
    @Test
    void testContractBasicProperties() {
        // Test basic property access
        assertNotNull(testContract.getId());
        assertEquals(testPlayerId, testContract.getOwnerUuid());
        assertNull(testContract.getContractorUuid()); // Should be null initially
        
        // Test status
        assertEquals(ContractStatus.OPEN, testContract.getStatus());
        
        // Test type
        assertEquals(ContractType.MINING, testContract.getType());
    }
    
    @Test
    void testContractValidation() {
        // Test valid contract
        assertNotNull(testContract);
        assertNotNull(testContract.getTitle());
        assertTrue(testContract.getReward() > 0);
        assertTrue(testContract.getDeadline() > 0);
        
        // Test contract with negative reward (should still create but validation logic may be added later)
        Contract invalidContract = Contract.builder()
            .ownerUuid(testPlayerId)
            .type(ContractType.MINING)
            .title("Invalid Contract")
            .description("Invalid")
            .reward(-100.0)
            .deadline(7L)
            .build();
            
        assertNotNull(invalidContract);
        assertEquals(-100.0, invalidContract.getReward());
    }
}
