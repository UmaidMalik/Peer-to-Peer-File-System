import java.util.Random;
import java.util.UUID;

public class RandomGenerator {

    private static String[] Word = { "cousin", "penalty", "gene", "meat", "debt",
            "year", "song", "restaurant", "hotel", "friendship", "piano", "actor",
            "growth", "pollution", "housing", "sympathy", "freedom", "chocolate",
            "session", "mood", "bird", "message", "river", "midnight", "stamp",
            "pride", "fill", "ball", "veil", "twitch", "trunk", "bronze", "lack",
            "rush", "soap", "lung", "hard", "arch", "art", "stool", "yard", "joy",
            "child", "build", "wealth", "stain", "slab", "shark", "soup",
            "goal", "ear", "debt", "mud", "world", "breath", "lab", "night",
            "news", "tongue", "hair", "map", "shirt", "two", "lake",
            "hall", "law", "son", "clothes", "week", "king", "song"};


    private static Random rand = new Random();

    public static String generateName() {

        return Word[rand.nextInt(Word.length)] +
                Word[rand.nextInt(Word.length)] +
                Word[rand.nextInt(Word.length)];

    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
