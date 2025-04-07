@echo off
echo Debugging Script Expression Checksum
echo ===================================

echo.
echo Creating test class...
mkdir -p bin

echo "
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class TestChecksum {
    private static final String ALPHABET = \"123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz\";
    
    public static void main(String[] args) throws Exception {
        String script = \"raw(deadbeef)\";
        
        // First SHA-256 hash
        MessageDigest digest = MessageDigest.getInstance(\"SHA-256\");
        byte[] firstHash = digest.digest(script.getBytes(StandardCharsets.UTF_8));
        
        System.out.println(\"Script: \" + script);
        System.out.println(\"First hash: \" + bytesToHex(firstHash));
        
        // Second SHA-256 hash
        digest.reset();
        byte[] doubleHash = digest.digest(firstHash);
        
        System.out.println(\"Double hash: \" + bytesToHex(doubleHash));
        
        // Take first 5 bytes
        byte[] checksumBytes = new byte[5];
        System.arraycopy(doubleHash, 0, checksumBytes, 0, 5);
        
        System.out.println(\"First 5 bytes: \" + bytesToHex(checksumBytes));
        
        // Encode in base58
        String checksum = base58Encode(checksumBytes);
        System.out.println(\"Base58 encoded: \" + checksum);
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format(\"%02x\", b & 0xFF));
        }
        return sb.toString();
    }
    
    private static String base58Encode(byte[] input) {
        // Special case for empty input
        if (input.length == 0) {
            return \"\";
        }
        
        // Count leading zeros
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            zeros++;
        }
        
        // Convert to big integer
        java.math.BigInteger value = java.math.BigInteger.ZERO;
        for (int i = zeros; i < input.length; i++) {
            value = value.shiftLeft(8).add(java.math.BigInteger.valueOf(input[i] & 0xFF));
        }
        
        // Convert to base58
        StringBuilder result = new StringBuilder();
        java.math.BigInteger base = java.math.BigInteger.valueOf(58);
        java.math.BigInteger zero = java.math.BigInteger.ZERO;
        
        while (value.compareTo(zero) > 0) {
            java.math.BigInteger mod = value.mod(base);
            value = value.divide(base);
            result.insert(0, ALPHABET.charAt(mod.intValue()));
        }
        
        // Add leading '1's for each leading zero byte
        for (int i = 0; i < zeros; i++) {
            result.insert(0, '1');
        }
        
        return result.toString();
    }
}" > TestChecksum.java

echo.
echo Compiling test class...
javac -cp "lib/*" TestChecksum.java

echo.
echo Running test...
java -cp ".;lib/*" TestChecksum

pause
pause