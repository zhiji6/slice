/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ipseorama.slice.stun;

import com.ipseorama.slice.IceEngine;
import com.ipseorama.slice.ORTC.RTCIceCandidatePair;
import com.ipseorama.slice.ORTC.enums.RTCIceRole;
import com.phono.srtplight.Log;
import java.util.ArrayList;

/**
 *
 * @author tim
 */
public class IceStunBindingTransaction extends StunBindingTransaction {

    private final int reflexPri;
    private RTCIceRole role;
    private final long tiebreaker;
    private final String outboundUser;
    private final boolean mayNominate;
    private RTCIceCandidatePair candidatePair;

    public IceStunBindingTransaction(IceEngine ice, String host, int port,
            int reflexPri,
            RTCIceRole role,
            long tiebreaker,
            String outboundUser,
    boolean mayNominate) {
        super(ice, host, port);
        this.reflexPri = reflexPri;
        this.role = role;
        this.tiebreaker = tiebreaker;
        this.outboundUser = outboundUser;
        this.mayNominate = mayNominate;
    }

    @Override
    public StunPacket buildOutboundPacket() {
        StunPacket bind = super.buildOutboundPacket();
        if (bind != null) {
            populateAttributes(bind);
        }
        return bind;
    }

    private void populateAttributes(StunPacket bind) {

        ArrayList<StunAttribute> attrs = new ArrayList();
        StunAttribute.addPriority(attrs, reflexPri);
        StunAttribute.addUsername(attrs, outboundUser);
        StunAttribute.addSoftware(attrs);
        StunAttribute.addIceCon(attrs, role, tiebreaker);
        if (mayNominate && role.equals(RTCIceRole.CONTROLLING)){
            StunAttribute.addUseCandidate(attrs);
        }
        StunAttribute.addMessageIntegrity(attrs);
        StunAttribute.addFingerprint(attrs);

        bind.setAttributes(attrs);
    }

    String getUserName() {
        return outboundUser;
    }

    public boolean sentUseCandidate(){
        return role.equals(RTCIceRole.CONTROLLING);
    }
    @Override
    public void received(StunPacket r) {
        if (r instanceof StunBindingResponse) {
            response = (StunBindingResponse) r;
            if (response.hasRequiredAttributes()) {
                complete = true;
                if (oncomplete != null) {
                    oncomplete.onEvent(this);
                }
            } else {
                Log.debug("Ignored incomplete response");
            }
        } else if (r instanceof StunErrorResponse){
            StunErrorResponse error = (StunErrorResponse) r;
            if (error.hasRequiredAttributes()){
                StunAttribute ecodat = error.getAttributeByName("ERROR-CODE");
                if (ecodat != null){
                    StunAttribute.ErrorAttribute e = ecodat.getError();
                    Log.warn("Error received "+e);
                    if (e.code == 87){
                        // role conflict
                        if (onerror != null) {
                            onerror.onEvent(e);
                        }
                        if (role == role.CONTROLLING){
                            role = role.CONTROLLED;
                        }
                    }
                }
            }
        } else {
            Log.warn("unexpected packet type into StunBinding transaction " + r.getClass().getSimpleName());
        }
    }

    public void setPair(RTCIceCandidatePair pair) {
        candidatePair = pair;
    }
    public RTCIceCandidatePair getPair(){
        return candidatePair;
    }
}
