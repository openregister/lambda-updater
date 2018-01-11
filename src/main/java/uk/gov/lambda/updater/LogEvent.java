package uk.gov.lambda.updater;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClient;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.cloudfront.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LogEvent implements RequestHandler<SNSEvent, Object> {

    public Object handleRequest(SNSEvent request, Context context) {
        final String postUrl;
        final String messageText;
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

        context.getLogger().log("Invocation started: " + timeStamp);

        postUrl = System.getenv("HOOK_URL");
        context.getLogger().log("[DEBUG] URL: " + postUrl);
        messageText = request.getRecords().get(0).getSNS().getMessage();

        context.getLogger().log("[DEBUG] Message to be sent: " + messageText);

        try {
            String tmpMessage = messageText.replace("{", "")
                    .replace("\"", "")
                    .replace(",", ", ")
                    .replace(":", ": ")
                    .replace("}", "")
                    // country registers to URL
                    .replace("registerName: country", "*registerName*: <https://country.register.gov.uk/|country>")
                    // payload bold
                    .replace("payload", "\\n*payload*")
                    .replace("add-item", "\\nadd-item")
                    .replace("append-entry", "\\nappend-entry");

            context.getLogger().log("[DEBUG] tmpMessage: " + tmpMessage);

            final HttpResponse<String> httpResponse = Unirest.post(postUrl)
                    .header("accept", "application/json")
                    .body("{\"text\":\"" + tmpMessage + "\", \"username\": \"updater-bot\", \"icon_emoji\": \":japanese_ogre:\"}")
                    .asString();

            context.getLogger().log("[INFO] Response status: " + httpResponse.getStatus());
        } catch (UnirestException e) {
            context.getLogger().log("[ERROR] Doing the call: " + e.getMessage());
        }

        timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation completed: " + timeStamp);

        context.getLogger().log("Invalidation started: " + timeStamp);

        context.getLogger().log("[DEBUG] 1) Credentials created");
        AmazonCloudFrontClient client = new AmazonCloudFrontClient(new EnvironmentVariableCredentialsProvider());
        context.getLogger().log("[DEBUG] 2) Client created");
        Paths invalidation_paths = new Paths().withItems("/records/*", "/records").withQuantity(2);
        context.getLogger().log("[DEBUG] 3) Invalidations path created");
        InvalidationBatch invalidation_batch = new InvalidationBatch(invalidation_paths, String.valueOf(System.currentTimeMillis()));
        context.getLogger().log("[DEBUG] 4) Invalidation batch set");
        CreateInvalidationRequest invalidation = new CreateInvalidationRequest("EXTJYVM2DAM9O", invalidation_batch);
        context.getLogger().log("[DEBUG] 5) Request created");
        CreateInvalidationResult ret = client.createInvalidation(invalidation);
        context.getLogger().log("[DEBUG] 6) Call done:" + ret.getInvalidation().getStatus());

        context.getLogger().log("Invalidation completed: " + timeStamp);

        return null;
    }
}