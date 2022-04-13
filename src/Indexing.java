import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class Indexing {
    private Map<String, List<Posting>> invIndex;
    private ArrayList<String> sceneNames;
    private JSONArray jsonScenes;
    private Map<String, Integer> playLengths;

    public Indexing() {
        invIndex = new LinkedHashMap<>();
        sceneNames = new ArrayList<>();
        jsonScenes = new JSONArray();
        playLengths = new HashMap<>();
    }

    /**
     * Build an inverted index with positional information that maps terms to Posting lists
     * @param inFile document collection to read in
     */
    private void buildII(String inFile) {
        try {
            JSONParser parser = new JSONParser();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(inFile)), "UTF-8"));
            JSONObject jsonObject = (JSONObject) parser.parse(br);                  // Parse file into JSONObject
            jsonScenes = (JSONArray) jsonObject.get("corpus");                      // Extract scene's elements
            int sumSceneLen = 0;
            String longestScene = "", shortestScene = "";
            int longestLen = 100, shortestLen = 100,  thisLen;
            for (int i = 0; i < jsonScenes.size(); i++) {
                JSONObject scene = (JSONObject) jsonScenes.get(i);
                String sceneName = scene.get("sceneId").toString();
                sceneNames.add(sceneName);
                String[] terms = scene.get("text").toString().split("\\s+");
                thisLen = terms.length;
                sumSceneLen += thisLen;
                // Find longest and shortest scenes
                if (thisLen > longestLen) {
                    longestScene = sceneName;
                    longestLen = thisLen;
                }
                else if (thisLen < shortestLen) {
                    shortestScene = sceneName;
                    shortestLen = thisLen;
                }
                // Build a map<playId, playLength>
                String play = scene.get("playId").toString();
                playLengths.putIfAbsent(play, 0);
                playLengths.put(play, playLengths.get(play) + thisLen);
                // Build inverted index
                int pos = 1;
                for (String term : terms) {
                    if (!invIndex.containsKey(term)) {
                        Posting posting = new Posting(i+1);
                        posting.addPosition(pos);
                        List<Posting> postings = new ArrayList<>();
                        postings.add(posting);
                        invIndex.put(term, postings);
                    }
                    else {
                        boolean postingExists = false;
                        for (Posting p : invIndex.get(term)) {
                            if (p.getDocID() == i+1) {
                                p.addPosition(pos);
                                postingExists = true;
                                break;
                            }
                        }
                        if (!postingExists) {
                            Posting posting = new Posting(i+1);
                            posting.addPosition(pos);
                            invIndex.get(term).add(posting);
                        }
                    }
                    pos++;
                }
            }
            System.out.println("Avg scene length: " + sumSceneLen/jsonScenes.size());
            System.out.println("Longest scene: " + longestScene);
            System.out.println("Shortest scene: " + shortestScene);
            // Find longest and shortest plays
            int longestPlayLen = Collections.max(playLengths.values());
            int shortestPlayLen = Collections.min(playLengths.values());
            for (Map.Entry<String, Integer> entry: playLengths.entrySet()) {
                if (entry.getValue() == longestPlayLen)
                    System.out.println("Longest play: " + entry.getKey());
                else if (entry.getValue() == shortestPlayLen)
                    System.out.println("Shortest play: " + entry.getKey());
            }
        } catch (IOException | ParseException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param term the word to find
     * @param docId the document to look through
     * @return number of times specified term occurs in specified doc
     */
    private int getTermFreq(String term, int docId) {
        int count = 0;
        for (String word : invIndex.keySet()) {
            if (word.equals(term)) {
                for (Posting p : invIndex.get(word)) {
                    if (p.getDocID() == docId) {
                        for (Integer pos : p.getPositions())
                            count++;
                        return count;
                    }
                    else if (p.getDocID() > docId)
                        return count;
                }
            }
        }
        return count;
    }

    /**
     * Find scene(s) where any of the terms in "terms" are mentioned.
     * @param terms the list of terms to search all scenes for
     * @return list of sceneIDs
     */
    private List<String> searchScenes(List<String> terms) {
        List<String> scenes = new ArrayList<>();
        for (String word : invIndex.keySet())
            if (terms.contains(word))
                for (Posting p : invIndex.get(word))
                    scenes.add(sceneNames.get(p.getDocID() - 1));
        Collections.sort(scenes);
        return scenes;
    }

    /**
     * Find scene(s) where the phrase is mentioned
     * @param phrase the phrase to find
     * @return list of sceneIDs
     */
    private List<String> searchScenes(String phrase) {
        HashSet<String> scenes = new HashSet<>();
        String[] words = phrase.split("\\s+");                    // ["tropical", "fish"]
        List<List<Posting>> postingLists = new ArrayList<>();
        for (String word : words) {
            if (invIndex.containsKey(word)) {
                postingLists.add(invIndex.get(word));
            }
        }
        boolean checkForNextWord;
        for (Posting first : postingLists.get(0)) {                     //tropical's posting list [1: 1, 3, 7], [2: 6, 17]
            for (int i = 1; i < words.length; i++) {                    //next word = fish
                checkForNextWord = false;
                for (Posting next : postingLists.get(i)) {              //fish's posting list     [1: 2, 4], [2: 18]
                    if (next.getDocID() == first.getDocID()) {          //if same doc
                        for (int firstPos : first.getPositions()) {     //check if next to each other
                            for (int nextPos : next.getPositions()) {
                                if (nextPos == firstPos + 1) {
                                    checkForNextWord = true;
                                    break;
                                }
                                else if (nextPos > firstPos)
                                    break;
                            }
                            if (checkForNextWord)
                                break;
                        }
                    }
                    else if (next.getDocID() > first.getDocID())        // if next is further in collection, don't iterate
                        break;
                    if (checkForNextWord) {
                        first = next;                                   //next might not be first posting
                        break;
                    }
                }
                if (!checkForNextWord)
                    break;
                if (i == words.length-1) {
                    scenes.add(sceneNames.get(first.getDocID() - 1));
                }
            }
        }
        List<String> listScenes = new ArrayList<>(scenes);
        Collections.sort(listScenes);
        return listScenes;
    }

    /**
     * Find the play(s) where term is mentioned.
     * @param term the word to find
     * @return list of playIDs
     */
    private List<String> searchPlays(String term) {
        HashSet<String> plays = new HashSet<>();
        String sceneId;
        for (String word : invIndex.keySet()) {
            if (term.equals(word)) {
                for (Posting p : invIndex.get(word)) {
                    sceneId = sceneNames.get(p.getDocID() - 1);
                    plays.add(sceneId.substring(0, sceneId.indexOf(":")));
                }
            }
        }
        List<String> listPlays = new ArrayList<>(plays);
        Collections.sort(listPlays);
        return listPlays;
    }

    public static void main(String[] args) throws IOException {
        Indexing indexing = new Indexing();
        String inputFile = args.length >= 1 ? args[0] : "shakespeare-scenes.json.gz";
        indexing.buildII(inputFile);
        PrintWriter terms0 = new PrintWriter(new FileWriter("terms0.txt"));
        PrintWriter terms1 = new PrintWriter(new FileWriter("terms1.txt"));
        PrintWriter terms2 = new PrintWriter(new FileWriter("terms2.txt"));
        PrintWriter terms3 = new PrintWriter(new FileWriter("terms3.txt"));
        PrintWriter phrase0 = new PrintWriter(new FileWriter("phrase0.txt"));
        PrintWriter phrase1 = new PrintWriter(new FileWriter("phrase1.txt"));
        PrintWriter phrase2 = new PrintWriter(new FileWriter("phrase2.txt"));
        PrintWriter graph = new PrintWriter(new FileWriter("graph.txt"));

        //Find scene(s) where the words thee or thou are used more frequently than the word you.
        //Create a plot. One series is count of "thee" or "thou" vs. sceneNum and count of "you" vs. sceneNum
        int docId, youCount, theeCount, thouCount;
        List<String> scenes = new ArrayList<>();
        for (int i = 0; i < indexing.sceneNames.size(); i++) {
            docId = i+1;
            youCount = indexing.getTermFreq("you", docId);
            theeCount = indexing.getTermFreq("thee", docId);
            thouCount = indexing.getTermFreq("thou", docId);
            graph.println(i + "," + (int) (theeCount + thouCount) + "," + youCount);
            if (theeCount > youCount || thouCount > youCount)
                scenes.add(indexing.sceneNames.get(i));
        }
        Collections.sort(scenes);
        scenes.forEach(s -> terms0.println(s));
        terms0.close();
        graph.close();

        //Find scene(s) where the place names venice, rome, or denmark are mentioned.
        List<String> places = new ArrayList<String>(Arrays.asList("venice", "rome", "denmark"));
        indexing.searchScenes(places).forEach(s -> terms1.println(s));
        terms1.close();

        //Find the play(s) where the name goneril is mentioned.
        indexing.searchPlays("goneril").forEach(s -> terms2.println(s));
        terms2.close();

        //Find the play(s) where the word soldier is mentioned.
        indexing.searchPlays("soldier").forEach(s -> terms3.println(s));
        terms3.close();

        //Find scene(s) where "poor yorick" is mentioned.
        indexing.searchScenes("poor yorick").forEach(s -> phrase0.println(s));
        phrase0.close();

        //Find the scene(s) where "wherefore art thou romeo" is mentioned.
        indexing.searchScenes("wherefore art thou romeo").forEach(s -> phrase1.println(s));
        phrase1.close();

        //Find the scene(s) where "let slip" is mentioned.
        indexing.searchScenes("let slip").forEach(s -> phrase2.println(s));
        phrase2.close();
    }
}
