import java.net.URL;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Reference example that uses the library discovery helper to find ONVIF
 * devices on the local network.
 */
public final class DiscoveryTest {
    private static final Logger LOG = Logger.getLogger(DiscoveryTest.class.getName());

    private DiscoveryTest() {
    }

    public static void main(String[] args) {
        try {
            Collection<URL> urls = de.onvif.discovery.OnvifDiscovery.discoverOnvifURLs();

            LOG.info("Discovered " + urls.size() + " ONVIF device endpoint(s).");
            for (URL url : urls) {
                LOG.info("Device URL: " + url);
            }

            if (urls.isEmpty()) {
                LOG.info("No ONVIF devices were discovered on the current network.");
                LOG.info("Check that devices are online, ONVIF is enabled, and UDP discovery is allowed.");
            }
        } catch (Exception e) {
            LOG.severe("Device discovery failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
