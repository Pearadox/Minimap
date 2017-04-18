
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.StringTokenizer;


public class MinimapServer
{

    static BufferedImage blueArena = imageToBufferedImage(ResourceLoader.getImage("Arena_BLUE.png"));
    static BufferedImage redArena = imageToBufferedImage(ResourceLoader.getImage("Arena_RED.png"));
    static BufferedImage robotPic = imageToBufferedImage(ResourceLoader.getImage("RobotPicture.png"));
    static BufferedImage rotated;
    static BufferedImage pearadoxLogo = imageToBufferedImage(ResourceLoader.getImage("Pearadox_Logo.png"));
    static JLabel roboIcon;
    static JFrame f;
    static JLabel map;
    static int mapWidth;
    static int mapHeight;
    static double currentAngle = 0;
    static Point currentCoordinate = new Point(180, 670);
    static boolean timedOut = true;
    static boolean isConnected = false;
    static double angleOffset = 0;
    static DatagramSocket serverSocket;
    static DatagramSocket calibrationServerSocket;
    static InetAddress IPAddress;

    static String received = ""; // regular updates: [{change in x} {change in y} {current angle} {alliance(b/r)}]
                                 // zeroing: [{"ZERO"} {coordinate key}]
    static String calibrationReceived = ""; //Zero Angle: "ZEROANGLE"
    										//Zero coordinate: "ZERO (coordinateKey)"
    

    static byte[] receiveData = new byte[16];
    static byte[] calibrationReceiveData = new byte[16];
    static byte[] sendData = new byte[16];
    static int port = 5800;
    static boolean receivedSomething = false;

    static ArrayList<Point> coordsKey = new ArrayList<Point>();

