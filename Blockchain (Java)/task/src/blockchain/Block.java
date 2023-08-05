package blockchain;

import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Block {

    private int minerId;
    private long id;
    private final long TIMESTAMP = new Date().getTime();
    private long magicNumber;
    private String previousBlockSha256;
    private String sha256;
    private long duration;
    private String prefixLength;
    private static Map<String, Integer> ledger = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<String> BLOCK_TXS = new CopyOnWriteArrayList<>(
            List.of("miner2 sent 30 VC to miner1"
                    , "miner2 sent 30 VC to miner3"
                    , "miner2 sent 30 VC to Nick"
                    , "miner2 sent 10 VC to Bob"
                    , "miner1 sent 10 VC to Alice"
                    , "Nick sent 1 VC to ShoesShop"
                    , "Nick sent 2 VC to FastFood" 
                    , "Nick sent 15 VC to CarShop" 
                    , "miner1 sent 90 VC to CarShop"
                    , "CarShop sent 10 VC to Worker1"
                    , "CarShop sent 10 VC to Worker2"
                    , "CarShop sent 10 VC to Worker3"
                    , "CarShop sent 30 VC to Director1"
                    , "CarShop sent 45 VC to CarPartsShop"
                    , "Bob sent 5 VC to GamingShop"
                    , "Alice sent 5 VC to BeautyShop"));

    private static final CopyOnWriteArrayList<String> BLOCK_MESSAGES = new CopyOnWriteArrayList<>(
            List.of("Tom: Hey, I'm first!"
                    , "Sarah: It's not fair!"
                    , "Sarah: You always will be first because it is your blockchain!"
                    , "Sarah: Anyway, thank you for this amazing chat."
                    , "Tom: You're welcome :)"
                    , "Nick: Hey Tom, nice chat"));
    private CopyOnWriteArrayList<String> blockData = new CopyOnWriteArrayList<>();

    public void proofOfWork(int prefixLength) {
        String prefix = StringUtil.repeatCharacter('0', prefixLength);
        setSha256(StringUtil.applySha256("Id: " + id + "\n" + "Timestamp: " + TIMESTAMP));
        long startTime = System.currentTimeMillis();
        setMagicNumber(ThreadLocalRandom.current().nextLong(100000000));
        while (!getSha256().substring(0,prefixLength).equals(prefix)) {
            setMagicNumber(ThreadLocalRandom.current().nextLong(100000000));
            setSha256(StringUtil.applySha256(
                    "Created by miner # " + getMinerId() + "\n" +
                            "Id: " + getId() + "\n" +
                            "Timestamp: " + getTimestamp() + "\n" +
                            "Magic number: " + getMagicNumber() + "\n" +
                            "Hash of the previous block:\n" + getPreviousBlockSha256() + "\n" +
                            "Block data: " + getBlockData()));
        }
        setDuration(System.currentTimeMillis() - startTime);
    }

    public int getMinerId() {
        return minerId;
    }

    public void setMinerId(int minerId) {
        this.minerId = minerId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return TIMESTAMP;
    }

    public long getMagicNumber() {
        return magicNumber;
    }

    public void setMagicNumber(long magicNumber) {
        this.magicNumber = magicNumber;
    }

    public String getPreviousBlockSha256() {
        return previousBlockSha256;
    }

    public void setPreviousBlockSha256(String previousBlockSha256) {
        this.previousBlockSha256 = previousBlockSha256;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getPrefixLength() {
        return prefixLength;
    }

    public void setPrefixLength(String prefixLength) {
        this.prefixLength = prefixLength;
    }

    public static Map<String, Integer> getLedger() {
        List<String> distinctPersons = BLOCK_TXS.stream()
                .flatMap(transaction -> {
                    String[] array = transaction.split(" ");
                    return List.of(array[0], array[array.length - 1]).stream();
                })
                .distinct()
                .toList();
        return distinctPersons.stream()
                .collect(Collectors.toConcurrentMap(person -> person, person -> 100));
    }

    public static CopyOnWriteArrayList<String> getBLOCK_TXS() {
        return BLOCK_TXS;
    }

    public CopyOnWriteArrayList<String> getBlockData() {
        return blockData;
    }

    public void setBlockData(CopyOnWriteArrayList<String> blockData) {
        this.blockData = blockData;
    }

    @Override
    public String toString() {
        return "Block{" +
                "minerId=" + minerId +
                ", id=" + id +
                ", timestamp=" + TIMESTAMP +
                ", magicNumber=" + magicNumber +
                ", previousBlockSha256='" + previousBlockSha256 + '\'' +
                ", sha256='" + sha256 + '\'' +
                ", duration=" + duration +
                '}';
    }
}

class StringUtil {
    /* Applies Sha256 to a string and returns a hash. */
    public static String applySha256(String input){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            /* Applies sha256 to our input */
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte elem: hash) {
                String hex = Integer.toHexString(0xff & elem);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String repeatCharacter(char ch, int count) {
        StringBuilder result = new StringBuilder();
        result.append(String.valueOf(ch).repeat(Math.max(0, count)));
        return result.toString();
    }
}