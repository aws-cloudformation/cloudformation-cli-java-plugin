package com.aws.rpdk;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Credentials {

    private static final String ACCESS_KEY_ID = "accessKeyId";
    private static final String SECRET_ACCESS_KEY = "secretAccessKey";
    private static final String SESSION_TOKEN = "sessionToken";

    @JsonProperty(ACCESS_KEY_ID)
    private String accessKeyId;

    @JsonProperty(SECRET_ACCESS_KEY)
    private String secretAccessKey;

    @JsonProperty(SESSION_TOKEN)
    private String sessionToken;
}
