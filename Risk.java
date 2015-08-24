import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * This is the controller that runs the Risk, the Warlight Way KotH competition
 * on Programming Puzzles and Code Golf Stack Exchange.
 *
 * Updated 24-08-15
 **/

public class Risk {
    private List<RiskPlayer> players = new ArrayList<RiskPlayer>();
    private Map<Integer, String> cmdMap = new HashMap<Integer, String>();
    private String[] clazz = {"java CodeGolf", "java LandGrab", "java Hermit", "java Castler", "java RandomHalver", "WeSwarm"};
    private PlayingField field = new PlayingField();

    private final int rounds = 20, turns = 1000, timeout = 2000;

    public Risk() {
        addPlayers();
        for (int k = 0; k < rounds; k++)
            init(k+1);
        java.util.Collections.sort(players, new java.util.Comparator<RiskPlayer>() {
            @Override
            public int compare(RiskPlayer r1, RiskPlayer r2) {
                return r2.score - r1.score;
            }
        });
        for (RiskPlayer p : players)
            System.out.println(p);
    }

    private void addPlayers() {
        int id = 0;
        for (String s : clazz) {
            players.add(new RiskPlayer(s, id));
            cmdMap.put(Integer.valueOf(id++), s);
        }
    }

    private void init(int round) {
        field.init();
        startGame(round);
    }

    private void startGame(int round) {
        for (RiskPlayer p : players) {
            Random r = new Random();
            while (true) {
                int a = r.nextInt(10), b = r.nextInt(10);
                if (field.territories[a][b].player == -1) {
                    field.territories[a][b].player = p.id;
                    field.territories[a][b].armies = 5;
                    break;
                }
            }
        }
        doProcess(round);
    }

