import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//	   this program is made by Baactiba and may not be republished without permission or credit.

public class Trainer
{
	static public void main (String args[]) throws IOException, URISyntaxException {
		for (int x = 0; x < ReplayAnalyzer.EXTENDED.length; x++) {
//			System.out.println("" + (x + 128) + " " + ReplayAnalyzer.EXTENDED[x]);
			if (ReplayAnalyzer.EXTENDED[x] == '¢') {ReplayAnalyzer.EXTENDED[x] = 'ø';}
		}
		new Board();
	}
}
class Window extends Canvas
{
	int width;
	int height;
	String name;
	static JFrame frame;
	private static final long serialVersionUID = 1873648274892L;
	public Window (int width, int height, String name, Board board, Color color)
	{
		frame = new JFrame(name);
		height = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight()/1.7);
		System.out.println(this.height);
		width = height;
		frame.setPreferredSize(new Dimension(width+24, height+44));
		frame.setMinimumSize(new Dimension(width+24, height+44));
		frame.setMaximumSize(new Dimension(width+24, height+44));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.getContentPane().setBackground(color);
		frame.setLocation(width, ((int)Toolkit.getDefaultToolkit().getScreenSize().getHeight()-width)/2);
		frame.add(board);
		frame.setVisible(true);
		board.start();
	}
}
class Board extends Canvas implements Runnable
{
	boolean input = true;
	Handler handler;
	boolean alive = false;
	Thread thread;
	final int w = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/3,
			h = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/3;
	Color color = new Color(121, 64, 25);
	public Board() throws FileNotFoundException {
		String sBoard = "";
		boolean empty = false;

		try
		{
			File jarDir = new File(Trainer.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
			File settingsFile = new File(jarDir, "startpos.txt");
			if (!settingsFile.exists())
				settingsFile = new File("startpos.txt");
			sBoard = new Scanner(settingsFile).nextLine();
		} catch (Exception e)
		{
			empty = true;
		}
		int[] spaces = new int[9];
		if (!empty)
		{
			if (sBoard.contains("replay_3x3") || sBoard.contains("replay:3x3"))
			{
				Obj.fromReplay = true;
				Obj.replay = sBoard.substring(sBoard.indexOf("3x3") + 3);
				int charOne = Obj.replay.charAt(0) - 32;
				int charTwo = Obj.replay.charAt(1) - 32;
				spaces = new int[] {0,0,0,0,0,0,0,0,0};
				int charOnePos = charOne % 16;
				int charTwoPos = charTwo % 16;
				int c1down = charOnePos % 4;
				int c1right = charOnePos / 4;
				int c2down = charTwoPos % 4;
				int c2right = charTwoPos / 4;
				spaces[c1right + 3 * (2 - c1down)] = 1 + charOne / 16;
				spaces[c2right + 3 * (2 - c2down)] = 1 + charTwo / 16;
				Obj.replay = Obj.replay.substring(2);
				System.out.println("Spawned initial tiles!");
				ReplayAnalyzer.advance();
			}
			else {
				Obj.fromReplay = false;
				for (int x = 0; x < 9; x++)
					spaces[x] = sBoard.charAt(x) - 48;
			}
		}
		else
		{
			spaces = new int[] {0,0,0,0,0,0,0,0,0};
			spaces = Space.spawn(spaces);
			spaces = Space.spawn(spaces);
		}

		Input.settings();

		handler = new Handler();
		new Window(w, h, "2048 3x3 Trainer", this, color);
		handler.addObj(new Tile(spaces, ID.Tile));
		handler.addObj(new Border(ID.Border, 0));
		handler.addObj(new Menu(ID.Menu, 2));
		this.addKeyListener(new Input(handler));
	}
	public synchronized void start()
	{
		thread = new Thread(this);
		thread.start();
		alive = true;
	}
	public synchronized void stop()
	{
		try
		{
			alive = false;
			thread.join();
		}
		catch (Exception e)
		{
		}
	}
	public void run()
	{
		long lastTime = System.nanoTime();
		double ticks = 60.0;
		double ns = 1000000000/ticks;
		double delta = 0;
		long timer = System.currentTimeMillis();
		while (alive)
		{
			long time = System.nanoTime();
			delta+=(time-lastTime)/ns;
			lastTime = time;
			while (delta > 0)
			{
				tick();
				delta--;
			}
			if (alive)
			{
				render();
			}
			if (System.currentTimeMillis()-timer > 1000)
			{
				timer+=1000;
				input = false;
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {}
		}
		stop();
	}
	void tick()
	{
		handler.tick();
	}
	void render()
	{
		BufferStrategy GameBs = this.getBufferStrategy();
		if (GameBs == null)
		{
			this.createBufferStrategy(3);
			return;
		}
		Graphics g = GameBs.getDrawGraphics();
		handler.render(g);
		GameBs.show();
	}
}
class Handler
{
	static LinkedList<Obj> objects = new LinkedList<>();
	public void tick()
	{
		if (objects.size()==3)
		{
			for (Obj object:objects)
			{
				object.tick();
			}
		}
	}
	public void render(Graphics g)
	{
		if (objects.size()==3)
			for (Obj object:objects)
			{
				object.render(g);
			}
	}
	public void unRender(Graphics g)
	{
		for (Obj object:objects)
		{
			object.unRender(g);
		}
	}
	public void addObj(Obj object)
	{
		objects.add(object);
	}
	public void removeObj(Obj object)
	{
		objects.remove(object);
	}
	public static void clear()
	{
		objects.clear();
	}
}
abstract class Obj
{
	abstract void tick();
	abstract void render(Graphics g);
	abstract void unRender(Graphics g);
	static int[] barColor = new int[3];
	static int[] space = new int[9];
	static final ArrayList<Mistake> mistakes = new ArrayList<>();
	static ArrayList<Mistake> analysis = new ArrayList<>();
	static ArrayList<Move> analysis2 = new ArrayList<>();
	static ArrayList<String[]> mainlines = new ArrayList<>();
	static ArrayList<String> APLs = new ArrayList<>();
	static PrintWriter mistakeOut = null;
	ID id;
	static int loads = 0;
	static boolean disp = true;
	static boolean deadScreen = false;
	static boolean reviewing = false;
	static int menuType = 0;
	static int reviewPage = 0;
	static double borderColor = 1.0;
	public Obj(int[] x2, ID id) // Tile constructor
	{
		space = x2;
		this.id=id;
	}
	public Obj(ID id, double borderColor) // Border constructor
	{
		this.id=id;;
	}
	public static void createPrintWriter()
	{
		Scanner fileIn = null;
		try
		{
			Thread.sleep(10);
			fileIn = new Scanner(new File("mistakes.txt"));
		} catch (Exception e) {e.printStackTrace();}
		mistakes.clear();
		while (fileIn.hasNextLine())
			Obj.mistakes.add(new Mistake(fileIn.nextLine().trim()));
		fileIn.close();
		try
		{
			mistakeOut = new PrintWriter("mistakes.txt");
		}
		catch (FileNotFoundException e) {}
		for (Mistake m:mistakes)
			mistakeOut.println(m.toString().trim());
		mistakes.clear();
		mistakeOut.flush();
	}
	public Obj(ID id, int menuType)
	{
		this.id = id;
	}
	static void setDisp()
	{
		System.out.println("Changed Display");
		disp = !disp;
	}
	static void setReviewing()
	{
		reviewing = !reviewing;
		System.out.println("Changed Review Status to " + reviewing + ".");
		if (reviewing)
		{
			analysis = loadMistakes();
			for (int x = 0; x < analysis.size()-1; x++) // Sort the mistakes by how bad they are
			{
				Mistake m = analysis.get(x);
				Mistake mNext = analysis.get(x+1);
				if (mNext.getScore()>m.getScore())
				{
					Mistake temp = m;
					analysis.set(x, mNext);
					analysis.set(x+1, temp);
					x-=3;
					if (x < 0)
						x=-1;
				}
			}
			if (analysis.size()>Input.replaySize) // only show 10 mistakes at a time
			{
				Collections.reverse(analysis);
				analysis.subList(Input.replaySize, analysis.size()).clear();
				Collections.reverse(analysis);
			}
			try {
				for (Mistake m : analysis) {
					mainlines.add(MainlineFinder.getMainline(new Space(Obj.interpret(m.pos))));
					APLs.add(MainlineFinder.getHosiMainline(new Board3x3(Obj.interpret(m.pos)), 2, 1));
				}
			} catch (ConcurrentModificationException e) {
				System.out.println("Concurrent modification or IOException 308");
			}
			analysis2.clear();
			for (Mistake m:analysis)
            {
                analysis2.add(Input.getMove2(m.getPos()));
            }
			setMenuType(4);
		}
		if (!reviewing)
		{
			analysis.clear();
			mainlines.clear();
			Obj.setMenuType(3);
		}
	}
	static void setDeadScreen(boolean b)
	{
		System.out.println("Death Screen");
		deadScreen = b;
		if (b)
		{
			mistakeOut.close();
			setMenuType(3);
		}
		setDisp();
	}
	static private ArrayList<Mistake> loadMistakes()
	{
		mistakeOut.close();
		ArrayList<Mistake>	ret = new ArrayList<>();
		Scanner fileIn = null;
		try
		{
			fileIn = new Scanner(new File("mistakes.txt"));
		}
		catch (Exception e)
		{
			System.out.println("Mistakes file does not exist!");
		}
		if (fileIn != null)
		{
			while (fileIn.hasNextLine())
				ret.add(new Mistake(fileIn.nextLine()));
		}
		return ret;
	}
	static boolean solidColor = false;
	static boolean fromReplay = false;
	static String replay;
	public static void setX(int x, int val)
	{
		space[x] = val;
	}
	public static void setX(int[] x)
	{
		space = x;
	}
	public static void setBorderColor(double x)
	{
		borderColor = x;
		if (solidColor)
			borderColor = 2.0;
	}
	public static void setMenuType(int menuTypee)
	{
		menuType = menuTypee;
	}
	public static void setReviewPage(String a, int size)
	{
		if (a.equals("plus"))
			reviewPage++;
		else
			reviewPage--;
		if (reviewPage > size-1)
			reviewPage--;
		if (reviewPage < 0)
			reviewPage++;
	}
	public static int[] getX()
	{
		return space;
	}
	public static boolean getDisp()
	{
		return disp;
	}
	public static boolean getDeadScreen()
	{
		return deadScreen;
	}
	public static boolean getReviewing()
	{
		return reviewing;
	}
	public ID getId()
	{
		return id;
	}
	public void setColor(int bColor)
	{
		borderColor = bColor;
	}
	public void setId(ID id)
	{
		this.id = id;
	}
	public static void setLoads(int i)
	{
		loads = i;
	}
	public static void addMistake(Mistake m)
	{
		mistakes.add(m);
		if (!m.getType().equals("Optimal")) {
			mistakeOut.println(m);
		}
		mistakeOut.flush();
	}
	public static void resetMistakes()
	{
		mistakes.clear();
	}
	static Image getImg(int val, int size)
	{
		Image img = null;
		try
		{
			img = ImageIO.read(new File("Tiles/"+(int)(Math.pow(2,val))+".png"));
		} catch (Exception e) {
			System.out.println("Tiles/"+(int)(Math.pow(2, val))+".png"); e.printStackTrace();
			System.exit(0);
		}
		return img;
	}
	public static int[] startString() throws FileNotFoundException
	{
		String sBoard = "";
		boolean empty = false;
		try
		{
			File jarDir = new File(Trainer.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
			File settingsFile = new File(jarDir, "startpos.txt");
			if (!settingsFile.exists())
				settingsFile = new File("startpos.txt");
			sBoard = new Scanner(settingsFile).nextLine();
		} catch (Exception e)
		{
			empty = true;
		}
		int[] spaces = new int[9];
		if (!empty)
		{
			if (sBoard.contains("replay_3x3") || sBoard.contains("replay:3x3"))
			{
				Obj.fromReplay = true;
				Obj.replay = sBoard.substring(sBoard.indexOf("3x3") + 3);
				int charOne = Obj.replay.charAt(0) - 32;
				int charTwo = Obj.replay.charAt(1) - 32;
				spaces = new int[] {0,0,0,0,0,0,0,0,0};
				int charOnePos = charOne % 16;
				int charTwoPos = charTwo % 16;
				int c1down = charOnePos % 4;
				int c1right = charOnePos / 4;
				int c2down = charTwoPos % 4;
				int c2right = charTwoPos / 4;
				spaces[c1right + 3 * (2 - c1down)] = 1 + charOne / 16;
				spaces[c2right + 3 * (2 - c2down)] = 1 + charTwo / 16;
				Obj.replay = Obj.replay.substring(2);
				ReplayAnalyzer.index = 0;
				System.out.println("Spawned initial tiles!");
				ReplayAnalyzer.advance();
			}
			else {
				Obj.fromReplay = false;
				for (int x = 0; x < 9; x++)
					spaces[x] = sBoard.charAt(x) - 48;
			}
		}
		else
		{
			spaces = new int[] {0,0,0,0,0,0,0,0,0};
			spaces = Space.spawn(spaces);
			spaces = Space.spawn(spaces);
		}

		Input.settings();

		return spaces;
	}
	public static int[] interpret(String start)
	{
		int[] ret = new int[start.length()];
		for (int x = 0; x < start.length(); x++)
			ret[x]=start.charAt(x)-48;
		return ret;
	}
}
class Menu extends Obj
{
	public static int place = 0;
	public Menu(ID id, int menuType)
	{
		super(id, menuType);
	}
	void tick()
	{

	}
	void render(Graphics g)
	{
		if (!disp)
		{
			int dimension = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight()/1.7);
			g.setColor(Color.DARK_GRAY);
			g.fillRect(0, 0, 1000, 1000);
			g.setFont(new Font("Monospaced", Font.BOLD, 26));
			g.setColor(Color.RED);
			if (menuType != 3 && menuType != 4)
				g.drawString("Ba"+Tile.getString()+"er", (dimension/2)-205, 60);
			if (menuType == 2)
			{
				barColor = barColor(barColor);
				g.setColor(Color.BLACK);
				double fullness = (double)loads/160340;
				g.fillRect(10, (int)(dimension/2.5), dimension-12, dimension/5);
				g.setColor(new Color(barColor[0], barColor[1], barColor[2]));
				g.fillRect(10, (int)(dimension/2.5), (int)(fullness*(dimension-12)), dimension/5);
			}
			else if (menuType == 3)
			{
				int curStreak = 0;
				int bestStreak = 0;
				g.setColor(Color.pink);
				g.setFont(new Font("Monospaced", Font.BOLD, 60));
				g.drawString("Game Over!", (dimension/2)-167, 51);
				int o = 0;
				int i = 0;
				int m = 0;
				int b = 0;
				double tScore = 0.0;
				double mScore = 1.0;
				try
				{
					Thread.sleep(10);
				} catch (InterruptedException e) {}
				int divisor = 0;
				synchronized(mistakes) {
					for (Mistake mistake : mistakes)
					{
						String type = mistake.getType();
						if (type.equals("Optimal")) {
							o++;
							curStreak++;
							if (curStreak > bestStreak)
								bestStreak = curStreak;
						}
						if (type.equals("Mistake"))
							m++;
						if (type.equals("Inaccuracy"))
							i++;
						if (type.equals("Blunder")) {
							b++;
						}
						if (!type.equals("Optimal"))
							curStreak = 0;
						if (mistake.getScore() > 0) {
							tScore += mistake.getScore();
							mScore *= mistake.getScore();
							divisor++;
						}
					}
				}
				tScore/=divisor;
				double perf = (double) o / (o + i + m + b);

				g.setFont(new Font("Monospaced", Font.BOLD, 20));
				g.setColor(Color.green);
				g.drawString(String.format("Perfect:%9s", o), 5, 360);
				g.setColor(Color.yellow);
				g.drawString(String.format("Amazing:%9s", i), 5, 390);
				g.setColor(Color.orange);
				g.drawString(String.format("Mistake:%9s", m), 5, 420);
				g.setColor(Color.red);
				g.drawString(String.format("Blunder:%9s", b), 5, 450);
				if (("" + bestStreak).length() == 2)
					g.setFont(new Font("Monospaced", Font.BOLD, 22));
				else
					g.setFont(new Font("Monospaced", Font.BOLD, 21));


				g.setColor(Color.RED);
				if (tScore >= 0.999250)
					g.setColor(Color.ORANGE);
				if (tScore >= 0.999850)
					g.setColor(Color.YELLOW);
				if (tScore >= 0.999960)
					g.setColor(Color.GREEN);
				if (tScore >= 0.999990)
					g.setColor(Color.CYAN);
				if (tScore >= 0.999995)
					g.setColor(new Color(135, 19, 242));
				String output1 = String.format("Avg Accuracy: %.5f%%.", tScore * 100);
				g.setFont(new Font("Monospaced", Font.BOLD, 35));
				g.drawString(output1, 14, 160);


				g.setColor(Color.RED);
				if (mScore >= 0.8000)
					g.setColor(Color.ORANGE);
				if (mScore >= 0.9500)
					g.setColor(Color.YELLOW);
				if (mScore >= 0.9875)
					g.setColor(Color.GREEN);
				if (mScore >= 0.9957)
					g.setColor(Color.CYAN);
				if (mScore >= 0.9983)
					g.setColor(new Color(135, 19, 242));
				String output2 = String.format("Combined Accuracy: %.5f%%.", mScore * 100);
				g.setFont(new Font("Monospaced", Font.BOLD, 29));
				g.drawString(output2, 7, 194);


				g.setColor(Color.RED);
				if (bestStreak >= 40)
					g.setColor(Color.ORANGE);
				if (bestStreak >= 90)
					g.setColor(Color.YELLOW);
				if (bestStreak >= 140)
					g.setColor(Color.GREEN);
				if (bestStreak >= 185)
					g.setColor(Color.CYAN);
				if (bestStreak >= 204)
					g.setColor(new Color(135, 19, 242));
				g.setFont(new Font("Monospaced", Font.BOLD, 20));
				g.drawString(String.format("Best streak:%5s",bestStreak), 5, 263);


				g.setColor(Color.RED);
				if (perf >= 0.7500)
					g.setColor(Color.ORANGE);
				if (perf >= 0.8300)
					g.setColor(Color.YELLOW);
				if (perf >= 0.8600)
					g.setColor(Color.GREEN);
				if (perf >= 0.8800)
					g.setColor(Color.CYAN);
				if (perf >= 0.9000)
					g.setColor(new Color(135, 19, 242));
				String output3 = String.format("Perfect Percent: %.2f%%.", perf * 100);
				g.setFont(new Font("Monospaced", Font.BOLD, 15));
				g.drawString(output3, 3, 291);


				int[] rSpace = new int[9];
                System.arraycopy(space, 0, rSpace, 0, space.length);
				int sum = 0;
				for (int x = 0; x < rSpace.length; x++)
				{
					int val = rSpace[x];
					if (val != 0)
						sum += (int)Math.pow(2, val);
					int d = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 16;
					Image img = null;
					if (val != 0)
						img = Tile.getImg(val,128);
					double xv = 2.2;
					double yv = 4.2;
					if (x > 0)
						xv+=1.0;
					if (x > 1)
						xv+=1.0;
					if (x > 2)
					{
						yv-=1.0;
						xv-=2.0;
					}
					if (x > 3)
						xv+=1.0;
					if (x > 4)
						xv+=1.0;
					if (x > 5)
					{
						yv-=1.0;
						xv-=2.0;
					}
					if (x > 6)
						xv+=1.0;
					if (x > 7)
						xv+=1.0;
					if (val != 0)
						g.drawImage(img, (int)(xv*d)+10, (int)(yv*d)+7, d-7, d-7,null);
				}
				g.setFont(new Font("Monospaced", Font.BOLD, 50));
				g.setColor(Color.pink);
				g.drawString("Sum: " + sum, (dimension/2) - 118, 100);
				int d = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/16;
				g.setColor(new Color(121, 64, 25));
				g.fillRect((int)(2.2*d+3), (int)(2.2*d), 7, (int)(3*d)+7);
				g.fillRect((int)(3.2*d+3), (int)(2.2*d), 7, (int)(3*d)+7);
				g.fillRect((int)(4.2*d+3), (int)(2.2*d), 7, (int)(3*d)+7);
				g.fillRect((int)(5.2*d+3), (int)(2.2*d), 7, (int)(3*d)+7);
				g.fillRect((int)(2.2*d+3), (int)(2.2*d), 3*d+7, 7);
				g.fillRect((int)(2.2*d+3), (int)(3.2*d), 3*d+7, 7);
				g.fillRect((int)(2.2*d+3), (int)(4.2*d), 3*d+7, 7);
				g.fillRect((int)(2.2*d+3), (int)(5.2*d), 3*d+7, 7);
			}
			else if (menuType == 4)
			{
				try {
					review(g, analysis);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	static public void review(Graphics g, ArrayList<Mistake> analysis) throws IOException
	{
		int dimension = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight()/1.7);
//		System.out.println(analysis.size());
		if (!analysis.isEmpty())
		{
//			if (place+1 < 5)
//				System.out.println(place);
			int[] tSpace = Obj.interpret(analysis.get(reviewPage).getPos());
			int[] rSpace = new int[9];
//			Space.pArr(tSpace);
//			System.out.println(1+reviewPage+"/"+Math.min(10, analysis.size()));
//			if (place+1 < 5)
//				System.out.println(place);
            System.arraycopy(tSpace, 0, rSpace, 0, 9);
//			if (place+1 < 5)
//				System.out.println(place);
			for (int x = 0; x < rSpace.length; x++)
			{ // The pause error is in this loop somewhere
				int val = rSpace[x];
				int d = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/12;
				Image img = null;
				if (val != 0)
					img = Tile.getImg(val,128);
				double xv = 0.5;
				double yv = 2.85;
				g.setFont(new Font("Monospaced", Font.BOLD, 20));
				if (x > 0)
					xv+=1.0;
				if (x > 1)
					xv+=1.0;
				if (x > 2)
				{
					yv-=1.0;
					xv-=2.0;
				}
				if (x > 3)
					xv+=1.0;
				if (x > 4)
					xv+=1.0;
				if (x > 5)
				{
					yv-=1.0;
					xv-=2.0;
				}
				if (x > 6)
					xv+=1.0;
				if (x > 7)
					xv+=1.0;
				if (val != 0)
					g.drawImage(img, (int)(xv*d)+10, (int)(yv*d)+7, d-7, d-7,null);
/*				try {
				g.setColor(Color.RED);
				g.drawString("Your move: "+analysis.get(reviewPage).getMove(), 133,20);
				g.setColor(Color.green);
				g.drawString("Best move/moves: " + Input.getMove(analysis.get(reviewPage).getPos()).getBestMove() , 115, 50);
				g.setColor(Color.orange);
				g.drawString("Move score: " + String.format("%.3f%%",100*analysis.get(reviewPage).getScore()),125,80);
				} catch (Exception e) {System.out.println("You suck at something, who knows what");} */
			}
			try
			{
				g.setColor(Color.RED);
				String ym = "Your move: "+analysis.get(reviewPage).getMoveShort().trim();
				g.drawString(ym, (15 + dimension  - g.getFontMetrics().stringWidth(ym)) / 2,20);
				g.setColor(Color.green);
				String bm = "Best move/moves: " + (analysis2.get(reviewPage)).getBestMoveShort().trim();
				String apl = APLs.get(reviewPage);
				if (apl.length() > 1)
					bm += " (" + apl + ")";
				g.drawString(bm , (15 + dimension - g.getFontMetrics().stringWidth(bm)) / 2, 40);
				g.setColor(Color.orange);
				String ms = "Move score: " + String.format("%.3f%%",100*analysis.get(reviewPage).getScore()).trim();
				g.drawString(ms,(15 + dimension - g.getFontMetrics().stringWidth(ms)) / 2,60);
				g.setColor(Color.pink);
				String mlb = Obj.mainlines.get(reviewPage)[0];
				String mlh = Obj.mainlines.get(reviewPage)[1];
				g.drawString(mlb, (15 + dimension - g.getFontMetrics().stringWidth(mlb)) / 2,80);
				g.drawString(mlh, (15 + dimension - g.getFontMetrics().stringWidth(mlh)) / 2,100);

			} catch (Exception e) {
				System.out.println("You suck at something, who knows what");
//				e.printStackTrace();
//				System.exit(0);
			}
//			if (place+1 < 5)
//				System.out.println(place);
			int d = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/12;
			g.setColor(new Color(121, 64, 25));
			g.fillRect((int)(0.5*d+3), (int)(0.85*d), 7, (int)(3*d)+7);
			g.fillRect((int)(1.5*d+3), (int)(0.85*d), 7, (int)(3*d)+7);
			g.fillRect((int)(2.5*d+3), (int)(0.85*d), 7, (int)(3*d)+7);
			g.fillRect((int)(3.5*d+3), (int)(0.85*d), 7, (int)(3*d)+7);
			g.fillRect((int)(0.5*d+3), (int)(0.85*d), 3*d+7, 7);
			g.fillRect((int)(0.5*d+3), (int)(1.85*d), 3*d+7, 7);
			g.fillRect((int)(0.5*d+3), (int)(2.85*d), 3*d+7, 7);
			g.fillRect((int)(0.5*d+3), (int)(3.85*d), 3*d+7, 7);
		}
		else
		{
			g.setColor(Color.GREEN);
			g.drawString("No mistakes! R to reset.",40,40);
		}
	}
	static int[] barColor(int[] col)
	{
		if (col[0]==0&&col[1]==0&&col[2]==0)
		{
			col[0]=128;
			col[1]=128;
			col[2]=128;
		}
		int tInd = (int)(Math.random()*3);
		for (tInd = 0; tInd < 3; tInd++)
		{
			int tCol = col[tInd];
			int tc = (int)(Math.random()*28)+1;
			tCol-=tc/2;
			tCol+=(int)(Math.random()*tc);
			if (tCol < 96)
				tCol=96;
			if (tCol > 255)
				tCol=255;
			col[tInd]=tCol;
		}
		return col;
	}
	void unRender(Graphics g)
	{
		g.dispose();
	}
}
class Border extends Obj
{
	public Border(ID id, double score)
	{
		super(id, score);
	}
	void tick()
	{

	}
	void render(Graphics g)
	{
		if (disp)
		{
			int d = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/12;
			g.setColor(Color.GREEN);
			if (borderColor < Input.colorThreshold[0])
				g.setColor(Color.YELLOW);
			if (borderColor < Input.colorThreshold[1])
				g.setColor(Color.ORANGE);
			if (borderColor < Input.colorThreshold[2])
				g.setColor(Color.RED);
			if (Obj.solidColor)
				g.setColor(Color.MAGENTA);
			g.fillRect((int)(0.5*d), (int)(0.5*d), 7, (3*d) +7);
			g.fillRect((int)(1.5*d), (int)(0.5*d), 7, (3*d) +7);
			g.fillRect((int)(2.5*d), (int)(0.5*d), 7, (3*d) +7);
			g.fillRect((int)(3.5*d), (int)(0.5*d), 7, (3*d) +7);
			g.fillRect((int)(0.5*d), (int)(0.5*d), 3*d+7, 7);
			g.fillRect((int)(0.5*d), (int)(1.5*d), 3*d+7, 7);
			g.fillRect((int)(0.5*d), (int)(2.5*d), 3*d+7, 7);
			g.fillRect((int)(0.5*d), (int)(3.5*d), 3*d+7, 7);
		}
	}
	void unRender(Graphics g)
	{
		g.dispose();
	}
}
class Tile extends Obj
{
	public Tile(int[] space, ID id)
	{
		super(space, id);
	}
	void tick()
	{

	}
	static String getString()
	{
		return "actiba's 2048 3x3 Train";
	}
	void render(Graphics g)
	{
		g.setColor(new Color(121, 64, 25));
		g.fillRect(0, 0, 1000, 1000);
		if (disp)
		{
			int[] rSpace = new int[9];
			for (int x = 0; x < space.length; x++)
				rSpace[x]=space[x];
//			rSpace[6] = 8;
//			rSpace[7] = 10;
//			rSpace[8] = 9;
			for (int x = 0; x < rSpace.length; x++)
			{
				int val = rSpace[x];
				Image i = null;
				if (val != 0)
					i = Obj.getImg(val,128);
				int d = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/12;
				double xv = 0.5;
				double yv = 2.5;
				if (x > 0)
					xv+=1.0;
				if (x > 1)
					xv+=1.0;
				if (x > 2)
				{
					yv-=1.0;
					xv-=2.0;
				}
				if (x > 3)
					xv+=1.0;
				if (x > 4)
					xv+=1.0;
				if (x > 5)
				{
					yv-=1.0;
					xv-=2.0;
				}
				if (x > 6)
					xv+=1.0;
				if (x > 7)
					xv+=1.0;
				if (val != 0)
					g.drawImage(i, (int)(xv*d)+7, (int)(yv*d)+7, d-7, d-7,null);
			}
		}
		g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
		g.setColor(Color.BLACK);
		ArrayList<String> info2 = new ArrayList<>(Input.info);
		if (info2.size() == 8) {
			for (int x = 0; x < 4; x++) {
				g.drawString(info2.get(x), 25, 15 + 15 * x);
			}
			for (int x = 4; x < 8; x++) {
				g.drawString(info2.get(x), 400, -45 + 15 * x);
			}
		}
	}
	void unRender(Graphics g)
	{
		g.dispose();
	}
}
enum ID
{
	 Space(), Tile(), Border(), Menu();
}
class Input extends KeyAdapter
{
	static RandomAccessFile[] files = new RandomAccessFile[1025];
	static int tableAutomoveWait = 150;
	static int replaySize = 10;
	static boolean displayingInfo = false;
	static ArrayList<String> info = new ArrayList<>();
	static double[] colorThreshold = new double[] {1 - 0.00000000000001, 0.995, 0.97};
	Handler handler;
	public Input(Handler handler) {
		Obj.setDisp();
		System.out.println("Setting menu type.");
		Obj.setMenuType(2);
		this.handler = handler;
		System.out.println("Creating printwriter.");
		Obj.createPrintWriter();
		System.out.println("Setting menu type.");
		Obj.setMenuType(1);
		System.out.println("Setting disp.");
		Obj.setDisp();
	}

	static JTextArea stuffs = new JTextArea();
	static {
		stuffs.setEditable(false);
		stuffs.setPreferredSize(new Dimension(650, 100));
		stuffs.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
	}

	public static void settings() throws FileNotFoundException {
		File wDir = new File("").getAbsoluteFile();
		File settingsFile = new File(wDir, "settings.txt");
		if (!settingsFile.exists())
			settingsFile = new File("settings.txt");
		Scanner fileIn = new Scanner(settingsFile);

		fileIn.next();
		fileIn.next();
		File child = new File(wDir, "/Tables/" + fileIn.next());
		for (File f : child.listFiles()) {
			int layer = Integer.parseInt(f.getName().split("\\.")[0]);
			Input.files[layer] = new RandomAccessFile(f, "r");
		}

		fileIn.next();
		fileIn.next();
		Input.replaySize = fileIn.nextInt();
		fileIn.next();
		fileIn.next();
		Input.tableAutomoveWait = fileIn.nextInt();
		fileIn.nextLine();
		fileIn.next();
		fileIn.next();
		Space.learny = false;
		Space.bigBrother = false;
		Space.twosOnly = false;
		switch (fileIn.nextInt()) {
			case 1:
				Space.learny = true;
				break;
			case 2:
				Space.bigBrother = true;
				break;
			case 3:
				Space.twosOnly = true;
				break;
			default:
				break;
		}
		fileIn.nextLine();
		fileIn.next();
		fileIn.next();
		Input.colorThreshold[0] = fileIn.nextDouble();
		fileIn.nextLine();
		fileIn.next();
		fileIn.next();
		Input.colorThreshold[1] = fileIn.nextDouble();
		fileIn.nextLine();
		fileIn.next();
		fileIn.next();
		Input.colorThreshold[2] = fileIn.nextDouble();
	}

	// Object[][] that contains the layers n shi. Each Object[] is long name, double sr.
/*	static public LayerNode[][] layers;
	static {
		JFrame progress = new JFrame();
		progress.setLayout(new BorderLayout());
		progress.setPreferredSize(new Dimension(650, 100));
		progress.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		progress.setLocationRelativeTo(null);
		progress.setTitle("Loading Tables");
		JScrollPane stuff = new JScrollPane(stuffs, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		progress.add(stuff);
		progress.pack();
		progress.setVisible(true);

		Thread hi2 = new Thread( () -> {
			layers = new LayerNode[1025][];
			for (int x = 0; x <= 1024; System.out.println(x++)) {
				if (Window.frame != null)
					Window.frame.setVisible(false);
				Obj.setLoads(x);
				stuffs.setText("Loading Tables. Progress: " + String.format("%.1f", 100.0 * x / 1024) + "%.");
				File f = new File("Tables/Tables/" + x + ".txt");
				if (f.exists()) {
					try {
						int len = (int) (f.length() / 16);
						layers[x] = new LayerNode[len];
						FileInputStream fis = new FileInputStream(f);
						for (int y = 0; y < len; y++)
							layers[x][y] = new LayerNode(new DataInputStream(fis));
					} catch (IOException e) { e.printStackTrace(); System.exit(0); }
				}
			}
			if (Window.frame != null)
				Window.frame.setVisible(true);
            progress.setVisible(false);
			finishedLoading = true;
			System.out.println("Finished loading");
		});
		hi2.start();
	} */

	static public Move getMove2(String name)
	{
//		System.out.println("Calling getMove2");
//		System.out.println(name);
		try {
			int[] arr = new int[9];
			int sum = 0;
			for (int x = 0; x < 9; x++) {
				int charAt = name.charAt(x);
				arr[x] = charAt - 48;
				sum += (int) Math.pow(2, charAt - 48);
				if (charAt == 48)
					sum--;
			}
			sum /= 2;
			Board3x3 thisMove = new Board3x3(arr);

			double[] rates = new double[4];
			for (int mv = 0; mv < 4; mv++) {
//				System.out.println("Calculating move " + mv);
				Map<Long, Integer> children = thisMove.getChildren(mv, 1);
//				System.out.println(mv + " " + children.size());
				int zeroCt = new Board3x3(thisMove.merge(mv)).getZeroCt();
				for (Map.Entry<Long, Integer> entry : children.entrySet())
					try {
						double delta = 0.9 / zeroCt * entry.getValue() * (binSearch(files[sum + 1], Board3x3.getSmallestSymmetry(entry.getKey())));
						rates[mv] += delta;
					} catch (Exception _) { }
				children = thisMove.getChildren(mv, 2);
				zeroCt = new Board3x3(thisMove.merge(mv)).getZeroCt();
				for (Map.Entry<Long, Integer> entry : children.entrySet())
					try {
						double delta = 0.1 / zeroCt * entry.getValue() * (binSearch(files[sum + 2], Board3x3.getSmallestSymmetry(entry.getKey())));
						rates[mv] += delta;
					} catch (Exception _) { }
			}
			return new Move(name, rates);
		} catch (Exception e) {
			int sum = 0;
			for (int x = 0; x < 9; x++) {
				int charAt = name.charAt(x);
				sum += (int) Math.pow(2, charAt - 48);
				if (charAt == 48)
					sum--;
			}
			System.out.println("Error at getMove() line 1092. Name: " + name);
			System.out.println(e.getMessage() + " " + sum);
			e.printStackTrace();
			return null;
		}
	}
	static public Move getMove2(Board3x3 thisMove) {
		try {
			int sum = thisMove.getSum();
			double[] rates = new double[4];
			for (int mv = 0; mv < 4; mv++) {
				Map<Long, Integer> children = thisMove.getChildren(mv, 1);
				int zeroCt = new Board3x3(thisMove.merge(mv)).getZeroCt();
				for (Map.Entry<Long, Integer> entry : children.entrySet())
					try {
						double delta = 0.9 / zeroCt * entry.getValue() * (binSearch(files[sum + 1], Board3x3.getSmallestSymmetry(entry.getKey())));
						rates[mv] += delta;
					} catch (Exception _) { }
				children = thisMove.getChildren(mv, 2);
				zeroCt = new Board3x3(thisMove.merge(mv)).getZeroCt();
				for (Map.Entry<Long, Integer> entry : children.entrySet())
					try {
						double delta = 0.1 / zeroCt * entry.getValue() * (binSearch(files[sum + 2], Board3x3.getSmallestSymmetry(entry.getKey())));
						rates[mv] += delta;
					} catch (Exception _) { }
			}
			return new Move(Input.namer(thisMove.getTiles()), rates);
		} catch (Exception e) {
			System.out.println("Error at getMove() line 1232. " + thisMove);
			return null;
		}
	}
	public static double binSearch(RandomAccessFile file, long l) throws IOException {
//		System.out.println("Searching for " + Input.namer(new Board3x3(new Board3x3(l).getSmallestSymmetry()).getTiles()) + " " + l);
		if (file == null)
			return 0.0;
		long left = 0;
		long right = file.length() / 16 - 1;
		while (left <= right) {

			long mid = left + (right - left) / 2;
			file.seek(mid * 16);
			long target = file.readLong();

			if (target == l)
				return file.readDouble();
			if (target < l)
				left = mid + 1;
			else
				right = mid - 1;
//			System.out.println(left + " " + mid + " " + right);
		}
		return 0.0;
	}
	int key;
	public static String namer(String line)
	{
		Scanner namerIn = new Scanner(line);
		return namerIn.next();

	}
	public static String namer(int[] space)
	{
		String ret = "";
		for (int tn:space)
		{
			if (tn < 10)
				ret+=""+tn;
			else
				ret+=":";
		}
		return ret;
	}
	static int gamesToPlay = 1;
	static Space prevSpace;
	public void keyPressed(KeyEvent e)
	{
		key = e.getKeyCode();
		System.out.println(key);
		for (Obj thisObj:handler.objects)
			if (thisObj.getId()==ID.Tile)
			{
				double score;
				Space space = new Space(Obj.getX());
				int[] spaceO = Obj.getX().clone();
                if (Obj.getDisp())
				{
					if (Obj.fromReplay)
					{
						while (true) {
							int move = ReplayAnalyzer.getMove();
							System.out.println("Move made: " + move);
							if (move == -1)
								break;
							System.out.println(move);
							score = grade(namer(Obj.getX()), move - 37);
							System.out.println("Move score: " + score);
							if (score < 1.1)
								Obj.addMistake(new Mistake(namer(Obj.getX()), move, score));
							switch (move) {
								case 37:
									Obj.setX(space.left());
									break;
								case 38:
									Obj.setX(space.up());
									break;
								case 39:
									Obj.setX(space.right());
									break;
								case 40:
									Obj.setX(space.down());
									break;
								default:
									break;
							}
							Obj.setBorderColor(score);
							space = new Space(Obj.getX());
							try {
								Space.spawn(Obj.getX());
							} catch (Exception _) {
								break;
							}
							System.out.println(Arrays.toString(Obj.getX()));
						}
					}
					else if (key == 10) // Enter: Get Table move. This used to be 10
					{
						startOutput();
						gamesToPlay = 0;
						while (true) {
                            while (true) {
                                int move;
                                if (!Space.blunderBot)
                                    move = getMoveFromTables(spaceO);
                                else
                                    move = getTrashMoveFromTables(spaceO);
								System.out.println("Move " + move);
                                if (move == -1)
                                    break;
                                int[] space2 = space.space;
                                int zc = 0;
                                for (int i : space2)
                                    if (i == 0)
                                        zc++;
                                if (zc == 0 && space2[0] != space2[1] && space2[0] != space2[3] && space2[1] != space2[2] && space2[1] != space2[4] && space2[2] != space2[5] && space2[3] != space2[4] && space2[3] != space2[6] && space2[4] != space2[5] && space2[4] != space2[7] && space2[5] != space2[8] && space2[6] != space2[7] && space2[7] != space2[8])
                                    break;
                                if (space.canMove(move)) {
                                    switch (move) {
                                        case 37:
                                            Obj.setX(space.left());
                                            break;
                                        case 38:
                                            Obj.setX(space.up());
                                            break;
                                        case 39:
                                            Obj.setX(space.right());
                                            break;
                                        case 40:
                                            Obj.setX(space.down());
                                            break;
                                        default:
                                            break;
                                    }
                                }
                                space = new Space(Obj.getX());
                                spaceO = Obj.getX().clone();
                                try {
                                    Thread.sleep(tableAutomoveWait);
                                } catch (InterruptedException exc) { exc.printStackTrace(); }
                            }
							System.out.println(gamesToPlay--);
							if (gamesToPlay <= 0)
								break;
							try {								// Line one to uncomment if inf looping.
								Thread.sleep(0);
							} catch (InterruptedException ex) {}
							// Auto reset
							Obj.resetMistakes();
							Obj.setX(space.reset());
							if (Obj.getDeadScreen())
								Obj.setDeadScreen(false);
							Obj.setMenuType(1);
							Obj.setBorderColor(1.0);			// Last line.
                            space = new Space(Obj.getX());
							spaceO = Obj.getX().clone();
							dataOut.flush();
						}
						endOutput();
						System.out.println("Deadgame time");
						System.out.println(Arrays.toString(Obj.getX()));
						deadgame(Obj.getX());
					}
					else if (key == 73) { // i: Toggle displaying info.
						Input.displayingInfo = !Input.displayingInfo;
					}
					else {
						if (key == 79) { // o: Pops the top of startpos.txt off the stack.
							try {
								Scanner s = new Scanner(Files.readString(Path.of("startpos.txt")));
								s.nextLine();
								var pr = new PrintWriter("startpos.txt");
								while (s.hasNextLine())
									pr.println(s.nextLine());
								pr.close();
							} catch (IOException arg) { System.out.println("Could not pop from startpos.txt."); }
						}
						else if (key == 80) { // p: Pushes current start position onto the top of startpos.txt
							try {
								String pos = Input.namer(space.space);
								String content = Files.readString(Path.of("startpos.txt"));
								var pr = new PrintWriter("startpos.txt");
								pr.println(pos + "\n" + content);
								pr.close();
							} catch (IOException arg) { System.out.println("Could not push position onto startpos.txt."); }
						}
						else if (key == 76) { // p: Pushes previous start position onto the top of startpos.txt
							try {
								String pos = Input.namer(prevSpace.space);
								String content = Files.readString(Path.of("startpos.txt"));
								var pr = new PrintWriter("startpos.txt");
								pr.println(pos + "\n" + content);
								pr.close();
							} catch (IOException arg) { System.out.println("Could not push position onto startpos.txt."); }
						}
						else { // human game.
							if (key == 75) { // kill game prematurely
								announceDeadGame();
							}
							prevSpace = new Space(space.space);
							if (key == 37 || key == 65)
								if (space.canMove(0)) {
									score = grade(namer(spaceO), 0);
									if (score < 1.1)
										Obj.addMistake(new Mistake(namer(spaceO), key, score));
									Obj.setX(space.left());
									Obj.setBorderColor(score);
								}
							if (key == 38 || key == 87)
								if (space.canMove(1)) {
									score = grade(namer(spaceO), 1);
									if (score < 1.1)
										Obj.addMistake(new Mistake(namer(spaceO), key, score));
									Obj.setX(space.up());
									Obj.setBorderColor(score);
								}
							if (key == 39 || key == 68)
								if (space.canMove(2)) {
									score = grade(namer(spaceO), 2);
									if (score < 1.1)
										Obj.addMistake(new Mistake(namer(spaceO), key, score));
									Obj.setX(space.right());
									Obj.setBorderColor(score);
								}
							if (key == 40 || key == 83)
								if (space.canMove(3)) {
									score = grade(namer(spaceO), 3);
									if (score < 1.1)
										Obj.addMistake(new Mistake(namer(spaceO), key, score));
									Obj.setX(space.down());
									Obj.setBorderColor(score);
								}
						}
					}
//					System.out.println("Deadgame reached.");
					deadgame(Obj.getX());
                    try {
                        updateInfo();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
				if (Obj.getDisp() || Obj.getDeadScreen())
				{
					if (key == 82)
					{
						Obj.resetMistakes();
						Obj.setX(space.reset());
						System.out.println("Hey! Resetting now 3...");
						if (Obj.getDeadScreen())
							Obj.setDeadScreen(false);
						Obj.setMenuType(1);
						Obj.setBorderColor(1.0);
						System.out.println("After reset space: " + Arrays.toString(Obj.getX()));
						System.out.println("From replay : " + Obj.fromReplay);
					}
				}
				if (key == 81 && !Obj.getReviewing() && Obj.getDeadScreen())
				{
					Obj.reviewPage = 0;
					Obj.setReviewing();
					Menu.place = 0;
				}
//				if (!Obj.getDeadScreen())
//					if (key == 27)
//						Obj.setDisp();
				if (Obj.getReviewing())
				{
					if (key == 37||key == 65)
					{
						Menu.place = 0;
						Obj.setReviewPage("minus", Obj.analysis.size());
					}
					if (key == 39||key == 68)
					{
						Menu.place = 0;
						Obj.setReviewPage("plus", Obj.analysis.size());
					}
					if (key == 82)
					{
						System.out.println("Second 82");
						Obj.setReviewing();
						try {
							PrintWriter out = new PrintWriter("mistakes.txt");
							out.close();
						} catch (Exception a) {}
						Obj.setMenuType(3);
					}
					if (key == 80) { // p: Push the examined position onto startpos.txt.
						try {
							String pos = Obj.analysis.get(Obj.reviewPage).getPos();
							System.out.println(pos);
							String content = Files.readString(Path.of("startpos.txt"));
							var pr = new PrintWriter("startpos.txt");
							pr.println(pos + "\n" + content);
							pr.close();
						} catch (IOException arg) { System.out.println("Could not push to startpos.txt."); }
					}
				}
			}
	}
	static public void updateInfo() throws IOException {
		info.clear();
		if (!displayingInfo) {
			return;
		}
		System.out.println("Updating info");
		Move m = Input.getMove2(Input.namer(Obj.space));
		info.add("Success Rate: " + m.getBestScore());
		String[] moves = new String[] {"L", "U", "R", "D"};
		double[] srs = new double[4];
		srs[0] = m.getScore(0);
		srs[1] = m.getScore(1);
		srs[2] = m.getScore(2);
		srs[3] = m.getScore(3);
		for (int x = 0; x < 3; x++) {
			if (x < 0)
				x = 0;
			if (srs[x] < srs[x + 1]) {
				double temp = srs[x];
				srs[x] = srs[x + 1];
				srs[x + 1] = temp;
				String temp2 = moves[x];
				moves[x] = moves[x + 1];
				moves[x + 1] = temp2;
				x -= 2;
			}
		}
		String[] ml = MainlineFinder.getMainline(new Space(Obj.space));
		info.add(ml[0]);
		info.add(ml[1]);
		info.add(MainlineFinder.getHosiMainline(new Space(Obj.space), 1, 2));
		for (int x = 0; x < 4; x++) {
			info.add(moves[x] + ": " + String.format("%.3f", 100 * srs[x] / m.getBestScore()) + "%");
		}
		System.out.println(info);
	}
	static private PrintWriter dataOut;
	public static void startOutput()
	{
		try {
			dataOut = new PrintWriter("Boards.txt");
		} catch (Exception e) {}
	}
	private void endOutput()
	{
		try {
			dataOut.close();
		} catch (Exception e) {}
	}
	public static int getMoveFromTables(int[] space) {
		String name = namer(space);
		if (dataOut != null)
			dataOut.println(name);
		Move m = null;
        m = Input.getMove2(name);
        if (m == null)
			return -1;
//		System.out.println(m.getBestMoveShort().charAt(0));
		return switch (m.getBestMoveShort().charAt(0)) {
			case 'U' -> 38;
			case 'L' -> 37;
			case 'D' -> 40;
			case 'R' -> 39;
			default -> -1;
		};
	}
	public static int getTrashMoveFromTables(int[] space) {
		String name = namer(space);
		if (dataOut != null)
			dataOut.println(name);
		Move m = null;
        m = Input.getMove2(name);
        if (m == null)
			return -1;
//		System.out.println(m.getBestMoveShort().charAt(0));
		return switch (m.getTrashMoveShort().charAt(0)) {
			case 'U' -> 38;
			case 'L' -> 37;
			case 'D' -> 40;
			case 'R' -> 39;
			default -> -1;
		};
	}
	public void deadgame(int[] space)
	{
		for (int x:space)
			if (x==0)
				return;
		if (space[0]!=space[1]&&space[0]!=space[3]&&space[1]!=space[2]&&space[1]!=space[4]&&space[2]!=space[5]&&space[3]!=space[4]&&space[3]!=space[6]&&space[4]!=space[5]&&space[4]!=space[7]&&space[5]!=space[8]&&space[6]!=space[7]&&space[7]!=space[8])
			announceDeadGame();
	}
	public void announceDeadGame()
	{
		System.out.println("Game over.");
		Obj.setDeadScreen(true);
	}
	public static double grade(String name, int i)
	{
		Move m = null;
        m = Input.getMove2(name);
        if (m == null) {
			System.out.println("2.0 by null move");
			return 2.0;
		}
		if (!m.exists) {
			System.out.println("2.0 by nonexistence");
			return 2.0;
		}
		if (m.getBestScore()==0.0) {
			System.out.println("2.0 by doomed position");
			return 2.0;
		}
//		System.out.println("Move analysis returning " + m.getScore(i) / m.getBestScore());
		return m.getScore(i) / m.getBestScore();
	}
}
class Space
{
	int[] space;
	public Space(int[] space)
	{
		this.space = space;
	}
	public Space(Board3x3 board) {
		this.space = new int[9];
		long name = board.getName();
		for (int x = 0; x < 9; x++) {
			space[x] = (int) ((name & (0b1111L << (4 * x))) >> (4 * x));
		}
	}
	int[] reset()
	{
		Obj.mistakeOut.close();
		Obj.createPrintWriter();
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {}
		synchronized (Obj.mistakes) {
			Obj.mistakes.clear();
		}
		try {
			return Obj.startString();
		} catch (FileNotFoundException e) {
		}
		return null;
	}
	boolean canMove(int m)
	{
		int[] spaceO = space.clone();
		int[] am = new int[9];
		if (m == 0)
			am = new Space(space.clone()).left();
		if (m == 1)
			am = new Space(space.clone()).up();
		if (m == 2)
			am = new Space(space.clone()).right();
		if (m == 3)
			am = new Space(space.clone()).down();
//		System.out.println("finished checking");
		space = spaceO.clone();
		if (arrEq(am,space))
		{
//			System.out.println("cannot move");
			return false;
		}
//		System.out.println("Can move");
		return true;
	}
	int[] right()
	{
		final int[] spaceO = space.clone();
		int[] tlist = new int[3];
		int[] tlist2 = new int[3];
		int[] tlist3= new int[3];
		tlist[0] = space[0];
		tlist[1] = space[1];
		tlist[2] = space[2];
		tlist = merge(tlist);
		tlist2[0] = space[3];
		tlist2[1] = space[4];
		tlist2[2] = space[5];
		tlist2 = merge(tlist2);
		tlist3[0] = space[6];
		tlist3[1] = space[7];
		tlist3[2] = space[8];
		tlist3 = merge(tlist3);
		space[0] = tlist[0];
		space[1] = tlist[1];
		space[2] = tlist[2];
		space[3] = tlist2[0];
		space[4] = tlist2[1];
		space[5] = tlist2[2];
		space[6] = tlist3[0];
		space[7] = tlist3[1];
		space[8] = tlist3[2];
		if (!Obj.fromReplay)
			if (!arrEq(space, spaceO))
				space = spawn(space);
//		System.out.println("finished moving");
		return space;
	}
	int[] left()
	{
		final int[] spaceO = space.clone();
		int[] tlist = new int[3];
		int[] tlist2 = new int[3];
		int[] tlist3= new int[3];
		tlist[0] = space[2];
		tlist[1] = space[1];
		tlist[2] = space[0];
		tlist = merge(tlist);
		tlist2[0] = space[5];
		tlist2[1] = space[4];
		tlist2[2] = space[3];
		tlist2 = merge(tlist2);
		tlist3[0] = space[8];
		tlist3[1] = space[7];
		tlist3[2] = space[6];
		tlist3 = merge(tlist3);
		space[0] = tlist[2];
		space[1] = tlist[1];
		space[2] = tlist[0];
		space[3] = tlist2[2];
		space[4] = tlist2[1];
		space[5] = tlist2[0];
		space[6] = tlist3[2];
		space[7] = tlist3[1];
		space[8] = tlist3[0];
		if (!Obj.fromReplay)
			if (!arrEq(space, spaceO))
				space = spawn(space);
		return space;
	}
	int[] up()
	{
		final int[] spaceO = space.clone();
		int[] tlist = new int[3];
		int[] tlist2 = new int[3];
		int[] tlist3= new int[3];
		tlist[0] = space[0];
		tlist[1] = space[3];
		tlist[2] = space[6];
		tlist = merge(tlist);
		tlist2[0] = space[1];
		tlist2[1] = space[4];
		tlist2[2] = space[7];
		tlist2 = merge(tlist2);
		tlist3[0] = space[2];
		tlist3[1] = space[5];
		tlist3[2] = space[8];
		tlist3 = merge(tlist3);
		space[0] = tlist[0];
		space[1] = tlist2[0];
		space[2] = tlist3[0];
		space[3] = tlist[1];
		space[4] = tlist2[1];
		space[5] = tlist3[1];
		space[6] = tlist[2];
		space[7] = tlist2[2];
		space[8] = tlist3[2];
		if (!Obj.fromReplay)
			if (!arrEq(space, spaceO))
				space = spawn(space);
		return space;
	}
	int[] down()
	{
		final int[] spaceO = space.clone();
		int[] tlist = new int[3];
		int[] tlist2 = new int[3];
		int[] tlist3= new int[3];
		tlist[0] = space[6];
		tlist[1] = space[3];
		tlist[2] = space[0];
		tlist = merge(tlist);
		tlist2[0] = space[7];
		tlist2[1] = space[4];
		tlist2[2] = space[1];
		tlist2 = merge(tlist2);
		tlist3[0] = space[8];
		tlist3[1] = space[5];
		tlist3[2] = space[2];
		tlist3 = merge(tlist3);
		space[0] = tlist[2];
		space[1] = tlist2[2];
		space[2] = tlist3[2];
		space[3] = tlist[1];
		space[4] = tlist2[1];
		space[5] = tlist3[1];
		space[6] = tlist[0];
		space[7] = tlist2[0];
		space[8] = tlist3[0];
		if (!Obj.fromReplay)
			if (!arrEq(space, spaceO))
				space = spawn(space);
		return space;
	}
	Space makeMove(int i) {
		int[] int2 = new int[9];
		System.arraycopy(space, 0, int2, 0, 9);
		switch(i) {
			case 37: {
				int[] tlist = new int[3];
				int[] tlist2 = new int[3];
				int[] tlist3 = new int[3];
				tlist[0] = space[2];
				tlist[1] = space[1];
				tlist[2] = space[0];
				merge(tlist);
				tlist2[0] = space[5];
				tlist2[1] = space[4];
				tlist2[2] = space[3];
				merge(tlist2);
				tlist3[0] = space[8];
				tlist3[1] = space[7];
				tlist3[2] = space[6];
				merge(tlist3);
				int2[0] = tlist[2];
				int2[1] = tlist[1];
				int2[2] = tlist[0];
				int2[3] = tlist2[2];
				int2[4] = tlist2[1];
				int2[5] = tlist2[0];
				int2[6] = tlist3[2];
				int2[7] = tlist3[1];
				int2[8] = tlist3[0];
				break;
			}
			case 38: {
				int[] tlist = new int[3];
				int[] tlist2 = new int[3];
				int[] tlist3 = new int[3];
				tlist[0] = space[0];
				tlist[1] = space[3];
				tlist[2] = space[6];
				merge(tlist);
				tlist2[0] = space[1];
				tlist2[1] = space[4];
				tlist2[2] = space[7];
				merge(tlist2);
				tlist3[0] = space[2];
				tlist3[1] = space[5];
				tlist3[2] = space[8];
				merge(tlist3);
				int2[0] = tlist[0];
				int2[1] = tlist2[0];
				int2[2] = tlist3[0];
				int2[3] = tlist[1];
				int2[4] = tlist2[1];
				int2[5] = tlist3[1];
				int2[6] = tlist[2];
				int2[7] = tlist2[2];
				int2[8] = tlist3[2];
				break;
			}
			case 39: {
				int[] tlist = new int[3];
				int[] tlist2 = new int[3];
				int[] tlist3= new int[3];
				tlist[0] = space[0];
				tlist[1] = space[1];
				tlist[2] = space[2];
				merge(tlist);
				tlist2[0] = space[3];
				tlist2[1] = space[4];
				tlist2[2] = space[5];
				merge(tlist2);
				tlist3[0] = space[6];
				tlist3[1] = space[7];
				tlist3[2] = space[8];
				merge(tlist3);
				int2[0] = tlist[0];
				int2[1] = tlist[1];
				int2[2] = tlist[2];
				int2[3] = tlist2[0];
				int2[4] = tlist2[1];
				int2[5] = tlist2[2];
				int2[6] = tlist3[0];
				int2[7] = tlist3[1];
				int2[8] = tlist3[2];
				break;
			}
			case 40: {
				int[] tlist = new int[3];
				int[] tlist2 = new int[3];
				int[] tlist3= new int[3];
				tlist[0] = space[6];
				tlist[1] = space[3];
				tlist[2] = space[0];
				merge(tlist);
				tlist2[0] = space[7];
				tlist2[1] = space[4];
				tlist2[2] = space[1];
				merge(tlist2);
				tlist3[0] = space[8];
				tlist3[1] = space[5];
				tlist3[2] = space[2];
				merge(tlist3);
				int2[0] = tlist[2];
				int2[1] = tlist2[2];
				int2[2] = tlist3[2];
				int2[3] = tlist[1];
				int2[4] = tlist2[1];
				int2[5] = tlist3[1];
				int2[6] = tlist[0];
				int2[7] = tlist2[0];
				int2[8] = tlist3[0];
				break;
			}
		}
		return new Space(int2);
	}
	Space makeSpawn(int i, int val) {
		int[] int2 = new int[9];
		System.arraycopy(space, 0, int2, 0, 9);
		int2[i] = val;
		return new Space(int2);
	}
	ArrayList<Integer> emptySpaces() {
		ArrayList<Integer> arr = new ArrayList<>();
		for (int x = 0; x < 9; x++)
			if (space[x] == 0)
				arr.add(x);
		return arr;
	}
	boolean dead() {
		if (emptySpaces().isEmpty())
            return space[0] != space[1] && space[0] != space[3] && space[1] != space[2] && space[1] != space[4] && space[2] != space[5] && space[3] != space[4] && space[3] != space[6] && space[4] != space[5] && space[4] != space[7] && space[5] != space[8] && space[6] != space[7] && space[7] != space[8];
		return false;
	}
	static public boolean arrEq(int[] first, int[] second)
	{
		if (first.length!=second.length)
			return false;
		for (int x = 0; x < first.length; x++)
			if (first[x]!=second[x])
				return false;
		return true;
	}
	static public void pArr(int[] arr)
	{
		for (int x:arr)
			System.out.print(x+" ");
		System.out.println();
	}
	static public int[] merge (int[] tiles)
	{
		for (int z = 0; z < tiles.length; z++)
			for (int x = tiles.length-1; x > 0; x--)
				if (tiles[x]==0)
				{
					tiles[x]=tiles[x-1];
					tiles[x-1]=0;
				}
		for (int x = tiles.length-1; x > 0; x--)
			if (tiles[x]!=0)
				if (tiles[x]==tiles[x-1])
				{
					tiles[x-1]=0;
					tiles[x]++;
				}
		for (int z = 0; z < tiles.length; z++)
			for (int x = tiles.length-1; x > 0; x--)
				if (tiles[x]==0)
				{
					tiles[x]=tiles[x-1];
					tiles[x-1]=0;
				}
		return tiles;
	}
	static boolean bigBrother = false; // Evil spawns but never kills the player.
	static boolean learny = false; 	   // Never gives the player a 0%. Chooses pathways by line chance from successes.
	static boolean twosOnly = false;
	static boolean blunderBot = false;
	static public int[] spawn(int[] space)
	{
//		System.out.println("Spawn called");
		if (Obj.fromReplay)
		{
			int pos = ReplayAnalyzer.getX();
			space[pos] = ReplayAnalyzer.getSpawnValue();
			System.out.println("Spawning from replay now! Position: " + pos + ", value: " + ReplayAnalyzer.getSpawnValue());
			ReplayAnalyzer.advance();
			return space;
		}
		ArrayList<Integer> empties= new ArrayList<>();
		for (int x = 0; x < 9; x++)
			if (space[x]==0)
				empties.add(x);
		if (bigBrother) {
			int[] retSpace = new int[9];
			boolean satisfied = false;
			double bestScore = 1.0;
			for (int i : empties) {
				space[i] = 1;
				double score = 1.0;
                score = Input.getMove2(Input.namer(space)).bScore;
                if (score > 0 && score < bestScore) {
					bestScore = score;
					System.arraycopy(space, 0, retSpace, 0, 9);
					satisfied = true;
				}
				space[i] = 2;
                score = Input.getMove2(Input.namer(space)).bScore;
                if (score > 0 && score < bestScore) {
					bestScore = score;
					System.arraycopy(space, 0, retSpace, 0, 9);
					satisfied = true;
				}
				space[i] = 0;
			}
			if (satisfied) {
				System.arraycopy(retSpace, 0, space, 0, 9);
				return space;
			}
			System.out.println("Unsatisfactory big brother");
		}
		if (learny) {
            ArrayList<int[]> boards = new ArrayList<>();
			ArrayList<Double> widths = new ArrayList<>();
			for (int i : empties) {
				space[i] = 1;
				double score = 1.0;
                score = Input.getMove2(Input.namer(space)).bScore;
                if (score > 0) {
					int[] target = new int[9];
					System.arraycopy(space, 0, target, 0, 9);
					widths.add(score * empties.size() * 0.9);
					boards.add(target);
				}
				space[i] = 2;
                score = Input.getMove2(Input.namer(space)).bScore;
                if (score > 0) {
					int[] target = new int[9];
					System.arraycopy(space, 0, target, 0, 9);
					widths.add(score * empties.size() * 0.1);
					boards.add(target);
				}
				space[i] = 0;
			}
			if (!widths.isEmpty()) {
				double totalSuccess = 0;
				for (Double d : widths)
					totalSuccess += d;
				double rand = Math.random() * totalSuccess;
				System.arraycopy(boards.getLast(), 0, space, 0, 9);
				for (int x = 0; x < widths.size(); x++) {
					if (rand < 0) {
						System.arraycopy(boards.get(x - 1), 0, space, 0, 9);
						return space;
					}
					rand -= widths.get(x);
				}
				return space;
			}
		}
		if (!empties.isEmpty())
		{
			int randInd = (int)(empties.size()*Math.random())+1;
			randInd = empties.get(randInd-1);
			int randVal = (int)(10*Math.random());
			if (twosOnly)
				space[randInd] = 1;
			else if (randVal == 5) {
				space[randInd] = 2;
//				GameSimulator.s4++;
			}
			else {
				space[randInd] = 1;
//				GameSimulator.s2++;
			}
		}
		if (empties.size()==0)
		{
			System.out.println("You just died!");
		}
		return space;
	}
	public int hashCode() {
		int ret = 0;
		int place = 0;
		for (int i : space)
			ret |= i << (4 * place++);
		return ret;
	}
	public boolean equals(Object o) {
		if (o instanceof Space s)
			return Arrays.equals(space, s.space);
		return false;
	}
	public String toString() {
		StringBuilder ret = new StringBuilder("Board3x3:\n");
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++)
				ret.append(space[3 * x + y]).append(" ");
			ret.append("\n");
		}
		return ret.toString();
	}
}
class Move
{
	String name = "";
	double rScore = 0;
	double lScore = 0;
	double uScore = 0;
	double dScore = 0;
	double bScore = 0;
	boolean exists = false;
	String line = "";
	public Move(String board, double[] successRates) {
		exists = true;
		name = board;
		dScore = successRates[0];
		lScore = successRates[1];
		uScore = successRates[2];
		rScore = successRates[3];
		bScore = Math.max(Math.max(dScore, uScore), Math.max(lScore, rScore));
	}
	public Move(String line)
	{
		this.line = line;
		if (!(line.equals("0")))
		{
			exists = true;
			Scanner lineIn = new Scanner(line);
			name = lineIn.next();
			bScore = lineIn.nextDouble();
			while (lineIn.hasNext())
			{
				String move = lineIn.next();
				double mv = lineIn.nextDouble();
				if (move.equals("L:"))
				{
					lScore = mv;
				}
				else if (move.equals("U:"))
				{
					uScore = mv;
				}
				else if (move.equals("D:"))
				{
					dScore = mv;
				}
				else if (move.equals("R:"))
				{
					rScore = mv;
				}
			}
		}
	}
	boolean exists()
	{
		return exists;
	}
	String getName()
	{
		return name;
	}
	double getScore(int move)
	{
		if (move == 1)
			return uScore;
		if (move == 2)
			return rScore;
		if (move == 3)
			return dScore;
		if (move == 0)
			return lScore;
		return 0;
	}
	double getBestScore()
	{
		return bScore;
	}
	double getTrashScore()
	{
		double min = 1.0;
		if (uScore > 0 && uScore < min)
			min = uScore;
		if (rScore > 0 && rScore < min)
			min = rScore;
		if (dScore > 0 && dScore < min)
			min = dScore;
		if (lScore > 0 && lScore < min)
			min = lScore;
		return min;
	}
	String getBestMove()
	{
		double gScore = lScore;
		String ret = "";
		if (uScore > lScore)
			gScore = uScore;
		if (rScore > gScore)
			gScore = rScore;
		if (dScore > gScore)
			gScore = dScore;
		if (lScore == gScore)
			ret+="Left ";
		if (rScore == gScore)
			ret+="Right ";
		if (uScore == gScore)
			ret+="Up ";
		if (dScore == gScore)
			ret+="Down ";
		ret = ret.trim();
		while (ret.indexOf(" ")!=-1)
			ret = ret.substring(0,ret.indexOf(" "))+", "+ret.substring(ret.indexOf(" "));
		return ret;
	}
	public String getBestMoveShort()
	{
		if (bScore == 0.0)
			return "sad";
		double gScore = lScore;
		String ret = "";
		if (uScore > lScore)
			gScore = uScore;
		if (rScore > gScore)
			gScore = rScore;
		if (dScore > gScore)
			gScore = dScore;
		if (lScore == gScore)
			ret+="L ";
		if (rScore == gScore)
			ret+="R ";
		if (uScore == gScore)
			ret+="U ";
		if (dScore == gScore)
			ret+="D ";
		ret = ret.trim();
		while (ret.indexOf(" ")!=-1)
			ret = ret.substring(0,ret.indexOf(" "))+","+ret.substring(ret.indexOf(" ")+1);
		return ret;
	}
	public String getAPL() {
		return MainlineFinder.getHosiMainline(new Board3x3(Obj.interpret(name)), 1, 2).substring(20);
	}
	public String getTrashMoveShort()
	{
		if (bScore == 0.0)
			return "sad";
		double gScore = lScore;
		String ret = "";
		if (uScore < lScore)
			gScore = uScore;
		if (rScore < gScore)
			gScore = rScore;
		if (dScore < gScore)
			gScore = dScore;
		if (lScore == gScore)
			ret+="L ";
		if (rScore == gScore)
			ret+="R ";
		if (uScore == gScore)
			ret+="U ";
		if (dScore == gScore)
			ret+="D ";
		ret = ret.trim();
		while (ret.indexOf(" ")!=-1)
			ret = ret.substring(0,ret.indexOf(" "))+","+ret.substring(ret.indexOf(" ")+1);
		return ret;
	}
	public String toString()
	{
		return ""+bScore+" "+lScore+" "+uScore+" "+rScore+" "+dScore+"       "+line;
	}
}
class Mistake
{
	String pos;
	int move;
	double score;
	String type = "Optimal";
	public Mistake(String line)
	{
		line = line.trim();
		Scanner lineIn = new Scanner(line);
		pos = lineIn.next(); //JESUS FUCKING CHRIST THIS LINE HAS BEEN THE SOURCE OF MY PROBLEMS FOR AN ETERNITY THANK GOD I FOUND IT
		while (pos.length()<9) // fixes it not fucking working
			pos = 0+pos;
		move = lineIn.nextInt();
		score = lineIn.nextDouble();
		type = lineIn.next();
		lineIn.close();
	}
	public Mistake(String pos, int move, double score)
	{
		this.pos = pos;
		this.move = move;
		this.score = score;
		if (score < (1 - 0.00000000000001) ) // with 1e-14
			type = "Inaccuracy";
		if (score < 0.995)
			type = "Mistake";
		if (score < 0.97)
			type = "Blunder";
	}
	public String toString()
	{
		return pos+" "+move+" "+score+" "+type;
	}
	String getPos()
	{
		return pos;
	}
	String getMove()
	{
		if (move == 37 || move == 65)
			return "Left";
		if (move == 38 || move == 87)
			return "Up";
		if (move == 39 || move == 68)
			return "Right";
		if (move == 40 || move == 83)
			return "Down";
		return "";
	}
	public String getMoveShort()
	{
		if (move == 37 || move == 65)
			return "L";
		if (move == 38 || move == 87)
			return "U";
		if (move == 39 || move == 68)
			return "R";
		if (move == 40 || move == 83)
			return "D";
		return "";
	}
	String getType()
	{
		return type;
	}
	double getScore()
	{
		return score;
	}
}
class ReplayAnalyzer
{
	static int index = 0;
	private static int position;
	private static int spawnValue;
	private static int move;
	public static char currentChar = '0';
	public static void advance()
	{
//		System.out.println(Obj.replay);
		if (Obj.replay.isEmpty())
			return;
		currentChar = Obj.replay.charAt(index++);
		int move = currentChar;
		if (move > 127)
			for (int x = 0; x < EXTENDED.length; x++)
				if (EXTENDED[x] == currentChar)
					move = x + 127;
		int moveOrig = move;
//		System.out.println("Analyzing " + move);
		move -= 32;
		int pos = move % 16;
		spawnValue = 1 + ((move - pos) % 32) / 16;
//		System.out.println("Spawn value: " + spawnValue);
		String moved = "";
		switch (move / 32) {
			case 0:
				moved = "U";
				ReplayAnalyzer.move = 38;
				break;
			case 1:
				moved = "R";
				ReplayAnalyzer.move = 39;
				break;
			case 2:
				moved = "D";
				ReplayAnalyzer.move = 40;
				break;
			case 3:
				moved = "L";
				ReplayAnalyzer.move = 37;
				break;
			default:
				break;
		}
		System.out.println(moved);
		if (moved.isBlank())
			System.out.println(currentChar + " " + moveOrig);
//		System.out.println("Pos (mod) : " + pos);
		int c1down = pos % 4;
		int c1right = pos / 4;
		position = c1right + 3 * (2 - c1down);
//		System.out.println("Pos (index) : " + pos);
//		Obj.replay = Obj.replay.substring(1);
	}
	public static int getX() { return position; }
	public static int getSpawnValue() { return spawnValue; }
	public static int getMove() { return move; }
	public static final char[] EXTENDED = { 0x00C7, 0x00FC, 0x00E9, 0x00E2,
			0x00E4, 0x00E0, 0x00E5, 0x00E7, 0x00EA, 0x00EB, 0x00E8, 0x00EF,
			0x00EE, 0x00EC, 0x00C4, 0x00C5, 0x00C9, 0x00E6, 0x00C6, 0x00F4,
			0x00F6, 0x00F2, 0x00FB, 0x00F9, 0x00FF, 0x00D6, 0x00DC, 0x00A2,
			0x00A3, 0x00A5, 0x20A7, 0x0192, 0x00E1, 0x00ED, 0x00F3, 0x00FA,
			0x00F1, 0x00D1, 0x00AA, 0x00BA, 0x00BF, 0x2310, 0x00AC, 0x00BD,
			0x00BC, 0x00A1, 0x00AB, 0x00BB, 0x2591, 0x2592, 0x2593, 0x2502,
			0x2524, 0x2561, 0x2562, 0x2556, 0x2555, 0x2563, 0x2551, 0x2557,
			0x255D, 0x255C, 0x255B, 0x2510, 0x2514, 0x2534, 0x252C, 0x251C,
			0x2500, 0x253C, 0x255E, 0x255F, 0x255A, 0x2554, 0x2569, 0x2566,
			0x2560, 0x2550, 0x256C, 0x2567, 0x2568, 0x2564, 0x2565, 0x2559,
			0x2558, 0x2552, 0x2553, 0x256B, 0x256A, 0x2518, 0x250C, 0x2588,
			0x2584, 0x258C, 0x2590, 0x2580, 0x03B1, 0x00DF, 0x0393, 0x03C0,
			0x03A3, 0x03C3, 0x00B5, 0x03C4, 0x03A6, 0x0398, 0x03A9, 0x03B4,
			0x221E, 0x03C6, 0x03B5, 0x2229, 0x2261, 0x00B1, 0x2265, 0x2264,
			0x2320, 0x2321, 0x00F7, 0x2248, 0x00B0, 0x2219, 0x00B7, 0x221A,
			0x207F, 0x00B2, 0x25A0, 0x00A0 };
	static {
		EXTENDED[157 - 127] = '×';
		EXTENDED[156 - 127] = 'Ø';
		EXTENDED[154 - 127] = 'ø';
		EXTENDED[152 - 127] = 'Ö';
	}
}