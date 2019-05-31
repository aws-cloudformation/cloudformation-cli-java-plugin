package com.amazonaws.cloudformation.proxy;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResponseData<T> {
    private T resourceModel;
}
