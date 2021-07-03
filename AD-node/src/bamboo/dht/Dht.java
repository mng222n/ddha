/*
 * Copyright (c) 2001-2003 Regents of the University of California.
 * All rights reserved.
 *
 * See the file LICENSE included in this distribution for details.
 */

package bamboo.dht;
import bamboo.api.BambooLeafSetChanged;
import bamboo.api.BambooNeighborInfo;
import bamboo.api.BambooRouteContinue;
import bamboo.api.BambooRouteDeliver;
import bamboo.api.BambooRouteInit;
import bamboo.api.BambooRouteUpcall;
import bamboo.api.BambooRouterAppRegReq;
import bamboo.api.BambooRouterAppRegResp;
import bamboo.db.StorageManager;
import bamboo.dmgr.DataManager;
import bamboo.dmgr.PutOrRemoveReq;
import bamboo.dmgr.PutOrRemoveResp;
import bamboo.lss.ASyncCore;
import bamboo.lss.DuplicateTypeException;
import bamboo.lss.Network;
import bamboo.lss.PriorityQueue;
import bamboo.lss.Rpc;
import bamboo.router.NeighborInfo;
import bamboo.router.RouteMsg;
import bamboo.router.Router;
import bamboo.util.GuidTools;
import bamboo.util.Pair;
import bamboo.util.StringUtil;
import bamboo.vivaldi.VirtualCoordinate;
import bamboo.vivaldi.Vivaldi;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import ostore.network.NetworkMessage;
import ostore.util.CountBuffer;
import ostore.util.InputBuffer;
import ostore.util.NodeId;
import ostore.util.OutputBuffer;
import ostore.util.QSException;
import ostore.util.QuickSerializable;
import ostore.util.QSBool;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.SingleThreadedEventHandlerIF;
import seda.sandStorm.api.SinkIF;
import static bamboo.db.StorageManager.GetByGuidReq;
import static bamboo.db.StorageManager.Key;
import static bamboo.db.StorageManager.ZERO_HASH;
import static bamboo.db.StorageManager.ZERO_KEY;
import static bamboo.util.Curry.*;
import static bamboo.util.GuidTools.guid_to_string;
import static bamboo.util.StringUtil.addr_to_sbuf;
import static java.lang.Math.ceil;
import static java.lang.Math.exp;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static bamboo.lss.UdpCC.ByteCount;

/**
 * Distributed hash table layer for Bamboo.
 *
 * @author Sean C. Rhea
 * @version $Id: Dht.java,v 1.75 2005/12/14 19:39:09 srhea Exp $
 */
