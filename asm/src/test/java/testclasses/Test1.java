package testclasses;

import java.io.InputStream;

public class Test1 {

    public void test() {
        InputStream stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream("".replaceAll("[.]", "/"));
    }
}
