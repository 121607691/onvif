import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Lightweight WS-Discovery example that sends UDP probe messages directly.
 */
public final class SimpleDiscovery {
    private static final Logger LOG = Logger.getLogger(SimpleDiscovery.class.getName());
    private static final int DISCOVERY_TIMEOUT_MS = 4000;
    private static final int DISCOVERY_PORT = 3702;
    private static final String DISCOVERY_ADDRESS = "239.255.255.250";

    private SimpleDiscovery() {
    }

    public static void main(String[] args) {
        LOG.info("Starting ONVIF WS-Discovery probe.");

        try {
            List<InetAddress> localAddresses = getLocalAddresses();
            LOG.info("Detected " + localAddresses.size() + " local IPv4 interface(s).");

            Set<String> deviceUrls = discoverDevices(localAddresses);
            if (deviceUrls.isEmpty()) {
                LOG.info("No ONVIF devices discovered.");
                LOG.info("Verify that devices are reachable, ONVIF is enabled, and multicast UDP is allowed.");
                return;
            }

            LOG.info("Discovered " + deviceUrls.size() + " ONVIF device endpoint(s):");
            for (String url : deviceUrls) {
                LOG.info("  - " + url);
            }
        } catch (Exception e) {
            LOG.severe("Discovery failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String buildProbeMessage() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<Envelope xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\" "
            + "xmlns=\"http://www.w3.org/2003/05/soap-envelope\">\n"
            + "  <Header>\n"
            + "    <wsa:MessageID xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">\n"
            + "      urn:uuid:" + UUID.randomUUID() + "\n"
            + "    </wsa:MessageID>\n"
            + "    <wsa:To xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">\n"
            + "      urn:schemas-xmlsoap-org:ws:2005:04:discovery\n"
            + "    </wsa:To>\n"
            + "    <wsa:Action xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">\n"
            + "      http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe\n"
            + "    </wsa:Action>\n"
            + "  </Header>\n"
            + "  <Body>\n"
            + "    <Probe xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
            + "xmlns=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\">\n"
            + "      <Types>tds:Device</Types>\n"
            + "      <Scopes/>\n"
            + "    </Probe>\n"
            + "  </Body>\n"
            + "</Envelope>";
    }

    private static List<InetAddress> getLocalAddresses() throws SocketException {
        List<InetAddress> addresses = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress address = inetAddresses.nextElement();
                if (address.getAddress().length == 4) {
                    addresses.add(address);
                    LOG.info("Using interface " + networkInterface.getName() + " - " + address.getHostAddress());
                }
            }
        }

        return addresses;
    }

    private static Set<String> discoverDevices(List<InetAddress> localAddresses) throws InterruptedException {
        Set<String> deviceUrls = new ConcurrentSkipListSet<>();
        ExecutorService executor = Executors.newCachedThreadPool();

        for (InetAddress localAddress : localAddresses) {
            executor.submit(() -> probeFromAddress(localAddress, deviceUrls));
        }

        executor.shutdown();
        executor.awaitTermination(DISCOVERY_TIMEOUT_MS + 1000L, TimeUnit.MILLISECONDS);
        return deviceUrls;
    }

    private static void probeFromAddress(InetAddress localAddress, Set<String> deviceUrls) {
        try (DatagramSocket socket = new DatagramSocket(0, localAddress)) {
            socket.setSoTimeout(DISCOVERY_TIMEOUT_MS);

            byte[] probeData = buildProbeMessage().getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(
                probeData,
                probeData.length,
                InetAddress.getByName(DISCOVERY_ADDRESS),
                DISCOVERY_PORT
            );
            socket.send(packet);

            long deadline = System.currentTimeMillis() + DISCOVERY_TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                byte[] buffer = new byte[4096];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(responsePacket);
                    String response = new String(
                        responsePacket.getData(),
                        0,
                        responsePacket.getLength(),
                        StandardCharsets.UTF_8
                    );
                    extractXAddrs(response, deviceUrls);
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
        } catch (IOException e) {
            LOG.fine("Skipping interface " + localAddress.getHostAddress() + ": " + e.getMessage());
        }
    }

    private static void extractXAddrs(String response, Set<String> deviceUrls) {
        int start = response.indexOf("<XAddrs>");
        int startTagLength = "<XAddrs>".length();
        if (start == -1) {
            start = response.indexOf("<wsdd:XAddrs>");
            startTagLength = "<wsdd:XAddrs>".length();
        }

        int end = response.indexOf("</XAddrs>");
        if (end == -1) {
            end = response.indexOf("</wsdd:XAddrs>");
        }

        if (start == -1 || end == -1 || end <= start) {
            return;
        }

        String xaddrs = response.substring(start + startTagLength, end);
        String[] urls = xaddrs.trim().split("\\s+");
        for (String url : urls) {
            if (!url.isEmpty()) {
                deviceUrls.add(url);
            }
        }
    }
}
