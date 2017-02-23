package com.transformer.json;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    public class TestA {
        int a=8;
        String str = "123";

        public TestA() {}
        public TestA( int a, String str ) {
            this.a= a;
            this.str= str;
        }
    }

    public class TestB {
        TestA a = new TestA();
        String x= null;
    }

    public class TestC {
        int a=8;

        @Transient
        String str = "123";
    }

    public class TestD {
        int a=8;

        @Transient
        String str = "123";

        TestD d;
    }

    @Test
    public void basicTest_1() throws Exception {
        assertEquals(JSONSerializer.Serialize(null), "null");
    }

    @Test
    public void basicTest_2() throws Exception {
        assertEquals(JSONSerializer.Serialize("Hola"), "\"Hola\"");
    }

    @Test
    public void basicTest_3() throws Exception {
        assertEquals(JSONSerializer.Serialize( new int[] {1,2,3}), "[1,2,3]");
    }

    @Test
    public void basicTest_31() throws Exception {
        assertEquals(JSONSerializer.Serialize( new Object[] {1,new TestA(),3}), "[1,{\"a\":8,\"str\":\"123\"},3]");
    }

    @Test
    public void basicTest_4() throws Exception {
        assertEquals(JSONSerializer.Serialize( new Object[] {1,null,"aa"}), "[1,null,\"aa\"]");
    }

    @Test
    public void basicTest_5() throws Exception {
        assertEquals(JSONSerializer.Serialize( new TestA() ), "{\"a\":8,\"str\":\"123\"}");
    }

    @Test
    public void basicTest_6() throws Exception {
        assertEquals(JSONSerializer.Serialize( new TestB() ), "{\"a\":{\"a\":8,\"str\":\"123\"},\"x\":null}");
    }

    @Test
    public void basicTest_7() throws Exception {
        assertEquals(JSONSerializer.Serialize( new TestC() ), "{\"a\":8}");
    }

    @Test
    public void basicTest_8() throws Exception {

        TestD d = new TestD();
        d.d=d;

        boolean catched = false;

        try {
            JSONSerializer.Serialize(d);
        } catch(CyclicObjectException e) {
            catched= true;
        }

        assertTrue(catched);
    }


    @Test
    public void basicTest_9() throws Exception {

        ArrayList<Object> al= new ArrayList<>();
        al.add( new TestA(1,"1qaz") );
        al.add( new TestA(2,"2wsx") );
        al.add( new TestA(3,"3edc") );

        ArrayList<Object> al2= new ArrayList<>();
        al2.add( new TestA(11,"11qqaazz") );
        al2.add( new TestA(22,"22wwssxx") );

        al.add( al2 );

        String str = JSONSerializer.Serialize(al);

        System.out.println(str);

        assertTrue( true );
    }


    @Test
    public void basicTest_10() throws Exception {

        ArrayList<Object> list= new ArrayList<>();
        list.add( new TestA(1,"1qaz") );
        list.add( new TestA(2,"2wsx") );
        list.add( new TestA(3,"3edc") );

        HashMap<String,Object> h = new HashMap<>();
        h.put( "a", 1 );
        h.put( "bb", 2 );
        h.put( "ccc", 3 );
        h.put( "dddd", list );

        String str = JSONSerializer.Serialize(h);
        System.out.println(str);
    }

    @Test
    public void basicTest_20() throws Exception {

        JSONObject json = new JSONObject("{\"a\":8,\"str\":\"aaabbb\"}");
        int ia= (int)json.opt("a");
        TestA a= JSONDeserializer.Deserialize(TestA.class, json );

        assertNotEquals( a, null );
        assertEquals(a.a,8);
        assertEquals(a.str,"aaabbb");
    }
}