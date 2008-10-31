package com.wss.ghostwriter.server;

import java.net.DatagramSocket;

import org.apache.log4j.Logger;

import com.wss.ghostwriter.core.service.Ports;
import com.wss.ghostwriter.server.service.IncomingThread;

public class Application {

    private static final Logger log = Logger.getLogger( Application.class );

    public static void main( String[] args ) throws Exception {

        log.info( "Welcome to the Ghost Network" );

        ( new IncomingThread( new DatagramSocket( Ports.SERVER ) ) ).start();
    }
}
