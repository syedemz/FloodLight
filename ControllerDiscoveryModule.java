package net.floodlightcontroller.controllerdiscovery;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.io.*;
import java.util.*;
import java.net.*;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMatchBmap;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFMatchV3.Builder;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanVid;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery; 
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService; 
//import net.floodlightcontroller.linkdiscovery.internal.LinkInfo;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.threadpool.IThreadPoolService; 
import net.floodlightcontroller.topology.ITopologyListener;
import net.floodlightcontroller.topology.NodePair; 
import net.floodlightcontroller.topology.TopologyInstance; 
import net.floodlightcontroller.topology.TopologyManager;
import net.floodlightcontroller.topology.ITopologyService;

import java.util.*;
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
//###########################

public class ControllerDiscoveryModule implements IOFMessageListener, IFloodlightModule, ILinkDiscoveryListener, ITopologyListener, IOFSwitchListener, ControllerDiscoveryService{
	
	//required services
	protected IFloodlightProviderService floodlightProvider;
	protected static Logger log = LoggerFactory.getLogger(ControllerDiscoveryModule.class); 
	protected ITopologyService topologyService;
	protected TopologyManager topologyManager;
	protected ILinkDiscoveryService linkDiscovery;
	protected IOFSwitchService switchService;
	//protected IRestApiService restApi;
	
	
	List <IOFSwitch> switchesList = new ArrayList (); 

	//output lists
	List <IPv4Address> controllerIPv4Addresses = new ArrayList();
	List <IOFSwitch> controllerGWSwitches = new ArrayList ();
	List <MacAddress> controllerGWMacAddresses = new ArrayList ();
	List <OFPort> controllerGWSwitchPorts = new ArrayList ();
	List <DatapathId> controllerGWSwitchIds = new ArrayList ();
	IPv4Address myIPaddress = null;
	
	
	 

