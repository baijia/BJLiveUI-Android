package com.baijiayun.live.ui;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
        new A();
    }

    private class A {
        B b = new B();

        A() {
            System.out.println("b=" + b);
        }
    }

    private class B {
    }
}