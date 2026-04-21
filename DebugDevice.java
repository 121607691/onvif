import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * 调试 IPC 设备的详细信息
 */
public class DebugDevice {
    private static final Logger LOG = Logger.getLogger(DebugDevice.class.getName());

    public static void main(String[] args) {
        String targetIp = "192.168.1.190";

        LOG.info("=== 调试 IPC 设备 " + targetIp + " ===\n");

        // 1. 测试基本连通性
        testConnectivity(targetIp);

        // 2. 测试 HTTP 服务
        testHttpService(targetIp);

        // 3. 测试 ONVIF 端点
        testOnvifEndpoints(targetIp);

        // 4. 获取原始响应
        getRawResponse(targetIp);
    }

    private static void testConnectivity(String ip) {
        LOG.info("1. 连通性测试:");
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, 80), 2000);
            socket.close();
            LOG.info("   ✅ HTTP 端口 80 连接成功");
        } catch (Exception e) {
            LOG.info("   ❌ HTTP 端口连接失败: " + e.getMessage());
        }
    }

    private static void testHttpService(String ip) {
        LOG.info("\n2. HTTP 服务测试:");
        String[] paths = {
            "/onvif/device_service",
            "/onvif/device",
            "/onvif/",
            "/Device/",
            "/",
            "/index.shtml",
            "/status.htm"
        };

        for (String path : paths) {
            testPath(ip, path);
        }
    }

    private static void testPath(String ip, String path) {
        try {
            URL url = new URL("http://" + ip + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int responseCode = conn.getResponseCode();
            String contentType = conn.getContentType();

            LOG.info("   " + path + " → " + responseCode +
                    (contentType != null ? " (" + contentType + ")" : ""));

            conn.disconnect();
        } catch (Exception e) {
            LOG.info("   " + path + " → 连接失败");
        }
    }

    private static void testOnvifEndpoints(String ip) {
        LOG.info("\n3. ONVIF 端点测试:");

        // GetDeviceInformation
        testSoapAction(ip, "GetDeviceInformation",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\">" +
            "  <s:Body><tds:GetDeviceInformation xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\"/></s:Body>" +
            "</s:Envelope>");

        // GetServices
        testSoapAction(ip, "GetServices",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\">" +
            "  <s:Body><tds:GetServices xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\"/></s:Body>" +
            "</s:Envelope>");
    }

    private static void testSoapAction(String ip, String action, String soapBody) {
        try {
            URL url = new URL("http://" + ip + "/onvif/device_service");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/soap+xml");
            conn.setRequestProperty("SOAPAction", "\"http://www.onvif.org/ver10/device/wsdl/" + action + "\"");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = soapBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            LOG.info("   " + action + " → " + responseCode);

            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    int totalChars = 0;
                    while ((line = br.readLine()) != null && totalChars < 1000) {
                        response.append(line);
                        totalChars += line.length();
                    }
                    LOG.info("     响应长度: " + totalChars + " 字符");
                    if (response.length() > 0) {
                        LOG.info("     响应内容: " + response.substring(0, Math.min(200, response.length())));
                    }
                }
            }

            conn.disconnect();

        } catch (Exception e) {
            LOG.info("   " + action + " → 错误: " + e.getMessage());
        }
    }

    private static void getRawResponse(String ip) {
        LOG.info("\n4. 原始响应获取:");

        try {
            URL url = new URL("http://" + ip + "/onvif/device_service");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/soap+xml");
            conn.setRequestProperty("SOAPAction", "\"http://www.onvif.org/ver10/device/wsdl/GetDeviceInformation\"");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // 发送请求
            String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\">" +
                "  <s:Body><tds:GetDeviceInformation xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\"/></s:Body>" +
                "</s:Envelope>";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = request.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            LOG.info("   请求发送成功");

            // 读取响应
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                LOG.info("   响应内容:");
                while ((line = br.readLine()) != null && lineCount < 50) {
                    LOG.info("     " + line);
                    lineCount++;
                }
                LOG.info("   ... (共 " + lineCount + " 行)");
            }

            conn.disconnect();

        } catch (Exception e) {
            LOG.info("   获取响应失败: " + e.getMessage());
        }
    }
}