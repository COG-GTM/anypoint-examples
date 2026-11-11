package org.mule.examples.springboot.mulecomponentbindings;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;

/**
 * The component binding {@code <binding interface="TwitterService" method="search">} to
 * {@code vm://twitterFlowRequest}: a messaging gateway implementing TwitterService sends the call to the
 * {@code twitterFlowRequest} channel, where {@code TwitterFlow} (TwitterRequestProcessor → twitter:search →
 * TwitterResponseProcessor) runs.
 */
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class TwitterBindingFlowConfig {

    public static final String CHANNEL = "twitterFlowRequest";

    public record TwitterSearchCall(String stock, Date date, int pageSize, int page) {
    }

    public record TwitterSearchQuery(String query, String since, String until) {
    }

    @MessagingGateway(defaultRequestChannel = CHANNEL)
    public interface TwitterFlowGateway {
        List<Model.Tweet> search(TwitterSearchCall call);
    }

    @Bean
    public Services.TwitterService twitterService(TwitterFlowGateway gateway) {
        return (stock, date, pageSize, page) -> gateway.search(new TwitterSearchCall(stock, date, pageSize, page));
    }

    @Bean
    public IntegrationFlow twitterFlow(TwitterClient client) {
        return IntegrationFlow.from(CHANNEL)
                .<TwitterSearchCall, TwitterSearchQuery>transform(TwitterBindingFlowConfig::prepare)
                .<TwitterSearchQuery>handle((q, h) -> client.search(q.query(), q.since(), q.until()))
                .get();
    }

    /** TwitterRequestProcessor: stock → "$stock", since = date, until = date + 10 days; rpp/page are unused. */
    static TwitterSearchQuery prepare(TwitterSearchCall call) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.setTime(call.date());
        cal.add(Calendar.DATE, 10);
        return new TwitterSearchQuery("$" + call.stock(), df.format(call.date()), df.format(cal.getTime()));
    }
}
