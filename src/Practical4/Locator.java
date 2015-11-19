package Practical4;

import java.util.LinkedList;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import sun.reflect.generics.tree.BaseType;

@SuppressWarnings("serial")
public class Locator extends TypedAtomicActor
{

    protected TypedIOPort input;
    protected TypedIOPort output;
    protected LinkedList<Tuple> distanceList = new LinkedList<>();
    protected Point[] node = new Point[4];

    public Locator(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException
    {
        super(container, name);

        input = new TypedIOPort(this, "input", true, false);
        output = new TypedIOPort(this, "output", false, true);

        output.setTypeEquals(ptolemy.data.type.BaseType.GENERAL);

        node[0] = new Point(0, 800);
        node[1] = new Point(800, 800);
        node[2] = new Point(0,0);
        node[3] = new Point(800, 0);
    }

    public Point calculateCoordinates()
    {
        Point p1 = node[distanceList.get(0).id - 1];
        Point p2 = node[distanceList.get(1).id - 1];
        Point p3 = node[distanceList.get(2).id - 1];

        double tempx = (p2.x - p1.x)/Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
        double tempy = (p2.y - p1.y)/Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
        Point ex = new Point(tempx, tempy);
        double i =  (ex.x * (p3.x - p1.x)) + (ex.y * (p3.y - p1.y));

        tempx = (p3.x - p1.x - i * ex.x)/Math.sqrt(Math.pow((p3.x - p1.x - i * ex.x), 2) + Math.pow((p3.y - p1.y - i * ex.y), 2));
        tempy = (p3.y - p1.y - i * ex.y)/Math.sqrt(Math.pow((p3.x - p1.x - i * ex.x), 2) + Math.pow((p3.y - p1.y - i * ex.y), 2));

        Point ey =  new Point(tempx, tempy);

        double d = Math.sqrt(Math.pow((p3.x - p1.x), 2) + Math.pow((p3.y - p1.y), 2));

        double j = (ey.x * (p3.x - p1.x)) + (ey.y * (p3.y - p1.y));

        double x = (Math.pow(distanceList.get(0).distance, 2) -
                Math.pow(distanceList.get(1).distance, 2) -
                Math.pow(d, 2)) / (2 * d);

        double y = ((Math.pow(distanceList.get(0).distance, 2) -
                Math.pow(distanceList.get(2).distance, 2) +
                Math.pow(i, 2) + Math.pow(j, 2)) / (2 * j)) - ((i * x) / j);

        distanceList.clear();

        System.out.println("Firefighter coordinates x = " + x + " y = " + y);

        return new Point(x, y);
    }

    public boolean assembleTokens(RecordToken token)
    {
        int id = ((IntToken)token.get("id")).intValue();
        Double distance = ((DoubleToken)token.get("distance")).doubleValue();

        if(!distanceList.contains(new Tuple(id, distance)))
        {
            distanceList.add(new Tuple(id, distance));

            if(distanceList.size() == 3)
            {
                return true;
            }
        }

        return false;
    }

    public void pruneDependencies() {
        super.pruneDependencies();
        removeDependency(input, output);
    }

    public Point getGridLocation(double x, double y)
    {
        int gridx;
        int gridy;

        if( x > 0 && x <= 200)
        {
            gridx = 1;
        }
        else if( x <= 400)
        {
            gridx = 2;
        }
        else if( x <= 600)
        {
            gridx = 3;
        }
        else if (x <= 800)
        {
            gridx = 4;
        }
        else
        {
            gridx =  0;
        }

        if( y > 0 && y <= 200)
        {
            gridy = 1;
        }
        else if( y <= 400)
        {
            gridy = 2;
        }
        else if( y <= 600)
        {
            gridy = 3;
        }
        else if (y <= 800)
        {
            gridy = 4;
        }
        else
        {
            gridy =  0;
        }

        System.out.println("Grid coordinates x = " + gridx + " y = " + gridy);

        return new Point(gridx, gridy);

    }

    public void fire() throws IllegalActionException
    {

        Token t = input.get(0);

        if (!this.assembleTokens((RecordToken) t))
        {
            return;
        }

        Point coordinates = this.calculateCoordinates();
        Point gridCoord = this.getGridLocation(coordinates.x , coordinates.y);

        RecordToken token  = new RecordToken(new String[]{"x", "y"},
                new Token[]{new DoubleToken(gridCoord.x), new DoubleToken(gridCoord.y)});

        output.send(0, token);
    }

    public class Point
    {
        double x;
        double y;

        public Point(double x, double y)
        {
            this.x = x;
            this.y = y;
        }


    }

    public class Tuple
    {
        public int id;
        public double distance;

        public Tuple(int a, double b)
        {
            id = a;
            distance = b;
        }
    }


}
