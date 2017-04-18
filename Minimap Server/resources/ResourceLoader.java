import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class ResourceLoader {

	static ResourceLoader rl = new ResourceLoader();
	
	public static Image getImage(String fileName)
	{
		return Toolkit.getDefaultToolkit().getImage(rl.getClass().getResource("Images/" + fileName));
	}
}
