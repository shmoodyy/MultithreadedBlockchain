package blockchain;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class Main {
    static final String DIRECTORY = System.getProperty("user.dir") + File.separator;

    public static void main(String[] args) throws Exception {
        int prefixLength = 0;
        List<Block> blockchain = createBlockchain(prefixLength);
        if (validateBlockchain(blockchain)) {
            printBlockchain(blockchain);
        }
    }

    public static List<Block> createBlockchain(int prefixLength) {
        List<Block> blockchain = new ArrayList<>();
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Callable<Block>> tasks = new ArrayList<>();

        generatePrivateKeys();

        addMiner(tasks);

        mine(blockchain, executor, tasks, prefixLength);
        executor.shutdown();

        return blockchain;
    }

    private static void generatePrivateKeys() {
        GenerateKeys gk;
        try {
            gk = new GenerateKeys(1024);
            gk.createKeys();
            gk.writeToFile("MyKeys/publicKey", gk.getPublicKey().getEncoded());
            gk.writeToFile("MyKeys/privateKey", gk.getPrivateKey().getEncoded());
        } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void addMiner(List<Callable<Block>> tasks) {
        for (int i = 0; i < 3; i++) {
            Callable<Block> task = Block::new;
            tasks.add(task);
        }
    }

    private static void mine(List<Block> blockchain, ExecutorService executor, List<Callable<Block>> tasks,
                             int prefixLength) {
        var currentBlockTxs = Block.getBLOCK_TXS();
        var ledger = Block.getLedger();
        for (int i = 0; i < 15; i++) {
            long blockStartTime = System.currentTimeMillis();
            // Create a list of CompletableFutures for each task
            List<CompletableFuture<Block>> completableFutures = tasks.stream()
                    .map(task -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return task.call();
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, executor))
                    .toList();


            // Wait for the first task to complete
            CompletableFuture<Block> firstCompletedFuture
                    = CompletableFuture.anyOf(completableFutures.toArray(new CompletableFuture[0])).thenApply(obj -> (Block) obj);

            int completedTaskIndex = -1;
            Block completedBlock = null;
            for (int j = 0; j < completableFutures.size(); j++) {
                Block resultBlock = completableFutures.get(j).join();
                completedBlock = firstCompletedFuture.join();
                if (resultBlock.equals(completedBlock)) {
                    completedTaskIndex = j;
                    break;
                }
            }

            // Get the result of the first completed task
            Block minedBlock = completedBlock;

            int copiedI = i;
            CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                try {
                    createBlockMessages(blockchain, ledger, currentBlockTxs, copiedI, blockStartTime, minedBlock);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, executor);

            int copiedPrefixLen = prefixLength;
            CompletableFuture<Void> future2 =
                    CompletableFuture.runAsync(() -> minedBlock.proofOfWork(copiedPrefixLen), executor);

            // Wait for both futures to complete
            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(future1, future2);
            combinedFuture.join();

            prefixLength = updateMinedBlock(prefixLength, i, completedTaskIndex, minedBlock);
            blockchain.add(minedBlock);
        }
    }

    private static int updateMinedBlock(int prefixLength, int i, int completedTaskIndex, Block minedBlock) {
        minedBlock.setId(i + 1);

        minedBlock.setMinerId(completedTaskIndex + 1); // Set the miner ID for the mined block
        if (minedBlock.getDuration() / 1000.00 < 0.1) {
            minedBlock.setPrefixLength("N was increased to " + (++prefixLength));
        } else if (minedBlock.getDuration() / 1000.00 > 1.0) {
            --prefixLength;
            minedBlock.setPrefixLength("N was decreased by 1");
        } else {
            minedBlock.setPrefixLength("N stays the same");
        }
        return prefixLength;
    }

    private static void createBlockMessages(List<Block> blockchain, Map<String, Integer> ledger,
                                            CopyOnWriteArrayList<String> currentBlockTxs,
                                            int i, long blockStartTime, Block minedBlock) throws Exception {
        if (i == 0) {
            minedBlock.setPreviousBlockSha256("0");
        } else {
            minedBlock.setPreviousBlockSha256(blockchain.get(i - 1).getSha256());
            CopyOnWriteArrayList<String> newBlockData = new CopyOnWriteArrayList<>();
            long elapsedBlockTime = System.currentTimeMillis() - blockStartTime;
            for (int j = 0; j < elapsedBlockTime * 10; j = (int) (j + (elapsedBlockTime))) {
                if (j < currentBlockTxs.size()) {
                    String tx = currentBlockTxs.get(ThreadLocalRandom.current().nextInt(currentBlockTxs.size()));
                    String[] txArray = tx.split("\\s+");
                    String sender = txArray[0];
                    String receiver = txArray[txArray.length - 1];
                    int amount = Integer.parseInt(txArray[2]);
                    int senderBalance = ledger.get(sender);
                    if (senderBalance - amount > 0) {
                        ledger.put(sender, senderBalance - amount);
                        ledger.put(receiver, senderBalance + amount);
                        newBlockData.add(tx);
                        new Transaction(tx, "MyKeys/privateKey")
                                .writeToFile(DIRECTORY + "MyData/SignedData");
                        currentBlockTxs.remove(0);

                        minedBlock.setBlockData(newBlockData);
                    }
                } else {
                    break;
                }
            }
        }
    }

    public static boolean validateBlockchain(List<Block> blockchain) throws Exception {
        boolean areHashesUnique = blockchain.stream()
                .map(Block::getSha256)
                .distinct()
                .count() == blockchain.size();

        boolean previousHashesValid = IntStream.range(1, blockchain.size())
                .allMatch(i -> blockchain.get(i).getPreviousBlockSha256().equals(blockchain.get(i - 1).getSha256()));

        boolean areIdsIncremented = IntStream.range(0, blockchain.size() - 1)
                .allMatch(i -> blockchain.get(i).getId() + 1 == blockchain.get(i + 1).getId());

        VerifyTransaction verifyMessage = new VerifyTransaction(DIRECTORY + "MyData/SignedData");
        List<byte[]> list = verifyMessage.getList();
        boolean isSignatureVerified = verifyMessage.verifySignature(list.get(0), list.get(1), "MyKeys/publicKey");

        return areHashesUnique && previousHashesValid && areIdsIncremented && isSignatureVerified;
    }

    public static void printBlockchain(List<Block> blockchain) {
        blockchain.forEach((block) -> System.out.printf("""
                    
                    Block:
                    Created by: miner%d
                    miner%d gets 100 VC
                    Id: %d
                    Timestamp: %d
                    Magic number: %d
                    Hash of the previous block:
                    %s
                    Hash of the block:
                    %s
                    Block data: %s
                    Block was generating for %d seconds
                    %s
                    """, block.getMinerId(), block.getMinerId(), block.getId(), block.getTimestamp(), block.getMagicNumber()
                , block.getPreviousBlockSha256(), block.getSha256()
                , block.getBlockData().isEmpty() ? "No transactions" : "\n" + String.join("\n", block.getBlockData())
                , block.getDuration() / 1000, block.getPrefixLength()));
    }
}