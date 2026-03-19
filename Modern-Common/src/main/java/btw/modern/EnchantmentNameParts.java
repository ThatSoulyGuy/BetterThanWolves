package btw.modern;

import java.util.Random;

public class EnchantmentNameParts {

    public static final EnchantmentNameParts instance = new EnchantmentNameParts();

    private Random rand = new Random();
    private String[] wordList = "the elder scrolls klaatu berata niktu xyzzy bless curse light darkness fire air earth water hot dry cold wet ignite snuff embiggen twist shorten stretch fiddle destroy imbue galvanize enchant free limited range of towards inside sphere cube self other ball mental physical grow shrink demon elemental spirit animal creature beast humanoid undead fresh stale ".split(" ");

    public String generateNewRandomName() {
        return this.generateRandomEnchantName();
    }

    public String generateRandomEnchantName() {
        int count = this.rand.nextInt(2) + 3;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(this.wordList[this.rand.nextInt(this.wordList.length)]);
        }
        return sb.toString();
    }

    public void setRandSeed(long seed) {
        this.rand.setSeed(seed);
    }

    public static EnchantmentNameParts getInstance() {
        return instance;
    }
}
