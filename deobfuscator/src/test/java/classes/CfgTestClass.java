package classes;

@SuppressWarnings("ALL")
public class CfgTestClass {

    public String method1(int a, int b, int c) {
        String str = "";
        if (a == 0) {
            if (b == 0) {
                str = "string1";
            } else {
                str = "string2";

            }
        } else if(a == 1) {
            if (c >= 2 && (a * b) == 0) {
                for (int i = 0; i < c; i++) {
                    b += 2;
                }
            }
            str = "output is: " + b;
        } else {
            try {
                float delta = b * b - 4 * a * c;
                if (delta > 0) {
                    str = "string3";

                } else if (delta == 0) {
                    str = "string4";
                } else {
                    str = "string6";
                    delta = delta / 0f;
                }
            } catch (Exception e) {
                str = "exception";
            }
        }
        return str;
    }
}
