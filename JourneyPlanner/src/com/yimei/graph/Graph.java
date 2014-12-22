package com.yimei.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;

import com.google.common.collect.Maps;

public class Graph
{
	public String feedLabel;
	
	private Map<Integer, Vertex> vertices; // the key is the vertex index
	private transient Map<Tuple2<Vertex, Vertex>, Arc> arcs; // the arc of each tuple <fromV, toV>
    private int numOfVertices;
    private int numOfArcs;
    
    /* constructor */
    
    public Graph() { // empty graph
        this.vertices = Maps.newHashMap();
        arcs = Maps.newHashMap();
        numOfVertices = 0;
        numOfArcs = 0;
    }
    
    public Graph(Collection<Vertex> vs) { // initialize with vertices
    	vertices = Maps.newHashMap();
    	arcs = Maps.newHashMap();
    	for (Vertex v : vs) {
    		addVertex(v);
    	}
    	numOfVertices = vs.size();
    	numOfArcs = 0;
    }
    
    public Graph(Collection<Vertex> vs, Collection<Arc> as) { // initialize with both vertices and arcs
    	vertices = Maps.newHashMap();
    	arcs = Maps.newHashMap();
    	for (Vertex v : vs) {
    		addVertex(v);
    	}
    	numOfVertices = vs.size();
    	
    	for (Arc a : as) {
    		if (!containsArc(a.getFromVertex(), a.getToVertex())) {
    			addArc(a);
    		}
    	}
    	numOfArcs = as.size();
    }
    
    /* methods */
    
    public Map<Integer, Vertex> getVertices() {
        return vertices;
    }
    
    public Vertex getVertex(int index) {
    	return vertices.get(index);
    }
    
    public int getNumOfVertices() {
    	return numOfVertices;
    }
    
    public int getNumOfArcs() {
    	return numOfArcs;
    }
    
    public void addVertex(Vertex v) {
    	vertices.put(v.getId(), v);
    	numOfVertices ++;
    }
    
    public void addArc(Arc a) {
    	if (!vertices.containsValue(a.getFromVertex())) { // fromv is not in the graph
    		addVertex(a.getFromVertex());
    	}
    	
    	if (!vertices.containsValue(a.getToVertex())) { // tov is not in the graph
    		addVertex(a.getToVertex());
    	}
    	
    	a.setId(numOfArcs);
    	a.attach(a.getFromVertex(), a.getToVertex());
    	arcs.put(new Fun.Tuple2<Vertex,Vertex>(a.getFromVertex(), a.getToVertex()), a);
    	numOfArcs ++;
    }
    
    public Arc getVerticesArc(Vertex fromV, Vertex toV) {
    	return arcs.get(new Fun.Tuple2<Vertex,Vertex>(fromV, toV));
    }
    
    public boolean containsVertex(Vertex v) {
    	return vertices.containsValue(v);
    }
    
    public boolean containsArc(Vertex fromV, Vertex toV) {
    	return (getVerticesArc(fromV, toV) != null);
    }
}
