import java.util.*;

/* 3x3 2048 Game Engine
 *
 * @author Baactiba
 *
 * @version 15 December 2024
 */

public class Game
{
    static HashMap<Row3, Row3> results = init(); // maps Strings like 011 to [2, 0, 0] - Merges left.
    Board3x3 board;
    public static void main(String[] args)
    {
        int count = 0;
        while (true)
        {
            count++;
            Game g = new Game();
            if (g.playGame(new RandomPlayer(false, false)) > 90)
                break;
        }
        System.out.println("Took " + count + " games.");
    }
    public Game()
    {
        board = new Board3x3();
    }
    public Game(int... values)
    {
        board = new Board3x3(values);
    }
    public int playGame(Player p)
    {
        while (!board.isDead())
        {
            boolean moved = board.move(p.getMove(board));
            if (moved)
                board.spawn();
        }
        p.gameOverNotify(board);
        return board.score;
    }
    static public HashMap<Row3, Row3> init()
    {
        HashMap<Row3, Row3> map = new HashMap<>();
        for (int x = 0; x < 11; x++)
            for (int y = 0; y < 11; y++)
                for (int z = 0; z < 11; z++)
                {
                    Row3 adding = new Row3(x, y, z);
                    Row3 child = new Row3(adding.getArr());
                    child.merge();
                    map.put(adding, child);
                }
        return map;
    }
}
class Board3x3
{
    Row3 row1;
    Row3 row2;
    Row3 row3;
    Random tileMachine = new Random();
    int score = 0;
    public Board3x3(int... values)
    {
        row1 = new Row3(values[0], values[3], values[6]);
        row2 = new Row3(values[1], values[4], values[7]);
        row3 = new Row3(values[2], values[5], values[8]);
    }
    public Board3x3(int[][] arr)
    {
        row1 = new Row3(arr[0]);
        row2 = new Row3(arr[1]);
        row3 = new Row3(arr[2]);
    }
    public Board3x3()
    {
        row1 = new Row3(0,0,0);
        row2 = new Row3(0,1,0);
        row3 = new Row3(0,0,0);
    }
    public boolean move(String move) // "U", "L", "D", "R"
    {
        Row3 r1orig = new Row3(row1);
        Row3 r2orig = new Row3(row2);
        Row3 r3orig = new Row3(row3);
        switch (move)
        {
            case "L":
            {
                row1 = new Row3(Game.results.get(row1));
                row2 = new Row3(Game.results.get(row2));
                row3 = new Row3(Game.results.get(row3));
                break;
            }
            case "R":
            {
                row1.reverse();
                row1 = new Row3(Game.results.get(row1));
                row1.reverse();
                row2.reverse();
                row2 = new Row3(Game.results.get(row2));
                row2.reverse();
                row3.reverse();
                row3 = new Row3(Game.results.get(row3));
                row3.reverse();
                break;
            }
            case "U":
            {
                int[] r1 = row1.getArr();
                int[] r2 = row2.getArr();
                int[] r3 = row3.getArr();
                Row3 col1 = new Row3(r1[0], r2[0], r3[0]);
                Row3 col2 = new Row3(r1[1], r2[1], r3[1]);
                Row3 col3 = new Row3(r1[2], r2[2], r3[2]);
                col1 = Game.results.get(col1);
                col2 = Game.results.get(col2);
                col3 = Game.results.get(col3);
                int[] c1 = col1.getArr();
                int[] c2 = col2.getArr();
                int[] c3 = col3.getArr();
                row1 = new Row3(c1[0], c2[0], c3[0]);
                row2 = new Row3(c1[1], c2[1], c3[1]);
                row3 = new Row3(c1[2], c2[2], c3[2]);
                break;
            }
            case "D":
            {
                int[] r1 = row1.getArr();
                int[] r2 = row2.getArr();
                int[] r3 = row3.getArr();
                Row3 col1 = new Row3(r3[0], r2[0], r1[0]);
                Row3 col2 = new Row3(r3[1], r2[1], r1[1]);
                Row3 col3 = new Row3(r3[2], r2[2], r1[2]);
                col1 = Game.results.get(col1);
                col2 = Game.results.get(col2);
                col3 = Game.results.get(col3);
                int[] c1 = col1.getArr();
                int[] c2 = col2.getArr();
                int[] c3 = col3.getArr();
                row1 = new Row3(c1[2], c2[2], c3[2]);
                row2 = new Row3(c1[1], c2[1], c3[1]);
                row3 = new Row3(c1[0], c2[0], c3[0]);
                break;
            }
            default:
                return false;
        }
        if (r1orig.equals(row1) && r2orig.equals(row2) && r3orig.equals(row3))
            return false;
        score++;
        return true;
    }
    public static HashMap<Long, DblArr<int[][], Double>> children = new HashMap<>();
    static int usedCount = 0;
    static int calculatedCount = 0;
    public DblArr<int[][], Double> children(String move) // "U", "L", "D", "R"
    {
        long extra = switch (move) {
            case "L" -> 1;
            case "D" -> 2;
            case "R" -> 3;
            default -> 0;
        };
        long key = (extra << 48) + asLong();
        if (children.containsKey(key))
        {
            usedCount++;
            return children.get(key);
        }

        DblArr<int[][], Double> ret = new DblArr<>(12);
        Row3 r1orig = new Row3(row1);
        Row3 r2orig = new Row3(row2);
        Row3 r3orig = new Row3(row3);
        Row3 new1;
        Row3 new2;
        Row3 new3;
        switch (move)
        {
            case "L":
            {
                new1 = new Row3(Game.results.get(row1));
                new2 = new Row3(Game.results.get(row2));
                new3 = new Row3(Game.results.get(row3));
                break;
            }
            case "R":
            {
                row1.reverse();
                new1 = new Row3(Game.results.get(row1));
                row1.reverse();
                new1.reverse();
                row2.reverse();
                new2 = new Row3(Game.results.get(row2));
                row2.reverse();
                new2.reverse();
                row3.reverse();
                new3 = new Row3(Game.results.get(row3));
                row3.reverse();
                new3.reverse();
                break;
            }
            case "U":
            {
                int[] r1 = row1.getArr();
                int[] r2 = row2.getArr();
                int[] r3 = row3.getArr();
                Row3 col1 = new Row3(r1[0], r2[0], r3[0]);
                Row3 col2 = new Row3(r1[1], r2[1], r3[1]);
                Row3 col3 = new Row3(r1[2], r2[2], r3[2]);
                col1 = Game.results.get(col1);
                col2 = Game.results.get(col2);
                col3 = Game.results.get(col3);
                int[] c1 = col1.getArr();
                int[] c2 = col2.getArr();
                int[] c3 = col3.getArr();
                new1= new Row3(c1[0], c2[0], c3[0]);
                new2 = new Row3(c1[1], c2[1], c3[1]);
                new3 = new Row3(c1[2], c2[2], c3[2]);
                break;
            }
            case "D":
            {
                int[] r1 = row1.getArr();
                int[] r2 = row2.getArr();
                int[] r3 = row3.getArr();
                Row3 col1 = new Row3(r3[0], r2[0], r1[0]);
                Row3 col2 = new Row3(r3[1], r2[1], r1[1]);
                Row3 col3 = new Row3(r3[2], r2[2], r1[2]);
                col1 = Game.results.get(col1);
                col2 = Game.results.get(col2);
                col3 = Game.results.get(col3);
                int[] c1 = col1.getArr();
                int[] c2 = col2.getArr();
                int[] c3 = col3.getArr();
                new1 = new Row3(c1[2], c2[2], c3[2]);
                new2 = new Row3(c1[1], c2[1], c3[1]);
                new3 = new Row3(c1[0], c2[0], c3[0]);
                break;
            }
            default:
                return null;
        }
        if (r1orig.equals(new1) && r2orig.equals(new1) && r3orig.equals(new1))
            return null;
        int[][] createdBoard = new int[][]{ new1.getArr(), new2.getArr(), new3.getArr() };
        int emptySpaces = 0;
        for (int[] r : createdBoard)
            for (int i : r)
                if (i == 0)
                    emptySpaces++;
        for (int x = 0; x < 9; x++)
        {
            int[][] e = arrCopy(createdBoard);
//            System.out.println("E is " + new Board3x3(e) + "\n");
            if (e[x / 3][x % 3] == 0)
            {
                int[][] e2 = arrCopy(e);
                int[][] e4 = arrCopy(e);
                e2[x / 3][x % 3] = 1;
                e4[x / 3][x % 3] = 2;
                ret.add(e2, 0.9 / emptySpaces);
                ret.add(e4, 0.1 / emptySpaces);
            }
        }
        children.put(key, ret);
        calculatedCount++;
        return ret;
    }
    public boolean isDead()
    {
        int[][] board = {row1.getArr(), row2.getArr(), row3.getArr()};
/*        for (int[] row : board)
            for (int num : row)
                if (num == 9)
                    return true; */
        for (int[] row : board)
            for (int num : row)
                if (num == 0)
                    return false;
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 2; y++)
            {
                if (x < 2)
                {
                    if (board[x][y] == board[x][y + 1] || board[x][y] == board[x + 1][y])
                        return false;
                }
                else
                {
                    if (board[x][y] == board[x][y + 1])
                        return false;
                }
            }
        return board[0][2] != board[1][2] && board[1][2] != board[2][2];
    }
    public void spawn()
    {
        int value = 1;
        if (tileMachine.nextInt(10) == -1) // Currently not allowing 4 spawns.
            value = 2;
        int zeroCount = 0;
        for (int x = 0; x < 9; x++)
            switch (x / 3)
            {
                case 0:
                    if (row1.get(x % 3) == 0)
                        zeroCount++;
                    break;
                case 1:
                    if (row2.get(x % 3) == 0)
                        zeroCount++;
                    break;
                case 2:
                    if (row3.get(x % 3) == 0)
                        zeroCount++;
                    break;
                default:
                    break;
            }
        int pos = tileMachine.nextInt(zeroCount);
        int posAt = 0;
        wt : for (int x = 0; x < 9; x++)
            switch (x / 3)
            {
                case 0:
                    if (row1.get(x % 3) == 0) {
                        if (posAt == pos) {
                            row1.set(x % 3, value);
                            break wt;
                        }
                        posAt++;
                    }
                    break;
                case 1:
                    if (row2.get(x % 3) == 0) {
                        if (posAt == pos) {
                            row2.set(x % 3, value);
                            break wt;
                        }
                        posAt++;
                    }
                    break;
                case 2:
                    if (row3.get(x % 3) == 0) {
                        if (posAt == pos) {
                            row3.set(x % 3, value);
                            break wt;
                        }
                        posAt++;
                    }
                    break;
                default:
                    break;
            }
    }
    public int[][] to3dArr()
    {
        int[][] ret = {row1.getArr(), row2.getArr(), row3.getArr()};
        return ret;
    }
    public long asLong()
    {
        long ret = 0;
        int ind = 0;
        for (int[] e : to3dArr())
            for (long i : e)
            {
                ret += i << 4 * ind++;
            }
//        System.out.println(ret);
        return ret;
    }
    public HashSet<String> legalMoves()
    {
        HashSet<String> ret = new HashSet<>();
        int[][] arr = to3dArr();
        if ((arr[0][0] == arr[0][1] || arr[0][1] == arr[0][2]) && arr[0][1] > 0 ||
                (arr[1][0] == arr[1][1] || arr[1][1] == arr[1][2]) && arr[1][1] > 0 ||
                (arr[2][0] == arr[2][1] || arr[2][1] == arr[2][2]) && arr[2][1] > 0) {
            ret.add("L");
            ret.add("R");
        }
        if ((arr[0][0] == arr[1][0] || arr[1][0] == arr[2][0]) && arr[1][0] > 0 ||
                (arr[0][1] == arr[1][1] || arr[1][1] == arr[2][1]) && arr[1][1] > 0 ||
                (arr[0][2] == arr[1][2] || arr[1][2] == arr[2][2]) && arr[1][2] > 0) {
            ret.add("U");
            ret.add("D");
        }
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 3; y++)
                if (arr[x][y] != 0)
                {
                    try {
                        if (arr[x][y + 1] == 0)
                            ret.add("R");
                    } catch (IndexOutOfBoundsException e) {}
                    try {
                        if (arr[x][y - 1] == 0)
                            ret.add("L");
                    } catch (IndexOutOfBoundsException e) {}
                    try {
                        if (arr[x - 1][y] == 0)
                            ret.add("U");
                    } catch (IndexOutOfBoundsException e) {}
                    try {
                        if (arr[x + 1][y] == 0)
                            ret.add("D");
                    } catch (IndexOutOfBoundsException e) {}
                }
        return ret;
    }
    public String toString()
    {
        return "" + row1 + "\n" + row2 + "\n" + row3;
    }
    public String prettyToString()
    {
        int[][] arr = to3dArr();
        return "" + arr[0][0] + "   " + arr[0][1] + "   " + arr[0][2] + "\n\n"
                + arr[1][0] + "   " + arr[1][1] + "   " + arr[1][2] + "\n\n"
                + arr[2][0] + "   " + arr[2][1] + "   " + arr[2][2];
    }
    private int[][] arrCopy(int[][] toCopy)
    {
        int[][] ret = new int[3][3];
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 3; y++)
                ret[x][y] = toCopy[x][y];
        return ret;
    }
    public boolean equals(Object o)
    {
        if (o instanceof Board3x3 target)
            return (row1.equals(target.row1) && row2.equals(target.row2) && row3.equals(target.row3));
        return false;
    }
    public int hashCode()
    {
        int power = 0;
        int ret = 0;
        int[][] thing = to3dArr();
        for (int x = 0; x < 9; x++)
        {
            ret += thing[x/3][x%3] << power;
            power += 4;
        }
        return ret;
    }
}
class Row3
{
    private int[] arr = new int[3];
    public Row3(int x, int y, int z)
    {
        arr[0] = x;
        arr[1] = y;
        arr[2] = z;
    }
    public Row3(int[] arr)
    {
        this.arr[0] = arr[0];
        this.arr[1] = arr[1];
        this.arr[2] = arr[2];
    }
    public Row3(Row3 r)
    {
        this(r.getArr());
    }
    public int get(int index)
    {
        return arr[index];
    }
    public void set(int index, int value)
    {
        arr[index] = value;
    }
    public int[] getArr()
    {
        return arr;
    }
    public void merge()
    {
        for (int x = 0; x < 3; x++)
            if (arr[x % 2] == 0 && arr[(x % 2) + 1] != 0)
            {
                arr[x % 2] = arr[(x % 2) + 1];
                arr[(x % 2) + 1] = 0;
            }
        if (arr[0] == arr[1] && arr[0] > 0)
        {
            arr[0]++;
            arr[1] = arr[2];
            arr[2] = 0;
        }
        else if (arr[1] == arr[2] && arr[1] > 0)
        {
            arr[1]++;
            arr[2] = 0;
        }
    }
    public void reverse()
    {
        int temp = arr[0];
        arr[0] = arr[2];
        arr[2] = temp;
    }
    public boolean equals(Object o)
    {
        if (o instanceof Row3 r)
            return r.get(0) == arr[0] && r.get(1) == arr[1] && r.get(2) == arr[2];
        return false;
    }
    public int hashCode()
    {
        return arr[0] + arr[1] << 4 + arr[2] << 8;
    }
    public String toString()
    {
        return "" + arr[0] + " " + arr[1] + " " + arr[2];
    }
}
class DblArr<K, V>
{
    ArrayList<K> arrOne;
    ArrayList<V> arrTwo;
    public DblArr(int size)
    {
        arrOne = new ArrayList<>(size);
        arrTwo = new ArrayList<>(size);
    }
    public void addKey(K k)
    {
        arrOne.add(k);
    }
    public void addValue(V v)
    {
        arrTwo.add(v);
    }
    public void add(K k, V v)
    {
        arrOne.add(k);
        arrTwo.add(v);
    }
    public ArrayList<K> getKeys()
    {
        return arrOne;
    }
    public ArrayList<V> getValues()
    {
        return arrTwo;
    }
}