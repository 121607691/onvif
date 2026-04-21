import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * 简化的 ONVIF 设备发现程序
 * 基于 WS-Discovery 协议直接发送 UDP Probe 消息
 */
public class SimpleDiscovery {
    private static final Logger LOG = Logger.getLogger(SimpleDiscovery.class.getName());

    // WS-Discovery 配置
    private static final int DISCOVERY_TIMEOUT = 4000;  // 4秒超时
    private static final int DISCOVERY_PORT = 3702;    // UDP 端口
    private static final String DISCOVERY_ADDRESS = "239.255.255.250";  // IPv4 多播地址

    // Probe 消息模板
    private static final String PROBE_MESSAGE =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<Envelope xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\" xmlns=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
        "  <Header>\n" +
        "    <wsa:MessageID xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">\n" +
        "      uuid:" + UUID.randomUUID() + "\n" +
        "    </wsa:MessageID>\n" +
        "    <wsa:To xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">\n" +
        "      urn:schemas-xmlsoap-org:ws:2005:04:discovery\n" +
        "    </wsa:To>\n" +
        "    <wsa:Action xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">\n" +
        "      http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe\n" +
        "    </wsa:Action>\n" +
        "  </Header>\n" +
        "  <Body>\n" +
        "    <Probe xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
        "           xmlns=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\">\n" +
        "      <Types>tds:Device</Types>\n" +
        "      <Scopes/>\n" +
        "    </Probe>\n" +
        "  </Body>\n" +
        "</Envelope>";

    public static void main(String[] args) {
        LOG.info("开始扫描网络中的 ONVIF 设备...");

        try {
            // 获取本机所有网络接口
            List<InetAddress> localAddresses = getLocalAddresses();
            LOG.info("本机网络接口: " + localAddresses.size() + " 个");

            Set<String> deviceUrls = discoverDevices(localAddresses);

            if (deviceUrls.isEmpty()) {
                LOG.info("没有发现到 ONVIF 设备");
                LOG.info("请确保：");
                LOG.info("1. IPC 设备已连接到网络");
                LOG.info("2. IPC 设备已启用 ONVIF 服务");
                LOG.info("3. 设备和电脑在同一网络段");
                LOG.info("4. 防火墙没有阻止 3702 端口");
            } else {
                LOG.info("发现到 " + deviceUrls.size() + " 个 ONVIF 设备:");
                for (String url : deviceUrls) {
                    LOG.info("  - " + url);

                    // 尝试获取设备基本信息
                    try {
                        String deviceInfo = getDeviceInfo(url);
                        LOG.info("    设备信息: " + deviceInfo);
                    } catch (Exception e) {
                        LOG.info("    获取设备信息失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOG.severe("设备发现失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取本机所有网络接口地址
     */
    private static List<InetAddress> getLocalAddresses() throws SocketException {
        List<InetAddress> addresses = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isLoopback() || !ni.isUp()) {
                continue;
            }

            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress addr = inetAddresses.nextElement();
                if (addr instanceof Inet4Address) {
                    addresses.add(addr);
                    LOG.info("  发现网络接口: " + ni.getName() + " - " + addr.getHostAddress());
                }
            }
        }
        return addresses;
    }

    /**
     * 发现设备
     */
    private static Set<String> discoverDevices(List<InetAddress> localAddresses) throws Exception {
        Set<String> deviceUrls = new ConcurrentSkipListSet<>();
        ExecutorService executor = Executors.newCachedThreadPool();

        for (InetAddress localAddr : localAddresses) {
            executor.submit(() -> {
                try {
                    // 创建接收 socket
                    int receivePort = 40000 + (int)(Math.random() * 20000);
                    DatagramSocket receiveSocket = new DatagramSocket(receivePort, localAddr);

                    // 创建发送 socket
                    DatagramSocket sendSocket = new DatagramSocket();

                    // 发送 Probe 消息
                    byte[] probeData = PROBE_MESSAGE.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(
                        probeData, probeData.length,
                        InetAddress.getByName(DISCOVERY_ADDRESS),
                        DISCOVERY_PORT
                    );
                    sendSocket.send(packet);

                    LOG.info("已发送 Probe 消息，等待响应...");

                    // 接收响应
                    receiveSocket.setSoTimeout(DISCOVERY_TIMEOUT);
                    long startTime = System.currentTimeMillis();

                    while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT) {
                        byte[] buffer = new byte[4096];
                        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                        receiveSocket.receive(responsePacket);

                        // 解析响应
                        String response = new String(responsePacket.getData(), 0, responsePacket.getLength(), StandardCharsets.UTF_8);
                        if (response.contains("XAddrs")) {
                            // 提取设备 URL
                            int start = response.indexOf("<XAddrs>");
                            int end = response.indexOf("</XAddrs>");
                            if (start != -1 && end != -1) {
                                String xaddrs = response.substring(start + 8, end);
                                String[] urls = xaddrs.trim().split("\\s+");
                                for (String url : urls) {
                                    if (!url.isEmpty()) {
                                        deviceUrls.add(url);
                                    }
                                }
                            }
                        }
                    }

                    sendSocket.close();
                    receiveSocket.close();

                } catch (Exception e) {
                    // 忽略超时等异常
                }
            });
        }

        // 等待所有任务完成
        executor.shutdown();
        executor.awaitTermination(DISCOVERY_TIMEOUT + 1000, TimeUnit.MILLISECONDS);

        return deviceUrls;
    }

    /**
     * 获取设备基本信息（简化版）
     */
    private static String getDeviceInfo(String deviceUrl) {
        try {
            URL url = new URL(deviceUrl + "/device_service");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                return "设备服务响应正常";
            } else {
                return "响应代码: " + conn.getResponseCode();
            }
        } catch (Exception e) {
            return "连接失败: " + e.getMessage();
        }
    }
}