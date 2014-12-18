package sample;

import io.netty.buffer.ByteBuf;
import net.tomp2p.connection.Bindings;
import net.tomp2p.connection.PeerConnection;
import net.tomp2p.dht.*;
import net.tomp2p.futures.*;
import net.tomp2p.nat.*;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.util.*;

/**
 * Created by johnson on 11/27/14.
 */
public class MyPeer {
    static Logger logger = LogManager.getLogger();
    static PeerDHT clientPeerDHT;
    static NeighborPeers neighborPeers = new NeighborPeers();
    static final Set<MessageReceiver> messageReceiverSet = new HashSet<MessageReceiver>();

    public static boolean initPeer(String str, String ip) {
        PeerAddress serverPeerAddress;
        try {
            clientPeerDHT = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(str)).ports(Utils.CLIENT_PORT).behindFirewall().start()).start();
            serverPeerAddress = new PeerAddress(Number160.ZERO, InetAddress.getByName(ip), Utils.TARGET_PORT, Utils.TARGET_PORT);
        }
        catch (Exception e) {
            logger.catching(e);
            return false;
        }
        FutureDiscover futureDiscover = clientPeerDHT.peer().discover().peerAddress(serverPeerAddress).start();
        futureDiscover.awaitUninterruptibly();
        if (futureDiscover.isSuccess())
            logger.info("*** FOUND THAT MY OUTSIDE ADDRESS IS " + futureDiscover.peerAddress());
        else {
            logger.warn("*** FAILED " + futureDiscover.failedReason());
//            clientPeerDHT.peer().connectionBean().channelServer().channelServerConfiguration().behindFirewall();
            PeerNAT peerNAT = new PeerBuilderNAT(clientPeerDHT.peer()).start();
//            FutureDiscover futureDiscoverNat = clientPeerDHT.peer().discover().peerAddress(serverPeerAddress).start();
            FutureNAT futureNAT = peerNAT.startSetupPortforwarding(futureDiscover);
            futureNAT.awaitUninterruptibly();
            if (futureNAT.isSuccess()) {
                logger.info("future nat success");
            }
            else {
                logger.warn(futureNAT.failedReason());
                FutureRelayNAT futureRelayNAT = peerNAT.startRelay(futureDiscover, futureNAT);
                futureRelayNAT.awaitUninterruptibly();
                if (futureRelayNAT.isSuccess()) {
                    logger.info("future relay nat success");
                }
                else {
                    logger.error(futureRelayNAT.failedReason());
                }
            }
        }
        serverPeerAddress = futureDiscover.reporter();

        FutureBootstrap futureBootstrap = clientPeerDHT.peer().bootstrap().peerAddress(serverPeerAddress).start();
        futureBootstrap.awaitUninterruptibly();
        if (futureBootstrap.isSuccess()) {
            for (PeerAddress p: futureBootstrap.bootstrapTo())
                logger.info("Bootstrapped to: " + p);
            neighborPeers.addPeer(getIdentification(serverPeerAddress.peerId()));

            clientPeerDHT.peer().objectDataReply(new ObjectDataReply() {
                @Override
                public Object reply(PeerAddress sender, Object request) throws Exception {
                    if (sender.peerId().equals(MyPeer.clientPeerDHT.peerID())) return "";
                    logger.debug("received message from " + sender);
                    ByteBuf byteBuf = Utils.decodeBase64ByteBuf((String)request);
                    for (MessageReceiver messageReceiver: messageReceiverSet) {
                        if (messageReceiver.checkMine(sender.peerId())) {
                            messageReceiver.onReceived(byteBuf);
                            return "OK";
                        }
                    }
                    logger.warn("No receiver detected");
                    return "No receiver detected";
                }
            });

            return true;
        }
        else {
            logger.error(futureBootstrap.failedReason());
            logout();
            return false;
        }
    }

    public static void registerMessageReceiver(MessageReceiver messageReceiver) {
        messageReceiverSet.add(messageReceiver);
    }

    public static PeerAddress getPeerAddress() {
        return clientPeerDHT.peerAddress();
    }

    static String getIdentification(Number160 peerID) {
        return "server";
    }

    public static boolean checkOnLine(Number160 number160) {
        return true;
    }

    public static void logout() {
        clientPeerDHT.shutdown();
        neighborPeers.clear();
    }

    public static void addPeer(String peerName) {
        neighborPeers.put(Number160.createHash(peerName), peerName);
    }

    public static void showMap() {
        for (PeerAddress peerAddress: clientPeerDHT.peerBean().peerMap().all()) {
            logger.debug(peerAddress);
        }
    }
}