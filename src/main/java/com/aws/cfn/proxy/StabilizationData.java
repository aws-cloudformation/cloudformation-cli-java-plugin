package com.amazonaws.cloudformation.proxy;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StabilizationData {
    private Double delayBase;
    private Integer initialDelay;
    private Integer maxDelay;
    private StabilizationMode stabilizationMode;
    private Integer stabilizationTime;
}
