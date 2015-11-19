package Practical2;


import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;

import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import java.lang.reflect.Type;


public class Processor extends TypedAtomicActor {

    protected TypedIOPort input;
    protected TypedIOPort output;
    protected TypedIOPort discard;
    protected TypedIOPort utilisation;
    protected double timeRef = 0;
    protected int compTime = 0;
    protected String taskName;


    public Processor(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException  {

        super(container, name);

        input = new TypedIOPort(this, "input", true, false);
        output = new TypedIOPort(this, "output", false, true);
        discard = new TypedIOPort(this, "discard", false, true);
        utilisation = new TypedIOPort(this, "utilisation", false, true);
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
            System.out.println("Processor is busy. Discarding token");
            discard.send(0, token);
            //utilisation.send(1, new DoubleToken(100));
            return;
        }

        IntToken intToken = (IntToken)((RecordToken) token).get("comptime");
        compTime = intToken.intValue();
        this.mapTask(compTime);
        timeRef = this.getTime();

        System.out.println("Received " + taskName + " task with computation time = " + compTime + "  at time = " + timeRef);

        utilisation.send(1, new DoubleToken(0));
        output.send(0, token);
    }

}
