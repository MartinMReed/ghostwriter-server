package com.wss.ghostwriter.server.service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.apache.log4j.Logger;

public abstract class AbstractWorkerThread extends Thread {

    private static final Logger log = Logger.getLogger( AbstractWorkerThread.class );

    private DatagramSocket datagramSocket;
    private boolean running;

    public AbstractWorkerThread(DatagramSocket datagramSocket) {

        setDatagramSocket( datagramSocket );
    }

    public void interrupt() {

        setRunning( false );

        super.interrupt();
    }

    protected abstract boolean execute( DatagramSocket datagramSocket, DatagramPacket datagramPacket ) throws Exception;

    public final void run() {

        setRunning( true );

        DatagramSocket datagramSocket = getDatagramSocket();

        try {

            byte[] buffer = new byte[datagramSocket.getReceiveBufferSize()];
            DatagramPacket datagramPacket = new DatagramPacket( buffer, buffer.length );

            while (isRunning()) {

                boolean yield = true;

                try {

                    yield = execute( datagramSocket, datagramPacket );
                }
                catch (Throwable t) {

                    log.error( "Lost packet", t );
                }

                if (yield) {

                    Thread.yield();
                }
            }
        }
        catch (Throwable t) {

            log.error( t );
        }
    }

    private DatagramSocket getDatagramSocket() {

        return datagramSocket;
    }

    private void setDatagramSocket( DatagramSocket datagramSocket ) {

        this.datagramSocket = datagramSocket;
    }

    private boolean isRunning() {

        return running;
    }

    private void setRunning( boolean running ) {

        this.running = running;
    }
}
