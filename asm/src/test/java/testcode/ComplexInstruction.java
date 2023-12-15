package testcode;

public class ComplexInstruction {

    public static String message() {
        StringBuilder builder = new StringBuilder();
        builder.append("Hello").append("Hello").append("Testing");
        return builder.toString();
    }
}
