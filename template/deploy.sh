#!sh
ACCOUNT=123456789012
HANDLERS_STACK_NAME=cfn-initech-tps-report-handlers
INFRA_STACK_NAME=cfn-handler-infra
RESOURCE_TYPE=Initech-TPS-Report
TEST_PAYLOAD='{"action":"Create","requestContext":{"invocation":1,"resourceType":"Initech::TPS::Report"},"resourceModel":{"author":"Peter","title":"PC Load Letter","typeName":"Initech::TPS::Report"}}'
OUTFILE=response.out

# create infra stack
awscfn create-stack --stack-name $INFRA_STACK_NAME --template-body file://./CloudFormationHandlerInfrastructure.yaml --capabilities CAPABILITY_NAMED_IAM --parameters ParameterKey=ManagementUserArn,ParameterValue="arn:aws:iam::$ACCOUNT:root"

# wait til its done...
...

# make sure infra stack can't be accidentally deleted
awscfn update-termination-protection --stack-name cfn-handler-infra --enable-termination-protection 

# I am no sed/awk guru but this works...
BUCKET=`aws cloudformation list-exports | grep ArtifactBucket -A1 | awk '/Value/ {print $0}' | sed 's/"Value": "//g' | sed 's/"//g' | sed 's/ //g'`

# Upload handler pkg to S3 bucket. NOTE: Bucket is using KMS so your user needs to be able to use the KMS Key (Infra Stack should set this up automatically)
aws s3 cp ../target/ResourceProviderExample-1.0.jar s3://$BUCKET

# Create the handlers
awscfn create-stack --stack-name $STACK_NAME --template-body file://./Handlers.yaml --parameters ParameterKey=ResourceType,ParameterValue=$RESOURCE_TYPE ParameterKey=PackageS3Key,ParameterValue=ResourceProviderExample-1.0.jar

# Get handler Function ARN
CREATE_HANDLER_ARN=`aws cloudformation list-exports | grep "$RESOURCE_TYPE-create-handler" -A1 | awk '/Value/ {print $0}' | sed 's/"Value": "//g' | sed 's/"//g' | sed 's/ //g'`


# Test Invoke that puppy
aws lambda invoke --function-name $CREATE_HANDLER_ARN --payload '{"action":"Create","requestContext":{"invocation":1,"resourceType":"Initech::TPS::Report"},"resourceModel":{"author":"Peter","title":"PC Load Letter","typeName":"Initech::TPS::Report"}}' $OUTFILE
less $OUTFILE
rm $OUTFILE
