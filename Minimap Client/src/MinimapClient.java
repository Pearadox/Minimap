import javax.swing.*;

import java.awt.Dimension;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Arrays;


public class MinimapClient
{

    static JFrame f;
    static JLabel lastCommand = new JLabel("Nothing");
    static byte[] sendData = new byte[0];
    static double currentAngle = 0;
    static byte[] receiveData = new byte[0];
    static InetAddress IPAddress;
    static String ipText = "";
    static boolean canStart = false;

    public static void main(String[] args) throws IOException, InterruptedException
    {
    	
    	JTextField ip = new JTextField();
    	ip.setPreferredSize(new Dimension(150, 20));
    	JButton start = new JButton("Start");
    	Panel p = new Panel();
        f = new JFrame("Minimap Remote");
        f.setDefaultCloseOperation(3);
        f.setSize(270,69);
        f.setLocation(1620, 0);
        f.setResizable(false);
        f.setVisible(true);
        p.setSize(300, 200);
        ip.setEditable(true);
        ip.setMinimumSize(new Dimension(150, 100));
        p.add(ip);
        p.add(start);
        start.addActionListener(new ActionListener()
        {
			@Override
			public void actionPerformed(ActionEvent e) {
				setCanStart(true);				
			}  	
        });
        ip.addKeyListener(new KeyListener()
        		{

					@Override
					public void keyPressed(KeyEvent e) {
						if(e.getKeyCode() == 10) setCanStart(true);
					}

					@Override
					public void keyReleased(KeyEvent e) {
						// TODO Auto-generated method stub
						
					}
					public void keyTyped(KeyEvent e) {
							
					}
        		});
        f.add(p);
        f.setSize(270, 70);
        f.add(lastCommand);
        ip.requestFocus();
        while(!canStart) Thread.sleep(1000);
        ipText = ip.getText();
		ip.setEditable(false);
		if(ipText.length() == 0) ipText = "localhost";
//		ipText = "192.168.1.69";
        IPAddress = InetAddress.getByName(ipText);
        System.out.println(IPAddress);
        f.remove(p);
        f.add(new JLabel("    IP: " + ipText));
        f.setSize(300,100);
        f.addKeyListener(new KeyListener()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {

            }

            @Override
            public void keyPressed(KeyEvent e)
            {
            	if(canStart){
	                if(e.getKeyCode() == 37) try
	                {
	                    rotateLeft(10);
	                } catch (IOException e1)
	                {
	                    e1.printStackTrace();
	                }
	                if(e.getKeyCode() == 39) try
	                {
	                    rotateRight(10);
	                } catch (IOException e1)
	                {
	                    e1.printStackTrace();
	                }
	                if(e.getKeyCode() == 87) try
	                {
	                    moveY(10);
	                } catch (IOException e1)
	                {
	                    e1.printStackTrace();
	                }
	                if(e.getKeyCode() == 83) try
	                {
	                    moveY(-10);
	                } catch (IOException e1)
	                {
	                    e1.printStackTrace();
	                }
	                if(e.getKeyCode() == 65) try
	                {
	                    moveX(-10);
	                } catch (IOException e1)
	                {
	                    e1.printStackTrace();
	                }
	                if(e.getKeyCode() == 68) try
	                {
	                    moveX(10);
	                } catch (IOException e1)
	                {
	                    e1.printStackTrace();
	                }
            	}
            }

            @Override
            public void keyReleased(KeyEvent e)
            {

            }
        });
        f.setVisible(false);
        f.setVisible(true);
//        sendData = "0 20 0 b".getBytes();
//        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 5800);
//        clientSocket.send(sendPacket);
//        String modifiedSentence = new String(receivePacket.getData());
//        System.out.println("FROM SERVER:" + modifiedSentence);
//        Thread t = new Thread(() ->
//        {
//        	while(true)
//        	{
//	        	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//	            try {
//	            	Thread.sleep(2000);
//					clientSocket.receive(receivePacket);
//					System.out.println(new String(receivePacket.getData()));
//				} catch (IOException e1) {
//					System.out.println("Nothing Received");
//				} catch (InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//        	}
//        });
//        t.start();
        }
    
    	public static void setCanStart(boolean b){canStart = b;}

        public static void moveY(int d) throws IOException
        {
            DatagramSocket clientSocket = new DatagramSocket();
            String command = "0 "+d+" "+currentAngle+" b";
            sendData = command.getBytes();
            lastCommand.setText(command);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 5800);
            try
            {
            	System.out.println("sending");
            	clientSocket.send(sendPacket);
            } catch(Exception e) {e.printStackTrace();}
        }

        public static void moveX(int d) throws IOException
        {
        	DatagramSocket clientSocket = new DatagramSocket();
        	String command =(d+" 0 "+currentAngle+" b"); 
            sendData = command.getBytes();
            lastCommand.setText(command);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 5800);
            clientSocket.send(sendPacket);
        }

        public static void rotateLeft(int d) throws IOException
        {
        	DatagramSocket clientSocket = new DatagramSocket();
        	String command = ("0 0 "+(currentAngle-=d)+" b");
            sendData = command.getBytes();
            lastCommand.setText(command);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 5800);
            clientSocket.send(sendPacket);
        }

        public static void rotateRight(int d) throws IOException
        {
        	DatagramSocket clientSocket = new DatagramSocket();
        	String command = ("0 0 "+(currentAngle+=d)+" b");
            sendData = command.getBytes();
            lastCommand.setText(command);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 5800);
            clientSocket.send(sendPacket);
        }
    }


