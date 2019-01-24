package com.aws.cfn;

import lombok.Data;

import java.util.Optional;

@Data
public class TestModel {

    private Optional<String> property1;

    private Optional<Integer> property2;

}
