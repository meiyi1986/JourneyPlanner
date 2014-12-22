package com.yimei.graph;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yimei.util.GeoCalculator;

/***
 * Each vertex can be a transfer or route vertex. A station consists of one transfer vertex and a number of route vertices.
 * For the same station, the cost from any route vertex to the transfer vertex takes the transfer time, while the cost from
 * the transfer vertex to any route vertex is zero.
 * @author e04499
 *
 */

public class Vertex
{
	private transient static final Logger LOG = LoggerFactory.getLogger(Vertex.class);
    private int index; // the unique index
    private double lat; // latitude
    private double lon; // longitude
    protected transient Arc[] incoming;
    protected transient Arc[] outgoing;
    
    /* PUBLIC CONSTRUCTORS */
    public Vertex(int index, double lat, double lon) {
        this.index = index;
        this.lat = lat;
        this.lon = lon;
        this.incoming = new Arc[0];
        this.outgoing = new Arc[0];
    }

    /* PUBLIC METHODS */

    public int getId() {
        return index;
    }

    public double getLat() {
    	return lat;
    }
    
    public double getLon() {
    	return lon;
    }
    
    public static double distanceBetween(Vertex v1, Vertex v2) {
    	return GeoCalculator.distance(v1.getLat(), v1.getLon(), v2.getLat(), v2.getLon(), 'K');
    }

    /* Arc UTILITY METHODS (use arrays to eliminate copy-on-write set objects) */

    /**
     * Get a collection containing all the arcs leading from this vertex to other vertices.
     * There is probably some overhead to creating the wrapper ArrayList objects, but this
     * allows filtering and combining arc lists using stock Collection-based methods.
     */
    public Collection<Arc> getOutgoing() {
        return Arrays.asList(outgoing);
    }

    /** Get a collection containing all the arcs leading from other vertices to this vertex. */
    public Collection<Arc> getIncoming() {
        return Arrays.asList(incoming);
    }

    /**
     * A static helper method to avoid repeated code for outgoing and incoming lists.
     * Synchronization must be handled by the caller, to avoid passing arc array pointers that may be invalidated.
     */
    private static Arc[] addArc(Arc[] existing, Arc e) {
        Arc[] copy = new Arc[existing.length + 1];
        int i;
        for (i = 0; i < existing.length; i++) {
            if (existing[i] == e) {
                LOG.error("repeatedly added Arc {}", e);
                return existing;
            }
            copy[i] = existing[i];
        }
        copy[i] = e; // append the new arc to the copy of the existing array
        return copy;
    }

    /**
     * A static helper method to avoid repeated code for outgoing and incoming lists.
     * Synchronization must be handled by the caller, to avoid passing arc array pointers that may be invalidated.
     */
    public static Arc[] removeArc(Arc[] existing, Arc e) {
        int nfound = 0;
        for (int i = 0; i < existing.length; i++) {
            if (existing[i] == e) nfound++;
        }
        if (nfound == 0) {
            LOG.error("Requested removal of an arc which isn't connected to this vertex.");
            return existing;
        }
        if (nfound > 1) {
            LOG.error("There are multiple copies of the arc to be removed.)");
        }
        Arc[] copy = new Arc[existing.length - nfound];
        for (int i = 0, j = 0; i < existing.length; i++) {
            if (existing[i] != e) copy[j++] = existing[i];
        }
        return copy;
    }

    /* FIELD ACCESSOR METHODS : READ/WRITE */

    public void addOutgoing(Arc arc) {
        synchronized (this) {
            outgoing = addArc(outgoing, arc);
        }
    }

    /** @return whether the arc was found and removed. */
    public boolean removeOutgoing(Arc arc) {
        synchronized (this) {
            int n = outgoing.length;
            outgoing = removeArc(outgoing, arc);
            return (outgoing.length < n);
        }
    }


    public void addIncoming(Arc arc) {
        synchronized (this) {
            incoming = addArc(incoming, arc);
        }
    }

    /** @return whether the arc was found and removed. */
    public boolean removeIncoming(Arc arc) {
        synchronized (this) {
            int n = incoming.length;
            incoming = removeArc(incoming, arc);
            return (incoming.length < n);
        }
    }

    public int getDegreeOut() {
        return outgoing.length;
    }

    public int getDegreeIn() {
        return incoming.length;
    }

    /* FIELD ACCESSOR METHODS : READ ONLY */

    public void setIndex(int index) {
        this.index = index;
    }

    /* SERIALIZATION METHODS */

    private void writeObject(ObjectOutputStream out) throws IOException {
        // arc lists are transient
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    /**
     * Clear this vertex's outgoing and incoming arc lists, and remove all the arcs
     * they contained from this vertex's neighbors.
     */
    public void removeAllArcs() {
        for (Arc e : outgoing) {
            Vertex target = e.getToVertex();
            if (target != null) {
                target.removeIncoming(e);
            }
        }
        for (Arc e : incoming) {
            Vertex source = e.getFromVertex();
            if (source != null) {
                source.removeOutgoing(e);
            }
        }
        incoming = new Arc[0];
        outgoing = new Arc[0];
    }
}