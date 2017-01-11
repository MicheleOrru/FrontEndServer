package com.ogb.fes.entity;


import java.util.ArrayList;
import java.util.HashMap;

import com.ogb.fes.utils.Rectangle;
import com.ogb.fes.utils.Utils;


/***
 * size:             Numero di nodi foglia partendo da me come nodo root
 * coveredNodeCount: Numero di nodi 1x1 che ricopro
***/
public class GPSNode {
	public final int MAX_CHILD_NODE = 100;
	public final int MAX_TREE_DEEP  = 2;
	
	public  GPSNode                  father;
	public  HashMap<String, GPSNode> childs;
	public  int                      currentLevel;
	private int                      size;
	private int                      coveredNodeCount;
	public  Rectangle                coveredArea;
	
	
	public GPSNode() {
		
		this.childs       = new HashMap<String, GPSNode>();
		this.currentLevel = -1;
		this.coveredArea  = null;
		this.father       = null;
		
		this.size             = 0;
		this.coveredNodeCount = 0;
	}
	
	public GPSNode(GPSNode father, GPSPoint point, int level) {
		this.childs       = new HashMap<String, GPSNode>();
		this.currentLevel = level;
		this.coveredArea  = new Rectangle(point, level);
		this.father       = father;
		
		this.size              = 0;
		this.coveredNodeCount  = 0;
	}
	
	
	public synchronized void appendChild(GPSPoint point, int level) {
		String key = Utils.gpsPointToNDNNname(point, false, false, currentLevel+1);
		//System.out.println("Append Child " + key + " at level " + level +"\n");
		GPSNode node = childs.get(key);
		if (node == null) {
			node = new GPSNode(this, point, currentLevel+1);
			childs.put(key, node);
			size++;
			coveredNodeCount++;
			
			if (level > (currentLevel+1)) {
				node.appendChild(point, level);
			}
		}
		else {
			if (level > (currentLevel+1)) {
				node.appendChild(point, level);
				size++;
				coveredNodeCount++;
			}
		}
		
		//Aggrego solo se non sono un nodo root (ovvero quello che contiene nodi 100x100)
		if (currentLevel > -1) {
			aggregateOwnChildIfNeeded();
		}
	}

	public synchronized void aggregateOwnChildIfNeeded() {
		
		//Se il numero totale di nodi sottostanti è il massimo che posso contenere li aggrego
		if (coveredNodeCount >= Math.pow(MAX_CHILD_NODE, MAX_TREE_DEEP-currentLevel) ) { 
			decreseSize(childs.size()-1);
			childs = new HashMap<String, GPSNode>();
		}
	}
	
	public synchronized void removeOwnChild(GPSPoint point, int level) {
		//System.out.println("Point to remove: " + point + " at level " + level + " from currentLevel " + currentLevel);
		if (level >= MAX_TREE_DEEP)
			return;
		
		if (level > currentLevel){
			String  key  = Utils.gpsPointToNDNNname(point, false, false, currentLevel+1);
			GPSNode node = childs.get(key);
			if (node != null) {
				node.removeOwnChild(point, level);
			}
		}
		else {
			//System.out.println("Point to remove: " + point + " at level " + level);
			if (childs.size() > 0) {
				decreseSize(getSize()-1);
				childs = new HashMap<String, GPSNode>();
				coveredNodeCount = (int)Math.pow(MAX_CHILD_NODE, 2 - currentLevel);
			}
		}	
	}
	
	public synchronized void decreseSize(int value) {
		size = size - value;
		
		if (father != null)
			father.decreseSize(value);
	}
	
	public synchronized int getSize() {
		return (size <= 0 ? 1 : size);
	}
	
	
	public Rectangle getCoveredArea() {
		if (currentLevel == MAX_TREE_DEEP)
			coveredArea.cover = 1.0;
		else
			coveredArea.cover = coveredNodeCount/(coveredArea.width*100*coveredArea.width*100);
		
		return coveredArea;
	}
	
	public ArrayList<Rectangle> getLeafArrayList() {
		ArrayList<Rectangle> res = new ArrayList<Rectangle>();
		
		if (childs.size() <= 0)
			res.add(getCoveredArea());
		else
		{
			for (GPSNode node : childs.values()) {
				res.addAll(node.getLeafArrayList());
			}
		}
		
		return res;
	}
	

	public int getNodesCountAtLevel(int level) {

		level++;
		switch (level) {
			case 0: //Tiles Root
			case 1: //Tiles 100x100
				return childs.size();
			case 2: //Tiles 10x10
			{
				int count = 0;
				for (GPSNode node : childs.values())
					count += node.childs.size();
				return count;
			}	
			case 3: //Tiles 1x1
			{
				int count = 0;
				for (GPSNode node : childs.values())
					for (GPSNode child : node.childs.values())
						count += child.childs.size();
				return count;
			}	
		}
		
		return 1;
	}
	
	public int getNodesCountFullCoveredAtLevel(int level) {
		
		level++;
		switch (level) {
			case 0: //Tiles Root
			case 1: //Tiles 100x100 
			{
				int count = 0;
				for (GPSNode child : childs.values()) {
					Rectangle rect = child.getCoveredArea();
					if (rect.cover == 1.0)
						count++;
				}
				return count;
			}
			case 2: //Tiles 10x10
			{
				int count = 0;
				for (GPSNode node : childs.values()) {
					for (GPSNode child : node.childs.values()) {
						Rectangle rect = child.getCoveredArea();
						if (rect.cover == 1.0)
							count++;
					}
				}
				return count;
			}	
			case 3: //Tiles 1x1
			{
				int count = 0;
				for (GPSNode node : childs.values()) {
					for (GPSNode child : node.childs.values()) {
						for (GPSNode leaf : child.childs.values()) {
							Rectangle rect = leaf.getCoveredArea();
							if (rect.cover == 1.0)
								count++;
						}
					}
				}
				return count;
			}	
		}
		
		return 1;
	}
	
