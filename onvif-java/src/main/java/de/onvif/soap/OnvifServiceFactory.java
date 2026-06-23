package de.onvif.soap;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service proxy factory that caches and reuses WSDL Schema parsing results.
 * Significantly reduces memory usage and initialization time through caching mechanism.
 *
 * @author ONVIF Optimization Team
 */
public class OnvifServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(OnvifServiceFactory.class);

    // Core: Cache parsed proxy factory configurations using Map
    private static final Map<String, JaxWsProxyFactoryBean> proxyCache = new ConcurrentHashMap<>();

    /**
     * Creates service proxy using cached Schema parsing results
     *
     * @param servicePort Service port binding provider
     * @param serviceAddr Service address
     * @param serviceClass Service class type
     * @param securityHandler Security handler
     * @param verbose Whether to enable verbose logging
     * @return Service proxy instance
     */
    public static synchronized <T> T createServiceProxy(
            BindingProvider servicePort,
            String serviceAddr,
            Class<T> serviceClass,
            SimpleSecurityHandler securityHandler,
            boolean verbose) {

        // Use service class name as cache key
        String cacheKey = serviceClass.getName();

        JaxWsProxyFactoryBean proxyFactory = proxyCache.get(cacheKey);
        if (proxyFactory == null) {
            logger.debug("First-time creating service proxy {} - parsing and caching Schema", serviceClass.getSimpleName());
            // Parse Schema and cache on first creation
            proxyFactory = createProxyFactory(serviceClass, verbose);
            proxyCache.put(cacheKey, proxyFactory);
        } else {
            logger.debug("Reusing cached service proxy configuration {} - skipping Schema parsing", serviceClass.getSimpleName());
        }

        // Set specific address for each instance - create new factory instance to avoid concurrency issues
        JaxWsProxyFactoryBean instanceFactory = cloneProxyFactory(proxyFactory);
        if (serviceAddr != null) {
            instanceFactory.setAddress(serviceAddr);
        }
        // Security handler is device-specific; attach per instance instead of caching in the template.
        if (securityHandler != null) {
            instanceFactory.getHandlers().add(securityHandler);
        }

        T proxy = instanceFactory.create(serviceClass);
        configureHttpConduit(proxy);
        return proxy;
    }

    /**
     * Create basic configuration for proxy factory
     */
    private static JaxWsProxyFactoryBean createProxyFactory(Class<?> serviceClass, boolean verbose) {

        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setServiceClass(serviceClass);

        SoapBindingConfiguration config = new SoapBindingConfiguration();
        config.setVersion(Soap12.getInstance());
        proxyFactory.setBindingConfig(config);

        if (verbose) {
            // Enable SOAP message logging (for debugging/development only)
            proxyFactory.getOutInterceptors().add(new LoggingOutInterceptor());
            proxyFactory.getInInterceptors().add(new LoggingInInterceptor());
        }

        return proxyFactory;
    }

    private static void configureHttpConduit(Object proxy) {
        Client client = ClientProxy.getClient(proxy);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        HTTPClientPolicy policy = http.getClient();
        if (policy == null) {
            policy = new HTTPClientPolicy();
            http.setClient(policy);
        }
        policy.setConnectionTimeout(36000);
        policy.setReceiveTimeout(32000);
        policy.setAllowChunking(false);
    }

    /**
     * Clone proxy factory to create independent instances
     */
    private static JaxWsProxyFactoryBean cloneProxyFactory(JaxWsProxyFactoryBean original) {
        JaxWsProxyFactoryBean clone = new JaxWsProxyFactoryBean();

        // Copy basic configuration
        clone.setServiceClass(original.getServiceClass());
        clone.setBindingConfig(original.getBindingConfig());

        // Copy interceptors only; handlers are attached per proxy instance.
        clone.getInInterceptors().addAll(original.getInInterceptors());
        clone.getOutInterceptors().addAll(original.getOutInterceptors());

        return clone;
    }

    /**
     * Clear cache - recommended to call when application shuts down
     */
    public static void clearCache() {
        logger.info("Clearing OnvifServiceFactory cache, releasing {} cached entries", proxyCache.size());
        proxyCache.clear();
    }

    /**
     * Get current cache size
     */
    public static int getCacheSize() {
        return proxyCache.size();
    }

    /**
     * Check if specified service type is already cached
     */
    public static boolean isCached(Class<?> serviceClass) {
        return proxyCache.containsKey(serviceClass.getName());
    }
}
