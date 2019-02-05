package com.aws.cfn;
import lombok.Data;
import java.util.Map;

@Data
public class ResponseData {

    private Map<String, Object> resourceModel;
}