	public ArrayList<Rectangle> getNodesRectAtLevel(int level) {
		ArrayList<Rectangle> resList = new ArrayList<Rectangle>();
		
		level++;
		switch (level) {
			case 0: //Tiles Root
			case 1: //Tiles 100x100 
			{
				for (GPSNode child : childs.values()) {
					Rectangle rect = child.getCoveredArea();
					resList.add(rect);
				}
				return resList;
			}
			case 2: //Tiles 10x10
			{
				for (GPSNode node : childs.values()) {
					for (GPSNode child : node.childs.values()) {
						Rectangle rect = child.getCoveredArea();
						resList.add(rect);
					}
				}
				return resList;
			}	
			case 3: //Tiles 1x1
			{
				for (GPSNode node : childs.values()) {
					for (GPSNode child : node.childs.values()) {
						for (GPSNode leaf : child.childs.values()) {
							Rectangle rect = leaf.getCoveredArea();
							resList.add(rect);
						}
					}
				}
				return resList;
			}	
		}
		
		return resList;
	}
	
	public ArrayList<Rectangle> getNodesRectPartialCoveredAtLevel(int level) {
		ArrayList<Rectangle> resList = new ArrayList<Rectangle>();
		
		level++;
		switch (level) {
			case 0: //Tiles Root
			case 1: //Tiles 100x100 
			{
				for (GPSNode node : childs.values()) {
					Rectangle rect = node.getCoveredArea();
					if (rect.cover < 1.0)
						resList.add(rect);
				}
				return resList;
			}
			case 2: //Tiles 10x10
			{
				for (GPSNode node : childs.values()) {
					for (GPSNode child : node.childs.values()) {
						Rectangle rect = child.getCoveredArea();
						if (rect.cover < 1.0)
							resList.add(rect);
					}
				}
				return resList;
			}	
			case 3: //Tiles 1x1
			{
				for (GPSNode node : childs.values()) {
					for (GPSNode child : node.childs.values()) {
						for (GPSNode leaf : child.childs.values()) {
							Rectangle rect = leaf.getCoveredArea();
							if (rect.cover < 1.0)
								resList.add(rect);
						}
					}
				}
				return resList;
			}	
		}
		
		return resList;
	}
	
	public void aggregateBestCoveredAtLevel(int level) {
		//Seleziono la lista dei nodi parzialmenti coperti
		ArrayList<Rectangle> nodes = getNodesRectPartialCoveredAtLevel(level);
		//Li ordino in base all'area che ricoprono 
		nodes.sort(Rectangle.comparator());
		//Scelgo quello che copre l'area maggiore
		Rectangle rect = nodes.get(0);
		
		//System.out.println("Aggregate node: " + rect + " at level " + level);
		removeOwnChild(new GPSPoint(rect.x, rect.y), level);
	}
	
	public void aggregateNodeAtLevel(int nodeIdx, int level) {
		ArrayList<Rectangle> list = getNodesRectAtLevel(level);
		if (nodeIdx < list.size()) {
			Rectangle rect = list.get(nodeIdx);
			removeOwnChild(new GPSPoint(rect.x, rect.y), level);
		}
	}
	
	public void printThreeAtMaxLevel(int maxLevel) {
		String treeString = "";
	
		if (maxLevel >= 2)
		{
			for (GPSNode node : childs.values()) {
				treeString += node.coveredArea.toString() + "\n";
				for (GPSNode child : node.childs.values()) {
					treeString += "\t"+ child.coveredArea.toString() + "\n";
					for (GPSNode leaf : child.childs.values()) {
						treeString += "\t\t"+ leaf.coveredArea.toString() + "\n";
					 }
				}
			}
		}
		else if (maxLevel == 1)
		{
			for (GPSNode node : childs.values()) {
				treeString += node.coveredArea.toString() + "\n";
				for (GPSNode child : node.childs.values()) {
					treeString += "\t"+ child.coveredArea.toString() + "\n";
				}
			}
		}
		else if (maxLevel == 0)
		{
			for (GPSNode node : childs.values()) {
				treeString += node.coveredArea.toString() + "\n";
			}
		}
		
		System.out.println(treeString);
	}
	
	public ArrayList<Rectangle> aggregateThree(int maxTiles) {
		
		long tiles_100x100 = getNodesCountFullCoveredAtLevel(0);
		if (tiles_100x100 > maxTiles) {
			return getNodesRectAtLevel(0);
		}

		long tiles_10x10 = getNodesCountFullCoveredAtLevel(1);
		if (tiles_10x10 > maxTiles) {
			return getNodesRectAtLevel(1);
		} 
			
		while (getSize() > maxTiles) {
			if (getNodesCountAtLevel(1) + getNodesCountFullCoveredAtLevel(0) > maxTiles) {
				aggregateBestCoveredAtLevel(0);
			}
			else {
				aggregateBestCoveredAtLevel(1);
			}		
		}
		
		return getLeafArrayList();
	}
	
	
	@Override
	public String toString() {
		String treeString = "";
		for (GPSNode node : childs.values()) {
			treeString += node.coveredArea.toString() + "\n";
			for (GPSNode child : node.childs.values()) {
				treeString += "\t"+ child.coveredArea.toString() + "\n";
				for (GPSNode leaf : child.childs.values()) {
					treeString += "\t\t"+ leaf.coveredArea.toString() + "\n";
				}
			}
		}
		
		return treeString;
	}

}
