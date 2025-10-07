import java.util.*;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
    
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    //side A class variables:
    int numTransmittedPackets_A;    //number of unique packets sent by A
    int numRetransmissions_A;   //number of retransmissions from A
    int numAcksReceived_A;  //number of acks that A receives from B
    Vector<Integer> outstandingPackets;   //vector to show current outstanding packets

    //side B class variables:
    int numLayer5Deliveries_B;  //number of packets delivered to layer 5 of B
    int numAcksSent_B;  //number of ACK messages sent by B

    //general class variables
    int numCorruptPackets;  //total number of corrupted packets encountered

    //general class methods
    //returns ratio of lost packets
    private double lostPacketRatio() {
        return
                (double) (numRetransmissions_A - numCorruptPackets)
                        / ((numTransmittedPackets_A + numRetransmissions_A) + numAcksSent_B);
    }

    //returns ratio of corrupted packets
    private double corruptPacketRatio() {
        return
                (double) numCorruptPackets
                        / ((numTransmittedPackets_A + numRetransmissions_A) + numAcksSent_B
                        - (numRetransmissions_A - numCorruptPackets));
    }

    //helper method, calculates a checksum to add to a message header
    private int createChecksum(String payload){
        //corruption is only applied to the message string of a packet in this code, so the checksum
        // will only account for the message string.
        //This method uses the 1s complement checksum with the bits of the characters in the string
        int sum = 0;
        for (char c : payload.toCharArray()) {
            sum += c;              //add the int value of the character
            sum = (sum & 0xFF) + (sum >> 8);  //remove overflow bits and add them back to the sum
        }
        sum ^= 0xFF;    //invert bits
        return sum;
    }

    //gets checksum and adds it to given packet
    // In this code, the checksum is NOT optional so no extra steps are needed to handle all-0s checksums
    private void addChecksum(Packet packet){
        int checksum = createChecksum(packet.getPayload());
        packet.setChecksum(checksum);
    }

    //verifies the checksum of a received message
    private boolean verifyChecksum(Packet packet){
        //Since the checksum is generated with 1s complement and the bits of the characters in the string,
        // it is verified in the same way.
        int checksum = createChecksum(packet.getPayload());
        checksum += packet.getChecksum();

        return checksum == 0;
    }

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
	WindowSize = winsize;
	LimitSeqNo = winsize*2; // set appropriately; assumes SR here!
	RxmtInterval = delay;
    }

    
    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
        //you need to create a packet, then call toLayer3 to send it to layer 3.
        //The event creation for the timeline should already be handled.
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        //I'll need a checksum calculation process.
        //Perhaps 1's complement?

        //In any case, check the checksum for corruption

        //if the ack is valid, figure out a way to track the packets with valid acks
        //if this is the last packet we need an ack for, stop the timer
        //AND then restart it since we're sending new packets (?)
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
        //consider the aforementioned tracking of outstanding packets that were just acked
        //If this occurs, we need to check which packets need to be retransmitted
        //then we need to retransmit them
        //AND we need to restart the timer (?)
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        numTransmittedPackets_A = 0;
        numRetransmissions_A = 0;
        numCorruptPackets = 0;
        numAcksReceived_A = 0;
        outstandingPackets = new Vector<>();
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {

    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        numAcksSent_B = 0;
        numLayer5Deliveries_B = 0;
    }

    // Use to print final statistics
    protected void Simulation_done()
    {
    	// TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
    	System.out.println("\n\n===============STATISTICS=======================");
    	System.out.println("Number of original packets transmitted by A: " + numTransmittedPackets_A);
    	System.out.println("Number of retransmissions by A: " + numRetransmissions_A);
    	System.out.println("Number of data packets delivered to layer 5 at B: " + numLayer5Deliveries_B);
    	System.out.println("Number of ACK packets sent by B: " + numAcksSent_B);
    	System.out.println("Number of corrupted packets: " + numCorruptPackets);
    	System.out.println("Ratio of lost packets: " + lostPacketRatio());
    	System.out.println("Ratio of corrupted packets: " + corruptPacketRatio());
    	System.out.println("Average RTT:" + "<YourVariableHere>");
    	System.out.println("Average communication time:" + "<YourVariableHere>");
    	System.out.println("==================================================");

    	// PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
    	System.out.println("\nEXTRA:");
    	System.out.println("Number of ACK packets received by A: " + numAcksReceived_A);
    }	

}
