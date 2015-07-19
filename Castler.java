import java.util.*;
import java.util.stream.Collectors;

/**
 * Wants to make a square castle... however if opponents interfere then will reluctantly make an odd-shaped castle   
 */
public class Castler {
    private static final int MAP_SIZE = 10;
	private int ownId;
	private int deployableArmyCount;
	private List<Territory> territories;
	private Territory[][] map;
	private Map<Territory,Territory> territoryHashMap;
	List<Territory> ownedTerritories;
	public int minRow;
	public int minCol;

	public static void main(String[] args)
	{
		new Castler(args);
	}
	
	Castler(String[] args)
	{
        ownId = Integer.parseInt(args[0]);
        deployableArmyCount = Integer.parseInt(args[1]);

        territories = new ArrayList<Territory>();
        map = new Territory[MAP_SIZE][MAP_SIZE]; 
        
        territoryHashMap = new HashMap<Territory,Territory>();
        
        for (String s : args[2].split(" ")) {
            Territory territory = new Territory(s.split(","));
           	territories.add(territory);
            territoryHashMap.put(territory, territory);
            map[territory.col][territory.row]=territory;
        }
        
        ownedTerritories = territories.stream().filter(t->t.id==ownId).collect(Collectors.toList());

        minRow=Integer.MAX_VALUE;
        minCol=Integer.MAX_VALUE;

        //find top left territory that is the corner of our castle :)
        int largestArea=0;
        for (Territory territory : ownedTerritories)
        {
        	int area=countRightDownConnected(territory,new int[MAP_SIZE][MAP_SIZE]);
        	if (area>largestArea)
        	{
        		largestArea=area;
        		minRow=territory.row;
        		minCol=territory.col;
        	}
        }

        // the average army size per owned territory
       	int meanArmySize=0;
       	for (Territory territory : ownedTerritories)
       	{
       		meanArmySize+=territory.armies;
       	}
       	meanArmySize/=ownedTerritories.size();
       
        
        int squareSideLength = (int) Math.ceil(Math.sqrt(ownedTerritories.size()));
        
        // if we own all territories inside the square of our castle, or we have stalled but have the numbers to expand... make the length of side of the square larger to allow expansion
        if (squareSideLength*squareSideLength == ownedTerritories.size() || meanArmySize>squareSideLength)
        {
        	squareSideLength++;
        }
        
        // lets collate all the enemy territories within the area of our desired castle square and marke them as candidates to be attacked.
        List<Territory> attackCandidates = new ArrayList<>();
        for (int y=minRow;y<minRow+squareSideLength;y++)
        {
        	for (int x=minCol;x<minCol+squareSideLength;x++)
            {
        		Territory territory = map[x%MAP_SIZE][y%MAP_SIZE];
        		if (territory!=null && territory.id!=ownId)
        		{
        			attackCandidates.add(territory); 
        		}
            }
        }
     
        
        // sort in ascending defensive army size.
        attackCandidates.sort((a,b)->a.armies-b.armies);
        
        List<Territory> unCommandedTerritories = new ArrayList<>(ownedTerritories);
        List<Move> moves = new ArrayList<>();
        Set<Territory> suicideAttackCandidate = new HashSet<>();
        
        // command owned territories to attack any territories within the area of the prescribed square if able to win. 
        for (int i=0;i<unCommandedTerritories.size();i++)
        {
        	Territory commandPendingTerritory =unCommandedTerritories.get(i);
        	List<Territory> neighbours = getNeighbours(commandPendingTerritory,map);
        	List<Territory> attackCandidatesCopy = new ArrayList<>(attackCandidates);
        	
        	// remove non-neighbour attackCandidates
        	attackCandidatesCopy.removeIf(t->!neighbours.contains(t));
        	
        	for (Territory attackCandidate : attackCandidatesCopy)
        	{
        		Battle battle = battle(commandPendingTerritory,attackCandidate);
        		if (battle.attackerWon)
        		{
        			attackCandidates.remove(attackCandidate);
        			suicideAttackCandidate.remove(attackCandidate);
        			unCommandedTerritories.remove(i--);
        			
        			Territory[][] futureMap = cloneMap(map);
        			futureMap[attackCandidate.col][attackCandidate.row].id=ownId;
        			
        			// default to sending the required armies to win + half the difference of the remainder
        			int armiesToSend = battle.minArmiesRequired + (commandPendingTerritory.armies-battle.minArmiesRequired)/2;
        			
        			// but if after winning, there is no threat to the current territory then we shall send most of the armies to attack
        			if (!underThreat(commandPendingTerritory, futureMap))
        			{
        				armiesToSend = commandPendingTerritory.armies-1;
        			}
        			moves.add(new Move(commandPendingTerritory,attackCandidate,armiesToSend));
        			
        			break;
        		}
        		else
        		{
        			// we can't win outright, add it to a list to attack kamikaze style later if needed.
        			suicideAttackCandidate.add(attackCandidate);
        		}
        	}
        }
        
     
        // Find edge territories.
        // A territory is deemed an edge if at least one of its neighbours are not owned by us.
        List<Territory> edgeTerritories = new ArrayList<>();
        ownedTerritories.forEach(owned->
        	getNeighbours(owned,map).stream().filter(neighbour->
        		neighbour.id!=ownId).findFirst().ifPresent(t->
        		edgeTerritories.add(owned)));

        // All edge territories that have not yet had orders this turn...
        List<Territory> uncommandedEdgeTerritories = edgeTerritories.stream().filter(t->unCommandedTerritories.contains(t)).collect(Collectors.toList());
        
        // Find edges that are under threat by hostile neighbours
        List<Territory> threatenedEdges = edgeTerritories.stream().filter(edge->underThreat(edge,map)).collect(Collectors.toList());
        
        // All threatened edge territories that have not yet had orders this turn...
        List<Territory> uncommandedThreatenedEdges = threatenedEdges.stream().filter(t->unCommandedTerritories.contains(t)).collect(Collectors.toList());
        
        // unthreatened edges
        List<Territory> unThreatenedEdges = edgeTerritories.stream().filter(edge->!threatenedEdges.contains(edge)).collect(Collectors.toList());
        List<Territory> uncommandedUnThreatenedEdges = unThreatenedEdges.stream().filter(t->unCommandedTerritories.contains(t)).collect(Collectors.toList());
        
        // map that describes the effect of moves. Ensures that we do not over commit on one territory and neglect others
		Territory[][] futureMap = cloneMap(map);

		//sort the threatened edges in ascending order of defense
        threatenedEdges.sort((a,b)->a.armies-b.armies); 
        
        int meanThreatenedEdgeArmySize = Integer.MAX_VALUE;
        if (!threatenedEdges.isEmpty())
        {
        	// calculate the average defense of the threatened edges
        	int[] total = new int[1];
        	threatenedEdges.stream().forEach(t->total[0]+=t.armies);
        	meanThreatenedEdgeArmySize = total[0]/threatenedEdges.size(); 
      	        
	        // command any unthreatened edges to bolster weak threatened edges. 
        	out:
	        for (int i=0;i<uncommandedUnThreatenedEdges.size();i++)
	        {
	        	Territory commandPendingTerritory = uncommandedUnThreatenedEdges.get(i);

	        	// the unthreatened edge has spare armies
	        	if (commandPendingTerritory.armies>1)
	        	{
	        		for (int x=MAP_SIZE-1;x<=MAP_SIZE+1;x++)
	        		{
	        			for (int y=MAP_SIZE-1;y<=MAP_SIZE+1;y++)
	        			{
	        				if (!(x==MAP_SIZE && y==MAP_SIZE))
	        				{
	        					int xx=commandPendingTerritory.col+x;
	        					int yy=commandPendingTerritory.row+y;
	        					Territory territory = futureMap[xx%MAP_SIZE][yy%MAP_SIZE];
	        					
	        					// if the current threatened edge has less than average defensive army then send all spare troops to from the uncommanded unthreatened edge. 
	        					if (territory!=null && territory.armies<meanThreatenedEdgeArmySize && threatenedEdges.contains(territory))
	        					{
	        						// update future map
		        					Territory clonedTerritory = (Territory) territory.clone();
		        					clonedTerritory.armies+=commandPendingTerritory.armies-1;
	        						futureMap[xx%MAP_SIZE][yy%MAP_SIZE]=clonedTerritory;
	        						
	        						moves.add(new Move(commandPendingTerritory,territory,commandPendingTerritory.armies-1));
	        						
	        	        			unCommandedTerritories.remove(commandPendingTerritory);
	        	        			uncommandedUnThreatenedEdges.remove(i--);
	        	        			uncommandedEdgeTerritories.remove(commandPendingTerritory);
	        	        			continue out;
	        					}
	        				}
	        			}
	        		}
	        	}
	        }
        	
	        // command any stronger threatened edges to bolster weak threatened edges. 
        	out:
        	
	        for (int i=0;i<uncommandedThreatenedEdges.size();i++)
	        {
	        	Territory commandPendingTerritory = uncommandedThreatenedEdges.get(i);
	        	
	        	// the threatened edge has more than average edge armies
	        	if (commandPendingTerritory.armies>meanThreatenedEdgeArmySize)
	        	{
	        		for (int x=MAP_SIZE-1;x<=MAP_SIZE+1;x++)
	        		{
	        			for (int y=MAP_SIZE-1;y<=MAP_SIZE+1;y++)
	        			{
	        				if (!(x==MAP_SIZE && y==MAP_SIZE))
	        				{
	        					int xx=commandPendingTerritory.col+x;
	        					int yy=commandPendingTerritory.row+y;
	        					Territory territory = futureMap[xx%MAP_SIZE][yy%MAP_SIZE];
	        					
	        					// if the current threatened edge has less than average defensive army then send the excess troops larger than the average edge armies amount from the uncommanded threatened edge. 
	        					if (territory!=null && territory.armies<meanThreatenedEdgeArmySize && threatenedEdges.contains(territory))
	        					{
	        						// update future map
		        					Territory clonedTerritory = (Territory) territory.clone();
		        					clonedTerritory.armies+=commandPendingTerritory.armies-meanThreatenedEdgeArmySize;
	        						futureMap[xx%MAP_SIZE][yy%MAP_SIZE]=clonedTerritory;
	        						moves.add(new Move(commandPendingTerritory,territory,commandPendingTerritory.armies-meanThreatenedEdgeArmySize));
	        						
	        	        			unCommandedTerritories.remove(commandPendingTerritory);
	        	        			uncommandedThreatenedEdges.remove(i--);
	        	        			uncommandedEdgeTerritories.remove(commandPendingTerritory);
	        	        			continue out;
	        					}
	        				}
	        			}
	        		}
	        	}
	        }
        }
	        
        // for any uncommanded non-edge territories, just move excess armies to the right or down
       	unCommandedTerritories.stream().filter(t->
       		t.armies>1 && !edgeTerritories.contains(t)).forEach(t->
       			moves.add(new Random().nextFloat()>0.5? (new Move(t,map[(t.col+1)%MAP_SIZE][t.row],t.armies-1)):(new Move(t,map[t.col][(t.row+1)%MAP_SIZE],t.armies-1))));
       	

       	
       	// lets perform suicide attacks if we are in a good position to do so... hopefully will whittle down turtling enemies.
        for (Territory target : suicideAttackCandidate)
        {
        	List<Territory> ownedNeighbours = getNeighbours(target, map).stream().filter(neighbour->neighbour.id==ownId).collect(Collectors.toList());
        	
        	for (Territory ownedTerritory : ownedNeighbours)
        	{
        		// if the edge has yet to be commanded and the territory has more than three times the average armies then it is likely that we are in a power struggle so just suicide attack!
        		if (uncommandedEdgeTerritories.contains(ownedTerritory) && ((ownedTerritory.armies)/3-1)>meanArmySize)
        		{
        			uncommandedEdgeTerritories.remove(ownedTerritory);
        			unCommandedTerritories.remove(ownedTerritory);
        			moves.add(new Move(ownedTerritory,target,ownedTerritory.armies-meanArmySize));
        		}
        	}
        }

        
        // deploy troops to the weakest threatened edges
        int armiesToDeploy =deployableArmyCount;

        Map<Territory,Integer> deployTerritories = new HashMap<>();
        while (armiesToDeploy>0 && threatenedEdges.size()>0)
        {
        	for (Territory threatenedEdge : threatenedEdges)
        	{
        		Integer deployAmount = deployTerritories.get(threatenedEdge);
        		if (deployAmount==null)
        		{
        			deployAmount=0;
        		}
        		deployAmount++;
        		deployTerritories.put(threatenedEdge,deployAmount);
        		armiesToDeploy--;
        		if (armiesToDeploy==0) break;
        	}
        }
        
        // no threatened edges needing deployment, so just add them to the "first" edge
        if (armiesToDeploy>0)
        {
        	deployTerritories.put(edgeTerritories.get(new Random().nextInt(edgeTerritories.size())),armiesToDeploy);
        }
        
        // send deploy command
        StringBuilder sb = new StringBuilder();
        deployTerritories.entrySet().stream().forEach(entry-> sb.append(entry.getKey().row + "," + entry.getKey().col + "," + entry.getValue()+" "));
        sb.append(" ");
        System.out.println(sb);

        StringBuilder sb1 = new StringBuilder();

        // send move command
        moves.stream().forEach(move-> sb1.append(move.startTerritory.row + "," + move.startTerritory.col + "," + move.endTerritory.row + "," + move.endTerritory.col + "," + move.armies+" "));
        sb1.append(" ");
        System.out.println(sb1);
        	        
    }

