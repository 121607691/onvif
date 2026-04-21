import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * 测试获取设备信息
 */
public class TestDeviceInfo {
    private static final Logger LOG = Logger.getLogger(TestDeviceInfo.class.getName());

    // 基本设备信息请求（简化版）
    private static final String GET_DEVICE_INFO_REQUEST =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
        "  <s:Body>\n" +
        "    <tds:GetDeviceInformation xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\"/>\n" +
        "  </s:Body>\n" +
        "</s:Envelope>";

    public static void main(String[] args) {
        String targetIp = "192.168.1.190";
        String[] paths = {
            "/onvif/device_service",
            "/onvif/device",
            "/onvif/",
            "/Device/",
            "/cgi-bin/ver40.cgi",
            "/axis-cgi/param.cgi?action=list&group=Brand"
        };

        for (String path : paths) {
            testDeviceService(targetIp, path);
        }
    }

    private static void testDeviceService(String ip, String path) {
        try {
            String url = "http://" + ip + path;
            LOG.info("测试: " + url);

            // 创建连接
            URL deviceUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) deviceUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/soap+xml");
            conn.setRequestProperty("SOAPAction", "\"http://www.onvif.org/ver10/device/wsdl/GetDeviceInformation\"");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = GET_DEVICE_INFO_REQUEST.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 获取响应
            int responseCode = conn.getResponseCode();
            LOG.info("  响应代码: " + responseCode);

            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    LOG.info("  响应长度: " + response.length() + " 字符");
                    if (response.length() > 0) {
                        LOG.info("  响应内容预览: " + response.substring(0, Math.min(200, response.length())));
                    }
                }
            } else if (responseCode == 401) {
                LOG.info("  需要认证");
            } else if (responseCode == 404) {
                LOG.info("  路径不存在");
            }

            conn.disconnect();

        } catch (Exception e) {
            LOG.info("  错误: " + e.getMessage());
        }
    }
}