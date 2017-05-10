package br.com.willianantunes.test.routes;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.twitter.TwitterComponent;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import br.com.willianantunes.test.entity.TwitterMessage;
import twitter4j.Status;

public class TwitterSnifferRoute extends RouteBuilder {	
	@PropertyInject("camel.component.twitter.consumer-key")
	private String consumerKey;	
	@PropertyInject("camel.component.twitter.consumer-secret")
	private String consumerSecret;	
	@PropertyInject("camel.component.twitter.access-token")
	private String accessToken;	
	@PropertyInject("camel.component.twitter.access-token-secret")
	private String accessTokenSecret;

	@Override
	public void configure() throws Exception {
		// Setup Twitter component
        TwitterComponent tc = getContext().getComponent("twitter", TwitterComponent.class);
        tc.setConsumerKey(consumerKey);
        tc.setConsumerSecret(consumerSecret);
        tc.setAccessToken(accessToken);
        tc.setAccessTokenSecret(accessTokenSecret);

        // http://camel.apache.org/json.html        
        JacksonDataFormat myJacksonDataFormat = configureJacksonDataFormat();

        // http://camel.apache.org/twitter.html
        // http://camel.apache.org/jpa.html
        fromF("twitter://search?type=polling&delay=%s&keywords=%s", 5000, "#SP1")
        	.routeId("TwitterSnifferRoute")
        	.log(LoggingLevel.DEBUG, "The user named ${body.user.name} posted the following text at ${body.createdAt}: ${body.text}")
        	.choice()
        		.when(myExchange -> myExchange.getIn().getBody() != null)
	        	.process(myExchange -> {
	        		// Twitter status
	        		Status status = myExchange.getIn().getBody(Status.class);
	        		// My bean to be persisted        		
	        		TwitterMessage myTwitterMessages = new TwitterMessage(status.getUser().getName(), status.getUser().getScreenName(), 
	        				LocalDateTime.ofInstant(status.getCreatedAt().toInstant(), ZoneId.systemDefault()), status.getText());        		
	        		// The body of the In message is assumed to be an entity bean to be persisted
	        		myExchange.getIn().setBody(myTwitterMessages);	        		
	        	})
	        	// Instead of send one exchange at time I could send a list using aggregate EIP, but I'd need to set entityType=java.util.ArrayList parameter to JPA component
	    		.to("jpa:br.com.willianantunes.test.entity.TwitterMessage")	        	
	        	.log("Inserted new TwitterMessage with ID ${body.id}");
        
        // http://camel.apache.org/scheduler.html
        from("scheduler://myScheduler?useFixedDelay=false&delay=10000")
        	.routeId("TweetMessageCountRoute")
        	.pollEnrich(String.format("jpa:br.com.willianantunes.test.entity.TwitterMessage?consumer.namedQuery=%s&consumeDelete=%s", "SELECT-ALL", false))
        	.choice()
        		.when(simple("${body} is 'java.util.List'"))
        			.log("We have ${body.size} tweet messages up until now...")
        			.to("direct:informMyQueue")
        		.otherwise()
        			.log("We have 1 tweet messages up until now...")
        		.endChoice();
        
        // http://camel.apache.org/activemq.html
        // http://camel.apache.org/jms.html
        from("direct:informMyQueue")
        	.routeId("ProducerTweetQueueRoute")
        	.filter(simple("${body.size} > 15"))
        		.split(body()) // This takes our List and create one message for each element
        		.marshal(myJacksonDataFormat)
        		.convertBodyTo(String.class) // jmsMessageType=Text is also an option
				.to("activemq:queue:Tweets.Trends")
				.log("All of rows was sent to the queue Tweets.Trends!")
				.to(String.format("jpa:br.com.willianantunes.test.entity.TwitterMessage?namedQuery=%s&useExecuteUpdate=%s", "DELETE-ALL", true))
				.log("The table was truncated...");

        // http://camel.apache.org/file2.html
        from("activemq:queue:Tweets.Trends")
        	.routeId("ConsumerTweetQueueRoute")
        	.unmarshal(myJacksonDataFormat)
        	.log("The following twitter user is passing by: ${body.userName}")
        	.setHeader("CamelFileName", simple("${body.userName}-${date:now:yyyyMMdd-hhmmss}.json"))
        	.marshal(myJacksonDataFormat).convertBodyTo(String.class) // In order to save a proper JSON text file
        	.to("file:C:\\tmp");
	}

	private JacksonDataFormat configureJacksonDataFormat() {
		JacksonDataFormat myJacksonDataFormat = new JacksonDataFormat(TwitterMessage.class);
        myJacksonDataFormat.setDisableFeatures(String.format("%s,%s", DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.name(), SerializationFeature.WRITE_DATES_AS_TIMESTAMPS.name()));
        myJacksonDataFormat.addModule(new JavaTimeModule());
        myJacksonDataFormat.setPrettyPrint(true);
        return myJacksonDataFormat;
	}	
}