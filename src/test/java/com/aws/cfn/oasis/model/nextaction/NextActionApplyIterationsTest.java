package com.aws.cfn.oasis.model.nextaction;

import com.aws.cfn.oasis.model.Response;
import com.aws.cfn.oasis.model.iteration.Iteration;
import com.aws.cfn.proxy.OperationStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class NextActionApplyIterationsTest {

    @Mock private List<Iteration> iterations;
    @Mock private Response<Object> response;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateNextAction_nullIterations_expectFailureFromConstructor() {
        new NextActionApplyIterations<>(null);
    }

    @Test
    public void testOperationStatus_expectSuccess() {
        final NextActionApplyIterations<Object> nextAction = new NextActionApplyIterations<>(iterations);
        assertEquals(OperationStatus.SUCCESS, nextAction.getOperationStatus());
    }

    @Test
    public void testDecorateResponse_validIterationList_expectResponseIncludesIterations() {
        final NextActionApplyIterations<Object> nextAction = new NextActionApplyIterations<>(iterations);

        nextAction.decorateResponse(response);

        verify(response).setOutputModel(iterations);
        verifyNoMoreInteractions(response);
    }
}