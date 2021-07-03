/*
 * Copyright (c) 2001-2003 Regents of the University of California.
 * All rights reserved.
 *
 * See the file LICENSE included in this distribution for details.
 */

package bamboo.softscribe;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedList;
import ostore.util.AssertionViolatedException;
import ostore.util.CountBuffer;
import ostore.util.InputBuffer;
import ostore.util.NodeId;
import ostore.util.OutputBuffer;
import ostore.util.QSException;
import ostore.util.QuickSerializable;
import bamboo.util.GuidTools;
import ostore.network.NetworkMessage;
import seda.sandStorm.api.QueueElementIF;

/**
 * MCastDown
 *
 * An event sent from/to another stage when it wants to multicast an object. 
 *  If INBOUND, event is headed to a higher-level app (i.e. being delivered)
 *  If OUTBOUND, event is headed to Scribe to be turned into an MCastDownMsg.
 *
 * @author  David Oppenheimer
 * @version $Id: MCastDown.java,v 1.3 2004/08/04 19:23:27 srhea Exp $
 */
public class MCastDown implements QueueElementIF, QuickSerializable {
    public boolean inbound;
    public BigInteger dstguid; //identifies group to which o should be multicast
    public QuickSerializable o;

    public MCastDown (boolean inbound, BigInteger dstguid, 
		      QuickSerializable o) {
	this.inbound = inbound;
	this.dstguid = dstguid;
	this.o = o;
    }

    public MCastDown (MCastDown other) {
	inbound = other.inbound;
	dstguid = other.dstguid;
	o = other.o;
    }

    public MCastDown (InputBuffer buffer) throws QSException {
        inbound = buffer.nextBoolean();
        dstguid = buffer.nextBigInteger();
        o = buffer.nextObject ();
    }

    public void serialize (OutputBuffer buffer) {
        buffer.add (inbound);
        buffer.add (dstguid);
        buffer.add (o);
    }


    public String toString () {
	return "(MCastDown  dstguid=" + GuidTools.guid_to_string (dstguid) +  
	    " inbound=" + inbound +
	    " o=" + o +
	    ")";
    }
}

