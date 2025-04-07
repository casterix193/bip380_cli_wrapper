package bip380_cli_wrapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles script-expression commands for BIP380
 */
public class ScriptExpressionCommand extends Command {
    private boolean verifyChecksum = false;
    private boolean computeChecksum = false;

    private String expression = null; // The script expression to be processed
    
    // Regular expressions for validating script formats
    private static final String KEY_EXPR_PATTERN = "(?:\\[[^\\]]*\\])?(?:xpub|xprv|[0-9a-fA-F]{64,130})";
    private static final Pattern PK_PATTERN = Pattern.compile("pk\\(\\s*(" + KEY_EXPR_PATTERN + ")\\s*\\)");
    private static final Pattern PKH_PATTERN = Pattern.compile("pkh\\(\\s*(" + KEY_EXPR_PATTERN + ")\\s*\\)");
    private static final Pattern MULTI_PATTERN = Pattern.compile("multi\\(\\s*(\\d+)\\s*,\\s*([^)]+)\\)");
    private static final Pattern SH_PK_PATTERN = Pattern.compile("sh\\(\\s*pk\\(\\s*(" + KEY_EXPR_PATTERN + ")\\s*\\)\\s*\\)");
    private static final Pattern SH_PKH_PATTERN = Pattern.compile("sh\\(\\s*pkh\\(\\s*(" + KEY_EXPR_PATTERN + ")\\s*\\)\\s*\\)");
    private static final Pattern SH_MULTI_PATTERN = Pattern.compile("sh\\(\\s*multi\\(\\s*(\\d+)\\s*,\\s*([^)]+)\\)\\s*\\)");
    private static final Pattern RAW_PATTERN = Pattern.compile("raw\\(\\s*([0-9a-fA-F]+)\\s*\\)");

    public ScriptExpressionCommand(List<String> args, boolean stdin) {
        super(args, stdin);
        parseFlags(args);
    }
    
    private void parseFlags(List<String> args) {
        List<String> nonFlagArgs = new ArrayList<>();
        
        for (String arg : args) {
            if (arg.equals("--verify-checksum")) {
                verifyChecksum = true;
            } else if (arg.equals("--compute-checksum")) {
                computeChecksum = true;
            } else if (!arg.equals("-")) {
                nonFlagArgs.add(arg);
            }
        }
        
        // Replace arguments with non-flag arguments
        this.arguments = nonFlagArgs;
        
        // Validate flags
        if (verifyChecksum && computeChecksum) {
            errorHandler.printErrorMessage("use only '--verify-checksum' or '--compute-checksum', not both");
            System.exit(1);
        }
    }

    @Override
    public int execute() {
        // If reading from STDIN, load from it
        if (stdin) {
            return loadFromStdin();
        }

        if (arguments.size() != 1) {
            errorHandler.printErrorMessage("Exactly one script expression is required");
            System.exit(1);
            return FAIL;
        }

        // Extract the expression (script or script#checksum)
        this.expression = arguments.get(0);
        return parseArg();
    }
    
    /**
     * Process a script expression string
     */
    @Override
    protected int parseArg() {
        if (expression == null || expression.trim().isEmpty()) {
            errorHandler.printErrorMessage("Empty script expression");
            return FAIL;
        }

        // Split by # to separate script and checksum
        String[] parts = expression.split("#", 2);
        String script = parts[0].trim();
        String providedChecksum = (parts.length > 1) ? parts[1].trim() : null;

        // Validate script syntax
        if (!isValidScript(script)) {
            errorHandler.printErrorMessage("Invalid script format");
            return FAIL;
        }

        // Handle checksum operations
        if (verifyChecksum) {
            if (providedChecksum == null) {
                errorHandler.printErrorMessage("no checksum");
                return FAIL;
            }
            String calculatedChecksum = calculateBIP380Checksum(script);
            if (calculatedChecksum.equals(providedChecksum)) {
                System.out.println("OK");
            } else {
                errorHandler.printErrorMessage("checksum verification failed");
                return FAIL;
            }
        } else if (computeChecksum) {
            String calculatedChecksum = calculateBIP380Checksum(script);
            System.out.println(script + "#" + calculatedChecksum);
        } else {
            // Just parse and echo if no checksum operation specified
            if (parts.length > 1) {
                // If checksum was provided, make sure it's valid format
                if (providedChecksum.length() != 8) {
                    errorHandler.printErrorMessage("invalid checksum length");
                    return FAIL;
                }
                System.out.println(expression);
            } else {
                System.out.println(script);
            }
        }
        return SUCCESS;
    }

