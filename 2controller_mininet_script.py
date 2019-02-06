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
    h1 = net.addHost( 'h1', mac='00:00:00:00:00:01', ip='10.0.0.10/8' )
    h2 = net.addHost( 'h2', mac='00:00:00:00:00:02', ip='10.0.0.20/8' )
    h3 = net.addHost( 'h3', mac='00:00:00:00:00:03', ip='10.0.0.30/8' )
    h4 = net.addHost( 'h4', mac='00:00:00:00:00:04', ip='10.0.0.40/8' )
    s5 = net.addSwitch( 's5', protocols='OpenFlow13', listenPort=6673, mac='00:00:00:00:00:05' )
    s6 = net.addSwitch( 's6', protocols='OpenFlow13', listenPort=6674, mac='00:00:00:00:00:06' )
    s7 = net.addSwitch( 's7', protocols='OpenFlow13', listenPort=6675, mac='00:00:00:00:00:07' )
    s8 = net.addSwitch( 's8', protocols='OpenFlow13', listenPort=6676, mac='00:00:00:00:00:08' )
    c9 = net.addController( 'c9', ip='192.168.56.101', port=6653 )
    c10 = net.addController( 'c10', ip='192.168.56.103', port=6653 )

    print "*** Creating links"
    net.addLink(h4, s8)
    net.addLink(h3, s7)
    net.addLink(s7, s8)
    net.addLink(s6, s7)
    net.addLink(s5, s6)
    net.addLink(h2, s6)
    net.addLink(h1, s5)

    print "*** Starting network"
    net.build()
    c9.start()
    c10.start()
    s8.start( [c10] )
    s7.start( [c10] )
    s6.start( [c9] )
    s5.start( [c9] )

    print "*** Running CLI"
    CLI( net )

    print "*** Stopping network"
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    topology()



