package com.amazonaws.cloudformation.proxy.service;

import com.amazonaws.cloudformation.proxy.ExistsException;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@lombok.Data
@lombok.EqualsAndHashCode
@lombok.ToString
public class ServiceClient {
    private final Map<String, Repository> respositories =
        new HashMap<>(10);
    private final AwsServiceException.Builder builder = Mockito.mock(AwsServiceException.Builder.class);

    public CreateResponse createRepository(CreateRequest r) {
        if (respositories.containsKey(r.getRepoName())) {
            throw new ExistsException(builder);
        }

        assertCredentials(r);

        Repository repo = new Repository();
        repo.setRepoName(r.getRepoName());
        String arn = r.getRepoName() + "-" + UUID.randomUUID().toString();
        repo.setArn(arn);
        repo.setCreated(DateTime.now());
        Set<String> users = new HashSet<>();
        users.add(r.getUserName());
        repo.setUsers(users);
        respositories.put(r.getRepoName(), repo);

        return new CreateResponse.Builder().repoName(r.getRepoName()).build();
    }

    public DescribeResponse describeRespository(DescribeRequest r) {
        Repository repo = respositories.get(r.getRepoName());
        if (repo == null) {
            throw new NotFoundException(builder);
        }
        assertCredentials(r);
        return new DescribeResponse.Builder().createdWhen(repo.getCreated())
            .repoArn(repo.getArn()).repoName(repo.getRepoName()).build();
    }

    private void assertCredentials(AwsRequest r) {
        Assertions.assertTrue(r.overrideConfiguration().isPresent());
        r.overrideConfiguration().ifPresent(config -> {
            Assertions.assertTrue(config.credentialsProvider().isPresent());
            config.credentialsProvider().ifPresent(
                p -> {
                    Assertions.assertTrue(p instanceof StaticCredentialsProvider);
                    AwsCredentials creds = p.resolveCredentials();
                    Assertions.assertEquals(creds.accessKeyId(), "accessKeyId");
                    Assertions.assertEquals(creds.secretAccessKey(), "secretKey");
                }
            );
        });

    }
}
