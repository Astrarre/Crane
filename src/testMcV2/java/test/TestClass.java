package test;

public class TestClass {
    public TestClass(int x) {

    }

    public void overriddenPublicVoid() {

    }

    public void overriddenPublicVoidBroken(int x) {

    }

    protected int overriddenProtectedIntArgs(int x1, int x2) {
        return x1 * x2;
    }
    protected int overriddenProtectedIntArgsBroken(int x1, int x2) {
        return x1 * x2;
    }


}
