import java.util.ArrayList;
import java.util.List;

public class Posting {
    int docID;
    List<Integer> positions;

    public Posting(int docID) {
        this.docID = docID;
        positions = new ArrayList<>();
    }

    public int getDocID() {
        return docID;
    }

    public List<Integer> getPositions() {
        return positions;
    }

    public List<Integer> addPosition(int pos) {
        positions.add(pos);
        return positions;
    }
}