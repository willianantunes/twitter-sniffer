package br.com.willianantunes.test.bean;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BeanInspector {
	private final Logger logger = LogManager.getLogger(BeanInspector.class);
	
	public void mySampleInvokedMethod(Exchange exchange, CamelContext context) {
		logger.trace("Inspecting all of the content of the Exchange->In->Headers...");
		exchange.getIn().getHeaders().keySet().stream().forEach(myKey -> {
			logger.trace("Key: {} / value: {}", myKey, exchange.getIn().getHeaders().get(myKey));
		});
		
		logger.trace("Inspecting all of the content of the Exchange->Properties...");
		exchange.getProperties().keySet().stream().forEach(myKey -> {
			logger.trace("Key: {} / value: {}", myKey, exchange.getIn().getHeaders().get(myKey));
		});
	}
}
