package com.ogb.fes.entity;


import java.util.ArrayList;

import com.ogb.fes.utils.CornerManager;
import com.ogb.fes.utils.DateTime;
import com.ogb.fes.utils.Rectangle;
import com.ogb.fes.utils.Utils;


public class GPSRect 
{	
	public GPSPoint northEst;
	public GPSPoint southWest;
	public boolean  isInternal;
	
	
	//Default Contructor
	public GPSRect() {
		this.northEst  = null;
		this.southWest = null;
	}
	
	public boolean isInternal() {
		return isInternal;
	}

	public void setInternal(boolean isInternal) {
		this.isInternal = isInternal;
	}

	public GPSRect(GPSPoint ne, GPSPoint sw) {
		this.northEst  = ne;
		this.southWest = sw;
		
		adjustCoordTo1x1();
	}
	
	public GPSRect(GPSPoint center, double areaKm2) {
		double edge   = Math.sqrt(areaKm2)/100;
		double width  = Utils.randomBetween(edge*1.5, edge*0.5);
		double height = (areaKm2/width)/100/100;
		
		this.southWest = new GPSPoint(Utils.floor10(center.latitude-width/2, 2), Utils.floor10(center.longitude-height/2, 2));
		this.northEst  = new GPSPoint(Utils.floor10(center.latitude+width/2, 2), Utils.floor10(center.longitude+height/2, 2));
		
		adjustCoordTo1x1();
	}
	
	public GPSRect(double x, double y, double width, double height) {		
		this.southWest = new GPSPoint(Utils.floor10(x, 2), Utils.floor10(y, 2));
		this.northEst  = new GPSPoint(Utils.floor10(x+width, 2), Utils.floor10(y+height, 2));
		
		adjustCoordTo1x1();
	}
	
