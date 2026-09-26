public class FineList 
{

    private final Node head;
    private final Node tail;

    public FineList() 
    {
        head = new Node(Integer.MIN_VALUE);
        tail = new Node(Integer.MAX_VALUE);

        head.next = tail;
    }

    public boolean add(int value) 
    {
        // TODO:
        // Use hand-over-hand locking.
        return false;
    }

    public boolean remove(int value) 
    {
        // TODO:
        // Use hand-over-hand locking.
        return false;
    }

    public boolean contains(int value) 
    {
        // TODO
        return false;
    }
}