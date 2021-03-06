package snmp;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;
import snmp.exceptions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 *
 */
public class SnmpV3 implements SnmpManager {
    private Snmp snmp;
    private TransportMapping<? extends Address> transport;
    private Authentication authentication;
    private Mapping mapping;

    /**
     *
     * @param authentication
     * @param mapping
     * @throws WrongTransportProtocolException
     * @throws WrongAuthenticationException
     * @throws WrongSnmpVersionException
     */
    public SnmpV3(Authentication authentication, Mapping mapping) throws WrongTransportProtocolException, WrongAuthenticationException, WrongSnmpVersionException {
        if (authentication instanceof USMAuthentication) {
            this.authentication = authentication;
            this.mapping = mapping;
            try {
                if (authentication.getTransportProtocol().equalsIgnoreCase("UDP")) {
                    transport = new DefaultUdpTransportMapping();
                } else if (authentication.getTransportProtocol().equalsIgnoreCase("TCP")) {
                    transport = new DefaultTcpTransportMapping();
                } else {
                    throw new WrongTransportProtocolException();
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            snmp = new Snmp(transport);
            snmp.getUSM().addUser(((USMAuthentication) authentication).getUsmUser());
        } else
            throw new WrongAuthenticationException("USMAuthentication has to be used!");
    }

    /**
     * The method getAsString(Oid oid) is using the @see SnmpManager#get to get a String value of the specified OID.
     *
     * @param oid - the requested OID
     * @return - a String with the result from the specified OID
     * @throws SNMPTimeOutException - will be thrown if a timeout with request happens
     * @throws snmp.exceptions.OIDDoesNotExistsException - will be thrown if the specified OID does not exist
     * @throws PDURequestFailedException - will be thrown if an error occurs within the request
     */
    public String getAsString(OID oid) throws SNMPTimeOutException,
            PDURequestFailedException, OIDDoesNotExistsException {
        // extract the response PDU (could be null if timed out)
        VariableBinding ret = get(new OID[]{oid}).get(0);
        String response = ret.getVariable().toString();
        if (response.equals("noSuchObject"))
            throw new OIDDoesNotExistsException();
        return response;
    }

    /**
     * The method get can be specified with an Array of requested OIDs. A Vector with elements of the subclass VariableBinding will be returned.
     * OID requested from the method GET can only return a value. Therefore the OIDd must be a scalar and not a branch.
     *
     * @param oids - the requested OIDs
     * @return - a Vector with VariableBindings
     * @throws SNMPTimeOutException - will be thrown if a timeout with request happens
     * @throws PDURequestFailedException - will be thrown if an error occurs within the request
     * @see org.snmp4j.smi.VariableBinding
     */
    public Vector<? extends VariableBinding> get(OID[] oids)
            throws SNMPTimeOutException, PDURequestFailedException {
        ResponseEvent responseEvent = null;
        Vector<? extends VariableBinding> vbs = null;
        try {
            // send the PDU
            responseEvent = snmp.send(createPDU(PDU.GET, oids), authentication.getTarget());
            Logger.getLogger(SnmpManager.class.getName()).log(Level.INFO,
                    responseEvent.toString());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        // extract the response PDU (could be null if timed out)
        if (responseEvent != null) {
            PDU responsePDU = responseEvent.getResponse();
            if (checkResponsePDU(responsePDU))
                vbs = responsePDU.getVariableBindings();
        } else {
            throw new SNMPTimeOutException();
        }

        return vbs;
    }

    /**
     * This method creates the requst and puts it in an PDU object. This object will be returnd and used from Methods such as get,walk and getnext.
     *
     * @param type - the type of the response, possible are PDU.GET,PDU.GETNEXT, PDU.GETBULK
     * @param oids - the requested OIDs
     * @return - the request PDU
     * @see org.snmp4j.ScopedPDU
     */
    public PDU createPDU(int type, OID[] oids) {
        // create the PDU
        PDU requestPDU = new ScopedPDU();
        requestPDU.setType(type);
        // put the oid you want to get
        for (OID oid : oids) {
            requestPDU.add(new VariableBinding(oid));
        }
        return requestPDU;
    }

    /**
     *
     * @param oids
     * @return
     * @throws SNMPTimeOutException
     * @throws PDURequestFailedException
     */
    public Vector<? extends VariableBinding> getNext(OID[] oids)
            throws SNMPTimeOutException, PDURequestFailedException {
        ResponseEvent responseEvent = null;
        Vector<? extends VariableBinding> vbs = null;
        try {


            // send the PDU
            responseEvent = snmp.send(createPDU(PDU.GETNEXT, oids), authentication.getTarget());
            Logger.getLogger(SnmpManager.class.getName()).log(Level.INFO,
                    responseEvent.toString());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        // extract the response PDU (could be null if timed out)
        if (responseEvent != null) {
            PDU responsePDU = responseEvent.getResponse();
            if (checkResponsePDU(responsePDU))
                vbs = responsePDU.getVariableBindings();
        } else {
            throw new SNMPTimeOutException();
        }
        return vbs;
    }

    /**
     * This method returns true or false, which depend on the response PDU.
     * If the response PDU is not null and don't have an error, true will be returned.
     *
     * @param responsePDU the responsePDU, which will be checked for errors.
     * @return - true if no error was invoked else false.
     * @throws PDURequestFailedException - If an error occurred in the response PDU.
     * @throws SNMPTimeOutException      - If a timeout occured
     * @see org.snmp4j.PDU
     */
    public boolean checkResponsePDU(PDU responsePDU)
            throws PDURequestFailedException, SNMPTimeOutException {
        if (responsePDU != null)
            if (responsePDU.getErrorStatus() == PDU.noError)
                return true;
            else
                throw new PDURequestFailedException(responsePDU);
        else
            throw new SNMPTimeOutException("Timeout: No Response from "
                    + authentication.getAddress());
    }

    /**
     * This method needs a valid root OID to return a VariableBinding list with the sub entities.
     *
     * @param rootID - The root OID
     * @return - a list containing VariableBinding
     */
    public List<VariableBinding> getSubtree(OID rootID) throws TreeEventException {
        TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
        treeUtils.setMaxRepetitions(Integer.MAX_VALUE);
        List<TreeEvent> events = treeUtils.getSubtree(authentication.getTarget(), rootID);

        // Get snmpwalk result.
        List<VariableBinding> varBindings = new ArrayList<VariableBinding>();
        for (TreeEvent event : events) {
            if (event != null) {
                if (event.isError())
                    throw new TreeEventException("oid [" + rootID + "] " + event.getErrorMessage());
                Collections.addAll(varBindings, event.getVariableBindings());
            }
        }
        return varBindings;
    }

    /**
     * @return - the mapping Object
     */
    public Mapping getMapping() {
        return mapping;
    }

    /**
     * This method has to be invoked before sending the message.
     * It listens for incoming messages.
     *
     * @throws IOException - if an IO operation exception occurs while starting the listener.
     */
//    @Override
    public void start() throws IOException {
        transport.listen();
    }

    /**
     * Stops the thread, which is listening for the incoming messages and releases all bound resources synchronously.
     *
     * @throws IOException - if any IO operation for the close fails.
     */
//    @Override
    public void stop() throws IOException {
        transport.close();
    }


}