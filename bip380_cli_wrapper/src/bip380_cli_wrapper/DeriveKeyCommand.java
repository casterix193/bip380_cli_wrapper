package bip380_cli_wrapper;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HexFormat;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.crypto.*;

public class DeriveKeyCommand extends Command {

	private static final MainNetParams params = MainNetParams.get();;
	private static final String PATH_STRING = "--path";
	
	public DeriveKeyCommand(List<String> args, boolean stdin) {
		super(args, stdin);
	}

    protected int parseArg() {
        // Add logic here if needed, or return a default value
        return 0; // Default return value
    }

	@Override
    public int execute() {
		
        if (parsePathArg() != SUCCESS) {
            return FAIL;
        }

        if (this.stdin) {
            return loadFromStdin();
        } else {
            if (arguments.isEmpty()) {
                errorHandler.printErrorMessage("No value provided");
                return FAIL;
            }
			String value = arguments.get(0);
			if (arguments.size() != 1) {
                errorHandler.printErrorMessage("Only one value is allowed");
                return FAIL;
            } else {
				return processValue(value);
			}
        }
    }

    protected int loadFromStdin() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim(); //TODO remove?
                if (line.isEmpty()) {
                    continue; // skip empty lines
                }
                if (processValue(line) != SUCCESS) {
                    return FAIL;
                }
            }
            return SUCCESS;
        } catch (IOException e) {
            errorHandler.printErrorMessage("Error reading from stdin: " + e.getMessage());
            return FAIL;
        }
    }

	private int processValue(String value) {
        if (value.startsWith(EXTENDED_PRIVATE_KEY_PREFIX_STRING)) {
            return handleXpriv(value);
        } else if (value.startsWith(EXTENDED_PUBLIC_KEY_PREFIX_STRING)) {
            return handleXpub(value);
        } else {
            return handleSeed(value);
        }
    }

	private int handleXpriv(String xpriv) {
        try {
            DeterministicKey key = DeterministicKey.deserializeB58(xpriv, params);
            if (stringPath == null || stringPath.isEmpty()) {
                // Echo xpriv and derive xpub
                System.out.println(getXpub(key) + ":" + xpriv);
            } else {
                // Derive new keys from path
                DeterministicKey derived = deriveFromPath(key, stringPath);
                System.out.println(getXpub(derived) + ":" + getXpriv(derived));
            }
            return SUCCESS;
        } catch (Exception e) {
            errorHandler.printErrorMessage("Invalid extended private key: " + e.getMessage());
            return FAIL;
        }
    }

    private int handleXpub(String xpub) {
        try {
            DeterministicKey key = DeterministicKey.deserializeB58(xpub, params);
            if (stringPath == null || stringPath.isEmpty()) {
                // Just echo the xpub
                System.out.println(xpub + ":");
            } else {
                // Derive new xpub from path (non-hardened only)
                DeterministicKey derived = deriveFromPath(key, stringPath);
                System.out.println(getXpub(derived) + ":");
            }
            return SUCCESS;
        } catch (Exception e) {
            errorHandler.printErrorMessage("Invalid extended public key: " + e.getMessage());
            return FAIL;
        }
    }

    private int handleSeed(String seedStr) {
        if (validateSeedValue(seedStr) != SUCCESS) {
            return FAIL;
        }
        
        try {
            byte[] seed = HexFormat.of().parseHex(seedStr.replaceAll("[ \\t]", ""));
            DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seed);
            
            if (stringPath == null || stringPath.isEmpty()) {
                // Output master keys
                System.out.println(getXpub(masterKey) + ":" + getXpriv(masterKey));
            } else {
                // Derive keys from path
                DeterministicKey derived = deriveFromPath(masterKey, stringPath);
                System.out.println(getXpub(derived) + ":" + getXpriv(derived));
            } //TODO maybe remove
            return SUCCESS;
        } catch (Exception e) {
            errorHandler.printErrorMessage("Seed processing failed: " + e.getMessage());
            return FAIL;
        }
    }
	
	private int parsePathArg() {
        if (arguments.contains(PATH_STRING)) {
            int index = arguments.indexOf(PATH_STRING);
            if (index + 1 >= arguments.size()) {
                errorHandler.printErrorMessage("Path argument is missing");
                return FAIL;
            }
            stringPath = arguments.get(index + 1);
            arguments.remove(index + 1);
            arguments.remove(index);
            
            if (validatePath() != SUCCESS) {
                return FAIL;
            }
        }
        return SUCCESS;
    }

	protected boolean isValidHex(String value) {
        return value != null && value.matches("^[0-9A-Fa-f\\t ]+$");
    }

	private int validateSeedValue(String value) {
        if (!isValidHex(value)) {
            errorHandler.printErrorMessage("Non-hexadecimal seed value '" + value + "'");
            return FAIL;
        } 
        
        String cleanValue = value.replaceAll("[ \\t]", "");
        int byteLength = cleanValue.length() / 2; //TODO: check if this is correct
        
        if (byteLength < 16 || byteLength > 64) {
            errorHandler.printErrorMessage("Seed value must be between 128 and 512 bits");
            return FAIL;
        }
        
        return SUCCESS;
    }

	private DeterministicKey deriveFromPath(DeterministicKey key, String pathStr) {
        List<ChildNumber> path = new ArrayList<>();
        String[] parts = pathStr.split("/");
        
        for (String part : parts) {
            if (part.isEmpty()) continue;
            
            boolean hardened = part.endsWith("h") || part.endsWith("H") || part.endsWith("'");
            String numStr = hardened ? part.substring(0, part.length() - 1) : part;
            
            try {
                int num = Integer.parseInt(numStr);
                path.add(new ChildNumber(num, hardened));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid path segment: " + part);
            }
        }
        
        DeterministicKey current = key;
        for (ChildNumber child : path) {
            if (current.isPubKeyOnly() && child.isHardened()) {
                throw new IllegalArgumentException("Cannot derive hardened path from public key");
            }
            current = HDKeyDerivation.deriveChildKey(current, child);
        }
        return current;
    }

    private static String getXpub(DeterministicKey key) {
        return key.serializePubB58(params);
    }

    private static String getXpriv(DeterministicKey key) {
        return key.serializePrivB58(params);
    }

	@Override
	public void help() {
		String lineSeparator = System.lineSeparator();
        StringBuilder helpMessage = new StringBuilder();
        helpMessage.append("Usage:").append(lineSeparator);
        helpMessage.append("  derive-key {value} [--path {path}] [-]: Derive a key from a value.").append(lineSeparator);
        helpMessage.append("  [-] used to pass values through STDIN").append(lineSeparator);
        System.out.print(helpMessage.toString());
	}

//    	private int validateKeyValue() {
//		if(this.arguments.isEmpty()) { 
//			errorHandler.printErrorMessage("No key value provided.");
//            return FAIL;
//		}
//		String keyValue = this.arguments.get(0); 
//		this.arguments.remove(0);
//		
//		try { 
//			if (keyValue.startsWith(EXTENDED_PRIVATE_KEY_PREFIX_STRING)) { 
//				try {
//					this.key = DeterministicKey.deserializeB58(keyValue, MainNetParams.get()); 
//				}
//				catch (Exception e) {
//					errorHandler.printErrorMessage("Invalid extended key: " + e.getMessage());
//			        return FAIL;
//				}
//				return SUCCESS;
//			}else if (keyValue.startsWith(EXTENDED_PUBLIC_KEY_PREFIX_STRING)) { 
//				this.key = DeterministicKey.deserializeB58(keyValue, MainNetParams.get()); 
//				return SUCCESS;
//		}else {
//            errorHandler.printErrorMessage("Key must start with 'xprv' or 'xpub'");
//            return FAIL;
//        }
//		} catch (Exception e) {
//	        errorHandler.printErrorMessage("Invalid extended key: " + e.getMessage());
//	        return FAIL;
//		}
//		
//	}
	
}


