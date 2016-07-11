/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.phono.srtplight.Log;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author thp This class manages a list of transactions - it extends HashMap
 * and adds convenience methods
 */
public class StunTransactionManager extends HashMap<Integer, StunTransaction> {

    long NAPLEN = 1000;

    public StunTransactionManager() {
        super();
    }
    
    public void addTransaction(StunTransaction t) {
        this.put(t.getTidHash(), t);
    }

    public void receivedPacket(StunPacket p){
        Log.debug("recvd stun packet from "+p.getFar());
        Integer tid = Arrays.hashCode(p.getTid());
        StunTransaction t = this.get(tid);
        if (t != null) {
            t.received(p);
        } else {
            Log.verb("no matching transaction");
        }
    }
    
    public void removeComplete(){
        this.values().removeIf((StunTransaction t)-> {return t.isComplete();});
    }
    /**
     *
     * @return the next time an action is due - or now + NAPTIME, whichever is
     * sooner
     */
    public long nextDue() {
        Iterator<StunTransaction> it = this.values().iterator();
        long ret = System.currentTimeMillis() + NAPLEN;
        while (it.hasNext()) {
            StunTransaction t = it.next();
            if (!t.isComplete() && (t.getDueTime() < ret)) {
                ret = t.getDueTime();
            }
        }
        return ret;
    }

    public List<StunPacket> transact(long now) {
        List<StunPacket> pkts = this.values().stream().filter((StunTransaction t) -> {
            return !t.isComplete() && t.getDueTime() <= now;
        }).map((StunTransaction t) -> {
            return t.buildOutboundPacket();
        }).collect(Collectors.toList());
        return pkts;
    }

}