	public GPSRect(double x, double y, double edgeSize) {		
		this(x,y, edgeSize, edgeSize);
		
		adjustCoordTo1x1();
	}
	
	
	//Getter Method
	public GPSPoint getNorthEst() {
		return northEst;
	}
	public GPSPoint getSouthWest() {
		return southWest;
	}

	
	//Setter Method
	public void setNorthEst(GPSPoint northEst) {
		this.northEst = northEst;
	}
	public void setSouthWest(GPSPoint southWest) {
		this.southWest = southWest;
	}
	
	
	//This methods provide decimal round algorithms for specified corner
	public void adjustCoordToDecimalNumberForCorners(int decimalDigit, int cornerMask) {
		switch (decimalDigit) {
			case 0:
				adjustCoordTo100x100(cornerMask);
				break;
			case 1:
				adjustCoordTo10x10(cornerMask);
				break;
			case 2:
				adjustCoordTo1x1(cornerMask); //CornerMask useless at this resolution, this round is mandatory
				break;
				
			default:
				adjustCoordTo1x1(cornerMask); //CornerMask useless at this resolution, this round is mandatory
				break;
		}
	}
	public void adjustCoordTo1x1(int cornerMask) {
		GPSPoint roundNorthEst = new GPSPoint();
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Top) == true)
			roundNorthEst.setLatitudeAndCeilToDecimal(northEst.latitude, 2);
		else
			roundNorthEst.setLatitude(northEst.latitude);
		
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Right) == true)
			roundNorthEst.setLongitudeAndCeilToDecimal(northEst.longitude, 2);
		else
			roundNorthEst.setLongitude(northEst.longitude);
		
		
		GPSPoint roundSouthWest = new GPSPoint();
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Down) == true)
			roundSouthWest.setLatitudeAndFloorToDecimal(southWest.latitude, 2);
		else
			roundSouthWest.setLatitude(southWest.latitude);
		
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Left) == true)
			roundSouthWest.setLongitudeAndFloorToDecimal(southWest.longitude, 2);
		else
			roundSouthWest.setLongitude(southWest.longitude);

		northEst  = roundNorthEst;
		southWest = roundSouthWest;
	}
	public void adjustCoordTo10x10(int cornerMask) {
		GPSPoint roundNorthEst = new GPSPoint();
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Top) == true)
			roundNorthEst.setLatitudeAndCeilToDecimal(northEst.latitude, 1);
		else
			roundNorthEst.setLatitude(northEst.latitude);
		
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Right) == true)
			roundNorthEst.setLongitudeAndCeilToDecimal(northEst.longitude, 1);
		else
			roundNorthEst.setLongitude(northEst.longitude);
		
		
		GPSPoint roundSouthWest = new GPSPoint();
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Down) == true)
			roundSouthWest.setLatitudeAndFloorToDecimal(southWest.latitude, 1);
		else
			roundSouthWest.setLatitude(southWest.latitude);
		
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Left) == true)
			roundSouthWest.setLongitudeAndFloorToDecimal(southWest.longitude, 1);
		else
			roundSouthWest.setLongitude(southWest.longitude);

		northEst  = roundNorthEst;
		southWest = roundSouthWest;
	}
	public void adjustCoordTo100x100(int cornerMask) {
		GPSPoint roundNorthEst = new GPSPoint();
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Top) == true)
			roundNorthEst.setLatitudeAndCeilToDecimal(northEst.latitude, 0);
		else
			roundNorthEst.setLatitude(northEst.latitude);
		
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Right) == true)
			roundNorthEst.setLongitudeAndCeilToDecimal(northEst.longitude, 0);
		else
			roundNorthEst.setLongitude(northEst.longitude);
		
		
		GPSPoint roundSouthWest = new GPSPoint();
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Down) == true)
			roundSouthWest.setLatitudeAndFloorToDecimal(southWest.latitude, 0);
		else
			roundSouthWest.setLatitude(southWest.latitude);
		
		if (CornerManager.cornerMaskContainsCorner(cornerMask, CornerManager.Corner.Left) == true)
			roundSouthWest.setLongitudeAndFloorToDecimal(southWest.longitude, 0);
		else
			roundSouthWest.setLongitude(southWest.longitude);

		northEst  = roundNorthEst;
		southWest = roundSouthWest;
	}
	

	//This methods provide decimal for all square corner (defaults methods)
	public void adjustCoordToDecimalNumber(int decimalDigit) {
		switch (decimalDigit) {
			case 0:
				adjustCoordTo100x100();
				break;
			case 1:
				adjustCoordTo10x10();
				break;
			case 2:
				adjustCoordTo1x1();
				break;
				
			default:
				adjustCoordTo1x1();
				break;
		}
	}
	public void adjustCoordTo1x1() {
		GPSPoint roundNorthEst = new GPSPoint();
		roundNorthEst.setLatitudeAndCeilToDecimal(northEst.latitude, 2);
		roundNorthEst.setLongitudeAndCeilToDecimal(northEst.longitude, 2);
		
		GPSPoint roundSouthWest = new GPSPoint();
		roundSouthWest.setLatitudeAndFloorToDecimal(southWest.latitude, 2);
		roundSouthWest.setLongitudeAndFloorToDecimal(southWest.longitude, 2);

		northEst  = roundNorthEst;
		southWest = roundSouthWest;
	}
	public void adjustCoordTo10x10() {
		GPSPoint roundNorthEst = new GPSPoint();
		roundNorthEst.setLatitudeAndCeilToDecimal(northEst.latitude, 1);
		roundNorthEst.setLongitudeAndCeilToDecimal(northEst.longitude, 1);
		
		GPSPoint roundSouthWest = new GPSPoint();
		roundSouthWest.setLatitudeAndFloorToDecimal(southWest.latitude, 1);
		roundSouthWest.setLongitudeAndFloorToDecimal(southWest.longitude, 1);

		northEst  = roundNorthEst;
		southWest = roundSouthWest;
	}
	public void adjustCoordTo100x100() {
		GPSPoint roundNorthEst = new GPSPoint();
		roundNorthEst.setLatitudeAndCeilToDecimal(northEst.latitude, 0);
		roundNorthEst.setLongitudeAndCeilToDecimal(northEst.longitude, 0);
		
		GPSPoint roundSouthWest = new GPSPoint();
		roundSouthWest.setLatitudeAndFloorToDecimal(southWest.latitude, 0);
		roundSouthWest.setLongitudeAndFloorToDecimal(southWest.longitude, 0);

		northEst  = roundNorthEst;
		southWest = roundSouthWest;
	}
	
	
	public ArrayList<Rectangle> computeOptimalCover(int maxTiles) {
		//Arrotondo allo zoom 10x10
		adjustCoordToDecimalNumber(2);
		
		long start = System.currentTimeMillis();
		long stop;
		
		ArrayList<Rectangle> innerTiles = computeInnerTiles(); 
		//Compute the starting tiles set
		ArrayList<Rectangle> resTiles = new ArrayList<Rectangle>(innerTiles); 
		
		//Se la selezione utente rispetta gia il requisito maxTiles le ritorno
		if (resTiles.size() < maxTiles) {
			stop = System.currentTimeMillis();
			System.out.println(DateTime.currentTime()+"1 - ComputerInnerTiles required " +  (stop-start));
			return resTiles;
		}
		
		stop = System.currentTimeMillis();
		System.out.println(DateTime.currentTime()+"2 - ComputerInnerTiles required " +  (stop-start));
		
		
		start = System.currentTimeMillis();
		
		//Bordi di un quadrato con risoluzione 100x100
		ArrayList<Rectangle> external100 = computeExternalTilesFromResolution(0);
		ArrayList<Rectangle> internal100 = computeInternalTilesFromResolution(0);
		
		//Se gia con le tile 100x100 supero la specifica, questo è il meglio che posso fare quindi ritorno
		if (external100.size() > maxTiles) {
			stop = System.currentTimeMillis();
			System.out.println(DateTime.currentTime()+"1 - ComputeBorders required " +  (stop-start));
			
			return external100;
		}
		
		external100.removeAll(internal100);
		external100.sort(Rectangle.comparator());
		
		
		//System.out.println("External Border 100: " + external100.size());
		
		//Bordi di un quadrato con risoluzione 10x10
		ArrayList<Rectangle> external10 = computeExternalTilesFromResolution(1);
		ArrayList<Rectangle> internal10 = computeInternalTilesFromResolution(1);
		external10.removeAll(internal10);
		external10.sort(Rectangle.comparator());
		
		stop = System.currentTimeMillis();
		System.out.println(DateTime.currentTime()+"2 - ComputeBorders required " +  (stop-start));
		
		//System.out.println("External Border 10: " + external10.size());
		start = System.currentTimeMillis();
		for (int i = 0; i <= external100.size(); i++) {
			//Riparto con la lista di base ad ogni giro
			resTiles = new ArrayList<>(innerTiles); 
			//System.out.println("1 - Starting tiles: " + resTiles.size());
			
			long startrem = System.currentTimeMillis();
			//Aggiungo tante tile 100x100 quante ne richiede il ciclo i  
			for (int j = 0; j < i; j++) {
				resTiles = removeCoveredTilesAndAddRect(resTiles, external100.get(j));
			}
			System.out.println(DateTime.currentTime()+"1 - RemoveCovered required " +  (System.currentTimeMillis()-startrem));
			
			//System.out.println("2 - Starting tiles: " + resTiles.size());
			//Se rispetta gia il requisito maxTiles ho finito e le ritorno
			if (resTiles.size() < maxTiles) {
				stop = System.currentTimeMillis();
				System.out.println(DateTime.currentTime()+"1 - ComputeOptimal required " +  (stop-start));
				return resTiles;
			}
			
			startrem = System.currentTimeMillis();
			for (Rectangle rect : external10) {
				resTiles = removeCoveredTilesAndAddRect(resTiles, rect);
				//System.out.println("2." + (count++) + " - Starting tiles: " + resTiles.size());
				//Se rispetta gia il requisito maxTiles ho finito e le ritorno
				if (resTiles.size() < maxTiles) {
					stop = System.currentTimeMillis();
					System.out.println(DateTime.currentTime()+"2 - ComputeOptimal required " +  (stop-start));
					return resTiles;
				}
			}
			System.out.println(DateTime.currentTime()+"2 - RemoveCovered required " +  (System.currentTimeMillis()-startrem));
			
		}
		stop = System.currentTimeMillis();
		System.out.println(DateTime.currentTime()+"3 - ComputeOptimal required " +  (stop-start));
		
		//System.out.println("3 - Starting tiles: " + resTiles.size());
		
		return resTiles;
	}
	private ArrayList<Rectangle> removeCoveredTilesAndAddRect(ArrayList<Rectangle> list, Rectangle rect) {
		ArrayList<Rectangle> removedList = new ArrayList<Rectangle>();
		
		for (int i = 0; i < list.size(); i++) {
			
			if (Utils.intersects(list.get(i), rect) == true) {
				if (rect.computeArea() > list.get(i).computeArea())
					removedList.add(list.get(i));
			}
		}
		list.removeAll(removedList);
		
		if (removedList.size() > 0)
			list.add(rect);
		
		return list;
	}
	
	
	public GPSNode computeInnerTilesTree() {
		//If the area is too big, calculate aprox innerTileThree (min res 10x10)
		if (computeArea()*100*100 > 5000)
			return computeInnerTilesTreeReduced10();
		
		
		GPSNode head = new GPSNode();
		
		adjustCoordTo1x1();
		
		//Use to calculate the tile width and tile height as 1/tileSize
		int tileSize = 100;
		int startLat = (int)Utils.floor10(southWest.latitude*tileSize,2);
		int startLng = (int)Utils.floor10(southWest.longitude*tileSize,2);
		int stopLat  = (int)Utils.ceil10(northEst.latitude*tileSize,2);
		int stopLng  = (int)Utils.ceil10(northEst.longitude*tileSize,2);
		
	
		for (int i=startLat; i<stopLat; i+=1) {
			for (int j=startLng; j<stopLng; j+=1) {
				double x = (double)i/tileSize;
				double y = (double)j/tileSize;

				head.appendChild(new GPSPoint(Utils.floor10(x, 2), Utils.floor10(y, 2)), 2);
			}
		}

		return head;
	}
	
	public GPSNode computeInnerTilesTreeReduced10() {
		GPSNode head = new GPSNode();
		
		//System.out.println(DateTime.currentTime()+"Tessellation - Compute Approximate Inner Three");
		adjustCoordTo10x10();
		
		//Use to calculate the tile width and tile height as 1/tileSize
		int tileSize = 100;
		int startLat = (int)Utils.floor10(southWest.latitude*tileSize,1);
		int startLng = (int)Utils.floor10(southWest.longitude*tileSize,1);
		int stopLat  = (int)Utils.ceil10(northEst.latitude*tileSize,1);
		int stopLng  = (int)Utils.ceil10(northEst.longitude*tileSize,1);
		
	
		for (int i=startLat; i<stopLat; i+=10) {
			for (int j=startLng; j<stopLng; j+=10) {
				double x = (double)i/tileSize;
				double y = (double)j/tileSize;

				head.appendChild(new GPSPoint(Utils.floor10(x, 1), Utils.floor10(y, 1)), 1);
			}
		}

		return head;
	}
	
	public ArrayList<Rectangle> computeOptimalCoverTree(int maxTiles) {
		long maxTiles_100x100 = estimateTiles100Count();
		//System.out.println("Estimate 100x100 = " + maxTiles_100x100);
		if (maxTiles_100x100 > maxTiles) {
			// Tessellation exception only 100x100 are possible
			maxTiles = (int)maxTiles_100x100;
			return computeExternalTilesFromResolution(0);
		}
		
		adjustCoordTo1x1();
		GPSNode headNode;
		//System.out.println("***************** AreaRequested: " + computeArea()*100*100);
		long maxTiles_10x10 = estimateTiles10Count();
		//System.out.println("***************** AreaWithOnly10: " + 1.0*maxTiles_10x10*100);
		
		if ( (1.0*maxTiles_10x10*100)/(1.0*computeArea()*100*100) < 2 ) {
			// performance optimization, do not attempt when using 10x10 stretch is lower than 0.3  
			headNode = computeInnerTilesTreeReduced10();
			//System.out.println(DateTime.currentTime()+"Tessellation - using only 10x10, 100x100");
		} 
		else {
			headNode = computeInnerTilesTree();
		}
  		//headNode = computeInnerTilesTreeReduced10();
		//headNode = computeInnerTilesTree();
		//System.out.println("Three: " + headNode);
		
		/*
		//Checking for 100x100 tiles count
		if (headNode.getNodesCountAtLevel(0) > maxTiles) {
			//Returning all the 100x100 tiles is the best that we can do, so return
			return headNode.getNodesRectAtLevel(0);
		}
		*/
		
		while (headNode.getSize() > maxTiles) {
			//System.out.println("Total tiles: " + headNode.getSize());

			//System.out.println("100x100 Full Covered: " + headNode.getNodesCountFullCoveredAtLevel(0));
			//System.out.println("10x10   Full Covered: " + headNode.getNodesCountFullCoveredAtLevel(1));
			//System.out.println("1x1     Full Covered: " + headNode.getNodesCountFullCoveredAtLevel(2));
			//System.out.println(headNode);

			//Checking if the 10x10 tiles + 100x100 full covered exceeded max tiles
			if (headNode.getNodesCountAtLevel(1) + headNode.getNodesCountFullCoveredAtLevel(0) > maxTiles) {
				//System.out.println("Need To Aggregate To One 100x100");
				//Aggrego le 10x10 ad una 100x100 (scegliendo la 100x100 a copertura migliore)
				//System.out.println("1 - 10x10 nodes count: " + headNode.getNodesCountAtLevel(1));
				headNode.aggregateBestCoveredAtLevel(0);
				//System.out.println("2 - 10x10 nodes count: " + headNode.getNodesCountAtLevel(1) + "\n");
			}
			else {
				//System.out.println("Need To Aggregate To One 10x10");
				//Aggrego le 1x1 ad una 10x10 (scegliendo la 10x10 a copertura migliore)	
				//System.out.println("1 - 1x1 nodes count: " + headNode.getNodesCountAtLevel(2));
				headNode.aggregateBestCoveredAtLevel(1);
				//System.out.println("2 - 1x1 nodes count: " + headNode.getNodesCountAtLevel(2) + "\n");
			}		
		}
		
		return headNode.getLeafArrayList();
	}
	
	//Use intersect filter for obtain a mixed resolution tiles list
	public ArrayList<Rectangle> computeInnerTiles() {
    	//System.out.println("Inner Tiles Starting with coord: " + this.toString());
    	
    	ArrayList<Rectangle> all_tiles = new ArrayList<Rectangle>();
	
	    //Find 100x100 km tiles
    	ArrayList<Rectangle> tiles_100x100 = computeInternalTilesFromResolution(0);
	
    	//Find 10x10 km tails
    	ArrayList<Rectangle> tiles_10x10 = computeInternalTilesFromResolution(1);
	
    	//Find 1x1 km tails
    	ArrayList<Rectangle> tiles_1x1 = computeInternalTilesFromResolution(2);
	
	
		//System.out.println("Tiles_100x100 count: " + tiles_100x100.size());
		//System.out.println("Tiles_10x10   count: " + tiles_10x10.size());
		//System.out.println("Tiles_1x1     count: " + tiles_1x1.size());
		
		//Add the max size tiles
		all_tiles.addAll(tiles_100x100);
	
		//System.out.println("1 - All_tiles count: " + all_tiles.size());
		boolean intersect = false;
		for (int i = 0; i < tiles_10x10.size(); i++) {
			intersect = false;
			for (int j = 0; j < all_tiles.size(); j++) {
				if (Utils.intersects(tiles_10x10.get(i), all_tiles.get(j)) == true)
					intersect = true;
			}
			
			if (intersect == false)
				all_tiles.add(tiles_10x10.get(i));
		}

	
		//System.out.println("2 - All_tiles count: " + all_tiles.size());
		intersect = false;
		for (int i = 0; i < tiles_1x1.size(); i++) {
			intersect = false;
			for (int j = 0; j < all_tiles.size(); j++) {
				if (Utils.intersects(tiles_1x1.get(i), all_tiles.get(j)) == true)
					intersect = true;
			}
	
			if (intersect == false)
				all_tiles.add(tiles_1x1.get(i));
		}
	
		/* Debug
		double x = northEst.latitude;
		double y = northEst.longitude;
		double w = southWest.latitude;
		double h = southWest.longitude;
		Rectangle rect = new Rectangle(x, y, w, h);
		all_tiles.clear();
		all_tiles.add(rect);
		*/
		
		//All Tails
		//System.out.println("3 - All_tiles count: " + all_tiles.size());
		return all_tiles;
	}
    public ArrayList<Rectangle> computeInternalTilesFromResolution(int res) {
		ArrayList<Rectangle> tiles = new ArrayList<Rectangle>();
		
		double startLat = Utils.ceil10(southWest.latitude, res);
		double startLng = Utils.ceil10(southWest.longitude, res);
		double stopLat  = Utils.floor10(northEst.latitude, res);
		double stopLng  = Utils.floor10(northEst.longitude, res);
		
		//Use to calculate the tile width and tile height as 1/tileSize
		int tileSize = (int)Math.pow(10.0, (double)res);
		
		for (int i=(int)(startLat*tileSize); i<(int)(stopLat*tileSize); i+=1) {
			for (int j=(int)(startLng*tileSize); j<(int)(stopLng*tileSize); j+=1) {
				double x = (double)i/(double)tileSize;
				double y = (double)j/(double)tileSize;
				
				Rectangle rect = new Rectangle(x, y, (1.0/tileSize), (1.0/tileSize));
				tiles.add(rect);
			}
		}

		return tiles;
	}
    public ArrayList<Rectangle> computeExternalTilesFromResolution(int res) {
		ArrayList<Rectangle> tiles = new ArrayList<Rectangle>();
		
		Rectangle statingRect = new Rectangle(this);
		
		double startLat = Utils.floor10(southWest.latitude, res);
		double startLng = Utils.floor10(southWest.longitude, res);
		double stopLat  = Utils.ceil10(northEst.latitude, res);
		double stopLng  = Utils.ceil10(northEst.longitude, res);
		
		//Use to calculate the tile width and tile height as 1/tileSize
		int tileSize = (int)Math.pow(10.0, (double)res);
		
		for (int i=(int)(startLat*tileSize); i<(int)(stopLat*tileSize); i+=1) {
			for (int j=(int)(startLng*tileSize); j<(int)(stopLng*tileSize); j+=1) {
				double x = (double)i/tileSize;
				double y = (double)j/tileSize;
				
				Rectangle rect = new Rectangle(x, y, res);
				rect.computeIntersectionAreaPercentage(statingRect);
				tiles.add(rect);
			}
		}

		return tiles;
	}
    
    public long estimateTiles100Count() {    
    	GPSRect rect = this.clone();
    	rect.adjustCoordTo100x100();
    	
    	int width  = (int)(rect.northEst.latitude-rect.southWest.latitude);
    	int height = (int)(rect.northEst.longitude-rect.southWest.longitude);
    	
    	return width * height;
    }
    
    public long estimateTiles10Count() {    
    	GPSRect rect = this.clone();
    	rect.adjustCoordTo10x10();
    	
    	double width  = (rect.northEst.latitude-rect.southWest.latitude);
    	double height = (rect.northEst.longitude-rect.southWest.longitude);
    	
    	return (long)((width*10.0) * (height*10.0));
    }
    
    
    public int computeResolution() {
    	Rectangle rect = new Rectangle(this);
    	System.out.println(DateTime.currentTime()+"GPSRect: "+ rect + " Resolution: " + rect.computeResolution());
    	
    	return rect.computeResolution();
    }
    
    public double computeArea() {
    	double width  = Math.abs(northEst.latitude-southWest.latitude);
    	double height = Math.abs(northEst.longitude-southWest.longitude);
    	
    	return Utils.floor10(width * height, 2);
    }
    
    public GPSRect computeRelativeRect(float relValue) {
    	GPSPoint ne = new GPSPoint(Utils.ceil10(northEst.latitude+relValue, 2),   Utils.ceil10(northEst.longitude+relValue, 2));
    	GPSPoint sw = new GPSPoint(Utils.floor10(southWest.latitude+relValue, 2), Utils.floor10(southWest.longitude+relValue, 2));
    	
    	return new GPSRect(ne, sw);
    }
    
    @Override
    public GPSRect clone() {
    	return new GPSRect(northEst.clone(), southWest.clone());
    }

	@Override
	public String toString() {
		return "Coordinate {northEst=" + northEst + ", southWest=" + southWest + "}";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		
		result = prime * result + ((northEst == null) ? 0 : northEst.hashCode());
		result = prime * result + ((southWest == null) ? 0 : southWest.hashCode());
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GPSRect other = (GPSRect) obj;
		if (northEst == null) {
			if (other.northEst != null)
				return false;
		} else if (!northEst.equals(other.northEst))
			return false;
		if (southWest == null) {
			if (other.southWest != null)
				return false;
		} else if (!southWest.equals(other.southWest))
			return false;
		
		return true;
	}
}
