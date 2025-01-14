import java.util.*;
public interface Player
{
    String getMove(Board3x3 g);
    void gameOverNotify(Board3x3 g);
    ArrayList<int[][]> getHistory();
}