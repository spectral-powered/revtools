package testclasses;

@SuppressWarnings("ConstantValue")
public class Test1 {

    public void test() {
        String a = "Hello";
        String b = "Goodbye";
        String c = "Hello";
        int num1 = 50;
        int num2 = 25;
        if(a.equals(b) && num1 == num2) {
            System.out.println("Case1 good");
        } else {
            System.out.println("Case1 bad");
        }

        if(num1 > num2) {
            System.out.println("Case2 good");
        } else if(num1 == num2) {
            System.out.println("Case2 else good");
        } else if(num2 * 2 == num1) {
            num2 = num1;
        } else {
            System.out.println("Case2 bad");
        }

        if(a.equals(c)) {
            System.out.println("Case3 good");
        } else {
            System.out.println("Case3 bad");
        }
    }
}
