package Assessment;

public class Node {

    public Integer numberOfFramesReceived = 0; // number of frames received
    public Integer lastBeaconFrame; // last received frame
    public Integer channel; // channel that node belongs to
    public Double t; // period
    public Double ReceiveStateTime; // node receive state time
    public Double lastBeaconFrameTime; // last received frame time
    public Double nextFirstFrameTime; // time when next frame will be sent
    public Double tempIgnoreStopTime; // time when next frame will be sent
    public boolean nEqualsTo1 = false; // indicates whether n = 1
    public boolean tempIgnore = false; // indicates whether node is in a sleeping state. In order not to waste time listening to that channel

    // Node representing Sync node
    public Node(int channel)
    {
        this.channel = channel;
    }
}