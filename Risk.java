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

public class Risk {
    private List<RiskPlayer> players = new ArrayList<RiskPlayer>();
    private Map<Integer, String> cmdMap = new HashMap<Integer, String>();
    private String[] clazz = {"java Player"
                              /*,
                               *Add other program commands here*/};
    private PlayingField field = new PlayingField();

    private final int rounds = 20, turns = 1000, timeout = 1000;

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
            System.out.println("Round: " + round + ", Turn: " + (k+1));
            List<String> commands = new ArrayList<String>();
            for (java.util.Iterator<RiskPlayer> i = clones.iterator(); i.hasNext();) {
                RiskPlayer p = i.next();
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
                if (numTerritories == 0) {
                    System.out.println(p.cmd + " has no territories");
                    i.remove();
                    if (clones.size() == 1)
                        break o;
                    continue;
                }

                for (Bonus b : field.bonusList) {
                    Set<Territory> s = new HashSet<Territory>(territories);
                    for (java.util.Iterator<Territory> it = s.iterator(); it.hasNext();) {
                        Territory t = it.next();
                        if (t.player != p.id)
                            it.remove();
                    }
                    if (s.containsAll(b.territories))
                        apt += b.value;
                }

                StringBuilder sb = new StringBuilder();
                for (Territory t : territories)
                    sb.append(t.toString()).append(" ");
                String cmd2 = sb.toString().trim();

                String cmd3 = getBonuses(p.id).trim();

                List<String> reply = null;
                try {
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
                } catch (NullPointerException ex) {
                    continue;
                }
                if (reply == null)
                    continue;

                int deployed = 0;
                Map<Territory, Integer> map = new HashMap<Territory, Integer>();
                for (String s : reply.get(0).split(" ")) {
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

                for (Territory t : map.keySet())
                    t.armies += map.get(t);

                for (String s : reply.get(1).split(" ")) {
                    if (!s.isEmpty() && field.territories[Integer.parseInt(s.split(",")[0])][Integer.parseInt(s.split(",")[1])].player != p.id) {
                        System.out.println("Invalid command by " + p.cmd + ": Player id and territory owner differ");
                        continue;
                    }
                    if (!s.isEmpty())
                        commands.add(s);
                }
            }

            java.util.Collections.shuffle(commands);
            for (String s : commands) {
                try {
                    String[] data = s.split(",");
                    int srcRow = Integer.parseInt(data[0]), srcCol = Integer.parseInt(data[1]),
                        dstRow = Integer.parseInt(data[2]), dstCol = Integer.parseInt(data[3]),
                        armies = Integer.parseInt(data[4]);
                    Territory src = field.territories[srcRow][srcCol], dst = field.territories[dstRow][dstCol];
                    if (armies <= 0) {
                        System.out.println("Invalid command by " + cmdMap.get(Integer.valueOf(src.player)) + ": Number of armies sent is not positive");
                        continue;
                    }
                    if (!src.getAdjacent().contains(dst)) {
                        System.out.println("Invalid command by " + cmdMap.get(Integer.valueOf(src.player)) + ": Destination territory is not adjacent to source territory");
                        continue;
                    }
                    if (src.armies - armies <= 0) {
                        System.out.println("Invalid command by " + cmdMap.get(Integer.valueOf(src.player)) + ": Armies to be sent is >= armies in territory by " +
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

    private List<String> getReply(String player, List<String> command) {
        try {
            List<String> l = new ArrayList<String>();
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream();
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            long start = System.currentTimeMillis();
            p.waitFor();
            long duration = System.currentTimeMillis() - start;
            l.add(br.readLine().trim());
            l.add(br.readLine().trim());
            p.destroy();
            br.close();
            if (duration > timeout) {
                System.out.println(player + " timed out");
                return null;
            }
            return l;
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            return null;
        } catch (NullPointerException ex) {
            System.out.println("Invalid command by " + player + ": no output");
            throw ex;
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
            if (row == 0) {
                if (col == 0) {
                    set.add(t[9][9]);
                    set.add(t[0][9]);
                    set.add(t[1][9]);
                    set.add(t[9][0]);
                    set.add(t[1][0]);
                    set.add(t[9][1]);
                    set.add(t[0][1]);
                    set.add(t[1][1]);
                }
                else if (col == 9) {
                    set.add(t[9][8]);
                    set.add(t[0][8]);
                    set.add(t[1][8]);
                    set.add(t[9][9]);
                    set.add(t[1][9]);
                    set.add(t[9][0]);
                    set.add(t[0][0]);
                    set.add(t[1][0]);
                }
                else {
                    set.add(t[9][col-1]);
                    set.add(t[0][col-1]);
                    set.add(t[1][col-1]);
                    set.add(t[9][col]);
                    set.add(t[1][col]);
                    set.add(t[9][col+1]);
                    set.add(t[0][col+1]);
                    set.add(t[1][col+1]);
                }
            }
            else if (row == 9) {
                if (col == 0) {
                    set.add(t[8][9]);
                    set.add(t[9][9]);
                    set.add(t[0][9]);
                    set.add(t[8][0]);
                    set.add(t[0][0]);
                    set.add(t[8][1]);
                    set.add(t[9][1]);
                    set.add(t[0][1]);
                }
                else if (col == 9) {
                    set.add(t[8][8]);
                    set.add(t[9][8]);
                    set.add(t[0][8]);
                    set.add(t[8][9]);
                    set.add(t[0][9]);
                    set.add(t[8][0]);
                    set.add(t[9][0]);
                    set.add(t[0][0]);
                }
                else {
                    set.add(t[8][col-1]);
                    set.add(t[9][col-1]);
                    set.add(t[0][col-1]);
                    set.add(t[8][col]);
                    set.add(t[0][col]);
                    set.add(t[8][col+1]);
                    set.add(t[9][col+1]);
                    set.add(t[0][col+1]);
                }
            }
            else {
                if (col == 0) {
                    set.add(t[row-1][9]);
                    set.add(t[row][9]);
                    set.add(t[row+1][9]);
                    set.add(t[row-1][0]);
                    set.add(t[row+1][0]);
                    set.add(t[row-1][1]);
                    set.add(t[row][1]);
                    set.add(t[row+1][1]);
                }
                else if (col == 9) {
                    set.add(t[row-1][8]);
                    set.add(t[row][8]);
                    set.add(t[row+1][8]);
                    set.add(t[row-1][9]);
                    set.add(t[row+1][9]);
                    set.add(t[row-1][0]);
                    set.add(t[row][0]);
                    set.add(t[row+1][0]);
                }
                else {
                    set.add(t[row-1][col-1]);
                    set.add(t[row][col-1]);
                    set.add(t[row+1][col-1]);
                    set.add(t[row-1][col]);
                    set.add(t[row+1][col]);
                    set.add(t[row-1][col+1]);
                    set.add(t[row][col+1]);
                    set.add(t[row+1][col+1]);
                }
            }
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
