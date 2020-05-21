package v1.test;

import java.util.ArrayList;
import java.util.List;

public interface TestInterface {
    default void instanceVoid() {

    }

    default int instanceIntArg(int x) {
        return 2;
    }

    static void staticVoidArgs(int x1, int x2, String x3) {

    }

//    public static  cast(TestInterface t) {
//        return (T) t;
//    }

    int staticField = 3;


    default void returnTypeAdded() {

    }

    default int returnTypeRemoved() {
        return 2;
    }

    default String returnTypeChanged() {
        return "";
    }

    default List<String> returnTypeWidened() {
        return new ArrayList<>();
    }

    default List<String> returnTypeNarrowed() {
        return new ArrayList<>();
    }


    default void parametersAdded() {

    }

    default void parametersRemoved(int x1, int x2) {

    }

    default void parameterTypeWidened(List<String> param) {

    }

    default void parameterTypeNarrowed(List<String> param) {

    }

    default void parameterReordered(int x1, String x2) {

    }

    default void methodRemoved() {

    }

}
