package com.aws.cfn.oasis.model.iteration;

import com.aws.cfn.oasis.model.entity.LogicalId;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class RenameLogicalIdIterationTest {

    private static final LogicalId LOGICAL_ID_1 = new LogicalId("Resource1");
    private static final LogicalId LOGICAL_ID_2 = new LogicalId("Resource2");
    private static final LogicalId LOGICAL_ID_3 = new LogicalId("Resource3");
    private static final LogicalId LOGICAL_ID_4 = new LogicalId("Resource4");

    @Test
    public void testBuildLogicalIdMapping_singleMapping_happyCase() {
        final RenameLogicalIdIteration iter = RenameLogicalIdIteration.builder().logicalIdMapping(
                LOGICAL_ID_1, LOGICAL_ID_2
        ).build();

        final ImmutableMap<LogicalId, LogicalId> expected = ImmutableMap.of(LOGICAL_ID_1, LOGICAL_ID_2);
        assertEquals(expected, iter.getLogicalIdMappings());
    }

    @Test
    public void testBuildLogicalIdMapping_emptyMapping_happyCase() {
        final RenameLogicalIdIteration iter = RenameLogicalIdIteration.builder().build();
        assertEquals(Collections.emptyMap(), iter.getLogicalIdMappings());
    }

    @Test
    public void testBuildLogicalIdMapping_multipleDifferentMappings_happyCase() {
        final RenameLogicalIdIteration iter = RenameLogicalIdIteration.builder()
                                                                      .logicalIdMapping(LOGICAL_ID_1, LOGICAL_ID_2)
                                                                      .logicalIdMapping(LOGICAL_ID_3, LOGICAL_ID_4)
                                                                      .build();
        final ImmutableMap<LogicalId, LogicalId> expected = ImmutableMap.of(
                LOGICAL_ID_1, LOGICAL_ID_2,
                LOGICAL_ID_3, LOGICAL_ID_4
        );

        assertEquals(expected, iter.getLogicalIdMappings());
    }

    @Test(expected = NullPointerException.class)
    public void testBuildingMapping_nullOldName_expectNPEFromBuilder() {
        RenameLogicalIdIteration.builder().logicalIdMapping(null, LOGICAL_ID_1);
    }

    @Test(expected = NullPointerException.class)
    public void testBuildingMapping_nullNewName_expectNPEFromBuilder() {
        RenameLogicalIdIteration.builder().logicalIdMapping(LOGICAL_ID_1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildingMapping_identicalIds_expectIAEFromBuilder() {
        RenameLogicalIdIteration.builder().logicalIdMapping(LOGICAL_ID_1, LOGICAL_ID_1);
    }
}