package com.aws.rpdk;

import lombok.Data;

@Data
public class Credentials {
    private String accessKeyId;
    private String secretAccessKey;
    private String sessionToken;
}
