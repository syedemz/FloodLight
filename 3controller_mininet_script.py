#!/usr/bin/python


from mininet.net import Mininet
from mininet.node import Controller, RemoteController, OVSKernelSwitch, IVSSwitch, UserSwitch
from mininet.link import Link, TCLink
from mininet.cli import CLI
from mininet.log import setLogLevel

def topology():

    "Create a network."
    net = Mininet( controller=RemoteController, link=TCLink, switch=OVSKernelSwitch )

    print "*** Creating nodes"
    h1 = net.addHost( 'h1', mac='00:00:00:00:00:01', ip='10.0.0.1/8' )
    h2 = net.addHost( 'h2', mac='00:00:00:00:00:02', ip='10.0.0.2/8' )
    h3 = net.addHost( 'h3', mac='00:00:00:00:00:03', ip='10.0.0.3/8' )
    h4 = net.addHost( 'h4', mac='00:00:00:00:00:04', ip='10.0.0.4/8' )
    h5 = net.addHost( 'h5', mac='00:00:00:00:00:05', ip='10.0.0.5/8' )
    h6 = net.addHost( 'h6', mac='00:00:00:00:00:06', ip='10.0.0.6/8' )
    h7 = net.addHost( 'h7', mac='00:00:00:00:00:07', ip='10.0.0.7/8' )
    h8 = net.addHost( 'h8', mac='00:00:00:00:00:08', ip='10.0.0.8/8' )
    h9 = net.addHost( 'h9', mac='00:00:00:00:00:09', ip='10.0.0.9/8' )
    h10 = net.addHost( 'h10', mac='00:00:00:00:00:10', ip='10.0.0.10/8' )
    s11 = net.addSwitch( 's11', protocols='OpenFlow13', listenPort=6673, mac='00:00:00:00:00:11' )
    s12 = net.addSwitch( 's12', protocols='OpenFlow13', listenPort=6674, mac='00:00:00:00:00:12' )
    s13 = net.addSwitch( 's13', protocols='OpenFlow13', listenPort=6675, mac='00:00:00:00:00:13' )
    s14 = net.addSwitch( 's14', protocols='OpenFlow13', listenPort=6676, mac='00:00:00:00:00:14' )
    s15 = net.addSwitch( 's15', protocols='OpenFlow13', listenPort=6677, mac='00:00:00:00:00:15' )
    s16 = net.addSwitch( 's16', protocols='OpenFlow13', listenPort=6678, mac='00:00:00:00:00:16' )
    s17 = net.addSwitch( 's17', protocols='OpenFlow13', listenPort=6679, mac='00:00:00:00:00:17' )
    c18 = net.addController( 'c18', ip='192.168.56.101', port=6653 )
    c23 = net.addController( 'c23', ip='192.168.56.102', port=6653 )
    c24 = net.addController( 'c24', ip='192.168.56.103', port=6653 )

    print "*** Creating links"
    net.addLink(s17, h10)
    net.addLink(s17, h9)
    net.addLink(s16, h8)
    net.addLink(s16, s17)
    net.addLink(s15, s16)
    net.addLink(s15, h7)
    net.addLink(h6, s14)
    net.addLink(h5, s14)
    net.addLink(s14, s15)
    net.addLink(s13, s14)
    net.addLink(s13, s15)
    net.addLink(h4, s13)
    net.addLink(s12, s13)
    net.addLink(s12, h3)
    net.addLink(s11, s12)
    net.addLink(h2, s11)
    net.addLink(h1, s11)

    print "*** Starting network"
    net.build()
    c18.start()
    c23.start()
    c24.start()
    s17.start( [c24] )
    s16.start( [c24] )
    s15.start( [c23] )
    s14.start( [c23] )
    s13.start( [c23] )
    s12.start( [c18] )
    s11.start( [c18] )

    print "*** Running CLI"
    CLI( net )

    print "*** Stopping network"
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    topology()