    private void doProcess(int round) {
        List<RiskPlayer> clones = new ArrayList<RiskPlayer>(players);
        o: for (int k = 0; k < turns; k++) {
        	String z = "";
        	for (int l = clones.size()-1; l >= 0; l--) {
        		RiskPlayer p = clones.get(l);
        		int n = 0;
        		for (Territory[] ta : field.territories)
        			for (Territory t : ta)
        				if (t.player == p.id)
        					n++;
        		if (n == 0) {
        			System.out.println(p.cmd + " has no territories");
        			clones.remove(p);
        		}

        		if (clones.size() == 1)
        			break o;
        	}

            System.out.println("Round: " + round + ", Turn: " + (k+1));
            List<String> commands = new ArrayList<String>();
            Map<Territory, Integer> map = new HashMap<Territory, Integer>();
            for (java.util.Iterator<RiskPlayer> i = clones.iterator(); i.hasNext();) {
            	RiskPlayer p = i.next();
                try {
                    int apt = 5;

                    final Set<Territory> territories = new HashSet<Territory>();
                    int numTerritories = 0;
                    for (Territory[] ta : field.territories) {
                        for (Territory t : ta) {
                            if (t.player == p.id) {
                                territories.add(t);
                                territories.addAll(t.getAdjacent());
                                numTerritories++;
                            }
                        }
                    }

                    for (Bonus b : field.bonusList) {
                        Set<Territory> s = new HashSet<Territory>(territories);
                        for (java.util.Iterator<Territory> it = s.iterator(); it.hasNext();) {
                            Territory t = it.next();
                            if (t.player != p.id)
                                it.remove();
                        }
                        if (s.containsAll(b.territories)) {
                            apt += b.value;
                            z += p.id + " " + b.id + " ";
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    for (Territory t : territories)
                        sb.append(t.toString()).append(" ");
                    String cmd2 = sb.toString().trim(), cmd3 = getBonuses(p.id).trim(), reply[];

                    List<String> command = new ArrayList<String>();
                    for (String s : p.cmd.split(" "))
                        command.add(s);
                    command.add(String.valueOf(p.id));
                    command.add(String.valueOf(apt));
                    command.add(cmd2);
                    command.add(cmd3);
                    if (k == 0)
                        command.add("X");
                    reply = getReply(p.cmd, command);
                    if (reply == null)
                        continue;

                    int deployed = 0;

                    for (String s : reply[0].split(" ")) {
                        if (s.isEmpty()) {
                            System.out.println("Invalid command by " + p.cmd + ": Empty string");
                            continue;
                        }
                        String[] data = s.split(",");
                        int row = Integer.parseInt(data[0]), col = Integer.parseInt(data[1]), armies = Integer.parseInt(data[2]);
                        if (field.territories[row][col].player != p.id) {
                            System.out.println("Invalid command by " + p.cmd + ": Player id and territory owner differ");
                            continue;
                        }
                        deployed += armies;
                        map.put(field.territories[row][col], armies);
                    }

                    if (deployed != apt) {
                        System.out.println("Invalid command by " + p.cmd + ": No. of armies to be deployed: " + apt + "; No. of armies deployed: " + deployed);
                        continue;
                    }

                    for (String s : reply[1].split(" ")) {
                        if (!s.isEmpty() && field.territories[Integer.parseInt(s.split(",")[0])][Integer.parseInt(s.split(",")[1])].player != p.id) {
                            System.out.println("Invalid command by " + p.cmd + ": Player id and territory owner differ");
                            continue;
                        }
                        if (!s.isEmpty())
                            commands.add(p.id + "," + s);
                    }
                } catch (Exception ex) {
                    System.out.println("Command from " + p.cmd + " caused exception to be thrown: " + ex);
                }
            }

            for (Territory t : map.keySet())
            	t.armies += map.get(t);

            java.util.Collections.shuffle(commands);
            for (String s : commands) {
                try {
                    String[] data = s.split(",");
                    int id = Integer.parseInt(data[0]),
                    	srcRow = Integer.parseInt(data[1]), srcCol = Integer.parseInt(data[2]),
                        dstRow = Integer.parseInt(data[3]), dstCol = Integer.parseInt(data[4]),
                        armies = Integer.parseInt(data[5]);
                    Territory src = field.territories[srcRow][srcCol], dst = field.territories[dstRow][dstCol];
                    if (src.player != id) {
                    	System.out.println("Invalid command by " + cmdMap.get(Integer.valueOf(id)) + ": Player does not own the specified territory");
                    	continue;
                    }
                    if (armies <= 0) {
                        System.out.println("Invalid command by " + cmdMap.get(Integer.valueOf(id)) + ": Number of armies sent is not positive");
                        continue;
                    }
                    if (!src.getAdjacent().contains(dst)) {
                        System.out.println("Invalid command by " + cmdMap.get(Integer.valueOf(id)) + ": Destination territory is not adjacent to source territory");
                        continue;
                    }
                    if (src.armies - armies <= 0) {
                        System.out.println("Invalid command by " + cmdMap.get(Integer.valueOf(id)) + ": Armies to be sent is >= armies in territory by " +
                            (armies - src.armies));
                        continue;
                    }
                    src.armies -= armies;

                    if (src.player == dst.player)
                        dst.armies += armies;
                    else {
                        int atkKills = (int) Math.round(armies * .6), defKills = (int) Math.round(dst.armies * .7);
                        dst.armies -= atkKills;
                        armies -= defKills;
                        if (dst.armies <= 0) {
                            if (armies > 0) {
                                dst.armies = armies;
                                dst.player = src.player;
                            }
                            else
                                dst.armies = 1;
                        }
                        else
                            src.armies += armies < 1 ? 0 : armies;
                    }
                } catch (ArrayIndexOutOfBoundsException ex) {
                    try {
                        System.out.println("Invalid command by player " +
                            cmdMap.get(field.territories[Integer.parseInt(s.split(",")[0])][Integer.parseInt(s.split(",")[1])].player) +
                                ":  not enough information given");
                    } catch (Exception e) {
                        System.out.println("Invalid command given");
                    }
                    continue;
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid command given");
                    continue;
                }
            }
            for (Territory[] ta : field.territories) {
                for (Territory t : ta)
                    System.out.print(t + "\t");
                System.out.println();
            }
            System.out.println();

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        for (RiskPlayer p : players) {
            for (RiskPlayer c : clones) {
                if (p.id == c.id) {
                    p.score += 100 / clones.size();
                    break;
                }
            }
        }
    }

    private String[] getReply(String player, List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream();
            long start = System.currentTimeMillis();
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor();
            long duration = System.currentTimeMillis() - start;

            String[] arr = new String[2];
            arr[0] = br.readLine().trim();
            arr[1] = br.readLine().trim();
            p.destroy();
            br.close();
            if (duration > timeout) {
                System.out.println(player + " timed out");
                return null;
            }
            return arr;
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            return null;
        } catch (NullPointerException ex) {
            System.out.println("Invalid command by " + player + ": no output");
            return null;
        }
    }

    private String getBonuses(int id) {
        String s = "";
        for (Bonus b : field.bonusList) {
            int numTerritories = 10;
            for (Territory[] ta : field.territories) {
                for (Territory t : ta) {
                    if (t.player == id && b.territories.contains(t))
                        numTerritories--;
                }
            }

            s += b.id + "," + b.value + "," + numTerritories + " ";
        }

        return s;
    }

    public static void main(String[] args) {
        new Risk();
    }

    private class PlayingField {
        Territory[][] territories = new Territory[10][10];
        List<Bonus> bonusList = new ArrayList<Bonus>();

        public void init() {
            for (int k = 0; k < 100; k++)
                territories[k/10][k%10] = new Territory(k/10, k%10);
            setBonuses((int)(Math.random()*3));
        }

        private void setBonuses(int config) {
            bonusList.clear();
            switch (config) {
                case 0:
                    for (int k = 0; k < 10; k++) {
                        Set<Territory> set = new HashSet<Territory>();
                        for (int l = 0; l < 10; l++)
                            set.add(territories[k][l]);
                        bonusList.add(new Bonus(set, k));
                    }
                    break;

                case 1:
                    for (int k = 0; k < 10; k++) {
                        Set<Territory> set = new HashSet<Territory>();
                        for (int l = 0; l < 10; l++)
                            set.add(territories[5*(k%2)+l/2][2*(k/2)+l%2]);
                        bonusList.add(new Bonus(set, k));
                    }
                    break;

                case 2:
                    List<Territory> list = new ArrayList<Territory>();
                    for (Territory[] ta : field.territories) {
                        for (Territory t : ta)
                            list.add(t);
                    }
                    for (int k = 0; k < 10; k++) {
                        Set<Territory> set = new HashSet<Territory>();
                        for (int l = 0; l < 10; l++)
                            set.add(list.remove((int) (Math.random() * list.size())));
                        bonusList.add(new Bonus(set, k));
                    }
                    break;
            }
        }
    }

    private class Territory {
        int player, armies, row, col, bonusId;
        public Territory(int row, int col) {
            this.row = row;
            this.col = col;
            player = -1;
            armies = 2;
        }

        public Set<Territory> getAdjacent() {
            Set<Territory> set = new HashSet<Territory>();
            Territory[][] t = field.territories;
            set.add(t[row == 0 ? 9 : row-1][col == 0 ? 9 : col-1]);
            set.add(t[row == 0 ? 9 : row-1][col]);
            set.add(t[row == 0 ? 9 : row-1][col == 9 ? 0 : col+1]);
            set.add(t[row][col == 0 ? 9 : col-1]);
            set.add(t[row][col]);
            set.add(t[row][col == 9 ? 0 : col+1]);
            set.add(t[row == 9 ? 0 : row+1][col == 0 ? 9 : col-1]);
            set.add(t[row == 9 ? 0 : row+1][col]);
            set.add(t[row == 9 ? 0 : row+1][col == 9 ? 0 : col+1]);
            return set;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Territory))
                return false;
            Territory t = (Territory) obj;
            return row == t.row && col == t.col;
        }

        @Override
        public int hashCode() {
            return java.util.Arrays.hashCode(new Object[] {Integer.valueOf(row), Integer.valueOf(col)});
        }

        @Override
        public String toString() {
            return row + "," + col + "," + bonusId + "," + player + "," + armies;
        }
    }

    private class Bonus {
        Set<Territory> territories;
        int id, value;

        public Bonus(Set<Territory> territories, int id) {
            this.territories = territories;
            this.id = id;
            for (Territory t : this.territories)
                t.bonusId = id;
            value = (int) (Math.random() * 5) + 5;
        }
    }

    private class RiskPlayer {
        String cmd;
        int id, score;

        public RiskPlayer(String cmd, int id) {
            this.cmd = cmd;
            this.id = id;
        }

        @Override
        public String toString() {
            int l = 0;
            for (RiskPlayer p : players) {
                if (p.cmd.length() > l)
                    l = p.cmd.length();
            }

            for (int k = cmd.length(); k < l; k++)
                cmd += " ";

            return cmd + "\t" + score;
        }
    }
}
