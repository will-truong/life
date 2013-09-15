import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

import javax.sound.sampled.*;

public class Life implements Runnable{ //separates music & window threads
	public static void main(String...aei) throws Exception{
	
	(new Thread(new Life())).start();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

		int width = (int)(dim.width*.1)*10; //dim.width-dim.width%10
		int height = (int)(dim.height*.1)*10; //(int)(dim.width*.95-(dim.width*.95)%10)
		Grid frame = new Grid(width, height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); }

	public void run() throws RuntimeException{
		for(int k = 0; k > -1; k++){
			String filename;
			switch(k){
			case 1: filename = "magi.wav"; break;
			case 2: filename = "magiVoc.wav"; break;
			default: filename = "chata.wav"; k = 0; break; }
			try{
				playMusic(filename); }
			catch (Exception e){
				e.printStackTrace(); } } }

	public void playMusic(String filename) throws Exception{
		AudioInputStream aStream = AudioSystem.getAudioInputStream(new File(filename));
		SourceDataLine music = AudioSystem.getSourceDataLine(aStream.getFormat());
		music.open(aStream.getFormat());
		music.start();
		int frameSize = aStream.getFormat().getFrameSize();
		byte[] audioBuffer = new byte[frameSize];
		long bitLength = frameSize*aStream.getFrameLength();
		for(long i = frameSize; i < bitLength; i+= frameSize){
			aStream.read(audioBuffer, 0, frameSize);
			music.write(audioBuffer, 0, frameSize); }
		music.stop();
		Thread.sleep(1000);} }

class Grid extends JFrame implements MouseListener, MouseMotionListener{
	int width, height, col, row, stage, draw, size=10;
	boolean step;
	// draw if 1, clear if 2, clearScreen if 3
	ArrayList<Cell> remove, add, temp = new ArrayList<Cell>(), cells = new ArrayList<Cell>();
	long x = 200;
	long generation;
	//make array of _possible_ cells, then count the number of adjacent and process accordingly
	ArrayList<Color> colors = new ArrayList<Color>();
	boolean toContinue = false;
	Color currentColor;
	Image virtualMem;
	Graphics canvas;
	
	

	Grid(int x, int y){
		currentColor = Color.WHITE;
		addMouseListener(this);
		addMouseMotionListener(this);
		generation = 0;
		colors.add(Color.BLUE);
		colors.add(Color.MAGENTA);
		colors.add(Color.CYAN);
		colors.add(Color.ORANGE);
		colors.add(Color.YELLOW);
		colors.add(Color.RED);
		colors.add(Color.GREEN);
		stage = 0; 
		draw = 0;
		col = row = 0;
		setUndecorated(true);
		setAlwaysOnTop(true);
		width = x > 1920 ? 1920 : x;
		height = y;
		setSize(width, height);
		setVisible(true);
		step = false;}
	
	public void step(Graphics g){
		//System.out.print("step, removing dupes");
		long z = generation%7;
		currentColor = colors.get((int) z); 
		step = false;
		generation++;
		temp = new ArrayList<Cell>();
		add = new ArrayList<Cell>();
		remove = new ArrayList<Cell>();
		removeDup();
		//System.out.println("aggregate1" + cells);
		for(Cell x : cells)
			aggregate(x.getNeighbors());
//		System.out.print("aggregate1 done, toRemove");
		toRemove();
		//System.out.print("toRemove done, aggregate2");
		temp = new ArrayList<Cell>();
		for(Cell x : cells) 
			aggregate(x.getNeighbors());
		//System.out.print("aggregate2 done, toAdd");
		toAdd(); 
		//System.out.print("toAdd done, erasing");
		for(Cell x : remove) {
			col = x.x;
			row = x.y;
			clearCell(g); }
		
		//System.out.print("erasing done, drawing");
		for(Cell x : add) {
			col = x.x;
			row = x.y;
			drawCell(g);
			cells.add(x); }
		//System.out.print("drawing done, sleeping");
		col = row = 0;
		draw = 0;
		try { Thread.sleep(x); } 
		catch (InterruptedException e) {
			e.printStackTrace(); }
		step = true; }
	
	public void aggregate(ArrayList<Cell> more){ //pre&post of temp: no dupes
		//System.out.println("more: " + more);
		//System.out.println("temp: " + temp);
		for(Cell x : more){
			Cell dupe = x.findDup(temp);
			if(dupe!=null)
				dupe.adjacent++;
			else
				temp.add(x); } }

