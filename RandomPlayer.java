import java.util.*;
public class RandomPlayer implements Player
{
    boolean printing;
    boolean logging;
    Random chooser = new Random();
    public RandomPlayer(boolean printing, boolean logging)
    {
        this.printing = printing;
        this.logging = logging;
    }
    public String getMove(Board3x3 g)
    {
        history.add(g.to3dArr());
        HashSet moves = g.legalMoves();
        Object[] arr = moves.toArray();
        if (printing)
            System.out.println(g + "\n");
        return (String)arr[chooser.nextInt(arr.length)];
    }
    public void gameOverNotify(Board3x3 g)
    {
//        System.out.println("Random player lost in " + g.score + " moves.");
//        System.out.println(g);
    }
    ArrayList<int[][]> history = new ArrayList<>();
    public ArrayList<int[][]> getHistory()
    {
        return history;
    }
}