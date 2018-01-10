package uk.gov.lambda.updater;

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

        return null;
    }
}