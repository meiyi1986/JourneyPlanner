package com.yimei.graph;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yimei.util.GeoCalculator;

public class Arc implements Serializable
{
	private static final Logger LOG = LoggerFactory.getLogger(Vertex.class);
    /**
     * Identifier of the arc. Negative means not set.
     */
    private int id;
    protected Vertex fromv;
    protected Vertex tov;
    private double distance; // the distance of this arc in kilometers (without considering the road network)

    public Arc(int id, Vertex v1, Vertex v2) {
        if (v1 == null || v2 == null) {
            String err = String.format("%s constructed with null vertex : %s %s", this.getClass(),
                    v1, v2);
            throw new IllegalStateException(err);
        }

        this.fromv = v1;
        this.tov = v2;
        this.id = id;

        // if (! vertexTypesValid()) {
        // throw new IllegalStateException(this.getClass() +
        // " constructed with bad vertex types");
        // }

        fromv.addOutgoing(this);
        tov.addIncoming(this);
        
        // calculate the distance
        distance = GeoCalculator.distance(v1.getLat(), v1.getLon(), v2.getLat(), v2.getLon(), 'K');
    }
    
    public int getId() {
    	return id;
    }

    public Vertex getFromVertex() {
        return fromv;
    }

    public Vertex getToVertex() {
        return tov;
    }
    
    public double getDistance() {
    	return distance;
    }
    
    /**
     * Returns true if it is partial - overriden by subclasses.
     */
    public boolean isPartial() {
        return false;
    }
    
    /**
     * Checks equivalency to another arc. Default implementation is trivial equality, but subclasses may want to do something more tricky.
     */
    public boolean isEquivalentTo(Arc e) {
        return this == e;
    }
    
    /**
     * Returns true if this arc is the reverse of another.
     */
    public boolean isReverseOf(Arc e) {
        return (this.getFromVertex() == e.getToVertex() &&
                this.getToVertex() == e.getFromVertex());
    }
    
    public void attachFrom(Vertex fromv) {
        detachFrom();
        if (fromv == null)
            throw new IllegalStateException("attaching to fromv null");
        this.fromv = fromv;
        fromv.addOutgoing(this);
    }

    public void attachTo(Vertex tov) {
        detachTo();
        if (tov == null)
            throw new IllegalStateException("attaching to tov null");
        this.tov = tov;
        tov.addIncoming(this);
    }

    /** Attach to new endpoint vertices, keeping arclists coherent */
    public void attach(Vertex fromv, Vertex tov) {
        attachFrom(fromv);
        attachTo(tov);
    }

    /**
     * Get a direction on paths where it matters, or null
     * 
     * @return
     */
    public String getDirection() {
        return null;
    }

    protected boolean detachFrom() {
        boolean detached = false;
        if (fromv != null) {
            detached = fromv.removeOutgoing(this);
            fromv = null;
        }
        return detached;
    }

    protected boolean detachTo() {
        boolean detached = false;
        if (tov != null) {
            detached = tov.removeIncoming(this);
            tov = null;
        }
        return detached;
    }

    /**
     * Disconnect from its endpoint vertices, keeping arclists coherent
     * 
     * @return
     */
    public int detach() {
        int nDetached = 0;
        if (detachFrom()) {
            ++nDetached;
        }
        if (detachTo()) {
            ++nDetached;
        }
        return nDetached;
    }

    /**
     * Arcs are not roundabouts by default.
     */
    public boolean isRoundabout() {
        return false;
    }
    
    public void setId(int id) {
    	this.id = id;
    }

    /* SERIALIZATION */

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // arc lists are transient, reconstruct them
        fromv.addOutgoing(this);
        tov.addIncoming(this);
    }

    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
        if (fromv == null) {
            System.out.printf("fromv null %s \n", this);
        }
        if (tov == null) {
            System.out.printf("tov null %s \n", this);
        }
        out.defaultWriteObject();
    }
}