	public void removeDup(){
		for(int i = cells.size()-1; i > -1; i--)
			for(int j = i; j > -1; j--)
				if(cells.get(i).equals(cells.get(j)) && i != j) {
					cells.remove(j); 
					i--; } }

	public void toAdd(){
		//System.out.print("toAdd");
		for(int i = temp.size()-1; i > -1; i--){
			if(temp.get(i).adjacent != 3 || temp.get(i).exists(cells))
				temp.remove(i); } //remove all cells that already exist or are under/overpopulated
		add = temp; }

	public void toRemove(){ //temp contains all possible cells adjacent to an current cell
		//System.out.print("toRemove");
		//System.out.print("removing temp cells");
		for(int i = temp.size()-1; i > -1; i--) //remove all temp cells that are under/overpopulated and/or don't exist
			if(!temp.get(i).exists(cells) || temp.get(i).adjacent != 2 && temp.get(i).adjacent != 3)
				temp.remove(i); //post: all cells in temp are cells that exist and are adjacent to 2 or 3 things
		//System.out.print("temp cells removed, removing actual cells");
		for(int i = cells.size()-1; i > -1; i--) //queue to remove all cells not in temp (i.e. under/overpopulated)
			if(!cells.get(i).exists(temp)) 
				remove.add(cells.get(i));
		/*System.out.print("actual cells removed"); */}
		
	public void mouseClicked(MouseEvent e){
		System.out.println("mClicked " + e.getButton());
		row = (int)(e.getYOnScreen()/size)*size;
		col = (int)(e.getXOnScreen()/size)*size;
		draw = e.getButton() == 1 ? 1 : 2;
		draw = draw == 1 && e.isShiftDown() ? 3 : draw; 
		draw = draw == 1 && e.isControlDown() ? 4 : draw;
		step = draw == 2 && e.isAltDown() ? !step : step;
		(new Thread(new Runnable(){
			public void run(){
				repaint(); } } ) ).start(); }

	public void paint(Graphics g){
		//System.out.println("PAINT" + " " + draw + " " + stage + " " + col + " " + row);
		if(stage == 0) {
			drawBg(g, Color.BLACK);
			drawGrid(g);
			drawSplash(g);
			stage++; }
		if(stage == 1){
			//System.out.println("FIRST");
			drawBg(g,Color.BLACK);
			drawGrid(g); 
			stage++; }
		while(step)
			step(g);
		if(draw == 1) {
			//System.out.println("DRAW");
			draw = 0;
			drawCell(g); }
		if(draw == 2){
			//System.out.println("!DRAW");
			draw = 0;
			clearCell(g); }
		if(draw == 3){
			//System.out.println("clearScreen");
			draw = 0;
			clearScreen(g); }
		if(draw == 4){
			//System.out.println("redrawScreen");
			draw = 0;
			clearScreen(g);
			drawSplash(g);
			drawBg(g, Color.BLACK);
			drawGrid(g); } } 

	public void drawBg(Graphics g, Color x){
		System.out.println("BG");
		g.setColor(x);
		g.fillRect(0, 0, width, height); }

	public void drawSplash(Graphics g){
		System.out.println("SPLASH");
		int colorNum = 0;
		//g.setFont(new Font("Times New Roman",Font.PLAIN,50));
		//g.setColor(Color.WHITE);
		//g.drawString("",500,500);
		for(long offset = 0; offset < 50; offset++) {
			colorNum = (int)Math.abs((Math.sin(2*Math.PI*offset/(width))*255));
			Color bw = new Color(colorNum, colorNum, colorNum);
			for(int i = 0; i < width; i++) { 
				offset = offset == (int)((int)(Long.MAX_VALUE/678.58)*678.58) ? 0 : offset;
				g.setColor(bw);
				double sinY = height/2 + (double)height/10*(Math.sin(2*Math.PI*i/width + (double)(offset)*10/height));
				double cosY = height/2 + (double)height/10*(Math.sin(2*Math.PI*i/width + width*2/9 + (double)(offset)*10/height));
				g.fillRect(i, (int)(sinY), 2, 2);
				g.fillRect(i, (int)(cosY), 2, 2); }
			try{ Thread.sleep(5); }
			catch(Exception e) { } } }	    

