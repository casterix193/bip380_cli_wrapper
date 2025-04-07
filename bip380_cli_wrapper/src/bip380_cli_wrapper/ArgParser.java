package bip380_cli_wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgParser {

	private static final String HELP_COMMAND = "--help";
    private static final String DERIVE_KEY_COMMAND = "derive-key";
    private static final String KEY_EXPRESSION_COMMAND = "key-expression";
    private static final String SCRIPT_EXPRESSION_COMMAND = "script-expression";
	private static final String TAKE_FROM_STDIN = "-";

	List<String> arguments;

	ArgParser(String[] args) {
		this.arguments = new ArrayList<>(Arrays.asList(args));
	}

	public List<String> getArguments() {
		return arguments;
	}

	public boolean checkHelp() {
		if (arguments.contains(HELP_COMMAND)) {
			arguments.remove(HELP_COMMAND);
			return true;
		}
		return false;
	}

	public boolean checkStdin() {
		if (arguments.contains(TAKE_FROM_STDIN)) {
			arguments.remove(TAKE_FROM_STDIN);
			return true;
		}
		return false;
	}

	public String getCommand() {
		String command = null;
		if (arguments.isEmpty()) {
			return null;
		}
		if (checkMultiCommand(arguments)) {
			throw new IllegalArgumentException("Only one command permitted.");
		}
		if (arguments.get(0).equals(DERIVE_KEY_COMMAND)) {
			command = DERIVE_KEY_COMMAND;
			arguments.remove(0);
		} else if (arguments.get(0).equals(KEY_EXPRESSION_COMMAND)) {
			command = KEY_EXPRESSION_COMMAND;
			arguments.remove(0);
		} else if (arguments.get(0).equals(SCRIPT_EXPRESSION_COMMAND)) {
			command = SCRIPT_EXPRESSION_COMMAND;
			arguments.remove(0);
		}
		return command;
	}

	private boolean checkMultiCommand (List<String> arguments) {
        return arguments.stream().filter(arg -> arg.equals(DERIVE_KEY_COMMAND) 
					|| arg.equals(KEY_EXPRESSION_COMMAND) 
					|| arg.equals(SCRIPT_EXPRESSION_COMMAND)).count() > 1;
    }


}
