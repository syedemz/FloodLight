package net.floodlightcontroller.controllerdiscovery;

import java.util.List;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.IFloodlightService;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;


public interface ControllerDiscoveryService extends IFloodlightService {

	public List<IPv4Address> getControllerAddressList();
	
	public List<IOFSwitch> getControllerGWSwtichList();
	
	public List<OFPort> getControllerGWSwitchport();
	
	public List<DatapathId> getControllerGWSwitchID();
}
