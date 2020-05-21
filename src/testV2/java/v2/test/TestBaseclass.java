package v2.test;

import test.TestClass;

public abstract class TestBaseclass extends TestClass implements TestInterface {
    public TestBaseclass(int x1, int x2) {
        super(x1);
    }

    public TestBaseclass(int x, String x2) {
        super(x);
    }

    protected TestBaseclass(String y) {
        super(2);
    }

    public void instancePublicVoid() {

    }

    protected int protectedIntArgs(int x1, int x2) {
        return x1 + x2;
    }


    public final int someGetter() {
        return 2;
    }

    public final void someSetter(int value) {

    }

    @Override
    public void overriddenPublicVoid() {
        super.overriddenPublicVoid();
    }


    protected String overriddenProtectedIntArgs(String x1, String x2) {
        return Integer.toString(super.overriddenProtectedIntArgs(Integer.parseInt(x1), Integer.parseInt(x2)));
    }

    public final int someGetterBroken(int x) {
        return 2;
    }

    public final void someSetterBroken() {

    }

    @Override
    public void overriddenPublicVoidBroken(int x) {
        super.overriddenPublicVoid();
    }


    public static void staticUnbroken() {

    }

    public static void staticBroken(int x) {

    }

    protected String overriddenProtectedIntArgsBroken(String x1) {
        return Integer.toString(super.overriddenProtectedIntArgs(Integer.parseInt(x1), 2));
    }

    public static final String constant = "FOO";

}
