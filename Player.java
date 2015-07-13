import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Player {
    public static void main(String[] args) {
        Random r = new Random();
        List<Territory> l = new ArrayList<Territory>();
        for (String s : args[2].split(" ")) {
            Territory t = new Territory(s.split(","));
            if (t.id == Integer.parseInt(args[0]))
                l.add(t);
        }

        Territory t1 = l.get(r.nextInt(l.size()));
        System.out.println(t1.row + "," + t1.col + "," + args[1]);

        java.util.Collections.shuffle(l);
        for (Territory t : l) {
            if (t.armies > 1) {
                int prow = 0, pcol = 0, trow = 0, tcol = 0;
                while (trow == 0 && tcol == 0) {
                    trow = r.nextInt(3) - 1;
                    tcol = r.nextInt(3) - 1;
                    prow = t.row + trow;
                    pcol = t.col + tcol;
                    prow = prow == -1 ? 9 : prow == 10 ? 0 : prow;
                    pcol = pcol == -1 ? 9 : pcol == 10 ? 0 : pcol;
                }
                System.out.println(t.row + "," + t.col + "," + prow + "," + pcol + "," + (t.armies - 1));
                break;
            }
        }
    }

    static class Territory {
        int id, row, col, armies;

        public Territory(String[] data) {
            id = Integer.parseInt(data[3]);
            row = Integer.parseInt(data[0]);
            col = Integer.parseInt(data[1]);
            armies = Integer.parseInt(data[4]);
        }
    }
}