	@Override
	public String getName() {
		
		return ControllerDiscoveryModule.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() throws IllegalArgumentException {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(ControllerDiscoveryService.class);
	    return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() throws IllegalArgumentException{
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	    m.put(ControllerDiscoveryService.class, this);
	    return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
		    l.add(IFloodlightProviderService.class);
		    l.add(ILinkDiscoveryService.class);
		    l.add(ITopologyService.class);
		    l.add(IOFSwitchService.class);
		    //l.add(IRestApiService.class);
		  return l;
		
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		
		this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		//this.floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		
		this.linkDiscovery = context.getServiceImpl(ILinkDiscoveryService.class);
		this.topologyService = context.getServiceImpl(ITopologyService.class);
		//this.linkDiscovery.addListener(this);
		//this.topologyService.addListener(this);
		
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
		//this.switchService.addOFSwitchListener(this);
		
		//this.restApi = context.getServiceImpl(IRestApiService.class);
		
	}

	@Override
	public void startUp(FloodlightModuleContext context)throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		this.linkDiscovery.addListener(this);
		this.topologyService.addListener(this);
		this.switchService.addOFSwitchListener(this);
		//restApi.addRestletRoutable(new MacWeb());

	}
	
	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		
		
		 if(!switchesList.contains(sw)){
		     switchesList.add(sw);
		 }
		 
		 
		 
		switch (msg.getType()) {
	    case PACKET_IN:
	        
			
	    	OFFactory myFactory = sw.getOFFactory();
	        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
	        MacAddress srcMac = eth.getSourceMACAddress();
			OFPacketIn myPacketIn = (OFPacketIn) msg;
            OFPort myInPort = 
                (myPacketIn.getVersion().compareTo(OFVersion.OF_12) < 0) 
                ? myPacketIn.getInPort() 
                : myPacketIn.getMatch().get(MatchField.IN_PORT);
	            //System.out.println("++++++++++++Switch ingress port new packet In++++++++++++" +myInPort.toString());
	        
			if (eth.getEtherType() == EthType.IPv4) {
	           
	            IPv4 ipv4 = (IPv4) eth.getPayload();
	            IPv4Address dstIp = ipv4.getDestinationAddress();
	            IPv4Address srcIp = ipv4.getSourceAddress();
	            //System.out.println("++++++++++++Source IpAddress new packet In++++++++++++" +ipv4.getSourceAddress().toString());
				
				if (ipv4.getProtocol().equals(IpProtocol.UDP)) {
	                /* We got a UDP packet; get the payload from IPv4 */
	                UDP udp = (UDP) ipv4.getPayload();
	  
	                /* Various getters and setters are exposed in UDP */
	                TransportPort srcPort = udp.getSourcePort();
	                TransportPort dstPort = udp.getDestinationPort();
	                
	                //System.out.println("++++++++++++++++++++++++srcTransportPort"+ srcPort.toString());
	                //System.out.println("++++++++++++++++++++++++dstPortTransportPort"+ dstPort.toString());
				     
					//Process packet_in controller discovery requests 
				    if (srcPort.equals(TransportPort.of(65003))){
					   
					   if(myIPaddress == null){
					     myIPaddress = srcIp;
					   }
					   
					   if(!controllerIPv4Addresses.contains(srcIp) && !srcIp.equals(myIPaddress) ){
					      controllerIPv4Addresses.add(srcIp);
		        		  controllerGWSwitches.add(sw);
		        		  controllerGWMacAddresses.add(srcMac);
						  controllerGWSwitchPorts.add(myInPort);
						  System.out.println("++++++++++++++++++++++++New Controller discovery request recived+++++++++++++++++++++++++++++++++++++++");
		        		  System.out.println("++++++++++++++++++++++++NewControllerIP:"+ srcIp.toString());
		        		  System.out.println("++++++++++++++++++++++++NewControllerMacAddress:"+ srcMac.toString());
						  System.out.println("++++++++++++++++++++++++NewController GWSwitchMacAddress:"+ sw.toString());
						  System.out.println("++++++++++++++++++++++++NewController GWSwitchOutputPort"+ myInPort.toString());
					   
					     
						 //sent controller discovery replay 
		                      Ethernet l2 = new Ethernet();
		                      l2.setSourceMACAddress(MacAddress.of("00:00:00:00:00:18"));
		                      l2.setDestinationMACAddress(srcMac);
		                      Integer obj = new Integer(7770);
		                      short s = obj.shortValue();
		                      l2.setVlanID(s);
		                      l2.setEtherType(EthType.IPv4);
		
		                      IPv4 l3 = new IPv4();
		                      l3.setSourceAddress(myIPaddress);
		                      l3.setDestinationAddress(srcIp);
		                      l3.setTtl((byte) 64);
		                      l3.setProtocol(IpProtocol.UDP);
		
		                      UDP l4 = new UDP();
		                      l4.setSourcePort(TransportPort.of(65004));
		                      l4.setDestinationPort(TransportPort.of(67));
		                      Data l7 = new Data();
		                      l7.setData("CONTROLLER_DISCOVERY_REPLAY".getBytes());
		                      l2.setPayload(l3);
		                      l3.setPayload(l4);
		                      l4.setPayload(l7);
		                      byte[] serializedData = l2.serialize();
		
		                      OFActionOutput po_output_actions = myFactory.actions().buildOutput()
                                 .setMaxLen(0xFFffFFff)
                                 .setPort(myInPort)
                               .build();
		
		
		
		                      OFPacketOut po = sw.getOFFactory().buildPacketOut() 
			                     .setData(serializedData)
				                 .setBufferId(OFBufferId.NO_BUFFER)
			                     .setActions(Collections.singletonList((OFAction) po_output_actions))
			                     .setInPort(OFPort.CONTROLLER)
			                  .build();
		
		                       System.out.println("+++++++Controller Discovery Replay packet+++++++:"+po.toString());	  
	                       sw.write(po);
			
	    
	    
	                      //add Flow rule for that controller
		                       Match match = myFactory.buildMatch()
		                          .setExact(MatchField.ETH_TYPE, EthType.IPv4)//match prerequest
				                  .setExact(MatchField.IP_PROTO, IpProtocol.UDP)//match prerequest
				                 .setExact(MatchField.IPV4_DST,srcIp)
                               .build();	
		
		                       //creat actions-list
		                       ArrayList<OFAction> actionList = new ArrayList<OFAction>();
                               OFActions actions = myFactory.actions();
                               OFActionOutput output = actions.buildOutput()
                                   .setMaxLen(0xFFffFFff)
                                   .setPort(myInPort)
                               .build();
                               actionList.add(output);
		  
		                       //create FlowMod
                               OFFlowAdd flowAdd = myFactory.buildFlowAdd()
                                 .setHardTimeout(0)
                                 .setIdleTimeout(0)
                                 .setPriority(1000)
                                 .setMatch(match)
                                 .setActions(actionList)
				               .build();
        
                           //send to Switch
                            sw.write(flowAdd);
					  }
					   
					   
					}
					
					//Process packet_in controller discovery replay
				    if (!controllerIPv4Addresses.contains(srcIp) && srcPort.equals(TransportPort.of(65004)) && dstIp.equals(myIPaddress)){
					
					      controllerIPv4Addresses.add(srcIp);
		        		  controllerGWSwitches.add(sw);
		        		  controllerGWMacAddresses.add(srcMac);
						  controllerGWSwitchPorts.add(myInPort);
						  System.out.println("++++++++++++++++++++++++New Controller discovery replay recived+++++++++++++++++++++++++++++++++++++++");
		        		  System.out.println("++++++++++++++++++++++++NewControllerIP:"+ srcIp.toString());
		        		  System.out.println("++++++++++++++++++++++++NewControllerMacAddress:"+ srcMac.toString());
						  System.out.println("++++++++++++++++++++++++NewController GWSwitchMacAddress:"+ sw.toString());
						  System.out.println("++++++++++++++++++++++++NewController GWSwitchOutputPort:"+ myInPort.toString());
						  
						  
						  //add Flow rule for that controller
		                       Match match = myFactory.buildMatch()
		                          .setExact(MatchField.ETH_TYPE, EthType.IPv4)//match prerequest
				                  .setExact(MatchField.IP_PROTO, IpProtocol.UDP)//match prerequest
				                 .setExact(MatchField.IPV4_DST,srcIp)
                               .build();	
		
		                       //creat actions-list
		                       ArrayList<OFAction> actionList = new ArrayList<OFAction>();
                               OFActions actions = myFactory.actions();
                               OFActionOutput output = actions.buildOutput()
                                   .setMaxLen(0xFFffFFff)
                                   .setPort(myInPort)
                               .build();
                               actionList.add(output);
		  
		                       //create FlowMod
                               OFFlowAdd flowAdd = myFactory.buildFlowAdd()
                                 .setHardTimeout(0)
                                 .setIdleTimeout(0)
                                 .setPriority(1000)
                                 .setMatch(match)
                                 .setActions(actionList)
				               .build();
        
                           //send to Switch
                            sw.write(flowAdd);
					}
					
					
					
					//Process packet_in controller discovery requests after switch removed
				    if (srcPort.equals(TransportPort.of(65005))){
					   
					   if(!srcIp.equals(myIPaddress)){
					     
						 //sent controller discovery replay 
		                      Ethernet l2 = new Ethernet();
		                      l2.setSourceMACAddress(MacAddress.of("00:00:00:00:00:18"));
		                      l2.setDestinationMACAddress(srcMac);
		                      Integer obj = new Integer(7770);
		                      short s = obj.shortValue();
		                      l2.setVlanID(s);
		                      l2.setEtherType(EthType.IPv4);
		
		                      IPv4 l3 = new IPv4();
		                      l3.setSourceAddress(myIPaddress);
		                      l3.setDestinationAddress(srcIp);
		                      l3.setTtl((byte) 64);
		                      l3.setProtocol(IpProtocol.UDP);
		
		                      UDP l4 = new UDP();
		                      l4.setSourcePort(TransportPort.of(65006));
		                      l4.setDestinationPort(TransportPort.of(67));
		                      Data l7 = new Data();
		                      l7.setData("CONTROLLER_DISCOVERY_REPLAY".getBytes());
		                      l2.setPayload(l3);
		                      l3.setPayload(l4);
		                      l4.setPayload(l7);
		                      byte[] serializedData = l2.serialize();
		
		                      OFActionOutput po_output_actions = myFactory.actions().buildOutput()
                                 .setMaxLen(0xFFffFFff)
                                 .setPort(myInPort)
                               .build();
		
		
		
		                      OFPacketOut po = sw.getOFFactory().buildPacketOut() 
			                     .setData(serializedData)
				                 .setBufferId(OFBufferId.NO_BUFFER)
			                     .setActions(Collections.singletonList((OFAction) po_output_actions))
			                     .setInPort(OFPort.CONTROLLER)
			                  .build();
		
		                   System.out.println("+++++++Controller Discovery Replay packet+++++++:"+po.toString());	  
	                       sw.write(po);
			
	    
	    
	                      //add Flow rule for that controller
		                       Match match = myFactory.buildMatch()
		                          .setExact(MatchField.ETH_TYPE, EthType.IPv4)//match prerequest
				                  .setExact(MatchField.IP_PROTO, IpProtocol.UDP)//match prerequest
				                 .setExact(MatchField.IPV4_DST,srcIp)
                               .build();	
		
		                       //creat actions-list
		                       ArrayList<OFAction> actionList = new ArrayList<OFAction>();
                               OFActions actions = myFactory.actions();
                               OFActionOutput output = actions.buildOutput()
                                   .setMaxLen(0xFFffFFff)
                                   .setPort(myInPort)
                               .build();
                               actionList.add(output);
		  
		                       //create FlowMod
                               OFFlowAdd flowAdd = myFactory.buildFlowAdd()
                                 .setHardTimeout(0)
                                 .setIdleTimeout(0)
                                 .setPriority(1000)
                                 .setMatch(match)
                                 .setActions(actionList)
				               .build();
        
                           //send to Switch
                           sw.write(flowAdd);
					   }
					   
					   
					}
					
					//Process packet_in controller discovery replay after switched removed
				    if (!controllerIPv4Addresses.contains(srcIp) && srcPort.equals(TransportPort.of(65006)) && dstIp.equals(myIPaddress) ){
					
					      controllerIPv4Addresses.add(srcIp);
		        		  controllerGWSwitches.add(sw);
		        		  controllerGWMacAddresses.add(srcMac);
						  controllerGWSwitchPorts.add(myInPort);
						  System.out.println("++++++++++++++++++++++++New Controller discovery request recived+++++++++++++++++++++++++++++++++++++++");
		        		  System.out.println("++++++++++++++++++++++++NewControllerIP:"+ srcIp.toString());
		        		  System.out.println("++++++++++++++++++++++++NewControllerMacAddress:"+ srcMac.toString());
						  System.out.println("++++++++++++++++++++++++NewController GWSwitchMacAddress:"+ sw.toString());
						  System.out.println("++++++++++++++++++++++++NewController GWSwitchOutputPort"+ myInPort.toString());
					}
				
				
				}
		        
	        }

	        
	        break;
	    default:
	        break;
	    }
	    return Command.CONTINUE;
	
	}