public class Dht extends bamboo.util.StandardStage
implements SingleThreadedEventHandlerIF, StorageManager.StorageMonitor  {

    static final int QUORUM_MASK    = 0x3 << 29;
    static final int ROOT_ONLY      = 0x0 << 29;
    static final int FIRST_ONLY     = 0x1 << 29;
    static final int QUORUM         = 0x2 << 29;
    static final int QUORUM_SYNC    = 0x3 << 29;
 
    static final int PROXIMITY_MASK = 0x3 << 27;
    static final int PNS_ONLY       = 0x0 << 27;
    static final int PRS            = 0x1 << 27;
    static final int SCALED_PRS     = 0x2 << 27;
    static final int SCALED_PRS_FD  = 0x3 << 27;

    static final int ITERATIVE_MASK = 0x1 << 26;
    static final int RECURSIVE      = 0x0 << 26;
    static final int ITERATIVE      = 0x1 << 26;

    static final int ITERATIVE_PARALLELISM_MASK = 0x3 << 24;
    static final int ITERATIVE_PARALLELISM_SHIFT = 24;

    static final int NEW_RECUR_MASK = 0x1 << 23;

    static final int MAXVALS_MASK   = 0x0000ffff;

    protected static int calcMaxGetRespSize() {
        GetValue gv = new GetValue(ByteBuffer.allocate(1024), 0, new byte[20]);
        CountBuffer cb = new CountBuffer();
        gv.serialize(cb);
        return cb.size();
    }
    protected static final int MAX_GET_RESP_SIZE = calcMaxGetRespSize();

    private static final boolean sendGetValues = true;

    protected Rpc rpc;
    protected Network network;
    protected ReturnToClient returnToClient;

    protected boolean initialized;
    protected LinkedList wait_q = new LinkedList();

    protected static final long app_id =
        bamboo.router.Router.applicationID(Dht.class);

    protected boolean iterative_routing;
    protected Random rand;

    protected long next_put_seq, next_get_seq;

    protected long next_put_seq () {
        long result = next_put_seq;
        if (next_put_seq == Long.MAX_VALUE)
            next_put_seq = 0;
        else 
            next_put_seq += 1;
        return result;
    }

    protected long next_get_seq () {
        long result = next_get_seq;
        if (next_get_seq == Long.MAX_VALUE)
            next_get_seq = 0;
        else 
            next_get_seq += 1;
        return result;
    }

    protected Map active_puts = new HashMap();
    protected Map<Long,Pair<GetReq,IterGetState>> active_gets = 
        new LinkedHashMap<Long,Pair<GetReq,IterGetState>>();
    protected Map forwarded_gets = new HashMap();

    protected int ttl_to_vdisk(int ttl_sec) {
        assert ttl_sec <= ttl_sec_ranges[ttl_sec_ranges.length - 1];
        int i = 0;
        while (ttl_sec > ttl_sec_ranges[i]) {
            ++i;
        }
        return i;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Per-client storage usage tracking
    //
    //////////////////////////////////////////////////////////////////////////

    public static int MAX_TTL_SEC = 3600 * 24 * 7;

    protected int[] ttl_sec_ranges = {
        3600, 3600 * 24, 3600 * 24 * 7};
    
    protected Map client_to_usage = new HashMap();
    protected long total_usage;

    public void storage_changed(boolean added,
                                InetAddress client_id, long size) {
        Long usage = (Long) client_to_usage.get(client_id);
        long new_usage = -1;
        if (added) {
            total_usage += size;
            if (usage == null) usage = new Long(0);
            new_usage = usage.longValue() + size;
            client_to_usage.put(client_id, new Long(new_usage));
        }
        else {
            total_usage -= size;
            assert total_usage >= 0;
            assert usage != null;
            new_usage = usage.longValue() - size;
            assert new_usage >= 0;
            if (new_usage == 0)
                client_to_usage.remove(client_id);
            else
                client_to_usage.put(client_id, new Long(new_usage));
        }
        if (logger.isInfoEnabled()) {
            StringBuffer buf = new StringBuffer(100);
            buf.append("client ");
            buf.append(client_id.getHostAddress());
            buf.append(" usage now ");
            StringUtil.byte_cnt_to_sbuf(new_usage, buf);
            buf.append(" of ");
            StringUtil.byte_cnt_to_sbuf(total_usage, buf);
            buf.append(" total.");
            logger.info(buf.toString());
        }
    }

    protected boolean below_fair_share(InetAddress client,
                                       int size, int ttl_sec) {
        int i = ttl_to_vdisk(ttl_sec);
        long current_usage = slop_usage (i, client);
        int client_count = client_to_usage.size();
        {
            Long l = (Long) client_to_usage.get(client);
            if (l != null) 
                current_usage += l.longValue ();
            else
                client_count += 1;
        }
        
        if (logger.isDebugEnabled ())
            logger.debug ("below_fair_share " + client.getHostAddress ());

        if (current_usage == 0) {
            if (logger.isDebugEnabled())
                logger.debug("fair share: no existing usage by this client");
            return true;
        }

        long new_client = current_usage + size;

        int k = 0; 
        int n = 1;
        long t = new_client;

        PriorityQueue pq = new PriorityQueue (n);
        pq.add (client, new_client);
        if (logger.isDebugEnabled())
            logger.debug("adding " + new_client);
        Iterator j = client_to_usage.keySet ().iterator ();
        while (j.hasNext ()) {
            InetAddress c = (InetAddress) j.next ();
            if (! c.equals (client)) {
                Long l = (Long) client_to_usage.get (c);
                long v = l.longValue () + slop_usage (i, c);
                pq.add (c, v);
                if (logger.isDebugEnabled())
                    logger.debug("adding " + l.longValue());
                n += 1;
                t += v;
            }
        }
        assert n == client_count;

        while (n > 0) {
            double nth = ((double) t) / n;
            if (logger.isDebugEnabled())
                logger.debug("n="+n+" t="+t+" k="+k+" nc="+new_client
                             +" nth="+nth);

            if (new_client <= nth) {
                System.out.println ("slop avail=" +
                        (slop_cap [i] - slop_size [i]));
                return slop_cap [i] - slop_size [i] >= k * 1024;
            }
            boolean once = false;
            while ((! pq.isEmpty ()) && (pq.getFirstPriority () < nth)) {
                if (logger.isDebugEnabled())
                    logger.debug("removing " + pq.getFirstPriority ());
                t -= pq.getFirstPriority ();
                n -= 1;
                assert t >= 0;
                assert n >= 0;
                pq.removeFirst ();
                once = true;
            }
            if (! once)
                break;
            k += 1;
        }

        return false;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Overall storage rate limiting
    //
    //////////////////////////////////////////////////////////////////////////

    /**
     * One token per byte.  Filled according to <code>fill_rates</code>, below.
     */
    protected double[] avail_tokens = {
        0.0, 0.0, 0.0};

    /**
     * Capacity limits for the token buckets.
     */
    protected double[] max_tokens = {
        1024.0 * 1024.0, 1024.0 * 1024.0, 1024.0 * 1024.0
        // 1024.0, 1024.0, 1024.0
    };

    /**
     * Fill rates in bytes per millisecond, only set by compute_rates ().
     */
    protected double[] fill_rates = {
        0.0, 0.0, 0.0};

    /**
     * The sizes of our virtual disks, in bytes.  The defaults work out to 
     * 2 kB per second per node with 8 replicas (leaf_set_size = 4).
     */
    protected double[] disk_sizes = {
        // 1024.0 * 1024.0,
        2.0 * 1024.0 * 8.0 * 60.0 * 60.0,
        2.0 * 1024.0 * 8.0 * 60.0 * 60.0 * 24.0,
        2.0 * 1024.0 * 8.0 * 60.0 * 60.0 * 24.0 * 7.0,
    };

    /**
     * Keep track of the disk sizes of our leaf set, and scale put rates
     * based on them.
     */
    protected Map ls_disk_sizes[] = {
        new HashMap(), new HashMap(), new HashMap()
    };

    /**
     * The last time we added any tokens to each bucket.
     */
    protected long[] last_fill_time = {
        0, 0, 0, 0};

    protected int min_replica_count;

    protected void compute_rates() {
        if (ls_disk_sizes[0].isEmpty()) {
            for (int i = 0; i < fill_rates.length; ++i) {
                fill_rates[i] = disk_sizes[i] / ttl_sec_ranges[i] / 1000.0;
                if (logger.isInfoEnabled()) {
                    StringBuffer sbuf = new StringBuffer(100);
                    sbuf.append("ttl=");
                    StringUtil.time_to_sbuf(ttl_sec_ranges[i], sbuf);
                    sbuf.append(", disk size=");
                    StringUtil.byte_cnt_to_sbuf(
                            Math.round(disk_sizes[i]), sbuf);
                    sbuf.append(", rate=");
                    StringUtil.byte_cnt_to_sbuf(
                            Math.round(fill_rates[i] * 1000.0), sbuf);
                    sbuf.append("/s");
                    logger.info(sbuf);
                }
            }
        }
        else {
            for (int i = 0; i < fill_rates.length; ++i) {

                /**
                 * Tuples that preceed our id get replicated on all of our
                 * predecessors, us, and all but the farthest of our successors.
                 * Likewise, tuples that succeed our id get replicated on all
                 * of our successors, us, and all but one of our predecessors.
                 * We thus have different replica sets depending on whether a
                 * tuple preceeds or succeeds us, and we have corresponding
                 * rates for each case.  To simplify things, we just pick the
                 * slower of the two rates by limiting to the
                 * min_replica_count + 1 largest disks.
                 */
                PriorityQueue ds =
                    new PriorityQueue(succs.length + preds.length + 1);
                for (Object peer : ls_disk_sizes[i].keySet()) {
                    Double size = (Double) ls_disk_sizes[i].get(peer);
                    ds.add(peer, Math.round(size.doubleValue()));
                }
                ds.add(my_node_id, Math.round(disk_sizes[i]));
                for (int k = 0; k < ds.size() - min_replica_count - 1; ++k)
                    ds.removeFirst();
                double min_size = (double) ds.getFirstPriority();

                fill_rates[i] = min_size /
                    ls_disk_sizes[i].size() /
                    ttl_sec_ranges[i] / 1000.0;

                if (logger.isInfoEnabled()) {
                    StringBuffer sbuf = new StringBuffer(100);
                    sbuf.append("ttl=");
                    StringUtil.time_to_sbuf(ttl_sec_ranges[i], sbuf);
                    sbuf.append(", min size=");
                    StringUtil.byte_cnt_to_sbuf(Math.round(min_size), sbuf);
                    sbuf.append(", ls size=");
                    sbuf.append(ds.size());
                    sbuf.append(", rate=");
                    StringUtil.byte_cnt_to_sbuf(
                            Math.round(fill_rates[i] * 1000.0), sbuf);
                    sbuf.append("/s, limited by ");
                    NodeId limit = (NodeId) ds.getFirst ();
                    sbuf.append(limit.address ().getHostAddress ());
                    sbuf.append(":");
                    sbuf.append(limit.port ());
                    logger.info(sbuf);
                }
            }
        }
    }

    protected BambooNeighborInfo[] preds;
    protected BambooNeighborInfo[] succs;
    protected int leaf_set_size;
    protected static final BigInteger MIN_GUID = BigInteger.valueOf(0);
    protected BigInteger MAX_GUID;
    protected static final BigInteger NEG_ONE =
        BigInteger.ZERO.subtract(BigInteger.ONE);
    protected DataManager dmgr;
    protected Router router;
    protected Vivaldi vivaldi;

    protected void handle_leaf_set_changed(BambooLeafSetChanged msg) {

        if (msg.preds.length == 0) {

            // We can only have no predecessors if we are the only node in
            // the network.  Go back to our original state.

            preds = null;
            succs = null;
            leaf_set_size = 0;
            return;
        }

        // Update our responsibilities and leaf set.

        preds = msg.preds;
        succs = msg.succs;

        Set<NodeId> unique = new LinkedHashSet<NodeId> ();
        for (int i = 0; i < msg.preds.length; ++i)
            unique.add (msg.preds[i].node_id);
        for (int i = 0; i < msg.succs.length; ++i)
            unique.add (msg.succs[i].node_id);
        leaf_set_size = unique.size ();

        Map[] new_ds = new HashMap[ls_disk_sizes.length];
        for (int i = 0; i < ls_disk_sizes.length; ++i) {
            new_ds[i] = new HashMap();
        }

        for (int i = 0; i < msg.preds.length; ++i) {
            for (int j = 0; j < ls_disk_sizes.length; ++j) {
                Double sz = (Double)
                    ls_disk_sizes[j].get(msg.preds[i].node_id);
                if (sz == null) sz = new Double(0);
                new_ds[j].put(msg.preds[i].node_id, sz);
            }
        }
        for (int i = 0; i < msg.succs.length; ++i) {
            for (int j = 0; j < ls_disk_sizes.length; ++j) {
                Double sz = (Double)
                    ls_disk_sizes[j].get(msg.succs[i].node_id);
                if (sz == null) sz = new Double(0);
                new_ds[j].put(msg.succs[i].node_id, sz);
            }
        }

        ls_disk_sizes = new_ds;

        compute_rates();
    }

    protected void sendDiskSizeMsg(InetSocketAddress peer, boolean reply) {
        network.send(new DiskSizeMsg(ttl_sec_ranges, disk_sizes, reply), peer);
    }

    protected Runnable swap_disk_sizes_cb = new Runnable() {
        public void run() {
            if (ls_disk_sizes[0].size() > 0) {
                int which = rand.nextInt(ls_disk_sizes[0].size());
                Iterator i = ls_disk_sizes[0].keySet().iterator();
                while (which-- > 0) i.next();
                sendDiskSizeMsg((NodeId) i.next(), true);
            }
            acore.registerTimer(rand.nextInt(4000) + 2000, this);
        }
    };

    protected Thunk2<DiskSizeMsg,InetSocketAddress> handleDiskSizeMsg = 
        new Thunk2<DiskSizeMsg,InetSocketAddress>() {

        public void run(DiskSizeMsg msg, InetSocketAddress peer) {
            boolean changed = false;
            for (int i = 0; i < ttl_sec_ranges.length; ++i) {
                if (i < msg.ttls.length && (msg.ttls[i] == ttl_sec_ranges[i])) {
                    Double old_size = (Double) ls_disk_sizes[i].get(peer);
                    if (old_size == null) {
                        // They think we're in their leaf set, but we don't
                        // think so.  This could be a very short term
                        // inconsistency, or it could be long lived.  In case
                        // it's the latter, we send back a disk size message.
                        // If we don't, they'll never get a disk size for us,
                        // so they may never accept any puts.

                        if (msg.reply)
                            sendDiskSizeMsg(peer, false);
                    }
                    else {
                        if (old_size.doubleValue() != msg.sizes[i]) {
                            if (logger.isInfoEnabled()) {
                                StringBuffer sbuf = new StringBuffer(100);
                                sbuf.append("disk size for ls peer ");
                                sbuf.append(peer);
                                sbuf.append(" changed to ");
                                StringUtil.byte_cnt_to_sbuf(
                                        Math.round(msg.sizes[i]), sbuf);
                                logger.info(sbuf);
                            }
                            ls_disk_sizes[i].put(
                                    peer, new Double(msg.sizes[i]));
                            changed = true;
                        }
                    }
                }
            }
            if (changed)
                compute_rates();
        }
    };

    protected boolean disk_space_avail(int size, int ttl_sec) {
        // Update the number of available tokens.
        int i = ttl_to_vdisk(ttl_sec);
        long now_ms = now_ms();
        long ms_since_last_fill = now_ms - last_fill_time[i];
        double tokens_to_add = Math.min(fill_rates[i] * ms_since_last_fill,
                                        max_tokens[i] - avail_tokens[i]);
        avail_tokens[i] += tokens_to_add;
        if (logger.isDebugEnabled()) {
            StringBuffer sbuf = new StringBuffer(30);
            NumberFormat n = NumberFormat.getInstance();
            n.setMaximumFractionDigits(3);
            n.setGroupingUsed(false);
            sbuf.append(n.format(ms_since_last_fill / 1000.0));
            sbuf.append("s since last update, adding ");
            sbuf.append(n.format(tokens_to_add));
            sbuf.append(" bytes to bucket for ttl of ");
            StringUtil.time_to_sbuf(ttl_sec_ranges[i], sbuf);
            sbuf.append(", which now has ");
            sbuf.append(n.format(avail_tokens[i]));
            sbuf.append(" bytes in it");
            logger.debug(sbuf.toString());
        }
        last_fill_time[i] = now_ms;

        // If there are enough tokens, then there is space available.
        return size <= avail_tokens[i];
    }

    protected void add_to_disk(int size, int ttl_sec) {
        int i = ttl_to_vdisk(ttl_sec);
        avail_tokens[i] -= size;
    }

    protected double disk_avail_rate(int ttl_sec) {
        int i = ttl_to_vdisk(ttl_sec);
        return fill_rates[i];
    }

    protected int storage_size(PutReqPayload payload) {
        return payload.value.limit() + StorageManager.Key.SIZE;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Slop space for implementing fair queuing.
    //
    //////////////////////////////////////////////////////////////////////////

    protected LinkedList slop = new LinkedList();
    protected int[] slop_size = { 0, 0, 0};
    protected int[] slop_cap = { 10 * 1024, 10 * 1024, 10 * 1024};
    protected Map [] slop_usage = {
        new HashMap (), new HashMap (), new HashMap ()
    };

    protected boolean slop_empty(int ttl_sec) {
        int i = ttl_to_vdisk(ttl_sec);
        return slop_size[i] == 0;
    }

    protected boolean slop_space_avail(
            InetAddress client, int size, int ttl_sec) {
        int i = ttl_to_vdisk(ttl_sec);
        return ((slop_size[i] + size <= slop_cap[i])
                && (slop_usage (i, client) + size
                    <= 2*(1024+StorageManager.Key.SIZE)));
    }

    protected void add_to_slop(BambooRouteDeliver msg,
                               PutReqPayload payload) {
        int i = ttl_to_vdisk(payload.ttl_sec);
        int storage_size = storage_size(payload);
        Long l = (Long) slop_usage[i].get (payload.client_id);
        if (l == null) {
            slop_usage[i].put (payload.client_id, new Long (storage_size));
        }
        else {
            slop_usage[i].put (payload.client_id,
                    new Long (l.longValue () + storage_size));
        }
        slop.addLast(new Object[] {msg, payload});
        slop_size[i] += storage_size;
        if (logger.isDebugEnabled())
            logger.debug("added " + payload + " to slop, size=" + slop_size[i]);
        if (slop.size() == 1) {
            double rate = disk_avail_rate(payload.ttl_sec);
            long wait_ms = Math.round(Math.ceil(storage_size / rate));
            acore.register_timer(wait_ms, slop_to_disk_cb, null);
            if (logger.isDebugEnabled())
                logger.debug("waiting " + wait_ms + " ms to retry");
        }
    }

    protected ASyncCore.TimerCB slop_to_disk_cb = new ASyncCore.TimerCB() {
        public void timer_cb(Object not_used) {
            int pass = 1;
            while (!slop.isEmpty()) {
                Object[] head = (Object[]) slop.getFirst();
                BambooRouteDeliver msg = (BambooRouteDeliver) head[0];
                PutReqPayload payload = (PutReqPayload) head[1];
                int storage_size = storage_size(payload);
                if (disk_space_avail(storage_size, payload.ttl_sec)) {
                    add_to_disk(storage_size, payload.ttl_sec);
                    int i = ttl_to_vdisk(payload.ttl_sec);
                    slop_size[i] -= storage_size;
                    slop.removeFirst();
                    Long l = (Long) slop_usage[i].get (payload.client_id);
                    if (l.longValue () - storage_size == 0) {
                        slop_usage[i].remove (payload.client_id);
                    }
                    else {
                        slop_usage[i].put (payload.client_id,
                                new Long (l.longValue () - storage_size));
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("removed " + payload +
                                " from slop, size=" + slop_size[i]);
                    }
                    PutOrRemoveReq outb = new PutOrRemoveReq(
                        payload.time_usec, payload.ttl_sec, payload.key,
                        payload.value, payload.secret_hash, payload.value_hash, 
                        payload.put, payload.client_id, my_sink, msg);
                    dispatch(outb);
                }
                else {
                    double rate = disk_avail_rate(payload.ttl_sec);
                    long wait_ms = Math.min(
                             1000, Math.round(Math.ceil(storage_size / rate)));
                    acore.register_timer(wait_ms, slop_to_disk_cb, null);
                    if ( (pass == 1) && logger.isDebugEnabled()) {
                        logger.debug("bad slop wait, needed extra " +
                                     wait_ms + " ms, size=" + storage_size);
                    }
                    else if (logger.isDebugEnabled())
                        logger.debug("waiting " + wait_ms + " ms to retry");
                    break;
                }
                ++pass;
            }
        }
    };

    protected long slop_usage (int disk, InetAddress client) {
        Long l = (Long) slop_usage[disk].get (client);
        if (l == null)
            return 0;
        else
            return l.longValue ();
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Public interface classes
    //
    //////////////////////////////////////////////////////////////////////////

    public static class PutReq implements QueueElementIF {
        public BigInteger key;
        public ByteBuffer value;
        public byte [] secret_hash;
        public byte [] value_hash;
        public boolean put;
        public SinkIF completion_queue;
        public Object user_data;
        public int ttl_sec;
        public InetAddress client_id;

        public PutReq(BigInteger k, ByteBuffer v, byte [] sh, byte [] vh, 
                      boolean p, SinkIF q, Object ud, int t, InetAddress c) {
            key = k; value = v; secret_hash = sh; value_hash = vh; put = p; 
            completion_queue = q; user_data = ud; ttl_sec = t; client_id = c;
        }

        public PutReq(BigInteger k, ByteBuffer v, byte [] vh, 
                      boolean p, SinkIF q, Object ud, int t, InetAddress c) {
            this(k, v, null, vh, p, q, ud, t, c);
        }

        public String toString() {
            return "(Dht.PutReq key=" + GuidTools.guid_to_string(key) +
                " put=" + put + " ttl=" + ttl_sec + " client_id=" +
                client_id.getHostAddress() + ")";
        }
    }

    public static class GetReq implements QueueElementIF {
        public BigInteger key;
        public int maxvals;
        public boolean all;
        public StorageManager.Key placemark;
        public SinkIF completion_queue;
        public Object user_data;
        public NodeId client;
        public GetReq(BigInteger k, int m, boolean a, StorageManager.Key p,
                      SinkIF q, Object ud, NodeId c) {
            key = k; maxvals = m; all = a; placemark = p;
            completion_queue = q; user_data = ud; client = c;
        }
        public String toString() {
            return "(Dht.GetReq key=" + GuidTools.guid_to_string(key) +
                " maxvals=" + maxvals + " all=" + all + " placemark=" +
                placemark + ")";
        }
    }

    public static class PutResp implements QueueElementIF {
        public int result;
        public Object user_data;
        public PutResp(int r, Object ud) { result = r; user_data = ud; }
        public String toString() { return "(Dht.PutResp)"; }
    }

    public static class GetValue implements QuickSerializable {
        public ByteBuffer value;
        public int ttlRemaining;
        public String hashAlgorithm;
        public byte[] secretHash; 
        public GetValue(ByteBuffer v, int t, byte[] s) {
            value = v; ttlRemaining = t; secretHash = s; 
            if (s == null) // For now only SHA is supported.
                hashAlgorithm = "";
            else 
                hashAlgorithm = "SHA";
        }
        public GetValue(InputBuffer buf) {
            int len = buf.nextInt();
            byte[] tmp = new byte[len];
            buf.nextBytes(tmp, 0, len);
            value = ByteBuffer.wrap(tmp);
            ttlRemaining = buf.nextInt();
            len = buf.nextInt();
            byte[] str = new byte[len];
            buf.nextBytes(str, 0, len);
            hashAlgorithm = new String(str);
            len = buf.nextInt();
            secretHash = new byte[len];
            buf.nextBytes(secretHash, 0, len);
        }
        public void serialize(OutputBuffer buf) {
            buf.add(value.limit() - value.position());
            buf.add(value.array(), value.arrayOffset() + value.position(), 
                    value.limit() - value.position());
            buf.add(ttlRemaining);
            byte[] str = hashAlgorithm.getBytes();
            buf.add(str.length);
            buf.add(str);
            buf.add(secretHash.length);
            buf.add(secretHash);
        }
    }

    public static class GetResp implements QueueElementIF {
        public Object user_data;
        public LinkedList values;
        public StorageManager.Key placemark;
        public GetResp(LinkedList v, Object ud, StorageManager.Key p) {
            user_data = ud; values = v; placemark = p;
        }
        public String toString() {
            return "(Dht.GetResp values.size ()=" + values.size() +
                " placemark=" + placemark + ")";
        }
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Network messages and payloads
    //
    //////////////////////////////////////////////////////////////////////////

    public static class PutReqPayload implements QuickSerializable {
        public static final int MAX_SIZE = 16384;
        public long time_usec;
        public BigInteger key;
        public ByteBuffer value;
        public boolean put;
        public NodeId return_addr;
        public long seq;
        public int ttl_sec;
        public InetAddress client_id;
        public boolean retried;
        public byte [] value_hash;
        public byte [] secret_hash;
        public PutReqPayload(long t, BigInteger k, ByteBuffer v,
                             boolean p, NodeId r, long s, int tt, 
                             InetAddress c, byte [] vh, byte [] sh) {
            time_usec = t; key = k; value = v; put = p; return_addr = r;
            seq = s; ttl_sec = tt; client_id = c; value_hash = vh;
            secret_hash = sh;
        }
        public PutReqPayload(InputBuffer buffer) throws QSException {
            time_usec = buffer.nextLong();
            key = buffer.nextBigInteger();
            int len = buffer.nextInt();
            if (len > MAX_SIZE)throw new QSException("len=" + len);
            byte[] tmp = new byte[len];
            buffer.nextBytes(tmp, 0, len);
            value = ByteBuffer.wrap(tmp);
            put = buffer.nextBoolean();
            return_addr = new NodeId(buffer);
            seq = buffer.nextLong();
            ttl_sec = buffer.nextInt();
            int client_id_len = buffer.nextInt();
            byte[] client_bytes = new byte[client_id_len];
            buffer.nextBytes(client_bytes, 0, client_id_len);
            try {
                client_id = InetAddress.getByAddress(client_bytes);
            }
            catch (UnknownHostException ex) {
                assert false;
            }
            retried = buffer.nextBoolean ();
            if (!put) {
                value_hash = new byte[20];
                buffer.nextBytes(value_hash, 0, 20);
            }
            if ((time_usec & 0x8000000000000000L) != 0) {
                secret_hash = new byte[20];
                buffer.nextBytes(secret_hash, 0, 20);
                time_usec &= 0x7fffffffffffffffL;
            }
            else {
                secret_hash = null;
            }
        }
        public void serialize(OutputBuffer buffer) {
            if ((secret_hash == null)
                || java.util.Arrays.equals(secret_hash, ZERO_HASH))
                buffer.add(time_usec);
            else
                buffer.add(0x8000000000000000L | time_usec);
            buffer.add(key);
            buffer.add(value.limit());
            buffer.add(value.array(), value.arrayOffset(), value.limit());
            buffer.add(put);
            return_addr.serialize(buffer);
            buffer.add(seq);
            buffer.add(ttl_sec);
            byte[] client_bytes = client_id.getAddress();
            buffer.add(client_bytes.length);
            buffer.add(client_bytes, 0, client_bytes.length);
            buffer.add(retried);
            if (!put) 
                buffer.add(value_hash, 0, 20);
            if ((secret_hash != null)
                && !java.util.Arrays.equals(secret_hash, ZERO_HASH))
                buffer.add(secret_hash, 0, 20);
        }

        public String toString() {
            return "(Dht.PutReqPayload time_usec=" + time_usec +
                " key=" + GuidTools.guid_to_string(key) +
                " put=" + put + " return_addr=" + return_addr +
                " seq=" + seq + " ttl=" + ttl_sec + " retried=" + retried + ")";
        }
    }

    public static class PutRespMsg extends NetworkMessage {
        public long seq;
        public int result;
        public PutRespMsg(NodeId dest, long s, int r) {
            super(dest, false); seq = s; result = r;
        }
        public PutRespMsg(InputBuffer buffer) throws QSException {
            super(buffer);
            seq = buffer.nextLong();
            result = buffer.nextInt();
        }
        public void serialize(OutputBuffer buffer) {
            super.serialize(buffer);
            buffer.add(seq);
            buffer.add(result);
        }
        public String toString() {
            return "(Dht.PutRespMsg seq=" + seq + " result=" + result + ")";
        }
    }

    public static class GetRespMsg extends NetworkMessage {
        public long seq;
        public BigInteger key;
        public LinkedList values;
        public StorageManager.Key orig_placemark;
        public StorageManager.Key new_placemark;
        public GetRespMsg(NodeId dest, long s, BigInteger k, LinkedList v,
                          StorageManager.Key o, StorageManager.Key n) {
            super(dest, false);
            seq = s; key = k; values = v; orig_placemark = o; new_placemark = n;
            assert new_placemark != null;
        }
        public GetRespMsg(InputBuffer buffer) throws QSException {
            super(buffer);
            seq = buffer.nextLong ();
            key = buffer.nextBigInteger();
            int count = buffer.nextInt();
            boolean getValues = false;
            if (count < 0) {
                getValues = true;
                count *= -1;
            }
            values = new LinkedList();
            while (count-- > 0) {
                if (getValues) {
                    GetValue value = new GetValue(buffer);
                    values.addLast(value);
                }
                else {
                    int len = buffer.nextInt();
                    byte[] data = new byte[len];
                    buffer.nextBytes(data, 0, len);
                    values.addLast(ByteBuffer.wrap(data));
                }
            }
            orig_placemark = new StorageManager.Key(buffer);
            new_placemark = new StorageManager.Key(buffer);
        }
        public void serialize(OutputBuffer buffer) {
            super.serialize(buffer);
            boolean useGetValues = (!values.isEmpty())
                && (values.getFirst() instanceof GetValue);
            buffer.add(seq);
            buffer.add(key);
            if (useGetValues)
                buffer.add(values.size() * -1);
            else
                buffer.add(values.size());
            Iterator i = values.iterator();
            while (i.hasNext()) {
                if (useGetValues) {
                    GetValue value = (GetValue) i.next();
                    value.serialize(buffer);
                }
                else {
                    ByteBuffer bb = (ByteBuffer) i.next();
                    buffer.add(bb.limit());
                    buffer.add(bb.array(), bb.arrayOffset(), bb.limit());
                }
            }
            orig_placemark.serialize(buffer);
            new_placemark.serialize(buffer);
        }
        public String toString() {
            return "(Dht.GetRespMsg seq=" + seq + 
                " key=" + GuidTools.guid_to_string(key) +
                " values.size ()=" + values.size() +
                " orig_placemark=" + orig_placemark +
                " new_placemark=" + new_placemark + ")";
        }
    }

    public static class ForwardThroughLeafSetReq 
        implements QuickSerializable, ByteCount {

        public NetworkMessage payload;

        public String byteCountKey() { 
            return ((ByteCount) payload).byteCountKey(); 
        }

        public boolean recordByteCount() { 
            if (payload instanceof ByteCount) 
                return ((ByteCount) payload).recordByteCount(); 
            else 
                return false;
        }

        public ForwardThroughLeafSetReq (NetworkMessage p) { payload = p; }
        public ForwardThroughLeafSetReq(InputBuffer buffer) throws QSException {
            payload = (NetworkMessage) buffer.nextObject ();
            payload.inbound = false;
            payload.peer = new NodeId (buffer);
        }
        public void serialize(OutputBuffer buffer) {
            buffer.add(payload);
            payload.peer.serialize (buffer);
        }
        public String toString() {
            return "(Dht.ForwardThroughLeafSetReq payload=" + payload + ")";
        }
    }

    public static class DiskSizeMsg implements QuickSerializable {
        public int[] ttls;
        public double[] sizes;
        public boolean reply;
        public DiskSizeMsg(int[] t, double[] s, boolean r) {
            ttls = t; sizes = s; reply = r;
        }
        public DiskSizeMsg(InputBuffer buf) throws QSException {
            int cnt = buf.nextInt();
            ttls = new int[cnt];
            sizes = new double[cnt];
            for (int i = 0; i < cnt; ++i) {
                ttls[i] = buf.nextInt();
                sizes[i] = buf.nextDouble();
            }
            reply = buf.nextBoolean();
        }
        public void serialize(OutputBuffer buf) {
            assert(ttls.length == sizes.length);
            buf.add(ttls.length);
            for (int i = 0; i < ttls.length; ++i) {
                buf.add(ttls[i]);
                buf.add(sizes[i]);
            }
            buf.add(reply);
        }
        public Object clone() throws CloneNotSupportedException {
            DiskSizeMsg result = (DiskSizeMsg) super.clone();
            result.ttls = new int[ttls.length];
            result.sizes = new double[sizes.length];
            for (int i = 0; i < ttls.length; ++i) {
                result.ttls[i] = ttls[i];
                result.sizes[i] = sizes[i];
            }
            result.reply = reply;
            return result;
        }
    }

    public Dht() throws Exception {

        ostore.util.TypeTable.register_type(GetValue.class);
        ostore.util.TypeTable.register_type(PutReqPayload.class);
        ostore.util.TypeTable.register_type(RpcGetResp.class);
        ostore.util.TypeTable.register_type(IterGetReq.class);
        ostore.util.TypeTable.register_type(IterGetResp.class);

        event_types = new Class[] {
            PutReq.class,
            GetReq.class,
        };

        inb_msg_types = new Class[] {
            PutRespMsg.class,
            GetRespMsg.class,
        };
    }

    public void init(ConfigDataIF config) throws Exception {
        super.init(config);
        get_timeout = configGetInt(config, "get_timeout", 5000);
        iterative_routing = config_get_boolean(config, "iterative_routing");
        String sm_name = config_get_string(config, "storage_manager_stage");
        StorageManager sm = (StorageManager) lookup_stage(config, sm_name);
        sm.register_monitor(this);
        min_replica_count = config_get_int(config, "min_replica_count");
        if (min_replica_count == -1)
            min_replica_count = 8;
        int virtual_disks = config_get_int(config, "virtual_disks");
        for (int i = 0; i < virtual_disks; ++i) {
            double disk_size = config_get_double(config, "disk_size_" + i);
            if (disk_size != -1)
                disk_sizes[i] = disk_size;
            int token_bucket_size =
                config_get_int(config, "token_bucket_size_" + i);
            if (token_bucket_size != -1)
                max_tokens[i] = token_bucket_size;
            int slop_size = config_get_int(config, "slop_size_" + i);
            if (slop_size != -1)
                slop_cap [i] = slop_size;
        }
        compute_rates();
        acore.registerTimer(0, ready);
    }

    protected Runnable ready = new Runnable() {
        public void run() {
            network = Network.instance(my_node_id);
            rpc = Rpc.instance (my_node_id);
            dmgr = DataManager.instance(my_node_id);
            router = Router.instance(my_node_id);
            vivaldi = Vivaldi.instance(my_node_id);
            rand = new Random(my_node_id.hashCode() ^ now_ms());
            returnToClient = 
                new ReturnToClient(logger, network, rpc, acore, router, rand);
            try {
                network.registerReceiver(DiskSizeMsg.class, handleDiskSizeMsg);
                network.registerReceiver(RecurGetReq.class,handleRecurGetReq);
                network.registerReceiver(RecurGetResp.class,handleRecurGetResp);
                rpc.registerRequestHandler(IterGetReq.class, iterGetReqHandler);
            }
            catch (DuplicateTypeException e) { BUG(e); }

            dispatch(new BambooRouterAppRegReq(
                        app_id, true, false, false, my_sink));
        }
    };

    public void handleEvent(QueueElementIF item) {
        if (logger.isDebugEnabled())
            logger.debug("got " + item);

        if (item instanceof BambooRouterAppRegResp) {
            BambooRouterAppRegResp resp = (BambooRouterAppRegResp) item;
            initialized = true;
            my_guid = resp.node_guid;
            my_neighbor_info = new NeighborInfo(my_node_id, my_guid);
            MODULUS = resp.modulus;
            MAX_GUID = resp.modulus.subtract(BigInteger.ONE);
            next_put_seq = rand.nextLong () & 0x7fffffffffffffffL;
            next_get_seq = rand.nextLong () & 0x7fffffffffffffffL;
            acore.registerTimer(rand.nextInt(4000) + 2000, swap_disk_sizes_cb);
            while (! wait_q.isEmpty())
                handleEvent ((QueueElementIF) wait_q.removeFirst ());
        }
        else if (!initialized) {
            wait_q.addLast(item);
        }
        else {
            if (item instanceof BambooLeafSetChanged) {
                handle_leaf_set_changed( (BambooLeafSetChanged) item);
            }
            else if (item instanceof PutReq) {
                handle_put_req( (PutReq) item);
            }
            else if (item instanceof PutRespMsg) {
                handle_put_resp_msg( (PutRespMsg) item);
            }
            else if (item instanceof GetReq) {
                handle_get_req( (GetReq) item);
            }
            else if (item instanceof GetRespMsg) {
                handle_get_resp_msg( (GetRespMsg) item);
            }
            else if (item instanceof BambooRouteDeliver) {
                BambooRouteDeliver msg = (BambooRouteDeliver) item;
                if (msg.payload instanceof PutReqPayload) {
                    handle_put_req_payload(msg, (PutReqPayload) msg.payload);
                }
                else {
                    logger.info("got unexpected deliver: "
                                + item.getClass().getName() + ".");
                }
            }
            else if (item instanceof BambooRouteUpcall) {
                logger.info("got unexpected upcall: "
                            + item.getClass().getName() + ".");
            }
            else if (item instanceof PutOrRemoveResp) {
                handle_put_or_remove_resp( (PutOrRemoveResp) item);
            }
            else if (item instanceof StorageManager.GetByGuidResp) {
                handle_get_by_guid_resp( (StorageManager.GetByGuidResp) item);
            }
            else {
                throw new IllegalArgumentException(
                    "got event of unexpected type: " +
                    item.getClass().getName() + ".");
            }
        }
    }

    protected static final long PUT_TIMEOUT = 30*1000;

    protected ASyncCore.TimerCB put_retry_cb = new ASyncCore.TimerCB () {
        public void timer_cb (Object user_data) {
            Long seq = (Long) user_data;
            Object [] pair = (Object []) active_puts.get (seq);
            if (pair != null) {
                PutReq req = (PutReq) pair [0];
                PutReqPayload payload = (PutReqPayload) pair [1];
                payload.retried = true;
                BambooRouteInit outb = new BambooRouteInit(req.key, app_id,
                        false /* no upcalls */, iterative_routing, payload);
                logger.info ("resending put, key=0x" 
                             + GuidTools.guid_to_string (req.key));
                dispatch(outb);
                acore.register_timer (PUT_TIMEOUT, this, seq);
            }
        }
    };

    protected void handle_put_req(PutReq req) {
        long now_us = now_ms() * 1000;
        Long seq = new Long (next_put_seq ());
        PutReqPayload payload = new PutReqPayload(
                now_us, req.key, req.value, req.put, my_node_id, 
                seq.longValue (), req.ttl_sec, req.client_id, 
                req.value_hash, req.secret_hash);
        active_puts.put(seq, new Object [] {req, payload});
        BambooRouteInit outb = new BambooRouteInit(req.key, app_id,
            false /* no upcalls */, iterative_routing, payload);
        dispatch(outb);
        acore.register_timer (PUT_TIMEOUT, put_retry_cb, seq);
    }

    protected void handle_put_resp_msg(PutRespMsg msg) {
        Object [] pair = (Object []) active_puts.remove (new Long(msg.seq));
        if (pair != null) {
            PutReq req = (PutReq) pair [0];
            PutResp outb = new PutResp(msg.result, req.user_data);
            enqueue(req.completion_queue, outb);
        }
    }

    protected long get_timeout;

    protected Thunk1<Long> getRetry = new Thunk1<Long>() {
        public void run(Long seq) {
            Pair<GetReq,IterGetState> pair = active_gets.get(seq);
            if (pair != null) {
                logger.info ("resending get, key=0x" 
                             + GuidTools.guid_to_string(pair.first.key));
                sendGetReq(pair.first, seq);
            }
        }
    };

    protected Thunk4<SinkIF,Object,LinkedList<GetValue>,Key> sendIterGetResp = 
        new Thunk4<SinkIF,Object,LinkedList<GetValue>,Key>() {
            public void run(SinkIF sink, Object userData, 
                            LinkedList<GetValue> values, Key placemark) {
                enqueue(sink, new GetResp(values, userData, placemark));
            }
        };

    protected void handle_get_req(GetReq req) {
        // This is the mode that seems to work best on PlanetLab.
        req.maxvals &= MAXVALS_MASK;
        req.maxvals |= QUORUM_SYNC | SCALED_PRS_FD | RECURSIVE 
                       | NEW_RECUR_MASK; 
        StorageManager.Key placemark = req.placemark;
        if (placemark == null)
            placemark = StorageManager.ZERO_KEY;
        Long seq = new Long (next_get_seq ());
        if ((req.maxvals & ITERATIVE_MASK) == ITERATIVE) {
            int parallelism = ((req.maxvals & ITERATIVE_PARALLELISM_MASK) 
                               >> ITERATIVE_PARALLELISM_SHIFT) + 1;
            IterGetState state = new IterGetState(
                    req.key, req.maxvals, placemark, seq, parallelism, 
                    curry(sendIterGetResp,req.completion_queue,req.user_data),
                    req.client);
            state.startIterGetReq();
        }
        else if ((req.maxvals & ITERATIVE_MASK) == RECURSIVE) {
            active_gets.put(seq, new Pair<GetReq,IterGetState>(req, null));
            sendGetReq(req, seq);
        }
        else 
            assert false;
    }

    protected void sendGetReq(GetReq req, Long seq) {
        if ((NEW_RECUR_MASK & req.maxvals) != 0) {
            Key placemark = req.placemark == null ? ZERO_KEY : req.placemark;
            handleRecurGetReq.run(new RecurGetReq(req.key, req.maxvals, 
                        placemark, seq.longValue(), req.client, my_node_id),
                    my_node_id);
            return;
        }
        acore.registerTimer(get_timeout, curry(getRetry, seq));
    }

    protected void handle_get_resp_msg(GetRespMsg msg) {
        Pair<GetReq,IterGetState> pair = active_gets.remove(new Long(msg.seq));
        if (pair == null) {
            if (logger.isDebugEnabled())
                logger.debug("Got unexpected " + msg);
            return;
        }

        StringBuffer sbuf = new StringBuffer(200);
        sbuf.append("received get resp key=0x");
        sbuf.append(GuidTools.guid_to_string(msg.key));
        sbuf.append(" return addr=");
        sbuf.append(my_node_id);
        sbuf.append(" seq=");
        sbuf.append(msg.seq);
        logger.info(sbuf.toString());

        GetReq req = (GetReq) pair.first;
        GetResp outb = new GetResp(
                msg.values, req.user_data, msg.new_placemark);
        enqueue(req.completion_queue, outb);
    }

    protected void handle_put_req_payload(BambooRouteDeliver msg,
                                          PutReqPayload payload) {

        int put_sz = storage_size(payload);
        int ttl_sec = payload.ttl_sec;
        InetAddress client = payload.client_id;

        // Make sure we have at least 8 neighbors before accepting any put.

        if (leaf_set_size + 1 < min_replica_count) {
            if (logger.isInfoEnabled ())
                logger.info ("don't have enough neighbors to accept put");
            PutRespMsg outb = new PutRespMsg(
                payload.return_addr, payload.seq, bamboo_stat.BAMBOO_CAP);
            outb.timeout_sec = 300;
            dispatch(outb);
            return;
        }

        if (slop_empty(ttl_sec) && disk_space_avail(put_sz, ttl_sec)) {
            if (logger.isDebugEnabled())
                logger.debug("client=" + client.getHostAddress() +
                             " slop is empty and disk space is available");
            add_to_disk(put_sz, ttl_sec);
            PutOrRemoveReq outb = new PutOrRemoveReq(
                payload.time_usec, ttl_sec, payload.key,
                payload.value, payload.secret_hash, payload.value_hash, 
                payload.put, client, my_sink, msg);
            dispatch(outb);
        }
        else if (slop_space_avail(client, put_sz, ttl_sec) &&
                 below_fair_share(client, put_sz, ttl_sec)) {

           if (logger.isDebugEnabled()) {
                if (slop_empty(ttl_sec)) {
                    logger.debug("client=" + client.getHostAddress() +
                                 " no disk space but below fair share");
                }
                else {
                    logger.debug("client=" + client.getHostAddress() +
                                 " slop not empty and below fair share");
                }
            }
            add_to_slop(msg, payload);
        }
        else {
            if (logger.isDebugEnabled()) {
                // TODO: maybe have a different return code for each of
                // these two cases?

                if (slop_space_avail(client, put_sz, ttl_sec))
                    logger.debug("client=" + client.getHostAddress() +
                                 " slop available but above fair share");
                else
                    logger.debug("client=" + client.getHostAddress() +
                                 " no slop space available");
            }
            PutRespMsg outb = new PutRespMsg(
                payload.return_addr, payload.seq,
                bamboo_stat.BAMBOO_CAP);
            outb.timeout_sec = 300;
            dispatch(outb);
        }
    }

    protected void handle_put_or_remove_resp(PutOrRemoveResp ack) {
        BambooRouteDeliver msg = (BambooRouteDeliver) ack.user_data;
        PutReqPayload payload = (PutReqPayload) msg.payload;
        PutRespMsg outb = new PutRespMsg(
                payload.return_addr, payload.seq, bamboo_stat.BAMBOO_OK);
        returnToClient.returnToClient(outb, null);
    }

    /////////////////////////////////////////////////////////////////
    //
    //                  Implementation of getValues()
    //
    /////////////////////////////////////////////////////////////////

    protected static class GetValuesState {
        public LinkedList<Pair<Key,ByteBuffer>> values = 
            new LinkedList<Pair<Key,ByteBuffer>>();
        public int size;
        public int maxvals;
        public StorageManager.Key largest_key = StorageManager.ZERO_KEY;
        public Thunk2<LinkedList<Pair<Key,ByteBuffer>>,Boolean> done;
        public GetValuesState(int m, 
                Thunk2<LinkedList<Pair<Key,ByteBuffer>>,Boolean> d) { 
            maxvals = m; done = d; 
        }
    }

    protected void getValues(BigInteger key, Key placemark, int maxvals,
            Thunk2<LinkedList<Pair<Key,ByteBuffer>>,Boolean> done) {
        maxvals &= MAXVALS_MASK;
        GetValuesState state = new GetValuesState(maxvals, done);
        GetByGuidReq outb = new GetByGuidReq(key, true /* primary */,
                                             placemark, my_sink, state);
        dispatch(outb);
    }

    /**
     * Returns all values whose keys are
     * greater than the given placemark, if any.  Returns a placemark equal to
     * the largest key of the values returned, or all zeros if no values are
     * returned.  All zeros is appropriate as a lower open limit on placemarks
     * since time_usec cannot be zero in any real key.
     * <p>
     * Using the standard StorageManager.Key.compareTo function, instead of a
     * Comparator that compares guids first, works here because all values for
     * a given get have the same guid.
     */
    protected void handle_get_by_guid_resp(StorageManager.GetByGuidResp resp) {
        GetValuesState state = (GetValuesState) resp.user_data;
        boolean allRead = false;

        if (resp.continuation != null) {
            if (resp.key.put == false) {
                // Don't return removes.  The continuation is non-null, so
                // check for more values.
                StorageManager.GetByGuidCont outb =
                    new StorageManager.GetByGuidCont(
                            resp.continuation, my_sink, state);
                dispatch(outb);
                return;
            }

            // TODO: it's stupid that we create this and then throw it away.
            // We might as well make this the values type...
            GetValue gv = new GetValue(resp.data,
                    resp.key.ttlRemaining(now_ms()),
                    resp.key.secret_hash);
            CountBuffer cb = new CountBuffer();
            gv.serialize(cb);

            if ((state.size + cb.size() <= MAX_GET_RESP_SIZE)
                && (state.values.size() < state.maxvals)) {

                state.size += cb.size();
                state.values.addLast(Pair.create(resp.key, resp.data));
                // There may still be more values; we need to check for them.
                StorageManager.GetByGuidCont outb =
                    new StorageManager.GetByGuidCont(resp.continuation, 
                                                     my_sink, state);
                dispatch(outb);
                return;
            }
            else {
                // There were more values, but we can't return them.  Close
                // the cursor.
                StorageManager.GetByGuidCont outb =
                    new StorageManager.GetByGuidCont(
                    resp.continuation, null, null);
                dispatch(outb);
            }
        }
        else {
            // Cursor already closed, and there are no more values.
            allRead = true;
        }

        // All done.  Run the callback.
        state.done.run(state.values, new Boolean(allRead));
    }

    /////////////////////////////////////////////////////////////////
    //
    //                         Get Upcalls
    //
    /////////////////////////////////////////////////////////////////

    protected NeighborInfo calcNextHop(BigInteger key, int maxvals) {
        return calcNextHop(key, maxvals, router.allNeighbors());
    }

    protected NeighborInfo calcNextHop(BigInteger key, int maxvals,
                                       Map<NeighborInfo,Long> neighbors) {
        if ((PROXIMITY_MASK & maxvals) == PNS_ONLY)
            return router.calcNextHopGreedy(key, neighbors);
        else if ((PROXIMITY_MASK & maxvals) == PRS)
            return router.calcNextHopPRS(key, neighbors);
        else if ((PROXIMITY_MASK & maxvals) == SCALED_PRS)
            return router.calcNextHopScaledPRS(key, neighbors);
        else if ((PROXIMITY_MASK & maxvals) == SCALED_PRS_FD)
            return router.calcNextHopScaledPRS(key, fdScaling, neighbors);
        else 
            assert false;
        return null;
    }

    /**
     * Holds the return addresses and sequence numbers for GetReqPayloads
     * we've received but haven't finished processing so that we don't start
     * all over on retried requests.
     */
    protected Set<Pair<NodeId,Long>> getsBeingProcessed =
        new HashSet<Pair<NodeId,Long>>();

    protected Function2<BigInteger,BigInteger,Long> fdScaling =
        new Function2<BigInteger,BigInteger,Long>() {
            public BigInteger run(BigInteger dist, Long lat) {
                try {
                    // Cap value so we don't get denom=Inf, which pisses of
                    // the constructor of BigDecimal.
                    long latency = min(lat.longValue(), 10*1000);
                    double denom = 1 + exp((latency - 100) / (8.616 * 2));
                    BigDecimal num = new BigDecimal(dist);
                    BigDecimal bdenom = new BigDecimal(denom);
                    BigDecimal quot = num.divide(bdenom,0,BigDecimal.ROUND_UP);
                    return quot.toBigInteger();
                }
                catch (Exception e) {
                    logger.fatal("Caught " + e + "; dist=" + dist 
                                 + " lat=" + lat);
                    System.exit(1);
                    return null;
                }
            }
        };

    protected void startNewStyleRecurGet(final long seq, final BigInteger key, 
            final Key placemark, final int maxvals, final NodeId client,
            final NodeId return_addr, Set<NeighborInfo> replicas) {

        Pair<NodeId,Long> pair = Pair.create(return_addr, new Long(seq));
        if (getsBeingProcessed.contains(pair)) {
            StringBuffer sbuf = new StringBuffer(200);
            sbuf.append("upcall for get req key=0x");
            sbuf.append(GuidTools.guid_to_string(key));
            sbuf.append(" return addr=");
            sbuf.append(return_addr);
            sbuf.append(" seq=");
            sbuf.append(seq);
            sbuf.append(" already being processed");
            logger.info(sbuf.toString());
            return;
        }
        getsBeingProcessed.add(pair);

        Set<NodeId> r = new LinkedHashSet<NodeId>();
        Set<NodeId> synced = new LinkedHashSet<NodeId>();
        for (NeighborInfo ni : replicas) {
            r.add(ni.node_id);
            if (!ni.node_id.equals(my_node_id) && dmgr.synced(key, ni.node_id))
                synced.add(ni.node_id);
        }

        Set<NodeId> others = new LinkedHashSet<NodeId>();
        others.addAll(r);
        others.removeAll(synced);
        others.remove(my_node_id);

        StringBuffer sbuf = new StringBuffer(200);
        sbuf.append("upcall for get req key=0x");
        sbuf.append(GuidTools.guid_to_string(key));
        sbuf.append(" return addr=");
        sbuf.append(return_addr);
        sbuf.append(" seq=");
        sbuf.append(seq);
        sbuf.append(" reached replica, new recur style");
        sbuf.append(", replicas=").append(r);
        sbuf.append(", synced=").append(synced);
        logger.info(sbuf.toString());

        getValues(key, placemark, maxvals, 
                  curry(sendRecurGetResp, return_addr, new Long(seq), r, synced, 
                        new Integer(maxvals), client, pair));
    }

    protected Thunk2<RecurGetReq,InetSocketAddress> handleRecurGetReq = 
        new Thunk2<RecurGetReq,InetSocketAddress>() {
            public void run(final RecurGetReq req, final InetSocketAddress peer) {

                int needed = dmgr.desiredReplicas();
                Set<NeighborInfo> replicas = 
                    router.leafSet().replicas(req.key, needed);
                final NeighborInfo nextHop = calcNextHop(req.key, req.maxvals);
                if ((router.leafSet().overlap() || (replicas.size() == needed))
                    || nextHop.equals(my_neighbor_info)) {
                    startNewStyleRecurGet(req.seq, req.key, req.placemark, 
                            req.maxvals, req.client, req.return_addr, replicas);
                }
                else {
                    network.send(req, nextHop.node_id, 5, new Thunk1<Boolean>() {
                            public void run(Boolean success) {
                                if (success.booleanValue()) {
                                    // Remove the node from the possiblyDown set.
                                    router.removeFromPossiblyDown(nextHop);
                                }
                                else {
                                    // Add the node to the possiblyDown set.
                                    router.addToPossiblyDown(nextHop);
                                    // Try again.
                                    handleRecurGetReq.run(req, peer);
                                }
                            }});
                }

            }
        };

    protected Thunk2<RecurGetResp,InetSocketAddress> handleRecurGetResp = 
        new Thunk2<RecurGetResp,InetSocketAddress>() {
            public void run(RecurGetResp msg, InetSocketAddress peer) {

                Pair<GetReq,IterGetState> pair = 
                    active_gets.remove(new Long(msg.seq));
                if (pair == null) {
                    if (logger.isDebugEnabled())
                        logger.debug("Got unexpected " + msg);
                    return;
                }

                GetReq req = pair.first;
                IterGetState state = pair.second;
                if (state == null) {
                    int parallelism = 
                        ((msg.maxvals & ITERATIVE_PARALLELISM_MASK) 
                         >> ITERATIVE_PARALLELISM_SHIFT) + 1;
                    StorageManager.Key placemark = req.placemark;
                    if (placemark == null)
                        placemark = StorageManager.ZERO_KEY;
                    state = new IterGetState(req.key, msg.maxvals, 
                            placemark, msg.seq, parallelism, 
                            curry(sendIterGetResp, req.completion_queue,
                                  req.user_data), req.client);
                }
                if (state.done == null)
                    return; // we've already called the done function

                NodeId n = msg.thisReplica;

                StringBuffer sbuf = new StringBuffer(200);
                sbuf.append("got new recur get resp key=0x");
                sbuf.append(GuidTools.guid_to_string(req.key));
                sbuf.append(" return addr=").append(my_node_id);
                sbuf.append(" seq=").append(msg.seq);
                sbuf.append(" from ").append(n);
                sbuf.append(" replicas=").append(msg.replicas);
                sbuf.append(" synced=").append(msg.synced);
                logger.info(sbuf.toString());

                state.tried.add(n);

                if (state.replicas.isEmpty()) {
                    state.replicas = msg.replicas;
                    state.needed = min(state.replicas.size(), 
                                       state.replicas.size()/2+1);
                }

                // Update responses and allRead.
                state.responses.put(n, msg.values);
                for (NodeId m : msg.synced) {
                    if (!state.responses.containsKey(m)) {
                        state.responses.put(m, 
                                new LinkedList<Pair<Key,ByteBuffer>>());
                    }
                }
                if (!msg.allRead)
                    state.allRead = false;

                // Send requests to any replicas we haven't sent them
                // to yet.
                if (state.responses.size() < state.needed) {
                    for (NodeId m : state.replicas) {
                        if (!state.outstanding.containsKey(m)
                                && !state.tried.contains(m)) {
                            state.sendReq(m, null, null);
                        }
                    }
                }

                state.advanceIterGetReq();
            }
        };


    protected BigInteger MODULUS;
    protected BigInteger my_guid;
    protected NeighborInfo my_neighbor_info;

    public BigInteger calc_dist (BigInteger a, BigInteger b) {
        return GuidTools.calc_dist (a, b, MODULUS);
    }

    public boolean in_range_mod (
	    BigInteger low, BigInteger high, BigInteger query) {
	return GuidTools.in_range_mod (low, high, query, MODULUS);
    }

    /////////////////////////////////////////////////////////////////
    //
    //                New Recursive Style Get Handler
    //
    /////////////////////////////////////////////////////////////////

    protected Thunk9<NodeId,Long,Set<NodeId>,Set<NodeId>,Integer,NodeId,Pair,
                     LinkedList<Pair<Key,ByteBuffer>>,Boolean> sendRecurGetResp =
          new Thunk9<NodeId,Long,Set<NodeId>,Set<NodeId>,Integer,NodeId,Pair,
                     LinkedList<Pair<Key,ByteBuffer>>,Boolean>() {

        public void run(NodeId dest, Long seq, Set<NodeId> replicas, 
                        Set<NodeId> synced, Integer maxvals, NodeId client, 
                        final Pair p, LinkedList<Pair<Key,ByteBuffer>> values,
                        Boolean allRead) {

            StringBuffer sbuf = new StringBuffer(200);
            sbuf.append("local read done for new style recur get req seq=");
            sbuf.append(seq);
            logger.info(sbuf.toString());

            RecurGetResp outb = new RecurGetResp(dest, seq.longValue(),
                    replicas, synced, values, allRead.booleanValue(),
                    my_node_id, maxvals.intValue(), client);

            returnToClient.returnToClient(outb, new Thunk1<Boolean>() {
                    public void run(Boolean notUsed) { 
                        if (p != null) {
                            boolean result = getsBeingProcessed.remove(p);
                            assert result;
                        }
                    }
                });
        }
    };

    /**
     * Does a top-k merge sort on the values received.
     */
    protected Pair<LinkedList<GetValue>,Key> compileResponse(
            int maxvals,
            Map<NodeId,LinkedList<Pair<Key,ByteBuffer>>> responses,
            boolean allRead) {

        LinkedList<GetValue> values = new LinkedList<GetValue>();
        int size = 0;
        maxvals = maxvals & MAXVALS_MASK;
        Key max = ZERO_KEY;
        while (values.size() < maxvals) {
            Key min = null;
            NodeId minReplica = null;
            Iterator<NodeId> i = responses.keySet().iterator();
            while (i.hasNext()) {
                NodeId replica = i.next();
                LinkedList<Pair<Key,ByteBuffer>> l = responses.get(replica);
                if (l.isEmpty()) { i.remove(); continue; }
                Key k = l.getFirst().first;
                if (min == null) {
                    min = k;  minReplica = replica;
                }
                else {
                    int compare = k.compareTo(min);
                    if (compare < 0) { 
                        min = k;  minReplica = replica;
                    }
                    else if (compare == 0) {
                        l.removeFirst(); if (l.isEmpty()) i.remove(); 
                    }
                }
            }
            if (min == null) {
                break;
            }
            else {
                LinkedList<Pair<Key,ByteBuffer>> l = 
                    responses.get(minReplica);
                Pair<Key,ByteBuffer> p = l.getFirst();
                GetValue gv = new GetValue(p.second,
                                           p.first.ttlRemaining(now_ms()),
                                           p.first.secret_hash);
                CountBuffer cb = new CountBuffer();
                gv.serialize(cb);
                if (size + cb.size() > MAX_GET_RESP_SIZE)
                    break;
                l.removeFirst();
                if (l.isEmpty()) responses.remove(minReplica);
                values.addLast(gv);
                size += cb.size();
                max = p.first;
            }
        }

        if (allRead && responses.isEmpty())
            max = ZERO_KEY;

        return Pair.create(values, max);
    }

    /////////////////////////////////////////////////////////////////
    //
    //              Iterative Get Client State Machine
    //
    /////////////////////////////////////////////////////////////////

    protected class IterGetState {
        public BigInteger key;
        public int maxvals;
        public Key placemark;
        public long seq;
        public int parallelism;
        public int needed;
        public NodeId client;
        public boolean allRead = true;
        public Thunk2<LinkedList<GetValue>,Key> done;
        public Map<NeighborInfo,VirtualCoordinate> nodes = 
            new LinkedHashMap<NeighborInfo,VirtualCoordinate>();
        public Map<NodeId,Object> outstanding = 
            new LinkedHashMap<NodeId,Object>();
        public Set<NodeId> tried = new LinkedHashSet<NodeId>();
        public Set<NodeId> replicas = new LinkedHashSet<NodeId>();
        public Map<NodeId,LinkedList<Pair<Key,ByteBuffer>>> responses = 
            new LinkedHashMap<NodeId,LinkedList<Pair<Key,ByteBuffer>>>();

        public IterGetState(BigInteger k, int m, Key p, long s, int a,
                            Thunk2<LinkedList<GetValue>,Key> d, NodeId c) {
            key = k; maxvals = m; placemark = p; seq = s; done = d;
            parallelism = a; client = c;
        }

        public void startIterGetReq() {
            for (NeighborInfo ni : router.leafSet().as_set()) {
                VirtualCoordinate vc = router.coordinate(ni);
                nodes.put(ni, vc);
            }
            for (NeighborInfo ni : router.routingTable().as_list()) {
                VirtualCoordinate vc = router.coordinate(ni);
                nodes.put(ni, vc);
            }
            // Check local node first.
            sendReq(my_node_id, null, null);
        }

        protected void advanceIterGetReq() {
            if (done == null)
                return;

            if (replicas.isEmpty()) {
                // Keep routing towards the root.
                VirtualCoordinate myVC = vivaldi.localCoordinates();
                while ((!nodes.isEmpty()) && (outstanding.size()<parallelism)) {
                    LinkedHashMap<NeighborInfo,Long> n = 
                        new LinkedHashMap<NeighborInfo,Long>();
                    for (NeighborInfo ni : nodes.keySet()) {
                        VirtualCoordinate vc = nodes.get(ni);
                        long lat = ((vc == null) ? Long.MAX_VALUE 
                                : round(ceil(myVC.distance(vc))));
                        n.put(ni, new Long(lat));
                    }
                    NeighborInfo result = calcNextHop(key, maxvals, n);
                    if (result.equals(my_neighbor_info)) {
                        // We're the root.
                        nodes.clear();
                    }
                    else {
                        nodes.remove(result);
                    }
                    sendReq(result.node_id, n.get(result), result.guid);
                }
            }
            else {
                if (responses.size() >= needed || outstanding.isEmpty()) {
                    StringBuffer sbuf = new StringBuffer(200);
                    sbuf.append(" iterative get req key=0x");
                    sbuf.append(GuidTools.guid_to_string(key));
                    sbuf.append(" return addr=").append(my_node_id);
                    sbuf.append(" seq=").append(seq);
                    sbuf.append(" done");
                    logger.info(sbuf.toString());

                    Pair<LinkedList<GetValue>,Key> p = 
                        compileResponse(maxvals, responses, allRead);
                    done.run(p.first, p.second);
                    done = null;

                    // Cancel any outstanding requests.
                    for (Object token : outstanding.values()) {
                        if (token != null) 
                            rpc.cancelSend(token);
                    }
                    outstanding.clear();
                }
                else {
                    StringBuffer sbuf = new StringBuffer(200);
                    sbuf.append("iterative get req key=0x");
                    sbuf.append(GuidTools.guid_to_string(key));
                    sbuf.append(" return addr=").append(my_node_id);
                    sbuf.append(" seq=").append(seq);
                    sbuf.append(" replicas=").append(replicas.size());
                    sbuf.append(" responses=").append(responses.size());
                    sbuf.append(" needed=").append(needed);
                    sbuf.append(" outstanding=").append(outstanding.size());
                    logger.info(sbuf.toString());
                }
            }
        }

        protected void sendReq(NodeId n, Long predictedRTT, BigInteger id) {
            StringBuffer sbuf = new StringBuffer(200);
            sbuf.append("sending iterative get req key=0x");
            sbuf.append(GuidTools.guid_to_string(key));
            sbuf.append(" return addr=").append(my_node_id);
            sbuf.append(" seq=").append(seq);
            sbuf.append(" to ").append(n);
            if (id != null)
                sbuf.append(", ").append(guid_to_string(id));
            sbuf.append(" predicted RTT=").append(predictedRTT);
            logger.info(sbuf.toString());

            IterGetReq req = new IterGetReq(key, maxvals, placemark, seq,
                                            vivaldi.localCoordinates(),
                                            client);
            Object token = rpc.sendRequest(n, req, 5, 
                    IterGetResp.class, 
                    curry(iterGetResp, n, predictedRTT, new Long(timer_ms())),
                    curry(iterGetTimeout, n));
            outstanding.put(n, token);
        }

        protected Thunk4<NodeId,Long,Long,IterGetResp> iterGetResp = 
            new Thunk4<NodeId,Long,Long,IterGetResp>() {
                public void run(NodeId n, Long predictedRTT, 
                                Long startTimeMillis, IterGetResp resp) {
                    if (done == null)
                        return; // we've already called the done function

                    outstanding.remove(n);
                    tried.add(n);

                    if (resp.replicas != null) {

                        StringBuffer sbuf = new StringBuffer(200);
                        sbuf.append("got iterative get resp key=0x");
                        sbuf.append(GuidTools.guid_to_string(key));
                        sbuf.append(" return addr=").append(my_node_id);
                        sbuf.append(" seq=").append(seq);
                        sbuf.append(" from ").append(n);
                        sbuf.append(" replicas=").append(resp.replicas);
                        sbuf.append(" synced=").append(resp.synced);
                        sbuf.append(" predicted RTT=").append(predictedRTT);
                        sbuf.append(" actual RTT=");
                        sbuf.append(timer_ms() - startTimeMillis.longValue());
                        logger.info(sbuf.toString());

                        // The node we talked to knew all the replicas.
                        if (replicas.isEmpty()) {
                            replicas = resp.replicas;
                            needed = min(replicas.size(), replicas.size()/2+1);
                        }

                        // Update responses and allRead.
                        responses.put(n, resp.values);
                        for (NodeId m : resp.synced) {
                            if (!responses.containsKey(m)) {
                                responses.put(m, 
                                        new LinkedList<Pair<Key,ByteBuffer>>());
                            }
                        }
                        if (!resp.allRead)
                            allRead = false;

                        // Cancel any outstanding requests to non-replicas.
                        Iterator<NodeId> i = outstanding.keySet().iterator();
                        while (i.hasNext()) {
                            NodeId m = i.next();
                            if (!replicas.contains(m)) {
                                Object token = outstanding.get(m);
                                if (token != null) 
                                    rpc.cancelSend(token);
                                i.remove();
                            }
                        }

                        // Send requests to any replicas we haven't sent them
                        // to yet.
                        if (responses.size() < needed) {
                            for (NodeId m : replicas) {
                                if (!outstanding.containsKey(m)
                                    && !tried.contains(m)) {
                                    sendReq(m, null, null);
                                }
                            }
                        }
                    }
                    else {
                        StringBuffer sbuf = new StringBuffer(200);
                        sbuf.append("got iterative get resp key=0x");
                        sbuf.append(GuidTools.guid_to_string(key));
                        sbuf.append(" return addr=").append(my_node_id);
                        sbuf.append(" seq=").append(seq);
                        sbuf.append(" from ").append(n);
                        sbuf.append(" neighbors=[");
                        Iterator<Pair<NeighborInfo,VirtualCoordinate>> i =
                            resp.neighbors.iterator();
                        while (i.hasNext()) {
                            Pair<NeighborInfo,VirtualCoordinate> p = i.next();
                            sbuf.append(p.first);
                            if (i.hasNext()) sbuf.append(", ");
                            if (!nodes.containsKey(p.first)
                                && !outstanding.containsKey(p.first.node_id)
                                && !tried.contains(p.first.node_id)) {
                                nodes.put(p.first, p.second);
                            }
                        }
                        sbuf.append("]");
                        sbuf.append(" predicted RTT=").append(predictedRTT);
                        sbuf.append(" actual RTT=");
                        sbuf.append(timer_ms() - startTimeMillis.longValue());
                        logger.info(sbuf.toString());
                    }
                    advanceIterGetReq();
                }
            };

        protected Thunk1<NodeId> iterGetTimeout = new Thunk1<NodeId>() {
                public void run(NodeId n) {
                    outstanding.remove(n);
                    tried.add(n);
                    advanceIterGetReq();
                }
            };
    }

    /////////////////////////////////////////////////////////////////
    //
    //              Iterative Get Server Functions
    //
    /////////////////////////////////////////////////////////////////

    protected Thunk3<InetSocketAddress,IterGetReq,Object> iterGetReqHandler = 
        new Thunk3<InetSocketAddress,IterGetReq,Object>() {
        public void run(InetSocketAddress peer, IterGetReq req, Object token) {
            int needed = dmgr.desiredReplicas();
            Set<NeighborInfo> replicas = 
                router.leafSet().replicas(req.key, needed);
            // If we know everyone, we can start a quorum.  Otherwise, return
            // the best people we know.

            if (router.leafSet().overlap() || (replicas.size() == needed)) {
                Set<NodeId> r = new LinkedHashSet<NodeId>();
                for (NeighborInfo ni : replicas)
                    r.add(ni.node_id);
                getValues(req.key, req.placemark, req.maxvals,
                          curry(iterGetDone, req, r, token));
            }
            else {
                LinkedHashMap<NeighborInfo,Long> n = 
                    new LinkedHashMap<NeighborInfo,Long>();
                for (NeighborInfo ni : router.leafSet().as_set()) {
                    VirtualCoordinate vc = router.coordinate(ni);
                    long lat = ((vc == null) ? Long.MAX_VALUE 
                            : round(ceil(req.vc.distance(vc))));
                    n.put(ni, new Long(lat));
                }
                for (NeighborInfo ni : router.routingTable().as_list()) {
                    VirtualCoordinate vc = router.coordinate(ni);
                    long lat = ((vc == null) ? Long.MAX_VALUE 
                            : round(ceil(req.vc.distance(vc))));
                    n.put(ni, new Long(lat));
                }

                LinkedList<Pair<NeighborInfo,VirtualCoordinate>> list = 
                    new LinkedList<Pair<NeighborInfo,VirtualCoordinate>>();
                Set<NeighborInfo> nodes = new HashSet<NeighborInfo>();
                for (int i = 0; !n.isEmpty() && i < 3; ++i) {
                    NeighborInfo result = calcNextHop(req.key, req.maxvals, n);
                    n.remove(result);
                    VirtualCoordinate vc = router.coordinate(result);
                    list.addLast(Pair.create(result, vc));
                }
                rpc.sendResponse(new IterGetResp(list, null, null, null, false,
                                                 req.maxvals, req.client), 
                                 token);
            }
        }
    };
        
    protected Thunk5<IterGetReq,Set<NodeId>,Object,
                     LinkedList<Pair<Key,ByteBuffer>>,Boolean> iterGetDone =
        new Thunk5<IterGetReq,Set<NodeId>,Object,
                   LinkedList<Pair<Key,ByteBuffer>>, Boolean>() {
            public void run(IterGetReq req, Set<NodeId> replicas, Object token, 
                            LinkedList<Pair<Key,ByteBuffer>> values, 
                            Boolean allRead) {
                Set<NodeId> synced = new LinkedHashSet<NodeId>();
                for (NodeId n : replicas) {
                    if (!n.equals(my_node_id) && dmgr.synced(req.key, n)) 
                        synced.add(n);
                }
                rpc.sendResponse(
                        new IterGetResp(null, replicas, synced, values,allRead,
                                        req.maxvals, req.client),
                        token);
             }
        };
}

