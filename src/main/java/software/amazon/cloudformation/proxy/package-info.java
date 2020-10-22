/**
 * This package provide facilities to make it easy to work against AWS APIs that
 * are eventually consistent for applying resource state. Developers need to
 * sequence APIs calls in order to effectively apply state. When dependent
 * resources are not available, e.g. when associating KMS Key with CloudWatch
 * Log group, the key might not have propogated to region leading to failures to
 * associate with the log group. This framework provides developers the
 * facilities to deal with all of these situations using a simple functional
 * model.
 * <p>
 * The framework handles the following for developers:
 * <ol>
 * <li>Taking care of communicating all progress events consistently with
 * CloudFormation including appropriate handshakes</li>
 * <li>Handles all client side error reporting correctly with appropriate
 * failure codes and errors messages</li>
 * <li>Handles common AWS service exceptions like Throttle and several other
 * retry errors based on HTTP codes</li>
 * <li>Can auto-renew STS token expiry transparently if needed or schedule
 * callback from CloudFormation requesting new credentials</li>
 * <li>Automatically handles AWS Lambda runtime constraints and schedules
 * callbacks when we near max computation time. Auto adjusts behavior based on
 * the runtime the handler is bound other modes for operating</li>
 * <li>Memoize all service call requests, responses, stabilization and retry
 * handling that ensures continuations of handler compute start from where we
 * left off for callbacks.</li>
 * <li>Provides default flat retry model for calls of 5 seconds with max timeout
 * of 2 minutes for all API calls. Allows for sensible defaults to be specified
 * for retry strategy by integrator on a per API call basis. Allows framework to
 * dynamically override backoff behavior for helping with emergencies with
 * downstream service latency and other situations defined by customer need</li>
 * <li>Provides developers with a simple sequential pattern of progress chaining
 * while doing all the heavy lifting above. Provides most common sensible
 * defaults with override capabilities for integrator to influence as needed.
 * Covers power users and simple users alike.</li>
 * <li>Brings consistent model across all service integrations across AWS
 * services on GitHub hosted code base.</li>
 * </ol>
 * <p>
 * <b><u>Anatomy of an AWS Web Service call</u></b>
 * <p>
 * Most AWS web service API calls follows a typical pattern shown below:
 * <ol>
 * <li>Initiate the call context context with an unique name for making an API
 * call. This name is used inside the callback context to track the different
 * parts of the API call sequence being made. All bookkeeping inside the
 * {@link software.amazon.cloudformation.proxy.StdCallbackContext} is prefixed
 * with this unique name for tracking E.g.
 * {@code initiator.initiate("logs:CreateLogGroup")} uses the AWS IAM action
 * name (recommended naming convention to understand permission in code) when
 * invoking CloudWatchLogs CreateLogGroup API. Developers can retrieve different
 * parts of this API call from
 * {@link software.amazon.cloudformation.proxy.StdCallbackContext#callGraphs()}
 * map. They can retrieve the {@code CreatLogGroupRequest} using
 * "logs.CreateLogGroup.request" key and the corresponding response
 * {@code CreateLogGroupResponse} using "logs.CreateLogGroup.response" key after
 * a successful API call is completed. Developers can use them as needed to get
 * data like ARNs from responses when needed.</li>
 * <li>Translate the incoming CFN resource model properties to the underlying
 * service API request. E.g.
 * {@code translate(translator::translatetocreaterequest)} translates incoming
 * CFN resource model to {@code CreateLogGroupRequest} for making the API
 * call.</li>
 * <li>Make the actual service call
 * {@code (r, c) -> c.injectCredentialsAndInvokeV2(r, c.client()::createLogGroup))}
 * The above code ensures that the right credentials are used to make the
 * service call. Developers can create the AWS services client statically and
 * re-use across all calls without worry. Developers can use response object
 * from the API call to update CloudFormation resource model.Typically we setup
 * ARN for the resource post creation. in Create Handlers. It is essential to
 * set these to communicate back to CloudFormation to indicate the handler did
 * successfully start creation and is in flight. Developers do not need to worry
 * about this handshake, the framework already takes care of this. Developers do
 * need to set primary identifier like ARNs if needed from response object to
 * update incoming resource model. This is shown in bold in code below
 *
 * <pre>
 * <code>
 *             return initiator.initiate("networkmanager:CreateGlobalNetwork")
 *             //
 *             // Make the request object to create from the model
 *             //
 *             .translate(model -&gt;
 *                 CreateGlobalNetworkRequest.builder()
 *                     .description(model.getDescription())
 *                     .tags(Utils.cfnTagsToSdkTags(model.getTags()))
 *                     .build())
 *             //
 *             // Make the call the create the global network. Delegate to framework to retry all errors and
 *             // report all errors as appropriate including service quote exceeded and others.
 *             //
 *             .call((r, c) -&gt; {
 *                 CreateGlobalNetworkResponse res = c.injectCredentialsAndInvokeV2(r, c.client()::createGlobalNetwork);
 *                 GlobalNetwork network = res.globalNetwork();
 *                 <b>initiator.getResourceModel().setArn(network.globalNetworkArn());
 *                 initiator.getResourceModel().setId(network.globalNetworkId());</b>;
 *                 return res;
 *             })
 *             //
 *             // Check to see if Global Network is available to use directly from the response or
 *             // stabilize as needed. Update model with Arn and Id as primary identifier on the model
 *             // object to communicate to CloudFormation about progress
 *             //
 *             .stabilize((_request, _response, _client, _model, _context) -&gt; {
 *                 GlobalNetworkState state = _response.globalNetwork().state();
 *                 return state == GlobalNetworkState.AVAILABLE ||
 *                     Utils.globalNetwork(_client, state.globalNetworkId()).state() == GlobalNetworkState.AVAILABLE;
 *             }).progress();
 *     </code>
 * </pre>
 *
 * </li>
 * <li>Handle stabilization for the current API. Several AWS services return
 * immediately from the API call, but the resource isn't ready to be consumed,
 * e.g. KMS Key, Kinesis steam, others. Developers might need to wait for the
 * resource to be in a good state, e.g. Kinesis Stream is active before
 * subsequent updates can be applied to the stream. A stabilization Lambda can
 * be optionally added that is a predicate function that returns true when the
 * resource is in the desired state. All Delete Handlers that need to wait for
 * an AWS resource to be deleted completely, will use this pattern show, as
 * shown below
 *
 * <pre>
 * <code>
 *         initiator.initiate("networkmanager:DeleteGlobalNetwork")
 *         //
 *         // convert from ResourceModel to DeleteGlobalNetworkRequest
 *         //
 *         .translate(m -&gt;
 *             DeleteGlobalNetworkRequest.builder()
 *                 .globalNetworkId(m.getId())
 *                 .build())
 *         //
 *         // Make the call to delete the network
 *         //
 *         .call((r, c) -&gt; {
 *             try {
 *                 return c.injectCredentialsAndInvokeV2(r, c.client()::deleteGlobalNetwork);
 *             } catch (ResourceNotFoundException e) {
 *                 // Informs CloudFormation that the resources was deleted already
 *                 <b>throw new software.amazon.cloudformation.exceptions.ResourceNotFoundException(e);</b>
 *             }
 *         })
 *         //
 *         // Wait for global network to transition to complete delete state, which is returned by a
 *         // ResourceNotFoundException from describe call below.
 *         //
 *         .stabilize(
 *             (_request, _response, _client, _model, _context) -&gt; {
 *                 //
 *                 // if we successfully describe it it still exists!!!
 *                 //
 *                 try {
 *                     globalNetwork(_client, _model.getId());
 *                 } catch (ResourceNotFoundException e) {
 *                     return true;
 *                 }
 *                 return false;
 *             }
 *         )
 *         .done(ignored -&gt; ProgressEvent.success(null, context));
 *     </code>
 * </pre>
 *
 * </li>
 * <li>Optionally handle errors, the framework already handles most errors and
 * retries ones that can retried and communicates error codes when appropriate.
 * This is usually the universal catch all exceptions block that can be used to
 * filter exceptions or handle errors across translate, call and stabilize
 * methods</li>
 * <li>Proceed with progressing to the chain next sequence of API calls or
 * indicate successful completion.
 * {@link software.amazon.cloudformation.proxy.OperationStatus#IN_PROGRESS}
 * indicates that we can the proceed to next part of API calls to make for
 * resource configuration. E.g. for CloudWatchLogs LogGroup we first create the
 * LogGroup, then we update retention policy, associated KMS key and finally
 * delegate to Read Handler to return the complete state for the resource
 *
 * <pre>
 * <code>
 *            return
 *                createLogGroup(initiator)
 *                .then(event -&gt; updateRetentionInDays(initiator, event))
 *                .then(event -&gt; associateKMSKey(initiator, event))
 *                // delegate finally to ReadHandler to return complete resource state
 *                .then(event -&gt; new ReadHandler().handleRequest(proxy, request, event.getCallbackContext(), logger));
 *    </code>
 * </pre>
 *
 * Usually the final step in the sequence returns
 * {@link software.amazon.cloudformation.proxy.OperationStatus#SUCCESS}. If any
 * of the steps in between has an error the chain will be skipped to return the
 * error with
 * {@link software.amazon.cloudformation.proxy.OperationStatus#FAILED} status
 * and an appropriate error message
 * software.amazon.cloudformation.proxy.ProgressEvent#getMessage() E.g. if
 * associateKMSKey had an error to associate KMS key for CloudWatchLogs to use,
 * the chain would exit with FAILED stauts and appropriate exception message.
 * Both {@link software.amazon.cloudformation.proxy.OperationStatus#SUCCESS} and
 * {@link software.amazon.cloudformation.proxy.OperationStatus#FAILED} are pivot
 * points in the chain that will skip the remainder of the chain.</li>
 * </ol>
 *
 * When integrating with AWS APIs, here are a few common patterns across Create,
 * Update, Delete (CUD)
 * <ol>
 * <li>Create and Update handlers can share common methods between them to apply
 * update to configuration. Using a common Util class to capture these methods
 * improve sharing and consistency</li>
 * <li>Create and Update handler should delegate to Read handler to return the
 * complete state of the resource including readonly properties from the
 * service</li>
 * <li>Delete Handler must handle resource not found errors/status always when
 * deleting. This ensures that if the resource was removed out of band of CFN,
 * we also remove it from CFN for correctness. The same handling works for
 * stabilization needs to ensure resource was completed</li>
 * </ol>
 * <p>
 * <b><u>When to re-use rebinding functionality for the model</u></b>
 * <p>
 * Rebinding the model is used when the model is immutable by design and we need
 * to create a new instance of the model for each part in the chain. This is to
 * pure for functional programming constructs. Below is an example for
 * traversing list APIs to iterate over to find object of interest. For each
 * iteration the new model must be rebound.
 *
 * <pre>
 * <code>
 *     void discoverIfAlreadyExistsWithAlias() {
 *         ListAliasesResponse aliases = ListAliasesResponse.builder().build();
 *         final BiFunction&lt;CallChain.Initiator&lt;KmsClient, ListAliasesResponse, StdCallbackContext&gt;,
 *             Integer,
 *             ProgressEvent&lt;ListAliasesResponse, StdCallbackContext&gt;&gt; invoker =
 *             (initiator_, iteration) -&gt;
 *                 initiator_
 *                     .initiate("kms:ListAliases-" + iteration)
 *                     .translate(m -&gt; ListAliasesRequest.builder().marker(m.nextMarker()).build())
 *                     .call((r, c) -&gt; c.injectCredentialsAndInvokeV2(r, c.client()::listAliases))
 *                     .success();
 *         int iterationCount = 0;
 *         do {
 *             <b>CallChain.Initiator&lt;KmsClient, ListAliasesResponse, StdCallbackContext&gt; initiator =
 *                 this.initiator.rebindModel(aliases);</b>
 *             ProgressEvent&lt;ListAliasesResponse, StdCallbackContext&gt; result = invoker.apply(initiator, iterationCount);
 *             if (!result.isSuccess()) {
 *                 throw new RuntimeException("Error retrieving key aliases " + result.getMessage());
 *             }
 *             aliases = result.getResourceModel();
 *             AliasListEntry entry = aliases.aliases().stream().filter(e -&gt; e.aliasName().equals(KEY_ALIAS)).findFirst()
 *                 .orElse(null);
 *             if (entry != null) {
 *                 kmsKeyId = entry.targetKeyId();
 *                 aliasArn = entry.aliasArn();
 *                 break;
 *             }
 *             if (aliases.nextMarker() == null) {
 *                 break;
 *             }
 *             ++iterationCount;
 *         } while (kmsKeyId == null);
 *     }
 * </code>
 * </pre>
 *
 * In the above code
 * <ol>
 * <li>The model ListAliasRequest is Immutable object. For the first iteration
 * the model has no next marker</li>
 * <li>Model is rebound in the code {@code this.initiator.rebindModel(aliases)}
 * to the latest batch of alias</li>
 * <li>aliases is reassigned {@code aliases = result.getResourceModel()} for
 * newly retrieved model to rebind for next loop</li>
 * </ol>
 *
 */
package software.amazon.cloudformation.proxy;
