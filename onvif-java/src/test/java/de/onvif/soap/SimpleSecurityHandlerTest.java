package de.onvif.soap;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;
import javax.xml.namespace.QName;
import org.junit.Test;
import org.w3c.dom.Element;

/** Preserves the ONVIF UsernameToken wire contract without an embedded broker. */
public class SimpleSecurityHandlerTest {
  private static final String WSSE =
      "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
  private static final String WSU =
      "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

  @Test
  public void outboundSoap12CarriesDigestNonceAndTimestampWithoutPlaintextPassword() throws Exception {
    var context = new Context(true);
    var handler = new SimpleSecurityHandler("synthetic-user", "synthetic-password");
    assertThat(handler.handleMessage(context)).isTrue();
    var header = context.message.getSOAPHeader();
    assertThat(header.getElementsByTagNameNS(WSSE, "Security").getLength()).isEqualTo(1);
    assertThat(header.getElementsByTagNameNS(WSSE, "Username").item(0).getTextContent())
        .isEqualTo("synthetic-user");
    var nonce = (Element) header.getElementsByTagNameNS(WSSE, "Nonce").item(0);
    var password = (Element) header.getElementsByTagNameNS(WSSE, "Password").item(0);
    var created = header.getElementsByTagNameNS(WSU, "Created").item(0).getTextContent();
    byte[] nonceBytes = Base64.getDecoder().decode(nonce.getTextContent());
    assertThat(nonceBytes).hasSize(16);
    assertThat(nonce.getAttribute("EncodingType"))
        .isEqualTo("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
    assertThat(password.getAttribute("Type"))
        .isEqualTo("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest");
    var digest = MessageDigest.getInstance("SHA-1");
    digest.update(nonceBytes);
    digest.update(created.getBytes(StandardCharsets.UTF_8));
    digest.update("synthetic-password".getBytes(StandardCharsets.UTF_8));
    assertThat(password.getTextContent()).isEqualTo(Base64.getEncoder().encodeToString(digest.digest()));
    assertThat(created).isEqualTo(handler.getLastUTCTime());
    assertThat(header.getTextContent()).doesNotContain("synthetic-password");
  }

  @Test
  public void inboundMessageDoesNotReceiveAnOutboundSecurityHeader() throws Exception {
    var context = new Context(false);
    assertThat(new SimpleSecurityHandler("synthetic-user", "synthetic-password").handleMessage(context))
        .isTrue();
    assertThat(context.message.getSOAPHeader().getChildNodes().getLength()).isZero();
  }

  private static final class Context extends HashMap<String, Object> implements SOAPMessageContext {
    private SOAPMessage message;
    Context(boolean outbound) throws Exception {
      message = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
      put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, outbound);
    }
    @Override public SOAPMessage getMessage() { return message; }
    @Override public void setMessage(SOAPMessage value) { message = value; }
    @Override public Object[] getHeaders(QName name, JAXBContext context, boolean allRoles) { return new Object[0]; }
    @Override public Set<String> getRoles() { return Set.of(); }
    @Override public void setScope(String name, Scope scope) {}
    @Override public Scope getScope(String name) { return Scope.APPLICATION; }
  }
}
