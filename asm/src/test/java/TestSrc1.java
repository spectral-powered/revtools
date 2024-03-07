public class TestSrc1 {
    private int field;

    public TestSrc1() {
        field = 0;
    }

    public int getField() {
        return this.field;
    }

    public void setField(int value) {
        this.field = value;
    }

    public int incrementField() {
        this.field++;
        return this.field;
    }

    public int[] createArray(int size) {
        return new int[size];
    }

    public int[][] createMultiArray(int rows, int cols) {
        return new int[rows][cols];
    }

    public static void staticMethod() {
        System.out.println("Static method called");
    }

    public void expr1() {
        int a = 10;
        int b = 20;
        int c = 200;
        if(a * b != c) {
            System.out.println("Bad" + a);
        } else {
            System.out.println("Good");
        }
    }
}