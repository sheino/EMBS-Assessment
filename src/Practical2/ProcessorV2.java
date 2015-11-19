package Practical2;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;

import java.util.*;

import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class ProcessorV2 extends TypedAtomicActor {

    protected TypedIOPort input;
    protected TypedIOPort output;
    protected LinkedList queue;
    protected double timeRef = 0;
    protected int compTime = 0;
    protected String taskName;


    public ProcessorV2(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException  {

        super(container, name);

        input = new TypedIOPort(this, "input", true, false);
        output = new TypedIOPort(this, "output", false, true);;
        queue = new LinkedList<Token>();
    }

    public double getTime()
    {
        return this.getDirector().getModelTime().getDoubleValue();
    }

    public void mapTask(int taskCompTime) {

        switch (taskCompTime)
        {
            case 30:
                taskName = "R";
                break;
            case 20:
                taskName = "S";
                break;
            case 140:
                taskName = "T";
                break;
        }

    }



    public void fire() throws IllegalActionException{

        Token token = input.get(0);

        if(this.getTime() - timeRef < compTime)
        {
            System.out.println("Processor is busy. Adding token to the queue size = " + queue.size() + " at time = " + this.getTime());
            queue.addLast(token);
            return;
        }

        if(queue.size() != 0)
        {
            System.out.println("Removing Token from queue size = " + queue.size() + " at time = " + this.getTime());

            queue.addLast(token);
            token = (Token)queue.getFirst();
            queue.removeFirst();
        }

        IntToken intToken = (IntToken)((RecordToken) token).get("comptime");
        compTime = intToken.intValue();
        this.mapTask(compTime);
        timeRef = this.getTime();

        System.out.println("Received " + taskName + " task with computation time = " + compTime + "  at time = " + timeRef);

        output.send(0, token);
    }

}