	/**
	 *	Recursive method that attempts to count area the territories in the square with the given territory as the top left corner  
	 */
	private int countRightDownConnected(Territory territory,int[][] visited) {

		int count=0;
		if (visited[territory.col][territory.row]>0) return visited[territory.col][territory.row];
		if (visited[territory.col][territory.row]<0) return 0;
		visited[territory.col][territory.row]=-1;
		
		
		if (territory!=null && territory.id==ownId)
		{
			if (visited[territory.col][territory.row]>0) return visited[territory.col][territory.row];
			
			count++;
			count+=countRightDownConnected(map[territory.col][(territory.row+1)%MAP_SIZE],visited);
			count+=countRightDownConnected(map[(territory.col+1)%MAP_SIZE][territory.row],visited);
			visited[territory.col][territory.row]=count;
		}
		return count;
	}

	/**
	 *	Performs a deep clone of the provided map  
	 */
	private Territory[][] cloneMap(Territory[][] map)
	{
		Territory[][] clone = new Territory[MAP_SIZE][MAP_SIZE];
		for (int x=0;x<MAP_SIZE;x++)
		{
			for (int y=0;y<MAP_SIZE;y++)
			{
				Territory territory = map[x][y];
				clone[x][y] = territory==null?null:territory.clone();
			}
		}
		return clone;
	}
        
