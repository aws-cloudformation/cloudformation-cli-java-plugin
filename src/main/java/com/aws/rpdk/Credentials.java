package com.aws.rpdk;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Credentials {
    private String accessKeyId;
    private String secretAccessKey;
    private String sessionToken;
}
