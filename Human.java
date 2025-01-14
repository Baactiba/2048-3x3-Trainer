import java.util.*;
public class Human implements Player
{
    public String getMove(Board3x3 g)
    {
        System.out.println(g);
        return new Scanner(System.in).next().substring(0,1).toUpperCase();
    }
    public void gameOverNotify(Board3x3 g)
    {
        System.out.println("Game over in " + g.score + " moves!");
        System.out.println(g);
    }
    public ArrayList<int[][]> getHistory()
    {
        return new ArrayList<>();
    }
}