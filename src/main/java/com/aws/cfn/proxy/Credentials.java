package com.amazonaws.cloudformation.proxy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Credentials {
    private String accessKeyId;
    private String secretAccessKey;
    private String sessionToken;
}