    /**
     * Calculate BIP380 checksum for a script
     * BIP380 uses a modified Base58 checksum:
     * 1. Calculate the double SHA256 hash of the script
     * 2. Take the first 5 bytes of the hash
     * 3. Encode them in base58
     */
    private String calculateBIP380Checksum(String script) {
        try {
        	script = script.replaceAll("\\s+", "");
            
            // First SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] firstHash = digest.digest(script.getBytes(StandardCharsets.UTF_8));
            
            // Second SHA-256 hash
            byte[] doubleHash = digest.digest(firstHash);
            
            // Take first 5 bytes
            byte[] checksumBytes = new byte[5];
            System.arraycopy(doubleHash, 0, checksumBytes, 0, 5);
            
            // Encode in base58
            return base58Encode(checksumBytes);
        } catch (NoSuchAlgorithmException e) {
            errorHandler.printErrorMessage("SHA-256 algorithm not found: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Base58 encoding implementation
     */
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    
    private String base58Encode(byte[] input) {
        // Special case for empty input
        if (input.length == 0) {
            return "";
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

    /**
     * Validate script expression formats
     */
    private boolean isValidScript(String script) {
        if (script == null || script.isEmpty()) {
            return false;
        }
        
        // Remove all whitespace before matching
        script = script.replaceAll("\\s+", "");
        
        // Check against each supported pattern
        if (PK_PATTERN.matcher(script).matches() ||
            PKH_PATTERN.matcher(script).matches() ||
            RAW_PATTERN.matcher(script).matches()) {
            return true;
        }
        
        // Check multi pattern separately to validate k <= n
        Matcher multiMatcher = MULTI_PATTERN.matcher(script);
        if (multiMatcher.matches()) {
            int k = Integer.parseInt(multiMatcher.group(1));
            String keyList = multiMatcher.group(2);
            int keyCount = keyList.split(",").length;
            return k >= 0 && k <= keyCount;
        }
        
        // Check sh patterns
        if (SH_PK_PATTERN.matcher(script).matches() ||
            SH_PKH_PATTERN.matcher(script).matches()) {
            return true;
        }
        
        // Check sh(multi) pattern
        Matcher shMultiMatcher = SH_MULTI_PATTERN.matcher(script);
        if (shMultiMatcher.matches()) {
            int k = Integer.parseInt(shMultiMatcher.group(1));
            String keyList = shMultiMatcher.group(2);
            int keyCount = keyList.split(",").length;
            return k >= 0 && k <= keyCount;
        }
        
        return false;
    }

    @Override
    public void help() {
        System.out.println("Usage: script-expression {expr} [-] [--verify-checksum] [--compute-checksum]");
        System.out.println("Parse and validate Bitcoin script expressions as defined in BIP380");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --verify-checksum    Verify the checksum provided with the script");
        System.out.println("  --compute-checksum   Calculate and output the checksum for the script");
        System.out.println("  -                    Read expressions from standard input");
        System.out.println();
        System.out.println("Supported script formats:");
        System.out.println("  pk(KEY)");
        System.out.println("  pkh(KEY)");
        System.out.println("  multi(k, KEY_1, KEY_2, ..., KEY_n)");
        System.out.println("  sh(pk(KEY))");
        System.out.println("  sh(pkh(KEY))");
        System.out.println("  sh(multi(k, KEY_1, KEY_2, ..., KEY_n))");
        System.out.println("  raw(HEX)");
        System.out.println();
        System.out.println("KEY can be any valid key expression as defined in BIP380");
    }
}