	public void drawGrid(Graphics g){
		System.out.println("Grid");
		g.setColor(new Color(10, 10, 10));
		for(int i = size; i < width; i+=size)
			g.drawLine(i,0,i,height);
		for(int i = size; i < height; i+=size)
			g.drawLine(0, i, width, i); }

	public void drawCell(Graphics g) {
		//System.out.println("DRAW " + col + " " + row);
		g.setColor(currentColor);
		g.fillRect(col, row, size, size);
		cells.add(new Cell(col,row,Color.WHITE,width,height,size)); }

	public void clearCell(Graphics g) {
		//System.out.println("CLEAR " + col + " " + row);
		boolean found = false;
		for(int i = 0; i < cells.size() && !found; i++)
			if(cells.get(i).x == col && cells.get(i).y == row) {
				found = true;
				cells.remove(i); }
		g.setColor(Color.BLACK);
		g.fillRect(col, row, size, size);
		g.setColor(new Color(10, 10, 10));
		g.drawRect(col, row, size, size); }

	public void clearScreen(Graphics g){
		while(cells.size()!=0) {
			draw = 3;
			col = cells.get(0).x;
			row = cells.get(0).y;
			clearCell(g); }
		cells = new ArrayList<Cell>();}

	public void mouseEntered(MouseEvent e) {
	} //unused

	public void mouseExited(MouseEvent e) {
	} //unused

	public void mousePressed(MouseEvent e) { 
		System.out.println("mPressed " + e.getButton());
		row = (int)(e.getYOnScreen()/size)*size;
		col = (int)(e.getXOnScreen()/size)*size;
		draw = e.getButton() == 1 && !e.isAltDown() ? 1 : 2;
		draw = draw == 1 && e.isShiftDown() ? 3 : draw; 
		draw = draw == 1 && e.isControlDown() ? 4 : draw;
		step = draw == 2 && e.isAltDown() ? !step : step;
		(new Thread(new Runnable(){
			public void run(){
				repaint(); } } ) ).start();
	}

	public void mouseReleased(MouseEvent e) {
	} //unused

	public void mouseDragged(MouseEvent e){
		System.out.println("mDragged " + e.getButton());
		draw = !e.isControlDown() ? 1 : 2;
		draw = draw == 1 && e.isShiftDown() ? 3 : draw; 
		draw = draw == 1 && e.isControlDown() ? 4 : draw;
		step = draw == 2 && e.isAltDown() ? !step : step;
		row = (int)(e.getYOnScreen()/size)*size;
		col = (int)(e.getXOnScreen()/size)*size;
		System.out.println(toContinue);
		(new Thread(new Runnable(){
			public void run(){
				repaint(); } } ) ).start(); }

	public static int trunc(int x, int y){ // truncates x into a multiple of y
		return (int)(x/y)*y;
	}

	public void mouseMoved(MouseEvent arg0) {
	} }

class Cell {
	// <2 neighbors, dead
	// 2 || 3, live
	// >3 dead
	// 3 == produce
	int x, y, adjacent, width, height, size;
	Color color;

	Cell(int x, int y, Color color, int width, int height, int size) {
		adjacent = 1;
		this.x = Grid.trunc(x, size);
		this.y = Grid.trunc(y,size); 
		this.color = color;
		this.width = width;
		this.height = height;
		this.size = size;}

	public String toString(){
		return x + " " + y + " " + color;  }

	public boolean equals(Cell x){
		return x.x == this.x && x.y == this.y;
	}

	public boolean isAdjacent(Cell other){
		return !equals(other) && Math.abs(other.x - x) < size && Math.abs(other.y - y) < size; }

	public boolean sameLoc(Cell cell){
		return cell.x == x && cell.y == y; }

	public ArrayList<Cell> getNeighbors(){
		ArrayList<Cell> neighbors = new ArrayList<Cell>();
		for(int x = this.x - size; x <= this.x + size; x+=size)
			for(int y = this.y - size; y <= this.y + size ; y+=size){
				Cell temp = new Cell(x, y, Color.WHITE, width, height, size);
				if(temp.isValid() && !equals(temp))
					neighbors.add(temp);}
		return neighbors; }

	public boolean isValid(){
		return x >= 0 && y >= 0 && x < width && y < height; }

	public boolean exists(ArrayList<Cell> cells){
		return findDup(cells)!=null;}

	public Cell findDup(ArrayList<Cell> cells){
		for(Cell x : cells)
			if(this.equals(x))
				return x;
		return null; }
}
