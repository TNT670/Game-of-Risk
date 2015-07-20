package visualiser;

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Random;

public class PlayerSpriteGenerator
{
  static boolean PALETTEIZED=false;
  static int MAX_COLOURS=4;
  static HashMap<Long, Color> paletteizedColourMap = new HashMap<>();
  static Color[] paletteizedColours = new Color[MAX_COLOURS];
  static int count=0;
  static
  {
    Random rand = new Random(6L);
    for (int i=0;i<MAX_COLOURS;i++)
    {
      paletteizedColours[i]= Color.getHSBColor(rand.nextFloat(), rand.nextFloat(), 0.85f);
    }
  }
  
	static Color generateColour(long seed)
	{
	  Color colour=null;
	
	  if (PALETTEIZED)
	  {
  	  colour = paletteizedColourMap.get( seed*1234567890L );
  	  
  	  if (colour==null)
  	  {
  	    colour = paletteizedColours[count=(count+1)%MAX_COLOURS];
  	    paletteizedColourMap.put( seed*1234567890L, colour );
  	  }
	  }
	  else
	  {
	    Random rand = new Random(seed*1234567890L);
	    colour=Color.getHSBColor(rand.nextFloat(), rand.nextFloat(), 0.85f);
	  }
	  return colour;
	}
	
	static Color generateSaturatedColour(long seed)
	{
	  Color colour=null;
	    Random rand = new Random(seed*1234567890L);
	    colour=Color.getHSBColor(rand.nextFloat(), rand.nextFloat(), 0.99f);

	  return colour;
	}
	
	static BufferedImage generate(long seed,Color colour)
	{
		Random rand = new Random(seed*1234567890L);

		int alienColour = colour.getRGB();
		BufferedImage image = new BufferedImage(5,5,BufferedImage.TYPE_INT_ARGB);
		for (int y=0;y<5;y++)
		{
			for (int x=0;x<3;x++)
			{
				int pixelColour = rand.nextBoolean()?0xFF000000|alienColour:0x00000000;
				image.setRGB(x, y, pixelColour);
				image.setRGB(4-x, y, pixelColour);
			}
		}
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice gs = ge.getDefaultScreenDevice();
	    GraphicsConfiguration gc = gs.getDefaultConfiguration();

	    BufferedImage bimage = gc.createCompatibleImage(5, 5, Transparency.BITMASK);

	    bimage.getGraphics().drawImage(image, 0, 0, null);
	    return bimage;
	}

}

