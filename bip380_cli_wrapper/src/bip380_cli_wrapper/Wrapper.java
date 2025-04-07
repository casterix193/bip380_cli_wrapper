package bip380_cli_wrapper;
import java.util.*;

public class Wrapper {

    protected static final int SUCCESS = 0;
    protected static final int FAIL = 1;
	
    private static final String DERIVE_KEY_COMMAND = "derive-key";
    private static final String KEY_EXPRESSION_COMMAND = "key-expression";
    private static final String SCRIPT_EXPRESSION_COMMAND = "script-expression";

	public static void main(String[] args) {
        try {
            System.exit(executeCommand(args));
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(FAIL);
        }
    }

    private static int executeCommand(String[] args) {
        ArgParser argParser = new ArgParser(args);
        
        boolean helpRequested = argParser.checkHelp();
        boolean stdinRequested = argParser.checkStdin();
        String command = argParser.getCommand();
        List<String> arguments = argParser.getArguments();

        if (command == null) {
            if (helpRequested) {
                help();
                return SUCCESS;
            }
            throw new IllegalArgumentException("No valid command provided. Use --help for usage information.");
        }

        Command commandHandler = createCommandHandler(command, arguments, stdinRequested);
        
        if (helpRequested) {
            commandHandler.help();
            return SUCCESS;
        }
        
        return commandHandler.execute();
    }

    private static Command createCommandHandler(String command, List<String> arguments, boolean stdin) {
        switch (command) {
            case DERIVE_KEY_COMMAND:
                return new DeriveKeyCommand(arguments, stdin);
            case KEY_EXPRESSION_COMMAND:
                return new KeyExpressionCommand(arguments, stdin);
            case SCRIPT_EXPRESSION_COMMAND:
                return new ScriptExpressionCommand(arguments, stdin);
            default:
                throw new IllegalArgumentException("Invalid command: " + command);
        }
    }
	
	public static void help() {
        String lineSeparator = System.lineSeparator();
        StringBuilder helpMessage = new StringBuilder();
        helpMessage.append("Usage:").append(lineSeparator);
        helpMessage.append("  derive-key {value} [--path {path}] [-]: Derive a key from a value.").append(lineSeparator);
        helpMessage.append("  key-expression {expr} [-]: Evaluate a key expression.").append(lineSeparator);
        helpMessage.append("  script-expression {expr} [-]: Evaluate a script expression.").append(lineSeparator);
        helpMessage.append("  help: Show this help message.").append(lineSeparator);
        System.out.print(helpMessage.toString());
    }

}     