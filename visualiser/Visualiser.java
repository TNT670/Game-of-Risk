package visualiser;

import gameofrisk.Risk;
import gameofrisk.Risk.Bonus;
import gameofrisk.Risk.Territory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Visualiser extends JPanel
{
	private static final int IMAGE_SIZE=6;
	private static final int FONT_SIZE = 9;
	private static final int TEXT_SIZE = FONT_SIZE;
	private static final int TEXT_OFFSET = FONT_SIZE-1;
	private static final int ICON_OFFSET = FONT_SIZE/2-IMAGE_SIZE/2;
	private static final int CELL_WIDTH = 30;
	private static final int MAP_SIZE = 10;
	private static final int CELL_HEIGHT = 30;
	private static final int MAX_ROUNDS = 1000;
	
	private Map<Integer, Color> playerColourMap = new HashMap<>();
	private Map<Integer, BufferedImage> playerSpriteMap = new HashMap<>();
	private BufferedImage displayImage;
	private BufferedImage gifdisplayImage;
	private GifSequenceWriter gifSequenceWriter;
	private BufferedImage legendImage;
	private BufferedImage backgroundImage;
	private int roundNo;
	private Map<Integer, String> players;
	private Territory[][] territories;
	private List<Bonus> bonusList;

	public Visualiser(Map<Integer, String> players1,Territory[][] territories,List<Bonus> bonusList)
	{
		  this.territories=territories;
		  this.bonusList=bonusList;
		  players=new HashMap<Integer, String>(players1);
		  players.put(-1,"Neutral Player");
		  
	    for (Integer playerId : players.keySet())
	    {
	      Color colour = PlayerSpriteGenerator.generateColour(players.get(playerId).hashCode()*playerId);
	      BufferedImage sprite = PlayerSpriteGenerator.generate(players.get(playerId).hashCode(),colour);
	      
	      playerColourMap.put(playerId,colour);
	      playerSpriteMap.put(playerId,sprite);
	      
	    }

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	      GraphicsDevice gs = ge.getDefaultScreenDevice();
	      GraphicsConfiguration gc = gs.getDefaultConfiguration();

	    
	      legendImage = gc.createCompatibleImage(200, 10+(players.keySet().size()+bonusList.size()+10)*TEXT_SIZE, Transparency.OPAQUE);
	      displayImage = gc.createCompatibleImage(MAP_SIZE*CELL_WIDTH+legendImage.getWidth()+10, MAP_SIZE*CELL_HEIGHT, Transparency.BITMASK);
	      gifdisplayImage=new BufferedImage(displayImage.getWidth(), displayImage.getHeight(), BufferedImage.TYPE_INT_RGB);
	    


		
		JFrame frame=new JFrame("Initialising...");
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.add(this);
		frame.setVisible(true);
		
		if (Risk.GUI_GIF_OUTPUT)
		{
		    try
		    {
		      gifSequenceWriter = new GifSequenceWriter(new FileImageOutputStream( new File( Risk.GUI_GIF_FILE ) ), gifdisplayImage.getType(), 50, true );
		    } catch (Exception e)
		    {
		      e.printStackTrace();
		    }
		}
	}
	
	public void reset()
	{
		roundNo=0;
	}
	
	public void cleanup()
	{
	      if (gifSequenceWriter!=null)
	      {
	          try
	          {
	        	  gifSequenceWriter.close();
	        	  gifSequenceWriter=null;
	          } catch(Exception e)
	          {
	        	  e.printStackTrace();
	          }
	      }
	}
	  
	public void update()
	{
  	    
	      Graphics2D g = (Graphics2D) displayImage.getGraphics();
	      g.setColor(new Color(0x202F20));
	      g.fillRect(0, 0, displayImage.getWidth(), displayImage.getHeight());
	      
	      Map<Integer,List<Territory>> territoryOwnership = new HashMap<>();
	      
	      for (Integer playerId : players.keySet())
	      {
	    	  territoryOwnership.put(playerId, new ArrayList<Territory>());
	      }
 	      
	      for (int x=0;x<MAP_SIZE;x++)
	      {
	    	  for (int y=0;y<MAP_SIZE;y++)
	    	  {
	    		  Territory territory = territories[y][x];
				  if (territory!=null)
				  {
					  territoryOwnership.get(territory.player).add(territory);
					  
				    BufferedImage playerSprite = playerSpriteMap.get(territory.player);
				    g.setColor(PlayerSpriteGenerator.generateSaturatedColour(territory.bonusId));
				    g.fillRect(x*CELL_WIDTH,y*CELL_HEIGHT,CELL_WIDTH,CELL_HEIGHT);
				    g.drawImage(playerSprite.getScaledInstance(CELL_WIDTH/2, CELL_HEIGHT/2, Image.SCALE_FAST),x*CELL_WIDTH+CELL_WIDTH/4,y*CELL_HEIGHT+CELL_HEIGHT/4,null);
				    g.setColor(Color.BLACK);
				    g.drawString(""+territory.armies,x*CELL_WIDTH, y*CELL_HEIGHT+IMAGE_SIZE+FONT_SIZE);

				  }
    	      }
    	  }
	      
	      
	      Graphics2D g2 = (Graphics2D) legendImage.getGraphics();
	      g2.setColor(new Color(0x202F20));
	      g2.fillRect(0, 0, legendImage.getWidth(), legendImage.getHeight());
	      g2.setColor(new Color(0xFFFFFF));
	      g2.setFont(new Font("SansSerif", Font.PLAIN, FONT_SIZE));
	      g2.drawString("Round "+(roundNo++)+"/"+MAX_ROUNDS, TEXT_SIZE, TEXT_OFFSET);
	      int count=0;
	      
	      
	      
  	    for (Integer playerId : players.keySet())
  	    {
  	    	int armies=0;
  	    	for (Territory territory : territoryOwnership.get(playerId))
  	    	{
  	    		armies +=territory.armies;
  	    	}

	        g2.setColor(playerColourMap.get(playerId));
	        g2.drawImage(playerSpriteMap.get(playerId), 0, 10+count*TEXT_SIZE+ICON_OFFSET, null);
	        g2.drawString(territoryOwnership.get(playerId).size()+" ("+armies+") - "+players.get(playerId), TEXT_SIZE, 10+count*TEXT_SIZE+TEXT_OFFSET);
	        count++;
	      }
  	  count+=3;
  	  
      g2.setColor(new Color(0xFFFFFF));
      g2.setFont(new Font("SansSerif", Font.PLAIN, FONT_SIZE));
      g2.drawString("Bonus", TEXT_SIZE, 10+count*TEXT_OFFSET);

  	    for (Bonus bonus : bonusList)
  	    {
  	    	String owner = "Under Contention";
  	    	int ownerId = -2;
  	    	if (bonus.territories.stream().allMatch(t->t.player==bonus.territories.stream().findFirst().get().player))
  	    	{
  	    		ownerId = bonus.territories.stream().findFirst().get().player;
  	    		owner=players.get(ownerId);
  	    	}
  	    	
	        g2.setColor(PlayerSpriteGenerator.generateSaturatedColour(bonus.id));
	        g2.fillRect(0,10+count*TEXT_SIZE+ICON_OFFSET, IMAGE_SIZE,IMAGE_SIZE);
	        g2.drawString(""+bonus.value, TEXT_SIZE, 10+count*TEXT_SIZE+TEXT_OFFSET);

	        g2.setColor(playerColourMap.get(ownerId));
	        g2.drawString(owner, TEXT_SIZE+30, 10+count*TEXT_SIZE+TEXT_OFFSET);

	        
	        count++;
	      }
	      
	      
	      g.drawImage(legendImage,MAP_SIZE*CELL_WIDTH+10,20,null);
	      
	      if(Risk.GUI_GIF_OUTPUT)
	      {
    	      try
    	      {
    	    	  gifdisplayImage.getGraphics().drawImage(displayImage, 0, 0, null);
    	    	  gifSequenceWriter.writeToSequence( gifdisplayImage );
    	      }catch (Exception e)
    	      {
    	    	  e.printStackTrace();
    	      }
	      }

	      repaint();
	  }
	  
	  @Override
	  public void paint(Graphics g)
	  {
	      Graphics2D g2 = (Graphics2D) g;

	      if (displayImage!=null)
	      {
	          g2.drawImage(displayImage, 0, 0,getSize().width,getSize().height, null);
	        
	      }

	  }
	
}


