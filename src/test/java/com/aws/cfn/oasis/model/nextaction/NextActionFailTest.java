package com.aws.cfn.oasis.model.nextaction;

import com.aws.cfn.oasis.model.Response;
import com.aws.cfn.proxy.HandlerErrorCode;
import com.aws.cfn.proxy.OperationStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class NextActionFailTest {

    @Mock private Response<Object> response;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDecorateResponse_expectMessageAndErrorCodeInResponse() {
        final NextActionFail<Object> nextAction = new NextActionFail<>("Test", HandlerErrorCode.AccessDenied);

        nextAction.decorateResponse(response);

        verify(response).setErrorCode(HandlerErrorCode.AccessDenied);
        verify(response).setMessage("Test");
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testOperationStatus_expectStatusFailed() {
        final NextActionFail<Object> nextAction = new NextActionFail<>("Test", HandlerErrorCode.AccessDenied);
        assertEquals(OperationStatus.FAILED, nextAction.getOperationStatus());
    }
}