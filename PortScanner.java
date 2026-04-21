import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * 端口扫描工具，用于查找 IPC 设备的 ONVIF 服务端口
 */
public class PortScanner {
    private static final Logger LOG = Logger.getLogger(PortScanner.class.getName());

    // 常见 ONVIF 相关端口
    private static final int[] COMMON_PORTS = {
        80,     // HTTP
        443,    // HTTPS
        8080,   // HTTP Alternate
        554,    // RTSP
        3702,   // WS-Discovery
        5000,   // Onvif
        8081,   // Onvif Alternate
        9000    // Onvif Alternate
    };

    public static void main(String[] args) {
        String targetIp = "192.168.1.190";
        int timeout = 2000; // 2秒超时

        LOG.info("开始扫描 " + targetIp + " 的端口...");

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<String>> futures = new ArrayList<>();

        for (int port : COMMON_PORTS) {
            futures.add(executor.submit(() -> scanPort(targetIp, port, timeout)));
        }

        // 等待所有扫描完成
        for (Future<String> future : futures) {
            try {
                String result = future.get(timeout * 2, TimeUnit.MILLISECONDS);
                if (result != null) {
                    LOG.info(result);
                }
            } catch (Exception e) {
                // 忽略超时
            }
        }

        executor.shutdown();

        LOG.info("端口扫描完成");
    }

    private static String scanPort(String ip, int port, int timeout) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return "端口 " + port + " 开放";
        } catch (IOException e) {
            return null;
        }
    }
}