	@Override
	public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
		for (int i = 0; i<updateList.size();i++){
			//System.out.println("##################"+updateList.get(i).toString());
		}
	}

	@Override
	public void topologyChanged(List<LDUpdate> linkUpdates) {
		for (int i = 0; i<linkUpdates.size();i++){
			//System.out.println("##################"+linkUpdates.get(i).toString());
		}
		
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		
		//new added switch
		IOFSwitch newSwitch = this.switchService.getSwitch(switchId);
		String dataPathId = switchId.toString();
		String newSrcMacAddress = dataPathId.substring(6);
		OFFactory myFactory = newSwitch.getOFFactory();
		
		
		//broadcast controller discovery packet
		Ethernet l2 = new Ethernet();
		l2.setSourceMACAddress(MacAddress.of("00:00:00:00:00:18"));
		l2.setDestinationMACAddress(MacAddress.BROADCAST);
		Integer obj = new Integer(7770);
		short s = obj.shortValue();
		l2.setVlanID(s);
		l2.setEtherType(EthType.IPv4);
		
		IPv4 l3 = new IPv4();
		IPv4Address controllerIPv4 = IPv4Address.of("192.168.56.101");
		IPv4Address destIP = IPv4Address.of("192.168.56.255");
		l3.setSourceAddress(controllerIPv4);
		l3.setDestinationAddress(IPv4Address.of(0xffFFffFF));
		l3.setTtl((byte) 64);
		l3.setProtocol(IpProtocol.UDP);
		
		UDP l4 = new UDP();
		l4.setSourcePort(TransportPort.of(65003));
		l4.setDestinationPort(TransportPort.of(67));
		Data l7 = new Data();
		l7.setData("CONTROLLER_DISCOVERY_REQUEST".getBytes());
		l2.setPayload(l3);
		l3.setPayload(l4);
		l4.setPayload(l7);
		byte[] serializedData = l2.serialize();
		
		OFActionOutput po_output_actions = myFactory.actions().buildOutput()
            .setMaxLen(0xFFffFFff)
            .setPort(OFPort.ALL)
            .build();
		
		
		
		OFPacketOut po = newSwitch.getOFFactory().buildPacketOut() 
			    .setData(serializedData)
				.setBufferId(OFBufferId.NO_BUFFER)
			    .setActions(Collections.singletonList((OFAction) po_output_actions))
			    .setInPort(OFPort.CONTROLLER)
			    .build();
		
		System.out.println("+++++++Controller Discovery packet+++++++:"+po.toString());	  
	    newSwitch.write(po);
			
	    
	    
	    //add broadcast Flow-rule for controller discovery packets
		  //creat Macth
		  Match match = myFactory.buildMatch()
		        .setExact(MatchField.ETH_TYPE, EthType.IPv4)//match prerequest
				.setExact(MatchField.IP_PROTO, IpProtocol.UDP)//match prerequest
				.setExact(MatchField.UDP_SRC,TransportPort.of(65003))
                .build();	
		
		  //creat actions-list
		     ArrayList<OFAction> actionList = new ArrayList<OFAction>();
             OFActions actions = myFactory.actions();
			 OFOxms oxms = myFactory.oxms();
 
			 /* Use OXM to modify data layer dest field. */
             OFActionSetField setDlSrc = actions.buildSetField()
                .setField(
                    oxms.buildEthSrc()
                    .setValue(MacAddress.of(newSrcMacAddress))
                    .build()
                )
                .build();
             //actionList.add(setDlSrc);
             /* Use builder again. */
             OFActionOutput output = actions.buildOutput()
                   .setMaxLen(0xFFffFFff)
                   .setPort(OFPort.ALL)
                   .build();
             actionList.add(output);
		  
		  //create FlowMod
          OFFlowAdd flowAdd = myFactory.buildFlowAdd()
                .setHardTimeout(0)
                .setIdleTimeout(0)
                .setPriority(1000)
                .setMatch(match)
                //.setActions(Collections.singletonList((OFAction) newSwitch.getOFFactory().actions().output(OFPort.ALL, 0xffFFffFF)))
                .setActions(actionList)
				.build();
        
          //send to Switch
          //newSwitch.write(flowAdd);
	
		
	}

	@Override
	public void switchRemoved(DatapathId switchId) {
		//removed switch
		IOFSwitch removedSwitch = this.switchService.getSwitch(switchId);
		String dataPathId = switchId.toString();
		String removedMacAddress = dataPathId.substring(6);
		
		while (controllerGWSwitches.contains(removedSwitch)){
		
		  int indexRemovedSwitch = controllerGWSwitches.indexOf(removedSwitch);
		  switchesList.remove(removedSwitch);
		  controllerGWSwitches.remove(removedSwitch);
		  IPv4Address controllerdstIp = controllerIPv4Addresses.get(indexRemovedSwitch);
		  controllerIPv4Addresses.remove(indexRemovedSwitch);
		  controllerGWMacAddresses.remove(indexRemovedSwitch);
		  controllerGWSwitchPorts.remove(indexRemovedSwitch);
		  
		  for (int i = 0; i < switchesList.size(); i++){
		     IOFSwitch sw = switchesList.get(i);
			 OFFactory myFactory = sw.getOFFactory();
			 
			 //broadcast controller discovery packet
		     Ethernet l2 = new Ethernet();
		     l2.setSourceMACAddress(MacAddress.of("00:00:00:00:00:18"));
		     l2.setDestinationMACAddress(MacAddress.BROADCAST);
		     Integer obj = new Integer(7770);
		     short s = obj.shortValue();
		     l2.setVlanID(s);
		     l2.setEtherType(EthType.IPv4);
		
		     IPv4 l3 = new IPv4();
		     IPv4Address controllerIPv4 = IPv4Address.of("192.168.56.101");
		     IPv4Address destIP = IPv4Address.of("192.168.56.255");
		     l3.setSourceAddress(controllerIPv4);
		     l3.setDestinationAddress(controllerdstIp);
		     l3.setTtl((byte) 64);
		     l3.setProtocol(IpProtocol.UDP);
		
		     UDP l4 = new UDP();
		     l4.setSourcePort(TransportPort.of(65005));
		     l4.setDestinationPort(TransportPort.of(67));
		     Data l7 = new Data();
		     l7.setData("CONTROLLER_DISCOVERY_REQUEST".getBytes());
		     l2.setPayload(l3);
		     l3.setPayload(l4);
		     l4.setPayload(l7);
		     byte[] serializedData = l2.serialize();
		
		     OFActionOutput po_output_actions = myFactory.actions().buildOutput()
                  .setMaxLen(0xFFffFFff)
                  .setPort(OFPort.ALL)
              .build();

		      OFPacketOut po = myFactory.buildPacketOut() 
			      .setData(serializedData)
				  .setBufferId(OFBufferId.NO_BUFFER)
			      .setActions(Collections.singletonList((OFAction) po_output_actions))
			      .setInPort(OFPort.CONTROLLER)
			   .build();
		
		      System.out.println("+++++++Controller Discovery packet+++++++:"+po.toString());	  
	          sw.write(po);

		  }
		 
	
		
		}
		
		
		
		
		
	}

	@Override
	public void switchActivated(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port,
			PortChangeType type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchChanged(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void switchDeactivated(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void linkDiscoveryUpdate(LDUpdate update) {
		// TODO Auto-generated method stub
		
	}

	public List<IPv4Address> getControllerAddressList() {
		// TODO Auto-generated method stub
		return controllerIPv4Addresses;
	}

	
	public List<IOFSwitch> getControllerGWSwtichList() {
		// TODO Auto-generated method stub
		return controllerGWSwitches;
	}

	
	public List<OFPort> getControllerGWSwitchport() {
		// TODO Auto-generated method stub
		return controllerGWSwitchPorts;
	}

	
	public List<DatapathId> getControllerGWSwitchID() {
		// TODO Auto-generated method stub
		return controllerGWSwitchIds;
	}

}
