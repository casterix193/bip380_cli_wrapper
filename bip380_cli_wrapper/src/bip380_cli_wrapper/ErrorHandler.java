package bip380_cli_wrapper;

public class ErrorHandler {

	public void printErrorMessage(String message) {
		System.err.println("Error: " + message);
	}

	public void report(String message) {
	    if (message != null && !message.isEmpty()) {
	        printErrorMessage(message);
	    } else {
	        printErrorMessage("Unknown error occurred");
	    }
	}
	
}
