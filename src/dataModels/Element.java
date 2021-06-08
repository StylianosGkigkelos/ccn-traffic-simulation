package dataModels;

// Class Element is used to describe a user in a channel
public class Element {
    public double timein, timeout, x , y;
    public int occupied;

    public Element(){
        timein = 0;
        timeout = 0;
        occupied = 0;
        x = 0;
        y = 0;
    }

    @Override
    public String toString() {
        return "Element{" +
                "timein=" + timein +
                ", timeout=" + timeout +
                ", occupied=" + occupied +
                '}';
    }
}
