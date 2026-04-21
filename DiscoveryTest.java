import java.net.URL;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * 使用 onvif 库发现本地 IPC 设备
 */
public class DiscoveryTest {
    private static final Logger LOG = Logger.getLogger(DiscoveryTest.class.getName());

    public static void main(String[] args) {
        try {
            // 使用 OnvifDiscovery 发现设备
            Collection<URL> urls = de.onvif.discovery.OnvifDiscovery.discoverOnvifURLs();

            LOG.info("发现到 " + urls.size() + " 个 ONVIF 设备:");
            for (URL url : urls) {
                LOG.info("设备地址: " + url.toString());
            }

            if (urls.isEmpty()) {
                LOG.info("没有发现到 ONVIF 设备，请确保：");
                LOG.info("1. IPC 设备已连接到网络");
                LOG.info("2. IPC 设备已启用 ONVIF 服务");
                LOG.info("3. 设备和电脑在同一网络段");
                LOG.info("4. 防火墙没有阻止 3702 端口");
            }
        } catch (Exception e) {
            LOG.severe("发现设备时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}