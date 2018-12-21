package com.aws.rpdk;

import lombok.Data;

@Data
public class AWSCredentials {

    private String accessKey;
    private String secretKey;
    private String sessionToken;

}
