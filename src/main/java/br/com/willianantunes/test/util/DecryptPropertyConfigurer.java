package br.com.willianantunes.test.util;

import java.util.Properties;

import org.apache.camel.spring.spi.BridgePropertyPlaceholderConfigurer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.ObjectUtils;

/**
 * Custom property place holder whose purpose is to convert a encrypted property to its normal value.
 * @author Willian Antunes
 * @see <a href="http://romiawasthy.blogspot.com.br/2012/02/encryptdecrpt-properties-in-spring.html">Encrypt/Decrypt Properties using PropertyPlaceholderConfigurer</a>
 * @see <a href="http://camel.apache.org/using-propertyplaceholder.html">Bridging Spring and Camel property placeholders</a>
 */
public class DecryptPropertyConfigurer extends BridgePropertyPlaceholderConfigurer {
	private final Logger logger = LogManager.getLogger(DecryptPropertyConfigurer.class);

	@Override
	protected void convertProperties(Properties props) {
		props.keySet().stream().forEach(s -> {
			String myPropertValue = props.getProperty(s.toString());
			try {
				String myConvertedValue = ProtectedConfigFile.decrypt(myPropertValue);
				if (!myConvertedValue.isEmpty() && !ObjectUtils.nullSafeEquals(myPropertValue, myConvertedValue)) {
					logger.info("The following property was converted: {}", s);
					props.setProperty(s.toString(), myConvertedValue);
				}
			} catch (Exception e) {
				logger.warn("The following property could not be converted: {}. Using standard value: {}. Details about why not: {}", 
						s.toString(), myPropertValue, e.getMessage());
			}
		});
	}
}