	/**
	 * Simulates a battle between an attacker and a defending territory
	 */
    private Battle battle(Territory attacker, Territory defender) 
    {
    	Battle battle = new Battle();
    	battle.attackerWon=false;
    	battle.loser=attacker;
    	battle.winner=defender;
    	
    	for (int i=0;i<attacker.armies;i++)
    	{
	    	int attackerArmies = i;
	    	int defenderArmies = defender.armies;
	        defenderArmies -= (int) Math.round(attackerArmies * .6);
	        attackerArmies -= (int) Math.round(defenderArmies * .7);
	        if (defenderArmies <= 0) {
	            if (attackerArmies > 0) {
	                defenderArmies = attackerArmies;
	                battle.attackerWon=true;
	                battle.loser=defender;
	                battle.winner=attacker;
	                battle.minArmiesRequired=i;
	                break;
	            }
	        }
    	}
        return battle;
	}

    /**
     * returns true if the provided territory is threatened by any hostile neighbours using the provided map 
     */
	private boolean underThreat(Territory territory,Territory[][] map)
    {
    	return !getNeighbours(territory,map).stream().filter(neighbour->neighbour.id!=ownId && neighbour.id!=-1).collect(Collectors.toList()).isEmpty();
    }

	/**
	 * returns the neighbours of the provided territory using the provided map 
	 */
    private List<Territory> getNeighbours(Territory territory,Territory[][] map) {
    	
    	List<Territory> neighbours = new ArrayList<>();
		for (int x=MAP_SIZE-1;x<=MAP_SIZE+1;x++)
		{
			for (int y=MAP_SIZE-1;y<=MAP_SIZE+1;y++)
			{
				if (!(x==MAP_SIZE && y==MAP_SIZE))
				{
					Territory t = map[(x+territory.col)%MAP_SIZE][(y+territory.row)%MAP_SIZE];
					if (t!=null) neighbours.add(t);
				}
			}
		}
		return neighbours;
	}
    
    static class Battle {
    	public int minArmiesRequired;
		Territory winner;
    	Territory loser;
    	boolean attackerWon;
    }
    
    static class Move
    {
    	public Move(Territory startTerritory, Territory endTerritory, int armiesToSend) 
    	{
			this.endTerritory=endTerritory;
			this.startTerritory=startTerritory;
			this.armies=armiesToSend;
		}
		Territory startTerritory;
    	Territory endTerritory;
    	int armies;
    }

	static class Territory implements Cloneable
	{
        public int id, row, col, armies;

        public Territory clone()
        {
        	try {
				return (Territory) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(e);
			}
        }

		public Territory(String[] data) {
            id = Integer.parseInt(data[3]);
            row = Integer.parseInt(data[0]);
            col = Integer.parseInt(data[1]);
            armies = Integer.parseInt(data[4]);
        }
        
        void add(Territory territory)
        {
        	row+=(territory.row);
        	col+=(territory.col);
        }
        
        @Override
        public int hashCode()
        {
        	return row*MAP_SIZE+col;
        }
        
        @Override
        public boolean equals(Object other)
        {
        	Territory otherTerritory = (Territory) other;
        	return row == otherTerritory.row && col == otherTerritory.col;
        }
        
    }
}
