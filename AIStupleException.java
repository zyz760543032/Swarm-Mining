public class AIStupleException extends Exception{
    String msg;
    AIStuple tuple;

    public AIStuple getTuple() {
        return tuple;
    }

    AIStupleException(AIStuple tuple){
        this.tuple = tuple;
        msg = "There are some errors near tuple:\n\r"+tuple;
    }
    public void print(){
        System.out.println(msg);
    }
}