    public static void main(String[] args) throws IOException
    {
    	mapWidth = blueArena.getWidth();
    	mapHeight = blueArena.getHeight();
        collectCoordinates();
        serverSocket = new DatagramSocket(5800);
        calibrationServerSocket = new DatagramSocket(5801);

        f = new JFrame("Minimap");
        f.setSize(mapWidth, mapHeight);
        f.setLocation(0, 0);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setResizable(false);
        f.setIconImage(pearadoxLogo);
        map = new JLabel(new ImageIcon(blueArena));
        roboIcon = new JLabel(new ImageIcon(robotPic));
        roboIcon.setBounds(currentCoordinate.x,currentCoordinate.y,robotPic.getWidth(),robotPic.getHeight());
        JPanel mapPanel = new JPanel();
        mapPanel.setPreferredSize(new Dimension(mapWidth, mapHeight));
        mapPanel.setVisible(true);
        mapPanel.add(map);
        map.add(roboIcon);
        f.add(mapPanel, BorderLayout.CENTER);
        f.pack();
        f.setVisible(true);
        
        Thread send = new Thread(() -> 
        {
        	while(true)
        	{
        		try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		updateTitle();
        	}
        });
        send.start();
        
        Thread calibrate = new Thread(() -> 
        {
        	while(true)
        	{
        		try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		try {
					updateCalibration();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        });
        calibrate.start();
        
        while(true)
        {
            try
            {
                Thread.sleep(10);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            update();
        }
        
    }
    
    public static void sendConfirm()
    {
    	if(receivedSomething)
    	{
	    	sendData = "pearadox".getBytes();
	        try
	        {
	            serverSocket.send(new DatagramPacket(sendData, sendData.length, IPAddress, port));
	        } catch (IOException e)
	        {
	            e.printStackTrace();
	        }
	        System.out.println("attempt to send");
    	}
    }
    
    public static void updateTitle()
    {
    	Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
        f.setTitle("(" + currentCoordinate.x + ", " + currentCoordinate.y + ") "
                + "  " + (currentAngle-angleOffset)
                + "°  [" + mouseLocation.x + ", " + mouseLocation.y +"]");
    }

    public static void update() throws IOException
    {
        //receive information
    	receiveData = new byte[16];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try
        {
        	serverSocket.receive(receivePacket);
        } catch(Exception e) {System.out.println("errored"); }
        System.out.println("CONTINUING");
        
        received = new String(receivePacket.getData());
        System.out.println("RECEIVED: " + received);
        IPAddress = receivePacket.getAddress();
        port = receivePacket.getPort();
        StringTokenizer st = new StringTokenizer(received);

        //analyze input - regular update
        String token1 = st.nextToken().trim();
        if(token1.equals("ZERO") || token1.equals("ZEROANGLE")) return;
        int changeInX = Integer.parseInt(token1);
        int changeInY = Integer.parseInt(st.nextToken());
        currentAngle = Double.parseDouble(st.nextToken());
        String alliance = "b";
        try{
        	alliance = st.nextToken().substring(0,1);
        } catch(Exception e) {}
        
        if(alliance.equals("r"))
        {
            map.setIcon(new ImageIcon(redArena));
        }
        else map.setIcon(new ImageIcon(blueArena));
        moveX(changeInX); moveY(changeInY);

        //rotate and move
        rotated = rotateImage(robotPic, currentAngle-angleOffset);
        roboIcon.setBounds(currentCoordinate.x,currentCoordinate.y,rotated.getWidth(), rotated.getHeight());
        roboIcon.setIcon(new ImageIcon(rotated));

    }

    public static void updateCalibration() throws IOException
    {
    	calibrationReceiveData = new byte[16];
        DatagramPacket receivePacket = new DatagramPacket(calibrationReceiveData, calibrationReceiveData.length);
        try
        {
        	calibrationServerSocket.receive(receivePacket);
        } catch(Exception e) {System.out.println("errored"); }
        System.out.println("CONTINUING");
        
        calibrationReceived = new String(receivePacket.getData());
        System.out.println("RECEIVED: " + calibrationReceived);
    	
    	StringTokenizer st = new StringTokenizer(calibrationReceived);
        //analyze input - zero position, angle
        String firstToken = st.nextToken().trim();
        if(firstToken.equals("ZERO"))
        {
        	int index = Integer.parseInt(st.nextToken().trim());
            currentCoordinate = coordsKey.get(index);
//            System.out.println(currentCoordinate);
            rotated = rotateImage(robotPic, currentAngle-angleOffset);
            roboIcon.setBounds(currentCoordinate.x,currentCoordinate.y,rotated.getWidth(), rotated.getHeight());
            return;
        }
        else if(firstToken.equals("ZEROANGLE"))
        {
        	angleOffset = currentAngle;
        	System.out.println(currentAngle-angleOffset);
        	setRotated(rotateImage(robotPic, currentAngle-angleOffset));
            roboIcon.setBounds(getCoords().x,getCoords().y,rotated.getWidth(), rotated.getHeight());
            roboIcon.setIcon(new ImageIcon(rotated));
        }
    }
    
    public static void setRotated(BufferedImage rotatedpic) { rotated = rotatedpic; }
    public static Point getCoords() { return currentCoordinate; }
    public static void setRobotPos(int x, int y, BufferedImage rotated) { roboIcon.setBounds(x,y,rotated.getWidth(), rotated.getHeight()); } 
    public static void scaleCurrentAngle() {currentAngle %= 360; currentAngle += 360; currentAngle %= 360;}
    public static boolean isConnected() {return isConnected;}
    public static void setIsConnected(boolean b) {isConnected = b;}
    public static void rotateRight(double angle)
    {
        angle %= 360;
        if(angle >= 180)
        {
            rotateLeft(angle - 180);
            return;
        }
        else if(angle <= -180)
        {
            rotateLeft(angle + 180);
            return;
        }
        currentAngle += angle-angleOffset;
    }
    public static void rotateLeft(double angle)
    {
        if(angle >= 180)
        {
            rotateRight(angle - 180);
            return;
        }
        else if(angle <= -180)
        {
            rotateRight(angle + 180);
            return;
        }
        currentAngle -= angle-angleOffset;
    }
    public static void moveX(int distance)
    {
        double temp = currentAngle-angleOffset + 90;
        temp += 360; temp %= 360;
        double radians = Math.toRadians(temp-angleOffset);
        double desiredx, desiredy;
        desiredx = Math.sin(radians) * distance;
        desiredy = Math.cos(radians) * distance;
        desiredy = currentCoordinate.y - desiredy;
        desiredx = currentCoordinate.x + desiredx;

        int finalx, finaly;
        if(desiredx <= 35) finalx = 35;
        else if(desiredx >= mapWidth-58) finalx = mapWidth-58;
        else finalx = (int)Math.round(desiredx);
        if(desiredy <= 40) finaly = 40;
        else if(desiredy >= mapHeight-68) finaly = mapHeight-68;
        else finaly = (int)Math.round(desiredy);
        currentCoordinate.setLocation(finalx, finaly);
    }
    public static void moveY(int distance)
    {
        currentAngle += 360-angleOffset; currentAngle %= 360;
        double radians = Math.toRadians(currentAngle-angleOffset);
        double desiredx, desiredy;
        desiredx = Math.sin(radians) * distance;
        desiredy = Math.cos(radians) * distance;
        desiredy = currentCoordinate.y - desiredy;
        desiredx = currentCoordinate.x + desiredx;

        int finalx, finaly;
        if(desiredx <= 35) finalx = 35;
        else if(desiredx >= mapWidth-58) finalx = mapWidth-58;
        else finalx = (int)Math.round(desiredx);
        if(desiredy <= 40) finaly = 40;
        else if(desiredy >= mapHeight-68) finaly = mapHeight-68;
        else finaly = (int)Math.round(desiredy);
        currentCoordinate.setLocation(finalx, finaly);
    }
    public static void moveLeftAbsolute(int distance)
    {
        if(currentCoordinate.x - distance <= 0)
        {
            currentCoordinate.setLocation(0, currentCoordinate.y);
            return;
        }
        currentCoordinate.setLocation(currentCoordinate.x - distance, currentCoordinate.y);
    }
    public static void moveRightAbsolute(int distance)
    {
        if(currentCoordinate.x + distance >= mapWidth)
        {
            currentCoordinate.setLocation(mapWidth, currentCoordinate.y);
            return;
        }
        currentCoordinate.setLocation(currentCoordinate.x + distance, currentCoordinate.y);
    }
    public static void moveUpAbsolute(int distance)
    {
        if(currentCoordinate.y - distance <= 0)
        {
            currentCoordinate.setLocation(currentCoordinate.x, 0);
            return;
        }
        currentCoordinate.setLocation(currentCoordinate.x, currentCoordinate.y - distance);
    }
    public static void moveDownAbsolute(int distance)
    {
        if(currentCoordinate.y + distance >= mapHeight)
        {
            currentCoordinate.setLocation(currentCoordinate.x, mapHeight);
            return;
        }
        currentCoordinate.setLocation(currentCoordinate.x, currentCoordinate.y + distance);
    }
    public static BufferedImage rotateImage(Image image, double angle)
    {
        angle += 360;
        angle %= 360;
        angle *= 11/90.*13 / 90;

        double sin = Math.abs(Math.sin(angle));
        double cos = Math.abs(Math.cos(angle));
        int originalWidth = image.getWidth(null);
        int originalHeight = image.getHeight(null);
        int newWidth = (int) Math.floor(originalWidth * cos + originalHeight * sin);
        int newHeight = (int) Math.floor(originalHeight * cos + originalWidth * sin);
        BufferedImage rotatedBI = new BufferedImage(newWidth, newHeight, BufferedImage.TRANSLUCENT);
        Graphics2D g2d = rotatedBI.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.translate((newWidth - originalWidth) / 2, (newHeight - originalHeight) / 2);
        g2d.rotate(angle, originalWidth / 2, originalHeight / 2);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return rotatedBI;
    }

    public static BufferedImage imageToBufferedImage(Image image) {
        if (image instanceof BufferedImage)
        return (BufferedImage)image;

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;

            if (hasAlpha == true)
                transparency = Transparency.BITMASK;

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();

            bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) { } //No screen

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;

            if (hasAlpha == true) {type = BufferedImage.TYPE_INT_ARGB;}
                bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }
    
    public static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage)
            return ((BufferedImage)image).getColorModel().hasAlpha();

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) { }

        // Get the image's color model
        return pg.getColorModel().hasAlpha();
    }

    public static void collectCoordinates()
    {
        coordsKey.add(new Point(50, 66));
        coordsKey.add(new Point(292, 50));
        coordsKey.add(new Point(116, 567));
        coordsKey.add(new Point(181, 609));
        coordsKey.add(new Point(236, 567));
        coordsKey.add(new Point(111, 671));
        coordsKey.add(new Point(180, 671));
        coordsKey.add(new Point(257, 671));
    }

    public static boolean isTimedOut() { return timedOut; }
    public static void setTimedOut(boolean b) {timedOut = b;}
    public static void setTimeout(int milliseconds)
    {
    	timedOut = false;
    	long tstart = System.currentTimeMillis();
    	Thread t = new Thread(() ->
    	{
    		while(System.currentTimeMillis() - tstart < milliseconds){}
    		setTimedOut(true);
    	});
    	t.start();
    }
}
