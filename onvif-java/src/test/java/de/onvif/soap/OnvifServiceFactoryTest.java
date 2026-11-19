package de.onvif.soap;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.xml.ws.BindingProvider;
import java.util.List;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.junit.After;
import org.junit.Test;
import org.onvif.ver10.device.wsdl.Device;
import org.onvif.ver10.device.wsdl.DeviceService;

public class OnvifServiceFactoryTest {

  @After
  public void clearFactoryCache() {
    OnvifServiceFactory.clearCache();
  }

  @Test
  public void shouldNotReuseFirstSecurityHandlerForSubsequentProxyInstancesOfSameServiceClass() {
    OnvifServiceFactory.clearCache();
    SimpleSecurityHandler firstHandler = new SimpleSecurityHandler("first", "first-pass");
    SimpleSecurityHandler secondHandler = new SimpleSecurityHandler("second", "second-pass");

    Device firstProxy = createDeviceProxy(firstHandler);
    Device secondProxy = createDeviceProxy(secondHandler);

    List<Object> firstHandlers = List.copyOf(((BindingProvider) firstProxy).getBinding().getHandlerChain());
    List<Object> secondHandlers = List.copyOf(((BindingProvider) secondProxy).getBinding().getHandlerChain());

    assertThat(firstHandlers).contains(firstHandler).doesNotContain(secondHandler);
    assertThat(secondHandlers).contains(secondHandler).doesNotContain(firstHandler);
  }

  @Test
  public void shouldResolveImportedSchemasFromThePackagedRuntimeCatalog() throws Exception {
    Bus bus = BusFactory.newInstance().createBus();
    try {
      String resolved = OASISCatalogManager.getCatalogManager(bus)
          .resolveURI("http://www.w3.org/2004/08/xop/include");
      assertThat(resolved).isNotNull().contains("/wsdl/local/www.w3.org/2004/08/xop/include");
      try (var schema = new java.net.URI(resolved).toURL().openStream()) {
        assertThat(new String(schema.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
            .contains("http://www.w3.org/2001/XMLSchema");
      }
    } finally {
      bus.shutdown(true);
    }
  }

  private static Device createDeviceProxy(SimpleSecurityHandler securityHandler) {
    DeviceService service = new DeviceService(null, DeviceService.SERVICE);
    BindingProvider port = (BindingProvider) service.getDevicePort();
    return OnvifServiceFactory.createServiceProxy(
        port,
        "http://127.0.0.1/onvif/device_service",
        Device.class,
        securityHandler,
        false);
  }
}
