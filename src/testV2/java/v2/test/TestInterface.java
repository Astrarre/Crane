package v2.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface TestInterface {
    default void instanceVoid() {

    }

    default int instanceIntArg(int x) {
        return 2;
    }

    static void staticVoidArgs(int x1, int x2, String x3) {

    }

    int staticField = 3;


    default int returnTypeAdded() {
        return 2;
    }

    default void returnTypeRemoved() {

    }

    default int returnTypeChanged() {
        return 2;
    }

    default Collection<String> returnTypeWidened() {
        return new ArrayList<>();
    }

    default ArrayList<String> returnTypeNarrowed() {
        return new ArrayList<>();
    }


    default void parametersAdded(int x1, int x2) {

    }

    default void parametersRemoved() {

    }

    default void parameterTypeWidened(Collection<String> param) {

    }

    default void parameterTypeNarrowed(ArrayList<String> param) {

    }

    default void parameterReordered(String x1, int x2) {

    }

}
