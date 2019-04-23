package com.aws.cfn.oasis.model.nextaction;

import com.aws.cfn.oasis.model.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class NextActionInProgressTest {

    private static final Object CALLBACK_CONTEXT = new Object();
    @Mock private Response<Object> response;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreate_noContextNoMessage_happyCase() {
        final NextActionInProgress<Object> nextAction = new NextActionInProgress<>(5);

        assertEquals(5, nextAction.getCallbackDelayMinutes());
        assertEquals(false, nextAction.getCallbackContext().isPresent());
        assertEquals(false, nextAction.getMessage().isPresent());
    }

    @Test
    public void testCreate_withContextNoMessage_happyCase() {
        final NextActionInProgress<Object> nextAction = new NextActionInProgress<>(
                5,
                CALLBACK_CONTEXT
        );

        assertEquals(5, nextAction.getCallbackDelayMinutes());
        assertEquals(true, nextAction.getCallbackContext().isPresent());
        assertEquals(false, nextAction.getMessage().isPresent());
        assertEquals(CALLBACK_CONTEXT, nextAction.getCallbackContext().get());
    }

    @Test
    public void testCreate_noContextWithMessage_happyCase() {
        final NextActionInProgress<Object> nextAction = new NextActionInProgress<>(
                5,
                "Message"
        );

        assertEquals(5, nextAction.getCallbackDelayMinutes());
        assertEquals(false, nextAction.getCallbackContext().isPresent());
        assertEquals(true, nextAction.getMessage().isPresent());
        assertEquals("Message", nextAction.getMessage().get());
    }

    @Test
    public void testCreate_WithContextWithMessage_happyCase() {
        final NextActionInProgress<Object> nextAction = new NextActionInProgress<>(
                5,
                CALLBACK_CONTEXT,
                "Message"
        );

        assertEquals(5, nextAction.getCallbackDelayMinutes());
        assertEquals(true, nextAction.getCallbackContext().isPresent());
        assertEquals(true, nextAction.getMessage().isPresent());
        assertEquals(CALLBACK_CONTEXT, nextAction.getCallbackContext().get());
        assertEquals("Message", nextAction.getMessage().get());
    }

    @Test
    public void testDecorateResponse_withContextAndMessage_expectBothAddedToResponse() {
        final NextActionInProgress<Object> nextAction = new NextActionInProgress<>(
                5,
                CALLBACK_CONTEXT,
                "Message"
        );

        nextAction.decorateResponse(response);

        verify(response).setMessage("Message");
        verify(response).setCallbackContext(CALLBACK_CONTEXT);
        verify(response).setCallbackDelayMinutes(5);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDecorateResponse_noContextNoMessage_expectOnlyMinutesAddedToResponse() {
        final NextActionInProgress<Object> nextAction = new NextActionInProgress<>(5);

        nextAction.decorateResponse(response);

        verify(response).setCallbackDelayMinutes(5);
        verifyNoMoreInteractions(response);
    }
}
