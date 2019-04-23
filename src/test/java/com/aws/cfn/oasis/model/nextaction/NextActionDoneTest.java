package com.aws.cfn.oasis.model.nextaction;

import com.aws.cfn.oasis.model.Response;
import com.aws.cfn.proxy.OperationStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class NextActionDoneTest {

    @Mock private Response<Object> response;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateNextAction_noMessage_happyCase() {
        final NextActionDone<Object> nextAction = new NextActionDone<>();

        assertEquals(false, nextAction.getMessage().isPresent());
        assertEquals(OperationStatus.SUCCESS, nextAction.getOperationStatus());
    }

    @Test
    public void testCreateNextAction_withMessage_happyCase() {
        final NextActionDone<Object> nextAction = new NextActionDone<>("My message");

        assertEquals(true, nextAction.getMessage().isPresent());
        assertEquals("My message", nextAction.getMessage().get());
        assertEquals(OperationStatus.SUCCESS, nextAction.getOperationStatus());
    }

    @Test
    public void testDecorateResponse_withMessage_expectMessageInResponse() {
        final NextActionDone<Object> nextAction = new NextActionDone<>("My message");

        nextAction.decorateResponse(response);

        verify(response).setMessage("My message");
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDecorateResponse_withoutMessage_expectMessageInResponse() {
        final NextActionDone<Object> nextAction = new NextActionDone<>();

        nextAction.decorateResponse(response);

        verifyNoMoreInteractions(response);
    }
}
