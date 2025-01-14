import org.encog.Encog;
import org.encog.engine.network.activation.*;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.io.*;
import java.util.*;
public class FeedbackHuman implements Player{
    Expectimax e;
    public static void main (String[] args)
    {
//        FeedbackHuman h = new FeedbackHuman("6a.txt", 56, 14);
        FeedbackHuman h = new FeedbackHuman();
        Game g = new Game(4,5,8,3,6,7,0,1,2);
        g.playGame(h);
    }
    public FeedbackHuman(String fileName, int i1, int i2)
    {
        e = new Expectimax(fileName, i1, i2);
    }
    public FeedbackHuman()
    {
        e = new Expectimax(true);
    }
    public String getMove(Board3x3 g)
    {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        System.out.println(g.prettyToString());
        Expectimax.evalsDone = 0;
        int depthDone = 1;
        while (Expectimax.evalsDone < 0)
        {
            System.out.println("Depth " + depthDone + ": " + e.eval(g, depthDone));
            depthDone++;
        }
        System.out.println();
        Expectimax.evalsDone = 0;
        long nodesToCheck = 50000;
        int emptySpaces = 0;
        for (int[] e : g.to3dArr())
            for (int q : e)
                if (q == 0)
                    emptySpaces++;
        nodesToCheck = (int)(nodesToCheck * Math.pow(1.6, 9 - emptySpaces));
        nodesToCheck = 100;
        System.out.println(nodesToCheck + " nodes: " + e.eval2(g, nodesToCheck, 1));
        nodesToCheck = 1000;
        System.out.println(nodesToCheck + " nodes: " + e.eval2(g, nodesToCheck, 1));
        nodesToCheck = 10000;
        System.out.println(nodesToCheck + " nodes: " + e.eval2(g, nodesToCheck, 1));
        nodesToCheck = 100000;
        System.out.println(nodesToCheck + " nodes: " + e.eval2(g, nodesToCheck, 1));
        nodesToCheck = 1000000;
        System.out.println(nodesToCheck + " nodes: " + e.eval2(g, nodesToCheck, 1));
        nodesToCheck = 10000000;
        System.out.println(nodesToCheck + " nodes: " + e.eval2(g, nodesToCheck, 1));
        nodesToCheck = 100000000;
        System.out.println(nodesToCheck + " nodes: " + e.eval2(g, nodesToCheck, 1));
        nodesToCheck = 10000000000L;
        System.out.println(nodesToCheck + " nodes: " + e.eval2(g, nodesToCheck, 1));
        System.out.println("Children looked up: " + Board3x3.usedCount);
        System.out.println("Children calculated: " + Board3x3.calculatedCount);
        Board3x3.usedCount = 0;
        Board3x3.calculatedCount = 0;
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