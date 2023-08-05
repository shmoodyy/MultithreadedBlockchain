package blockchain;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

// Transaction Signature and Verification objects taken and modified from: https://mkyong.com/java/java-digital-signatures-example/
public class VerifyTransaction {
    private List<byte[]> list;

    @SuppressWarnings("unchecked")
    //The constructor of VerifyTransaction class retrieves the byte arrays from the File
    //and prints the message only if the signature is verified.
    public VerifyTransaction(String filename) throws Exception {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename));
        this.list = (List<byte[]>) in.readObject();
        in.close();
    }

    //Method for signature verification that initializes with the Public Key,
    //updates the data to be verified and then verifies them using the signature
    boolean verifySignature(byte[] data, byte[] signature, String keyFile) throws Exception {
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(getPublic(keyFile));
        sig.update(data);

        return sig.verify(signature);
    }

    //Method to retrieve the Public Key from a file
    public PublicKey getPublic(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public List<byte[]> getList() {
        return list;
    }
}