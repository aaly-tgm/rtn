package snmp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import snmp.exceptions.*;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class SnmpV2cTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private SnmpManager snmp;

    private Authentication authentication;
    private Mapping mapping;

    @Before
    public void setUp()
            throws WrongAuthenticationException, WrongSnmpVersionException, WrongTransportProtocolException {
        mapping = new Mapping();
        //mapping.load("NETSCREEN-SMI.mib");
        mapping.load("/res/NS-POLICY.mib");
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c);
        snmp = new SnmpV2c(authentication, mapping);

    }

    @Test
    public void testSnmpV2cConstructorWrongSnmpVersionException()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException {
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version1);

        try {
            snmp = new SnmpV2c(authentication, mapping);
            fail("Should have thrown a WrongSnmpVersionException because SNMP version is wrong!");
        } catch (WrongSnmpVersionException e) {
            assertThat(e.getMessage(), containsString("Should be version 2c"));
        }
    }

    @Test
    public void testSnmpV2cConstructor()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException {
        authentication = new CommunityAuthentication("tcp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c);
        snmp = new SnmpV2c(authentication, mapping);
    }

    @Test
    public void testSnmpV2cConstructorWrongTransportProtocolException()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException {
        exception.expect(WrongTransportProtocolException.class);
        authentication = new CommunityAuthentication("tls", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c);
        snmp = new SnmpV2c(authentication, mapping);
    }

    @Test
    public void testSnmpV2cConstructorWrongAuthenticationException()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException {
        exception.expect(WrongAuthenticationException.class);
        authentication = new USMAuthentication("udp", "10.0.100.10", 161, "qazxswed", "qazxswed", SecurityLevel.AUTH_PRIV, "authPrivMd5Aes", SnmpConstants.version3);
        snmp = new SnmpV2c(authentication, mapping);
    }

    @Test
    public void testGetAsString()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException, SNMPTimeOutException,
            OIDDoesNotExistsException, PDURequestFailedException, IOException {
        String expected = "NetScreen-5GT version 5.0.0r8.1 (SN: 0064122003000765, Firewall+VPN)";
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c, 2000);
        snmp = new SnmpV2c(authentication, mapping);
        snmp.start();
        String result = snmp.getAsString(new OID(".1.3.6.1.2.1.1.1.0"));
        snmp.stop();
        assertEquals(expected, result);
    }

    @Test
    public void testGetAsStringOIDDoesNotExistsException()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException, SNMPTimeOutException,
            PDURequestFailedException, IOException, OIDDoesNotExistsException {
        exception.expect(OIDDoesNotExistsException.class);
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c);
        snmp = new SnmpV2c(authentication, mapping);
        snmp.start();
        String result = snmp.getAsString(new OID(".1.3.6.1.4.1.9.9.661.1.1.1.13"));
        snmp.stop();
    }

    @Test
    public void testGetAsStringSNMPTimeOutException()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException, SNMPTimeOutException,
            PDURequestFailedException, IOException, OIDDoesNotExistsException {
        exception.expect(SNMPTimeOutException.class);
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c, 1, 1);
        snmp = new SnmpV2c(authentication, mapping);
        snmp.start();
        String result = snmp.getAsString(new OID(".1.3.6.1.2.1.1.1.0"));
        snmp.stop();
    }

    @Test
    public void testGet()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException, SNMPTimeOutException,
            OIDDoesNotExistsException, PDURequestFailedException, IOException {
        String expected = "NetScreen-5GT version 5.0.0r8.1 (SN: 0064122003000765, Firewall+VPN)";
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c);
        snmp = new SnmpV2c(authentication, mapping);
        snmp.start();
        Vector<? extends VariableBinding> result = snmp.get(new OID[]{new OID(".1.3.6.1.2.1.1.1.0")});
        snmp.stop();
        assertEquals(expected, result.get(0).getVariable().toString());
    }

    @Test
    public void testGetSNMPTimeOutException()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException,
            SNMPTimeOutException, PDURequestFailedException, IOException {
        exception.expect(SNMPTimeOutException.class);
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c, 0, 1);
        snmp = new SnmpV2c(authentication, mapping);
        snmp.start();
        Vector<? extends VariableBinding> result = snmp.get(new OID[]{new OID(".1.3.6.1.2.1.1.1.0")});
        snmp.stop();
    }

    @Test
    public void testGetNext()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException, SNMPTimeOutException,
            OIDDoesNotExistsException, PDURequestFailedException, IOException {
        String expected = "1.3.6.1.4.1.3224.1.14";
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c);
        snmp = new SnmpV2c(authentication, mapping);
        snmp.start();
        Vector<? extends VariableBinding> result = snmp.getNext(new OID[]{new OID(".1.3.6.1.2.1.1.1.0")});
        snmp.stop();
        assertEquals(expected, result.get(0).getVariable().toString());
    }

    @Test
    public void testGetNextSNMPTimeOutException()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException,
            SNMPTimeOutException, PDURequestFailedException, IOException {
        exception.expect(SNMPTimeOutException.class);
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c, 1, 3);
        snmp = new SnmpV2c(authentication, mapping);
        Exception e = null;

        snmp.start();
        Vector<? extends VariableBinding> result = snmp.getNext(new OID[]{new OID(".1.3.6.1.2.1.1.1.0")});
        snmp.stop();
    }

    @Test
    public void testGetMapping() {
        Mapping expected = mapping;
        Mapping result = snmp.getMapping();

        assertEquals(expected, result);
    }

    @Test
    public void testGetSubtreeIndexOutOfBoundsException() throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException, IOException, TreeEventException {
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c);
        snmp = new SnmpV2c(authentication, mapping);
        snmp.start();
        List<? extends VariableBinding> result = snmp.getSubtree(new OID(".1.3.6.1.2.1.1.1.0"));
        snmp.stop();
    }

    @Test
    public void testGetSubtree()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException, IOException, TreeEventException {
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c);
        snmp = new SnmpV2c(authentication, mapping);
        snmp.start();
        List<? extends VariableBinding> result = snmp.getSubtree(new OID(mapping.readOID("nsPlyId")));
        snmp.stop();
        List<Variable> varialbe = new Vector<Variable>();
        for (VariableBinding vb : result)
            varialbe.add(vb.getVariable());
        List mockedList = mock(List.class);
        if (varialbe.size() != 0)
            when(mockedList.get(0)).thenReturn(varialbe.get(0).toInt());
        assertEquals(mockedList.get(0), varialbe.get(0).toInt());
    }

    @Test
    public void testGetSubtreeTreeEventException()
            throws WrongSnmpVersionException, WrongTransportProtocolException, WrongAuthenticationException, IOException, TreeEventException {
        exception.expect(TreeEventException.class);
        authentication = new CommunityAuthentication("udp", "10.0.100.10", 161, "5xHIT", SnmpConstants.version2c,1,1);
        snmp = new SnmpV2c(authentication, mapping);
        snmp.start();
        List<? extends VariableBinding> result = snmp.getSubtree(new OID(mapping.readOID("nsPlyId")));
        snmp.stop();
    }
}