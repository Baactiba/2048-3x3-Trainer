import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

//	   this program is made by Baactiba and may not be republished without permission or credit.

public class Trainer
{
	static public void main (String args[]) throws IOException
	{
		Board game = new Board();
	}
}
class Window extends Canvas
{
//	int width;
//	int height;
//	String name;
	private static final long serialVersionUID = 1873648274892L;
	public Window (int width, int height, String name, Board board, Color color)
	{
		JFrame frame = new JFrame(name);
		width = 450;
		height = 450;
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
	public Board() throws FileNotFoundException
	{
		int[] spaces = new int[6]; spaces[0]=1; spaces[1]=1; spaces[2]=0; spaces[3]=1; spaces[4]=1; spaces[5]=3;
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
	static int[] space = new int[6];
	static ArrayList<Mistake> mistakes = new ArrayList<>();
	static ArrayList<Mistake> analysis = new ArrayList<>();
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
			mistakeOut = new PrintWriter(new File("mistakes.txt"));
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
			if (analysis.size()>10) // only show 10 mistakes at a time
			{
				Collections.reverse(analysis);
				analysis.subList(10, analysis.size()).clear();
				Collections.reverse(analysis);
			}
			setMenuType(4);
		}
		if (!reviewing)
		{ 
			analysis.clear();
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
		if (!m.getType().equals("Optimal"))
			mistakeOut.println(m);
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
			img = ImageIO.read(new File(""+(int)(Math.pow(2,val))+".png"));
		} catch (Exception e) {System.out.println(""+(int)(Math.pow(2, val))+".png");}
		return img;
	}
	public static int[] startString()
	{
		Scanner fileIn =  null;
		try 
		{
			fileIn = new Scanner(new File("settings.txt"));
		} catch (Exception e) {}
		if (fileIn != null)
			if (fileIn.hasNext())
				return interpret(fileIn.next());
			else
				return interpret("110113");
		else
			return interpret("110113");
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
			g.setColor(Color.DARK_GRAY);
			g.fillRect(0, 0, 1000, 1000);
			g.setFont(new Font("Monospaced", Font.BOLD, 26));
			g.setColor(Color.RED);
			if (menuType != 3 && menuType != 4)
				g.drawString("Ba"+Tile.getString()+"er", 10, 30);
			if (menuType == 2)
			{
				barColor = barColor(barColor);
				g.setColor(Color.BLACK); 
				double fullness = (double)loads/160340;
				g.fillRect(10, 183, 438, 81);
				g.setColor(new Color(barColor[0], barColor[1], barColor[2]));
				g.fillRect(10, 183, (int)(fullness*435), 81);
			}
			if (menuType == 3)
			{
				g.setColor(Color.RED);
				g.setFont(new Font("Monospaced", Font.BOLD, 60));
				g.drawString("Game Over!", 56, 51);
				int o = 0;
				int i = 0;
				int m = 0;
				int b = 0;
				double tScore = 0.0;
				try
				{
					Thread.sleep(10);
				} catch (InterruptedException e) {}
				for (Mistake mistake:mistakes)
				{
					String type = mistake.getType();
					if (type.equals("Optimal"))
						o++;
					if (type.equals("Mistake"))
						m++;
					if (type.equals("Inaccuracy"))
						i++;
					if (type.equals("Blunder"))
						b++;
					tScore+=mistake.getScore();
				}
				tScore/=mistakes.size();
				
				
				g.setFont(new Font("Monospaced", Font.BOLD, 20));
				g.setColor(Color.green);
				g.drawString("Perfects : "+o, 10, 350);
				g.setColor(Color.yellow);
				g.drawString("Erratums : "+i, 10, 380);
				g.setColor(Color.orange); 
				g.drawString("Mistakes : "+m, 10, 410);
				g.setColor(Color.red);
				g.drawString("Blunders : "+b, 10, 440);
				if (tScore == 1)
					g.setColor(Color.green);
				if (tScore < 1)
					g.setColor(Color.yellow);
				if (tScore < 0.99)
					g.setColor(Color.orange);
				if (tScore < 0.97)
					g.setColor(Color.red);
				String output = String.format("Accuracy:%.5f%%.", tScore*100);
				if (tScore == 1)
					output = String.format("Accuracy:%.4f%%.", tScore*100);
				g.setFont(new Font("Monospaced", Font.BOLD, 20));
				g.setFont(new Font("Monospaced", Font.BOLD, 40));
				g.drawString(output, 5, 100);
				

				int[] rSpace = new int[9];
				for (int x = 0; x < space.length; x++)
					rSpace[x]=space[x];
				rSpace[6] = 8;
				rSpace[7] = 10;
				rSpace[8] = 9;
				for (int x = 0; x < rSpace.length; x++)
				{
					int val = rSpace[x];
					int d = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/16;
					Image img = null;
					if (val != 0)
						img = Tile.getImg(val,128);
					double xv = 2.2;
					double yv = 2.2;
					if (x > 0)
						xv+=1.0;
					if (x > 1)
						xv+=1.0;
					if (x > 2)
						yv+=1.0;
					if (x > 3)
						yv+=1.0;
					if (x > 4)
					{
						yv-=1.0;
						xv-=1.0;
					}
					if (x > 5)
						xv-=1.0;
					if (x > 6) 
						yv+=1.0;
					if (x > 7)
						xv+=1.0;
					if (val != 0)
						g.drawImage(img, (int)(xv*d)+10, (int)(yv*d)+7, d-7, d-7,null);
				}
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
			if (menuType == 4)
			{
				review(g, analysis);
			}
		}
	}
	static public void review(Graphics g, ArrayList<Mistake> analysis)
	{
		if (analysis.size()>0)
		{
			if (place++ < 5)
				System.out.println(place);
			int[] tSpace = Obj.interpret(analysis.get(reviewPage).getPos());
			int[] rSpace = new int[9];
//			Space.pArr(tSpace);
//			System.out.println(1+reviewPage+"/"+Math.min(10, analysis.size()));
			if (place++ < 5)
				System.out.println(place);
			for (int x = 0; x < 9; x++)
			{
				if (x < 6)
				rSpace[x]=tSpace[x];
				if (x == 6)
					rSpace[x]=8;
				if (x == 7)
					rSpace[x]=10;
				if (x == 8)
					rSpace[x]=9;
			}
			if (place++ < 5)
				System.out.println(place);
			for (int x = 0; x < rSpace.length; x++)
			{ // The pause error is in this loop somewhere
				int val = rSpace[x];
				int d = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/12;
				Image img = null;
				if (val != 0)
					img = Tile.getImg(val,128);
				double xv = 0.5;
				double yv = 0.85;
				g.setFont(new Font("Monospaced", Font.BOLD, 20));
				if (x > 0)
					xv+=1.0;
				if (x > 1)
					xv+=1.0;
				if (x > 2)
					yv+=1.0;
				if (x > 3)
					yv+=1.0;
				if (x > 4)
				{
					yv-=1.0;
					xv-=1.0;
				}
				if (x > 5)
					xv-=1.0;
				if (x > 6)
					yv+=1.0;
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
			try {
				g.setColor(Color.RED);
				g.drawString("Your move: "+analysis.get(reviewPage).getMove(), 133,20);
				g.setColor(Color.green);
				g.drawString("Best move/moves: " + Input.getMove(analysis.get(reviewPage).getPos()).getBestMove() , 115, 50);
				g.setColor(Color.orange);
				g.drawString("Move score: " + String.format("%.3f%%",100*analysis.get(reviewPage).getScore()),125,80);
				} catch (Exception e) {System.out.println("You suck at something, who knows what");} 

			if (place++ < 5)
				System.out.println(place);
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
//		if (!disp)
//			g.dispose();
		if (disp)
		{
			int d = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/12;
			if (borderColor == 1.0)
				g.setColor(Color.GREEN);
			if (borderColor < 1.0)
				g.setColor(Color.YELLOW);
			if (borderColor < 0.97)
				g.setColor(Color.ORANGE);
			if (borderColor < 0.85)
				g.setColor(Color.RED);
			g.fillRect((int)(0.5*d), (int)(0.5*d), 7, (int)(3*d)+7);
			g.fillRect((int)(1.5*d), (int)(0.5*d), 7, (int)(3*d)+7);
			g.fillRect((int)(2.5*d), (int)(0.5*d), 7, (int)(3*d)+7);
			g.fillRect((int)(3.5*d), (int)(0.5*d), 7, (int)(3*d)+7);
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
			rSpace[6] = 8;
			rSpace[7] = 10;
			rSpace[8] = 9;
			for (int x = 0; x < rSpace.length; x++)
			{
				int val = rSpace[x];
				Image i = null;
				if (val != 0)
					i = Obj.getImg(val,128);
				int d = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/12;
				double xv = 0.5;
				double yv = 0.5;
				if (x > 0)
					xv+=1.0;
				if (x > 1)
					xv+=1.0;
				if (x > 2)
					yv+=1.0;
				if (x > 3)
					yv+=1.0;
				if (x > 4)
				{
					yv-=1.0;
					xv-=1.0;
				}
				if (x > 5)
					xv-=1.0;
				if (x > 6)
					yv+=1.0;
				if (x > 7)
					xv+=1.0;
				if (val != 0)
					g.drawImage(i, (int)(xv*d)+7, (int)(yv*d)+7, d-7, d-7,null);
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
	int[] space = new int[5];
	static HashMap<String, Move> moves = new HashMap<>();
	Handler handler;
	public Input(Handler handler) throws FileNotFoundException
	{
		Obj.setDisp();
		Obj.setMenuType(2);
		int liner = 0;
		this.handler = handler;
		Scanner fileIn = new Scanner(new File("moves.txt"));
		while (fileIn.hasNext())	
		{
			Obj.setLoads(liner++);
			String line = fileIn.nextLine();
			moves.put(namer(line), new Move(line));
		}
		Obj.createPrintWriter();
		Obj.setMenuType(1);
		Obj.setDisp();
	}
	static public Move getMove(String name)
	{
		return moves.get(name);
	}
	int key;
	public String namer(String line)
	{
		Scanner namerIn = new Scanner(line);
		return namerIn.next();
		
	}
	public String namer(int[] space)
	{
		String ret = "";
		for (int tn:space)
			ret+=""+tn;
		return ret;
	}
	public void keyPressed(KeyEvent e)
	{
		key = e.getKeyCode();
//		System.out.println(key);
		for (Obj thisObj:handler.objects)
			if (thisObj.getId()==ID.Tile)
			{
				double score = 0.0;
				Space space = new Space(Obj.getX());
				int[] spaceO = Obj.getX().clone();
				if (Obj.getDisp())
				{
					if (key == 37||key == 65)
						if (space.canMove(0))
						{
							score = grade(moves, namer(spaceO), 0);
							if (score < 1.1)
								Obj.addMistake(new Mistake(namer(spaceO),key, score));
							Obj.setX(space.left());
							Obj.setBorderColor(score);
						}
					if (key == 38 ||key == 87)
						if (space.canMove(1))
						{
							score = grade(moves, namer(spaceO), 1);
							if (score < 1.1)
								Obj.addMistake(new Mistake(namer(spaceO),key, score));
							Obj.setX(space.up());
							Obj.setBorderColor(score);
						}
					if (key == 39 || key == 68)
						if (space.canMove(2))
						{
							score = grade(moves, namer(spaceO), 2);
							if (score < 1.1)
								Obj.addMistake(new Mistake(namer(spaceO),key, score));
							Obj.setX(space.right());
							Obj.setBorderColor(score);
						}
					if (key == 40 || key == 83)
						if (space.canMove(3))
						{
							score = grade(moves, namer(spaceO), 3);
							if (score < 1.1)
								Obj.addMistake(new Mistake(namer(spaceO),key, score));
							Obj.setX(space.down());
							Obj.setBorderColor(score);
						}
					deadgame(Obj.getX());
				}
				if (Obj.getDisp() || Obj.getDeadScreen())
				{
					if (key == 82)
					{
						Obj.resetMistakes();
						Obj.setX(space.reset());
						if (Obj.getDeadScreen())
							Obj.setDeadScreen(false);
						Obj.setMenuType(1);
						Obj.setBorderColor(1.0);
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
						Obj.setReviewing();
						try {
							PrintWriter out = new PrintWriter(new File("mistakes.txt"));
							out.close();
						} catch (Exception a) {}
						Obj.setMenuType(3);
					}
				}
			}
	}
	public void deadgame(int[] space)
	{
		for (int x:space)
			if (x==0)
				return;
		if (space[5]==7)
			announceDeadGame();
		if (space[1]!=space[0]&&space[1]!=space[2]&&space[3]!=space[2]&&space[4]!=space[3]&&space[5]!=space[1]&&space[5]!=space[3])
			announceDeadGame();
	}
	public void announceDeadGame()
	{
		System.out.println("Game over.");
		Obj.setDeadScreen(true);
	}
	public double grade(HashMap<String, Move> moves, String name, int i)
	{
		Move m = moves.get(name);
		if (!m.exists)
			return 2.0;
		if (m.getBestScore()==0.0)
			return 2.0;
		m.getScore(i);
		return m.getScore(i)/m.getBestScore();
	}
}
class Space
{
	int[] space = new int[6];
	public Space(int[] space)
	{
		this.space = space;
	}
	int[] reset()
	{
		Obj.mistakeOut.close();
		Obj.createPrintWriter();
		try {
		Thread.sleep(10);
		} catch (InterruptedException e) {}
		Obj.mistakes.clear();
		return Obj.startString();
	}
	boolean canMove(int m)
	{
		int[] spaceO = space.clone();
		int[] am = new int[6];
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
		tlist[0] = space[0];
		tlist[1] = space[1];
		tlist[2] = space[2];
		tlist = merge(tlist);
		space[0] = tlist[0];
		space[1] = tlist[1];
		space[2] = tlist[2];
		if (!arrEq(space, spaceO))
			space = spawn(space);
//		System.out.println("finished moving");
		return space;
	}
	int[] left()
	{
		final int[] spaceO = space.clone();
		int[] tlist = new int[3];
		tlist[0] = space[2];
		tlist[1] = space[1];
		tlist[2] = space[0];
		tlist = merge(tlist);
		space[2] = tlist[0];
		space[1] = tlist[1];
		space[0] = tlist[2];
		tlist = new int[2];
		tlist[0] = space[3];
		tlist[1] = space[5];
		tlist = merge(tlist);
		space[3] = tlist[0];
		space[5] = tlist[1];
		if (!arrEq(space, spaceO))
			space = spawn(space);
		return space;
	}
	int[] up()
	{
		final int[] spaceO = space.clone();
		int[] tlist = new int[3];
		tlist[0] = space[4];
		tlist[1] = space[3];
		tlist[2] = space[2];
		tlist = merge(tlist);
		space[4] = tlist[0];
		space[3] = tlist[1];
		space[2] = tlist[2];
		if (!arrEq(space, spaceO))
			space = spawn(space);
		return space;
	}
	int[] down()
	{
		final int[] spaceO = space.clone();
		int[] tlist = new int[3];
		tlist[0] = space[2];
		tlist[1] = space[3];
		tlist[2] = space[4];
		tlist = merge(tlist);
		space[2] = tlist[0];
		space[3] = tlist[1];
		space[4] = tlist[2];
		tlist = new int[2];
		tlist[0] = space[1];
		tlist[1] = space[5];
		tlist = merge(tlist);
		space[1] = tlist[0];
		space[5] = tlist[1];
		if (!arrEq(space, spaceO))
			space = spawn(space);
		return space;
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
	static public int[] spawn (int[] space)
	{
		ArrayList<Integer> empties= new ArrayList<>();
		for (int x = 0; x < 6; x++)
			if (space[x]==0)
				empties.add(x);
		if (empties.size()!=0)
		{
			int randInd = (int)(empties.size()*Math.random())+1;
			randInd = empties.get(randInd-1);
			int randVal = (int)(10*Math.random());
			if (randVal == 5)
				space[randInd] = 2;
			else
				space[randInd] = 1;
		}
		if (empties.size()==0)
		{
			System.out.println("You just died!");
		}
		return space;
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
				if (move.equals("R"))
				{
					lScore = mv;
				}
				if (move.equals("L"))
				{
					rScore = mv;
				}
				if (move.equals("U"))
				{
					uScore = mv;
				}
				if (move.equals("D"))
				{
					dScore = mv;
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
		if (move == 0)
			return lScore;
		if (move == 1)
			return uScore;
		if (move == 2)
			return rScore;
		if (move == 3)
			return dScore;
		return 0;
	}
	double getBestScore()
	{
		return bScore;
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
		pos = ""+lineIn.nextInt(); //JESUS FUCKING CHRIST THIS LINE HAS BEEN THE SOURCE OF MY PROBLEMS FOR AN ETERNITY THANK GOD I FOUND IT
		while (pos.length()<6) // fixes it not fucking working
			pos = ""+0+pos;
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
		if (score < 1)
			type = "Inaccuracy";
		if (score < 0.97)
			type = "Mistake";
		if (score < 0.85)
			type = "Blunder";
	}
	public String toString()
	{
		return ""+pos+" "+move+" "+score+" "+type;
	}
	String getPos()
	{
		return pos;
	}
	String getMove()
	{
		if (move == 37)
			return "Left";
		if (move == 38)
			return "Up";
		if (move == 39)
			return "Right";
		if (move == 40)
			return "Down";
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