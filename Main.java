public class Main
{
    public static void main (String[] args)
    {
//        Player p = new AI("7.txt", 4, false, false);
        Player p = new AI(10000000, false, true);
        int minScore = 3700;
        int maxScore = 0;
        int totalScore = 0;
        int trials = 1;
        for (int x = 0; x < trials; x++)
        {
            Game g = new Game(9,8,2,6,5,1,2,0,1);
            g.playGame(p);
            totalScore += g.board.score;
            int score = g.board.score;
            if (score > maxScore)
                maxScore = score;
            if (score < minScore)
                minScore = score;
            System.out.println(x);
        }
        System.out.printf("Average score: %.3f moves. Min score: %d. Max score: %d.", (double)totalScore / trials, minScore, maxScore);

    }
}