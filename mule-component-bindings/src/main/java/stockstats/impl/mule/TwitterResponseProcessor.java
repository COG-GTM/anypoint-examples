/**
 * MuleSoft Examples
 * Copyright 2014 MuleSoft, Inc.
 *
 * This product includes software developed at
 * MuleSoft, Inc. (http://www.mulesoft.com/).
 */

package stockstats.impl.mule;

import java.util.List;
import java.util.stream.Collectors;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;

import stockstats.impl.Tweet;
import twitter4j.QueryResult;

/**
 * A Mule custom transformer that transforms the Mule Twitter cloud connector response
 * into the TwitterService.search response type.
 */
public class TwitterResponseProcessor implements MessageProcessor {
	
	
	@Override
	public MuleEvent process(MuleEvent event) throws MuleException {
		
		MuleMessage msg = event.getMessage();
		
		QueryResult queryResult = (QueryResult) msg.getPayload();
		
		List<Tweet> tweetList = queryResult.getTweets().stream()
				.map(t -> {
					Tweet tweet = new Tweet();
					tweet.setId(t.getId() + "");
					tweet.setText(t.getText());
					return tweet;
				})
				.collect(Collectors.toList());
		msg.setPayload(tweetList);
		
		return event;
	}

}
