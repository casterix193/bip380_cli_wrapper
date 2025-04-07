package bip380_cli_wrapper;

import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.crypto.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.bitcoinj.base.Base58;

public abstract class Command {

	protected static final int SUCCESS = 0;
    protected static final int FAIL = 1;
    protected static final String EXTENDED_PRIVATE_KEY_PREFIX_STRING = "xprv";
    protected static final String EXTENDED_PUBLIC_KEY_PREFIX_STRING = "xpub";
	
	ErrorHandler errorHandler = new ErrorHandler();
	List<String> arguments;
	protected boolean stdin;
	protected String stringPath;
	
	public Command (List<String> args, boolean stdin) {
		this.arguments = args;
		this.stdin = stdin;
	}
	 
	protected int loadFromStdin() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) { 
                if (line.isEmpty()) {
                    continue;
                } else {
                    if (parseArg() != SUCCESS) {
						errorHandler.printErrorMessage("Arg parsing failed.");
						return FAIL;
					}
                }
            }
			return SUCCESS;
		
        } catch (IOException e) {
            errorHandler.printErrorMessage("Error reading from standard input: " + e.getMessage());
            return FAIL;
        }
    }

	// Hex validation helper method
    protected boolean isValidHex(String value) {
        return value != null && value.matches("^[0-9A-Fa-f ]+$");
    }
    
    // Key validation helpers
    protected int validateExtendedKey(String key) {
        if (key == null || key.isEmpty()) {
            errorHandler.printErrorMessage("Key cannot be empty");
            return FAIL;
        }
        
        // Extended private key check
        if (key.startsWith(EXTENDED_PRIVATE_KEY_PREFIX_STRING)) {
            try {
                DeterministicKey.deserializeB58(key, MainNetParams.get());
                return SUCCESS;
            } catch (Exception e) {
                errorHandler.printErrorMessage("Invalid extended private key: " + e.getMessage());
                return FAIL;
            }
        }
        
        // Extended public key check
        if (key.startsWith(EXTENDED_PUBLIC_KEY_PREFIX_STRING)) {
            try {
                DeterministicKey.deserializeB58(key, MainNetParams.get());
                return SUCCESS;
            } catch (Exception e) {
                errorHandler.printErrorMessage("Invalid extended public key: " + e.getMessage());
                return FAIL;
            }
        }
        
        errorHandler.printErrorMessage("Key must start with 'xprv' or 'xpub'");
        return FAIL;
    }
    
    // Path parsing helpers
    protected int validatePath() {
        if (this.stringPath == null || this.stringPath.isEmpty()) { 
            errorHandler.printErrorMessage("Path cannot be empty");
            return FAIL;
        }
        
        if (this.stringPath.endsWith("/")) { 
            errorHandler.printErrorMessage("Path cannot end with '/'");
            return FAIL;
        }
        
        if (this.stringPath.equals("/")) { 
            errorHandler.printErrorMessage("Path cannot be just '/'");
            return FAIL;
        }
        
        if (this.stringPath.contains("//")) { 
            errorHandler.printErrorMessage("Path cannot contain consecutive '/'");
            return FAIL; 
        }
        
        String[] pathParts;
        if (this.stringPath.startsWith("/")) {
            pathParts = this.stringPath.substring(1).split("/");
        } else {
            pathParts = this.stringPath.split("/");
        }
        
        for (String part : pathParts) {
            if (part.isEmpty()) {
                errorHandler.printErrorMessage("Path cannot contain empty parts");
                return FAIL;
            }
            
            if (part.equals("*") || part.equals("*h") || part.equals("*H") || part.equals("*'")) {
                // Wildcard is valid
                continue;
            }
            
            if (!validateDerivationStep(part)) {
                return FAIL;
            }
        }
        
        return SUCCESS;
    }
    
    protected boolean validateDerivationStep(String step) {
        boolean isHardened = step.endsWith("h") || step.endsWith("H") || step.endsWith("'");
        String numPart = isHardened ? step.substring(0, step.length() - 1) : step;
        
        try {
            int index = Integer.parseInt(numPart);
            if (index < 0 || index >= 0x7FFFFFFF) { // 2^31 - 1
                errorHandler.printErrorMessage("Path index out of range: " + numPart);
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            errorHandler.printErrorMessage("Invalid number in path: " + numPart);
            return false;
        }
    }
    // Key expression validation
    protected boolean validateKeyOrigin(String keyOrigin) {
        // Split by '/'
        String[] parts = keyOrigin.split("/");
        
        // First part should be exactly 8 hex characters (fingerprint)
        if (!parts[0].matches("[0-9a-fA-F]{8}")) {
            errorHandler.printErrorMessage("Invalid fingerprint in key origin: " + parts[0]);
            return false;
        }
        
        // Remaining parts should be valid derivation steps
        for (int i = 1; i < parts.length; i++) {
            if (!validateDerivationStep(parts[i])) {
                return false; // Error already reported in validateDerivationStep
            }
        }
        
        return true;
    }
    
    // WIF validation helper
    protected boolean validateWIF(String wif) {
    	try {
	        // This would be implemented with proper Base58 and checksum validation
	        // For now, this is a placeholder
	    	byte[] decoded = Base58.decode(wif);
	    	
	    	//min length of WIF key 1+32(spec key)+4checksum = 37
	    	if (decoded.length < 37) {
	            errorHandler.printErrorMessage("WIF key is too short");
	            return false;
	        }
	    	// sec checsum (last 4byte)
	    	byte[] checksum = Arrays.copyOfRange(decoded, decoded.length - 4, decoded.length);
	        byte[] keyWithVersion = Arrays.copyOfRange(decoded, 0, decoded.length - 4);
	        
	        //check the first byte (supposed to be 0x80)
	        if (keyWithVersion[0] != (byte)0x80) {
	            errorHandler.printErrorMessage("Invalid WIF version byte, expected 0x80");
	            return false;
	        }
	        // checksum verification
	        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
	        byte[] firstSHA = sha256.digest(keyWithVersion);
	        byte[] secondSHA = sha256.digest(firstSHA);
	        
	        // second SHA-256, first 4byte is used as checksum
	        byte[] calculatedChecksum = Arrays.copyOfRange(secondSHA, 0, 4);
	        
	        // comparison between the calculated checksum and the coming one
	        if (!Arrays.equals(calculatedChecksum, checksum)) {
	            errorHandler.printErrorMessage("Invalid WIF checksum");
	            return false;
	        }
	        //WIF format was approved
	        return true;
    	} catch (IllegalArgumentException e) {
            errorHandler.printErrorMessage("Invalid Base58 encoding in WIF key: " + e.getMessage());
            return false;
        } catch (NoSuchAlgorithmException e) {
            errorHandler.printErrorMessage("SHA-256 algorithm not available");
            return false;
        }
    	
        
    }
    
    // Abstract methods that must be implemented by subclasses
    protected abstract int execute();

    protected abstract int parseArg();
    
	public abstract void help();
	
}
