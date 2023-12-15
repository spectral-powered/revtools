package testcode;

public interface IIterfClass {

    String getName();

    default String getMessage() {
        return "Message Text";
    }

}
