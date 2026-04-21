import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.regex.*;

/**
 * 解析 IPC 设备的基本信息
 */
public class ParseDeviceInfo {
    private static final Logger LOG = Logger.getLogger(ParseDeviceInfo.class.getName());

    private static final String GET_DEVICE_INFO_REQUEST =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
        "  <s:Body>\n" +
        "    <tds:GetDeviceInformation xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\"/>\n" +
        "  </s:Body>\n" +
        "</s:Envelope>";

    public static void main(String[] args) {
        String targetIp = "192.168.1.190";

        try {
            // 获取设备信息 XML
            String deviceInfoXml = getDeviceInfo(targetIp);

            // 解析 XML
            parseDeviceInfo(deviceInfoXml);

            // 尝试获取媒体服务信息
            getMediaServices(targetIp);

        } catch (Exception e) {
            LOG.severe("获取设备信息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getDeviceInfo(String ip) throws Exception {
        String url = "http://" + ip + "/onvif/device_service";

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
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

        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        conn.disconnect();

        return response.toString();
    }

    private static void parseDeviceInfo(String xml) {
        LOG.info("=== 设备基本信息 ===");

        // 使用正则表达式提取信息
        String manufacturer = extractValue(xml, "<Manufacturer>(.*?)</Manufacturer>");
        String model = extractValue(xml, "<Model>(.*?)</Model>");
        String firmwareVersion = extractValue(xml, "<FirmwareVersion>(.*?)</FirmwareVersion>");
        String serialNumber = extractValue(xml, "<SerialNumber>(.*?)</SerialNumber>");
        String deviceId = extractValue(xml, "<DeviceUUID>(.*?)</DeviceUUID>");

        LOG.info("制造商: " + (manufacturer != null ? manufacturer : "未知"));
        LOG.info("型号: " + (model != null ? model : "未知"));
        LOG.info("固件版本: " + (firmwareVersion != null ? firmwareVersion : "未知"));
        LOG.info("序列号: " + (serialNumber != null ? serialNumber : "未知"));
        LOG.info("设备UUID: " + (deviceId != null ? deviceId : "未知"));
    }

    private static void getMediaServices(String ip) {
        try {
            // 获取媒体服务地址
            String url = "http://" + ip + "/onvif/device_service";
            String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "  <s:Body>\n" +
                "    <tds:GetMediaProfiles xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\"/>\n" +
                "  </s:Body>\n" +
                "</s:Envelope>";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/soap+xml");
            conn.setRequestProperty("SOAPAction", "\"http://www.onvif.org/ver10/device/wsdl/GetMediaProfiles\"");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = request.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            conn.disconnect();

            // 解析媒体配置
            parseMediaProfiles(response.toString());

        } catch (Exception e) {
            LOG.info("获取媒体服务失败: " + e.getMessage());
        }
    }

    private static void parseMediaProfiles(String xml) {
        LOG.info("\n=== 媒体配置信息 ===");

        // 提取所有 Profile
        Pattern profilePattern = Pattern.compile("<Profiles.*?</Profiles>", Pattern.DOTALL);
        Matcher matcher = profilePattern.matcher(xml);

        int profileCount = 0;
        while (matcher.find()) {
            profileCount++;
            String profileXml = matcher.group();

            String token = extractValue(profileXml, "<Token>(.*?)</Token>");
            String name = extractValue(profileXml, "<Name>(.*?)</Name>");

            LOG.info("Profile #" + profileCount + ":");
            LOG.info("  Token: " + (token != null ? token : "未知"));
            LOG.info("  名称: " + (name != null ? name : "未知"));
        }

        if (profileCount == 0) {
            LOG.info("未找到媒体配置");
        }
    }

    private static String extractValue(String xml, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(xml);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
}