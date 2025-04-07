package bip380_cli_wrapper;

import java.util.List;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.crypto.*;
import org.bitcoinj.crypto.HDPath;



public class KeyExpressionCommand extends Command{

	public KeyExpressionCommand(List<String> args, boolean stdin) {
		super(args, stdin);
	}
	
	@Override
	public int execute() {
	    if (this.stdin) {
	        return loadFromStdin();
	    } else if (!arguments.isEmpty()) {
	        return parseArg();
	    } else {
	        errorHandler.printErrorMessage("No key expression provided");
	        return FAIL;
	    }
	}

	@Override
	protected int parseArg() {
		// if (super.arguments.isEmpty()) {
		// 	errorHandler.printErrorMessage("No value provided.");
		// 	return FAIL;
		// }
		String expression = super.arguments.get(0);
	    if (expression == null || expression.isEmpty()) {
	        errorHandler.printErrorMessage("Key expression cannot be empty");
	        return FAIL;
	    }
	    
	    // calling parseKeyExpression method
	    parseKeyExpression(expression);
	    return SUCCESS;
	}

	
	@Override
	public void help() {
		String lineSeparator = System.lineSeparator();
        StringBuilder helpMessage = new StringBuilder();
        helpMessage.append("Usage:").append(lineSeparator);
        helpMessage.append("  key-expression {expr} [-]: Evaluate a key expression.").append(lineSeparator);
        helpMessage.append("  [-] used to pass values through STDIN").append(lineSeparator);
        System.out.print(helpMessage.toString());
	}
	
	protected boolean validateKey(String key) {
        // Hex encoded public key check
        if ((key.startsWith("02") || key.startsWith("03")) && key.length() == 66) {
            // Compressed public key format
            if (!key.matches("[0-9a-fA-F]{66}")) {
                errorHandler.printErrorMessage("Invalid hex in compressed public key");
                return false;
            }
            return true;
        } else if (key.startsWith("04") && key.length() == 130) {
            // Uncompressed public key format
            if (!key.matches("[0-9a-fA-F]{130}")) {
                errorHandler.printErrorMessage("Invalid hex in uncompressed public key");
                return false;
            }
            return true;
        } else if (key.startsWith("xpub") || key.startsWith("xprv")) {
            // Extended key validation using Command's validateExtendedKey
            return validateExtendedKey(key) == SUCCESS;
        } else if (key.length() >= 51 && key.length() <= 52) {
            // WIF private key validation using Command's validateWIF
            return validateWIF(key);
        }
        
        errorHandler.printErrorMessage("Unrecognized key format");
        return false;
    }

    protected boolean validateDerivationPath(String path) {
        if (path.isEmpty()) return true;
        
        // Use Command's validatePath method
        return validatePath() == SUCCESS;
    }
    
    protected void parseKeyExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            errorHandler.printErrorMessage("Key expression cannot be empty");
            return;
        }
        
        // Key origin (optional)
        String keyOrigin = null;
        String actualKey = expression;
        String derivationPath = null;
        
        // Check for key origin info
        if (expression.startsWith("[")) {
            int closingBracketIndex = expression.indexOf(']');
            if (closingBracketIndex == -1) {
                errorHandler.printErrorMessage("Missing closing bracket for key origin");
                return;
            }
            keyOrigin = expression.substring(1, closingBracketIndex);
            actualKey = expression.substring(closingBracketIndex + 1);
        }
        
        // Check for derivation path after the key
        int firstSlashIndex = actualKey.indexOf('/');
        if (firstSlashIndex != -1) {
            derivationPath = actualKey.substring(firstSlashIndex);
            actualKey = actualKey.substring(0, firstSlashIndex);
        }
        
        // Validate each part using Command's methods
        if (keyOrigin != null && !validateKeyOrigin(keyOrigin)) {
            return; // Error already reported in validateKeyOrigin
        }
        
        if (!validateKey(actualKey)) {
            return; // Error already reported in validateKey
        }
        
        if (derivationPath != null && !validateDerivationPath(derivationPath)) {
            return; // Error already reported in validateDerivationPath
        }
        
        System.out.println(expression);
    }
}

