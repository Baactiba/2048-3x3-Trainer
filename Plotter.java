import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import java.util.*;
import javax.swing.*;
import java.awt.*;

public class Plotter {

    public static void main(String[] args)
    {
        e = new Expectimax(true);
        plot(30000, 2,3,5,1,2,7,1,2,8);
//        StaticEvaluator.toggleSimple();
//        StaticEvaluator.setModel("d4.5");
//        StaticEvaluator.setSettings(100000000000000L, 0, 0, 0);
//        System.out.println(StaticEvaluator.getMove(new Board3x3(2,3,5,1,2,7,1,2,8).to3dArr()));
    }
    static Expectimax e;
    public static void timePlot(int milliseconds, int... board)
    {
        double globalMin = 1.0;
        double globalMax = 0.0;
        long startDepth = 10;
        XYSeries tSeries = new XYSeries("R");

        long millis = System.currentTimeMillis();
        while (System.currentTimeMillis() - millis < milliseconds)
        {
            millis = System.currentTimeMillis();
            HashMap<String, Double> moves = e.eval2(new Board3x3(board), startDepth, 1);
            System.out.println("Depth: " + moves.get("DEPTH"));
            if (moves.get("DEPTH") == null)
                break;
            moves.remove("DEPTH");
            System.out.println(moves);
            ArrayList<Double> values = new ArrayList<>(moves.values());
            Collections.sort(values);
            if (values.get(values.size()-1) > globalMax)
                globalMax = values.get(values.size()-1);
            if(values.get(values.size()-2) < globalMin)
                globalMin = values.get(values.size()-2);
            startDepth = (long)(Math.ceil(1.2 * startDepth));
            tSeries.add(startDepth, System.currentTimeMillis() - millis);
        }
        System.out.println(globalMin);
        System.out.println(globalMax);

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(tSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Time as a Function of Nodes",
                "Nodes (log scale)",
                "Compute time",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        LogAxis xAxis = new LogAxis ("Nodes (log scale)");
        xAxis.setBase(10);
        xAxis.setTickUnit(new NumberTickUnit(1));
        plot.setDomainAxis(xAxis);

        NumberAxis yAxis = new NumberAxis("Compute time");
        yAxis.setRange(0, milliseconds);
        yAxis.setTickUnit(new NumberTickUnit(milliseconds / 4));
        plot.setRangeAxis(yAxis);
        Font axisLabelFont = new Font("SansSerif", Font.BOLD, 14);
        xAxis.setLabelFont(axisLabelFont);
        yAxis.setLabelFont(axisLabelFont);
        Font tickLabelFont = new Font("SansSerif", Font.PLAIN, 12);
        xAxis.setTickLabelFont(tickLabelFont);
        yAxis.setTickLabelFont(tickLabelFont);
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }
    public static void plot(int milliseconds, int... board)
    {
        double globalMin = 1.0;
        double globalMax = 0.0;
        long startDepth = 10;
        XYSeries rSeries = new XYSeries("R");
        XYSeries uSeries = new XYSeries("U");
        XYSeries lSeries = new XYSeries("L"); // Corrected series name
        XYSeries dSeries = new XYSeries("D"); // Corrected series name

        long millis = System.currentTimeMillis();
        while (System.currentTimeMillis() - millis < milliseconds)
        {
            millis = System.currentTimeMillis();
            HashMap<String, Double> moves = e.eval2(new Board3x3(board), startDepth, 1);
            System.out.println("Depth: " + moves.get("DEPTH"));
            if (moves.get("DEPTH") == null)
                break;
            moves.remove("DEPTH");
            System.out.println(moves);
            if (moves.get("L") != null)
                lSeries.add(startDepth, moves.get("L"));
            if (moves.get("U") != null)
                uSeries.add(startDepth, moves.get("U"));
            if (moves.get("R") != null)
                rSeries.add(startDepth, moves.get("R"));
            if (moves.get("D") != null)
                dSeries.add(startDepth, moves.get("D"));
            ArrayList<Double> values = new ArrayList<>(moves.values());
            Collections.sort(values);
            if (values.get(values.size()-1) > globalMax)
                globalMax = values.get(values.size()-1);
            if(values.get(values.size()-2) < globalMin)
                globalMin = values.get(values.size()-2);
            startDepth = (long)(Math.ceil(1.1 * startDepth));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(rSeries);
        dataset.addSeries(uSeries);
        dataset.addSeries(lSeries);
        dataset.addSeries(dSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Moves as a Function of Nodes",
                "Nodes (log scale)",
                "Values",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        LogAxis xAxis = new LogAxis("Nodes (log scale)");
        xAxis.setBase(10);
        xAxis.setTickUnit(new NumberTickUnit(1));
        plot.setDomainAxis(xAxis);

        NumberAxis yAxis = new NumberAxis("Values");
        yAxis.setRange(globalMin - (globalMax - globalMin)/3, globalMax + (globalMax - globalMin)/3);
        yAxis.setTickUnit(new NumberTickUnit(((globalMax-globalMin) * 1.5) / 6));
        plot.setRangeAxis(yAxis);

        // Increase font size for axis labels
        Font axisLabelFont = new Font("SansSerif", Font.BOLD, 14);
        xAxis.setLabelFont(axisLabelFont);
        yAxis.setLabelFont(axisLabelFont);

        // Increase font size for tick labels
        Font tickLabelFont = new Font("SansSerif", Font.PLAIN, 12);
        xAxis.setTickLabelFont(tickLabelFont);
        yAxis.setTickLabelFont(tickLabelFont);

        // Display chart in a Swing application
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }
}