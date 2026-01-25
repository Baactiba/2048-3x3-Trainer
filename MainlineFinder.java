import java.io.FileNotFoundException;
import java.util.*;
public class MainlineFinder
{

    public static void main (String[] args) throws FileNotFoundException {

        Input.settings();

        Board3x3 test = new Board3x3(9,8,2,7,6,2,1,0,2);
//        System.out.println(Input.getMove2(test));
//        System.out.println(getMainline(test, 0.5, 1, 2));
        System.out.println(getHosiMainline(test, 1));
        System.out.println(getHosiMainline(test, 2));
        System.out.println(getHosiMainline(test, 1,2));
    }

    final static double EPSILON = 0.00000000000001;

    static public String[] getMainline(Object o) {
        Board3x3.stored.clear();
        return new String[] {getMainline(o, 0.5, 1, 2), getHosiMainline(o, 1)};
    }

    static public String getMainline(Object o, double threshold, int... spawningTiles) {
        Board3x3 space = o instanceof Board3x3 s ? s : new Board3x3(((Space)o).space);
        if (Input.getMove2(space).getBestScore() == 0)
            return "";
        ArrayList<ArrayList<Info>> boardChances = new ArrayList<>();
        ArrayList<Info> layer1 = new ArrayList<>();
        layer1.add(new Info(space));
        boardChances.add(layer1);
        threshold *= Input.getMove2(space).getBestScore() * (1 - EPSILON);
        for (int x = 0; x < 25; x++) {
            ArrayList<Info> nextLayer = new ArrayList<>();
            for (Info i : boardChances.getLast()) {
                ArrayList<String> letters = new ArrayList<>(Set.of("L", "D", "R", "U"));
                for (char c : i.name.toCharArray()) {
                    String mini = "" + c;
                    letters.remove(mini);
                    letters.addFirst(mini);
                }
                for (String s : letters) {
                    Info i2 = i.add(s, spawningTiles);
                    if (i2.getSr() >= threshold)
                        if (nextLayer.size() < 100)
                            nextLayer.add(i2);
                }
            }
            if (nextLayer.isEmpty())
                break;
            boardChances.add(nextLayer);
        }
        Info target = boardChances.getLast().getFirst();
        if (spawningTiles.length == 2)
            return "Main line (B): " + target.name + String.format(" (%.2f%%)", 50 * target.getSr() / threshold);
        else if (spawningTiles[1] > spawningTiles[0])
            return "Main line (H): " + target.name;
        return target.name;

    }
    static public String getHosiMainline(Object o, int... vals) {
        Board3x3 space = o instanceof Board3x3 s ? s : new Board3x3(((Space)o).space);
        if (Input.getMove2(space).getBestScore() == 0)
            return "";
        ArrayList<ArrayList<HInfo>> boardChances = new ArrayList<>();
        ArrayList<HInfo> layer1 = new ArrayList<>();
        layer1.add(new HInfo(space));
        boardChances.add(layer1);
        for (int x = 0; x < 24; x++) {
            ArrayList<HInfo> nextLayer = new ArrayList<>();
            for (HInfo i : boardChances.getLast()) {
                ArrayList<String> letters = new ArrayList<>(Set.of("L", "D", "R", "U"));
                for (char c : i.name.toCharArray()) {
                    String mini = "" + c;
                    letters.remove(mini);
                    letters.addFirst(mini);
                }
                for (String s : letters) {
                    HInfo i2 = i.add(s, vals);
                    if (i2 != null)
                        if (nextLayer.size() < 100)
                            nextLayer.add(i2);
                }
            }
            if (nextLayer.isEmpty())
                break;
            System.out.println(nextLayer.size() + " " + Board3x3.stored.size());
            boardChances.add(nextLayer);
        }
        if (vals.length == 1)
            return "Main line (H): " + boardChances.getLast().getFirst().name;
        else if (vals[1] > vals[0])
            return "Always Played Line: " + boardChances.getLast().getFirst().name;
        return boardChances.getLast().getFirst().name;
    }
}
class Info
{
    String name;
    HashMap<Board3x3, Double> contents;
    public Info(Board3x3 space) {
        name = "";
        contents = new HashMap<>();
        contents.put(space, 1.0);
    }
    public Info(String name) {
        this.name = name;
    }
    public Info add(String dir, int... vals) {
        Info i2 = new Info(name + dir);
        HashMap<Board3x3, Double> childChances = new HashMap<>();
        for (Board3x3 b : contents.keySet()) {
            Move m;
            if (Board3x3.stored.containsKey(b))
                m = Board3x3.stored.get(b);
            else {
                m = Input.getMove2(b);
                Board3x3.stored.put(b, m);
            }
            int iDir = switch (dir) {
                case "L" -> 0;
                case "U" -> 1;
                case "R" -> 2;
                default -> 3;
            };
            if (m.getScore(iDir) >= (m.getBestScore() * (1.0 - MainlineFinder.EPSILON)) && (m.getTrashScore() <= 1.0 - MainlineFinder.EPSILON || name.isEmpty())) {
                double parChc = contents.get(b);
                HashMap<Board3x3, Double> children = b.moveSpawns(iDir, vals);
                if (children != null)
                    for (Board3x3 c : children.keySet()) {
                        if (!childChances.containsKey(c))
                            childChances.put(c, 0.0);
                        childChances.put(c, childChances.get(c) + parChc * children.get(c));
                    }
            }
        }
        i2.contents = childChances;
        return i2;
    }
    public double getSr() {
        double ret = 0;
        for (Board3x3 b : contents.keySet())
            ret += contents.get(b) * Input.getMove2(b).getBestScore();
        return ret;
    }
    public String toString() {
        String ret = name + "\n";
        for (Board3x3 b : contents.keySet())
            ret += b.toString();
        return ret;
    }
}
class HInfo
{
    String name;
    HashSet<Board3x3> contents;
    public HInfo(Board3x3 space) {
        name = "";
        contents = new HashSet<>();
        contents.add(space);
    }
    public HInfo(String name) {
        this.name = name;
    }
    public HInfo add(String dir, int... vals) {
        HInfo i2 = new HInfo(name + dir);
        HashSet<Board3x3> childChances = new HashSet<>();
        for (Board3x3 b : contents) {
            Move m;
            if (Board3x3.stored.containsKey(b))
                m = Board3x3.stored.get(b);
            else {
                m = Input.getMove2(b);
                Board3x3.stored.put(b, m);
            }
            int iDir = switch (dir) {
                case "L" -> 0;
                case "U" -> 1;
                case "R" -> 2;
                default -> 3;
            };
            if (m.getBestScore() == 0)
                return null;
            if (m.getScore(iDir) >= (m.getBestScore() * (1.0 - MainlineFinder.EPSILON)) && (m.getTrashScore() <= 1.0 - MainlineFinder.EPSILON || name.isEmpty()))
                childChances.addAll(new HashSet<>(b.moveSpawns(iDir, vals).keySet()));
            else
                return null;
        }
        i2.contents = childChances;
        return i2;
    }
}