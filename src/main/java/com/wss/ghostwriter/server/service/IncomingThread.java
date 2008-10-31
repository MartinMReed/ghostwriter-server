package com.wss.ghostwriter.server.service;

import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import com.wss.ghostwriter.core.model.Message;
import com.wss.ghostwriter.core.model.MouseMovement;
import com.wss.ghostwriter.core.model.ScreenCapture;
import com.wss.ghostwriter.core.service.CommunicationCode;

public class IncomingThread extends AbstractWorkerThread {

    private static final Logger log = Logger.getLogger( IncomingThread.class );

    private Robot robot;

    public IncomingThread(DatagramSocket datagramSocket) throws Exception {

        super( datagramSocket );

        setRobot( new Robot() );
    }

    private void send( DatagramSocket datagramSocket, DatagramPacket datagramPacket, Message message ) throws Exception {

        byte[] packet = message.toPacket();

        datagramPacket.setData( packet, 0, packet.length );
        datagramSocket.send( datagramPacket );
    }

    protected boolean execute( DatagramSocket datagramSocket, DatagramPacket datagramPacket ) throws Exception {

        // blocks until data is received
        datagramSocket.receive( datagramPacket );

        Message message = Message.fromPacket( datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength() );

        try {

            switch (message.getCode()) {

                case CommunicationCode.PING: {

                    ping( datagramSocket, datagramPacket, message );
                    break;
                }

                case CommunicationCode.MOUSE_MOVEMENT: {

                    mouseMovement( datagramSocket, datagramPacket, message );
                    break;
                }

                case CommunicationCode.MOUSE_PRESS: {

                    mousePress( datagramSocket, datagramPacket, message );
                    break;
                }

                case CommunicationCode.MOUSE_RELEASE: {

                    mouseRelease( datagramSocket, datagramPacket, message );
                    break;
                }

                case CommunicationCode.KEY_PRESS: {

                    keyPress( datagramSocket, datagramPacket, message );
                    break;
                }

                case CommunicationCode.SCREEN_CAPTURE_REQUEST: {

                    screenCaptureRequest( datagramSocket, datagramPacket, message );
                    break;
                }
            }
        }
        catch (Throwable t) {

            log.error( "Error processing message code[" + message.getCode() + "], value[" + message.getValue() + "]", t );
        }

        return true;
    }

    private void ping( DatagramSocket datagramSocket, DatagramPacket datagramPacket, Message message ) throws Exception {

        send( datagramSocket, datagramPacket, new Message( CommunicationCode.PONG, message.getValue() ) );
    }

    private void mouseMovement( DatagramSocket datagramSocket, DatagramPacket datagramPacket, Message message ) throws Exception {

        Robot robot = getRobot();

        MouseMovement mouseMovement = MouseMovement.fromString( message.getValue() );

        Point point = MouseInfo.getPointerInfo().getLocation();
        point.translate( mouseMovement.getX(), mouseMovement.getY() );
        robot.mouseMove( point.x, point.y );
    }

    private void mousePress( DatagramSocket datagramSocket, DatagramPacket datagramPacket, Message message ) throws Exception {

        Robot robot = getRobot();

        robot.mousePress( InputEvent.BUTTON1_MASK );
    }

    private void mouseRelease( DatagramSocket datagramSocket, DatagramPacket datagramPacket, Message message ) throws Exception {

        Robot robot = getRobot();

        robot.mouseRelease( InputEvent.BUTTON1_MASK );
    }

    private void keyPress( DatagramSocket datagramSocket, DatagramPacket datagramPacket, Message message ) throws Exception {

        Robot robot = getRobot();

        int keycode = Integer.parseInt( message.getValue() );

        robot.keyPress( keycode );
        robot.keyRelease( keycode );
    }

    private void screenCaptureRequest( DatagramSocket datagramSocket, DatagramPacket datagramPacket, Message message ) throws Exception {

        Robot robot = getRobot();

        Rectangle screenSize = new Rectangle( Toolkit.getDefaultToolkit().getScreenSize() );
        BufferedImage bufferedImage = robot.createScreenCapture( screenSize );

        ScreenCapture screenCapture = ScreenCapture.fromString( message.getValue() );
        Image scaledImage = bufferedImage.getScaledInstance( screenCapture.getWidth(), screenCapture.getHeight(), Image.SCALE_SMOOTH );

        ImageIO.write( bufferedImage, "png", new File( "lol.png" ) );

        int[] rgb = new int[screenCapture.getWidth() * screenCapture.getHeight()];
        PixelGrabber pixelGrabber = new PixelGrabber( scaledImage, 0, 0, screenCapture.getWidth(), screenCapture.getHeight(), rgb, 0, screenCapture.getWidth() );
        pixelGrabber.grabPixels();

        byte[] rgbBytes = new byte[rgb.length];
        for (int i = 0; i < rgb.length; i++) {

            rgbBytes[i] = (byte) rgb[i];
        }

        int responseLength = 100;
        int responseCount = (int) Math.ceil( (double) rgbBytes.length / (double) responseLength );
        send( datagramSocket, datagramPacket, new Message( CommunicationCode.SCREEN_CAPTURE_RESPONSE_A, String.valueOf( responseCount ) ) );

        for (int i = 0; i < rgbBytes.length; i += responseLength) {

            int availableLength = rgbBytes.length - i;
            int length = ( availableLength < responseLength ) ? availableLength : responseLength;
            send( datagramSocket, datagramPacket, new Message( CommunicationCode.SCREEN_CAPTURE_RESPONSE_B, new String( rgbBytes, i, length ) ) );
        }
    }

    public Robot getRobot() {

        return robot;
    }

    private void setRobot( Robot robot ) {

        this.robot = robot;
    }
}
