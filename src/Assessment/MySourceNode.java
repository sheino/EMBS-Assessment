package Assessment;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;

import ptolemy.actor.util.Time;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import java.util.ArrayList;

public class MySourceNode extends TypedAtomicActor {

    // Ports
    protected TypedIOPort input;
    protected TypedIOPort numberOfSyncNodes;
    protected TypedIOPort output;
    protected TypedIOPort channel;

    // Global variables
    protected ArrayList<Node> nodes = new ArrayList<>(); // List of sync nodes
    protected Double currentTime; // current time during simulation
    protected int currentChannel = 15; // channel that is currently being listened to
    protected int currentChannelBeforeSend = 11; // channel that was being listened to before frame was sent to the node
    protected Integer forceSwitchChannel; // channel that we force switch after sending a frame if it is specified
    protected boolean frameJustSent = false; // Indicates whether frame was just sent to node
    protected boolean initialised = false; // Indicates whether actor was initialised

    // Constants
    protected final static double TIME_OFFSET = 0.01; // Time offset
    protected final static double TINY_TIME_OFFSET = 0.0001; // Small time offset
    protected final static double LONG_TIME_OFFSET = 5.9; // Long time offset

    public MySourceNode(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException
    {
        super(container, name);

        input = new TypedIOPort(this, "input", true, false);
        numberOfSyncNodes = new TypedIOPort(this, "numberOfSyncNodes", true, false);
        output = new TypedIOPort(this, "output", false, true);
        channel = new TypedIOPort(this, "channel", false, true);
    }

    // Changes channel to the one specified in the argument
    public void changeChannel(int channel) throws IllegalActionException
    {
        this.currentChannel = channel;
        this.channel.send(0, new IntToken(channel));
    }

    // Decides to which channel we should listen to
    public int decideWitchChannelToSwitchTo()
    {
        for(Node node: this.nodes)
        {
            // Listen to the channel where n is unknown. This helps with synchronisation
            // Ignore channels that are in ignore state and have ignoreStopTime set
            if(node.t == null && (!node.tempIgnore || node.tempIgnoreStopTime == null))
            {
                return node.channel;
            }
        }

        // Otherwise stay on a current channel
        return this.currentChannel;
    }

    // Sends frame to the node
    public void sendFrame(int channel) throws IllegalActionException
    {
        // Saves current channel
        this.currentChannelBeforeSend = this.currentChannel;

        // Changes channel to the one that frame is being sent to
        this.changeChannel(channel);
        this.output.send(0, new IntToken(0));

        // Sets boolean flag in order to return to previous channel after the frame was sent
        this.frameJustSent = true;
        this.setFireTime(this.currentTime + TIME_OFFSET);
    }

    // Tells director when to fire next, based on argument
    public void setFireTime(double time) throws  IllegalActionException
    {
        Time nextFire = new Time(this.getDirector(), time);
        getDirector().fireAt(this, nextFire);
    }

    // Sets the last received frame for nodes and attempts to calculate t or when is the next reception period for the node
    public void setLastReceivedFrameForNode(Node node, int frame) throws  IllegalActionException
    {
        if (node.lastBeaconFrame != null) {
            if (node.t == null) {

                // Dealing with situation where n = 1
                if (node.lastBeaconFrame == 1 && frame == 1)
                {
                    node.tempIgnore = false;
                    this.forceSwitchChannel = null;

                    // calculate t
                    node.t = (this.currentTime - node.lastBeaconFrameTime) / 12;
                    node.ReceiveStateTime = this.currentTime + node.t;

                    // Set the flag that this nodes n = 1; so we could calculate next Receive state time directly
                    node.nEqualsTo1 = true;

                    // Sets fire time for next reception period
                    this.setFireTime(node.ReceiveStateTime);
                }
                else
                {

                    if (node.lastBeaconFrame > frame)
                    {
                        // Calculate t for the node
                        node.t = (this.currentTime - node.lastBeaconFrameTime) / (node.lastBeaconFrame - frame);

                        // Sets fire time for reception period
                        node.ReceiveStateTime = this.currentTime + frame * node.t;
                        this.setFireTime(node.ReceiveStateTime);

                        // Decide witch channel to switch to next
                        this.changeChannel(this.decideWitchChannelToSwitchTo());
                    }

                    // if n > 1  we need different equation to calculate t
                    if(node.tempIgnore)
                    {
                        node.tempIgnore = false;
                        this.forceSwitchChannel = null;

                        // Calculate t for the node
                        node.t = (this.currentTime - node.lastBeaconFrameTime) / 12;

                        // Sets fire time for reception period
                        node.ReceiveStateTime = this.currentTime + frame * node.t;
                        this.setFireTime(node.ReceiveStateTime);

                        // Decide witch channel to switch to next
                        this.changeChannel(this.decideWitchChannelToSwitchTo());
                    }
                }
            } else
            {

                // If t is known  calculate next receive period for node
                node.ReceiveStateTime = this.currentTime + frame * node.t;
                this.setFireTime(node.ReceiveStateTime);

                this.changeChannel(this.decideWitchChannelToSwitchTo());
            }
        }
        else
        {
            // if first frame is one we go into ignore state and check other channels.
            if (frame == 1)
            {
                node.tempIgnore = true;

                // Fire when minimum ignore time has passed. To change state for the node
                node.tempIgnoreStopTime = currentTime + LONG_TIME_OFFSET;
                this.setFireTime(node.tempIgnoreStopTime);
                this.changeChannel(this.decideWitchChannelToSwitchTo());
            }
        }

        // Setting frame and frame received time
        node.lastBeaconFrame = frame;
        node.lastBeaconFrameTime = this.currentTime;
    }

    // Loops through all nodes checking if any of them are at their reception period and sends frame if that is the case
    // Returns true if frame was sent and false otherwise
    public boolean checkNodesForReceiveState() throws IllegalActionException
    {
        // Checking whether any nodes are at their reception period
        for (Node node : nodes) {
            if ((node.ReceiveStateTime != null && node.numberOfFramesReceived < 2)
                    && (Double.compare(this.currentTime, node.ReceiveStateTime - TINY_TIME_OFFSET) >= 0
                    && Double.compare(this.currentTime, node.ReceiveStateTime + TIME_OFFSET) <= 0) )
            {
                // Sends the frame to that node
                this.sendFrame(node.channel);

                // Increments the number of Frames sent to that specific node
                node.numberOfFramesReceived++;

                double time;
                if(node.nEqualsTo1)
                {
                    // if n = 1 calculate next reception period directly
                    time = this.currentTime + 12 * node.t;
                    node.ReceiveStateTime = time;
                }
                else
                {
                    // if n > 1 calculate when next frame will be sent
                    time = this.currentTime + node.t * 11 - 2 * TIME_OFFSET;
                    node.nextFirstFrameTime = time;
                }

                // Sets fire time either when next frame will be sent or when node will enter its reception period
                this.setFireTime(time);
                return true;
            }
        }

        return false;
    }

    // Loops through all nodes checking if any of them are at their ignore state end
    // Returns true if yes and false otherwise
    public boolean checkNodesForTempIgnoreStateEnd()throws IllegalActionException
    {
        for(Node node : nodes)
        {
            if ((node.tempIgnoreStopTime != null && node.tempIgnore)
                    &&  Double.compare(node.tempIgnoreStopTime, this.currentTime + TINY_TIME_OFFSET) <= 0)
            {
                node.tempIgnoreStopTime = null;

                // We force to switch to this channel until we get a frame for it.
                // Its to make sure we do not accidentally miss it
                this.forceSwitchChannel = node.channel;
                this.changeChannel(node.channel);

                return true;
            }
        }

        return false;
    }

    // Loops through all nodes checking whether any of them are at transmit state
    // Returns true if yes and falls otherwise
    public boolean checkNodesForTransmitState() throws IllegalActionException
    {
        for(Node node : nodes)
        {
            if ((node.nextFirstFrameTime != null)
                    && (Double.compare(node.nextFirstFrameTime, this.currentTime - TINY_TIME_OFFSET) >= 0
                    &&  Double.compare(node.nextFirstFrameTime, this.currentTime + TINY_TIME_OFFSET) <= 0))
            {
                // changes channel to the one that is about to transmit
                this.changeChannel(node.channel);
                this.setFireTime(this.currentTime + TIME_OFFSET);
                return true;
            }
        }

        return false;
    }

    // Gets the number of sync nodes and initialises the actor
    // Returns true if actor was initialised; false if actor was already initialised
    public boolean initialise()throws IllegalActionException
    {
        if(!this.initialised)
        {
            Token token = numberOfSyncNodes.get(0);
            int numberOfNodes = ((IntToken)token).intValue();

            // Initialises the number of nodes based on numberOfSyncNodes token input
            for(int i = 0; i < numberOfNodes; i++)
            {
                nodes.add(new Node(i + 11));
            }

            // Makes sure we are listening to the default channel
            this.changeChannel(this.currentChannel);

            this.initialised = true;
            return true;
        }

        return false;
    }

    public void fire() throws IllegalActionException{

        // Gets the number of sync nodes and initialises the actor
        if(this.initialise()){return;}

        // Sets current time of the simulation
        this.currentTime = this.getDirector().getModelTime().getDoubleValue();

        // Switches back to the channel that actor was listening to before send or to forceSwitchChannel if it exists
        if(this.frameJustSent)
        {
            this.frameJustSent = false;
            if(this.forceSwitchChannel == null)
            {
                this.changeChannel(this.currentChannelBeforeSend);
            }
            else
            {
                this.changeChannel(this.forceSwitchChannel);
            }

            return;
        }


        // Checks weather any nodes are at receive state and send the frame
        // If true we return from the fire method in order not to accidentally overwrite any tokens
        if(this.checkNodesForReceiveState()){return;}

        // Checks weather any nodes are at the end of ignore state
        // If true we return from the fire method in order not to accidentally overwrite any tokens
        if(this.checkNodesForTempIgnoreStateEnd()){return;}

        // Checks weather any nodes are at transmit state and send the frame
        // If true we return from the fire method in order not to accidentally overwrite any tokens
        if(this.checkNodesForTransmitState()){return;}

        // Gets the token if it exists
        if(input.hasToken(0))
        {
            Token token = input.get(0);
            int frame = ((IntToken)token).intValue();


            // Sets the frame value for the node and does some calculations if applicable
            this.setLastReceivedFrameForNode(nodes.get(currentChannel - 11), frame);
        }
    }
}
