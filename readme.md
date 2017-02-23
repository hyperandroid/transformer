# Transformer 

A Java <-> Json converter with type inference.

## Why `Transformer`

In my day to day, I have to map a lot of Java to Json objects and vice versa. 
It is a tedious work to map a Class instance to a Json and deal with class modifications, objects
 structure, etc. Transformer comes handy for automatic, reflection-based type mappings.

Currently, `Transformer` uses runtime reflection for mapping between Java and Json. There's some
WIP to make `Transformer` work 

## How

Transformer exposes two main class objects: `JSONSerializer` and `JSONDeserializer`.

### `JSONSerializer` 

Serialization happens by runtime reflection. It recursively scans object's fields to create a Json
representation of it.

Fields with names of the form `this$` are not serialized since they identify inner classes. Also,
fields annotated with `@Transient` are not serialized.

`JSONSerializer` exposes just one method: `Serialize( Object obj )`. This method takes track of cyclic
serializations issues by keeping a `HashMap` (could be `SparseArray` but depending on your objects
hashmap might be more suitable) where an object is associated with its `hashCode`. If a previously
existing `hashCode` is found, it is assumed a cyclic reference in the Object fields graph, and the
serialization would fire a `CyclicObjectException`. This is a `RuntimeException` and hance not 
mandatory to catch.

`JSONSerializer` has two specific use cases for `List<?>` and `Map<?,?>` objects. `List` objects
will be serialized as Json arrays, and `Map` objects as Json objects.
 
#### Serialization examples:

##### Serialization of null
`assertEquals(JSONSerializer.Serialize(null), "null")`

##### Serialization of String
`assertEquals(JSONSerializer.Serialize("Hola"), "\"Hola\"")`. 

Note double quotes on resulting string.

##### Serialization of primitive array
`assertEquals(JSONSerializer.Serialize( new int[] {1,2,3}), "[1,2,3]")`. 

Yes, serialize arrays like a boss.

##### Serialization of mixed Object array.
`assertEquals(JSONSerializer.Serialize( new Object[] {1,new TestA(),3}), "[1,{\"a\":8,\"str\":\"123\"},3]")`

##### Serialization of mixed Object array with null values.
`assertEquals(JSONSerializer.Serialize( new Object[] {1,null,"aa"}), "[1,null,\"aa\"]")`. 

##### Serialization of simple Java object:

Given these simple Java class elements:
```Java
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
```

`assertEquals(JSONSerializer.Serialize( new TestA() ), "{\"a\":8,\"str\":\"123\"}");`

##### Serialization of more complex Java object

`assertEquals(JSONSerializer.Serialize( new TestB() ), "{\"a\":{\"a\":8,\"str\":\"123\"},\"x\":null}");`

##### Serialization of `@Transient` fields

`assertEquals(JSONSerializer.Serialize( new TestC() ), "{\"a\":8}");`

##### Serialization of cyclic object (catch error)

```Java
    void testRaiseException() {
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
```

* Serialization of List

```Java
    void testSerializeList() {
        ArrayList<Object> al= new ArrayList<>();
        al.add( new TestA(1,"1qaz") );
        al.add( new TestA(2,"2wsx") );
        al.add( new TestA(3,"3edc") );
    
        ArrayList<Object> al2= new ArrayList<>();
        al2.add( new TestA(11,"11qqaazz") );
        al2.add( new TestA(22,"22wwssxx") );
    
        al.add( al2 );
    
        String str = JSONSerializer.Serialize(al);
    }
```

```
# result:
str= [
    {"a":1,"str":"1qaz"},
    {"a":2,"str":"2wsx"},
    {"a":3,"str":"3edc"},
    [
        {"a":11,"str":"11qqaazz"},
        {"a":22,"str":"22wwssxx"}
    ]
]
```

##### Serialization of Map

```Java
    void testSerialieMap() {
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
    }
```

```
// result:
str = {
    "bb":2,
    "a":1,
    "ccc":3,
    "dddd":[
        {"a":1,"str":"1qaz"},
        {"a":2,"str":"2wsx"},
        {"a":3,"str":"3edc"}
    ]
}
```

### JSONDeserializer

Deserialization is the inverse process of serialization, with some major caveats:

* Deserialization target must have all fields qualified at the class level. `Transformer` can not infer a type for `Object` or `List<String>`,
but will do a good job at mapping `ArrayList<Integer>`.
* Deserialization target fields must be of type primitive `(boolean, byte, char, short, int, long, float, double, String)`,
type `List`, or any other user defined class type whose fields follow the same rules. 
The `List` type, must be fully qualified, e.g. `ArrayList<TestA>`.
* Deserialization target objects can be main or inner classes. 

Like `JSONSerializer`, `JSONDeserializer` will omit set values for fields annotated at `@Transient`.
  
The process of deserialization may crash raising a `MatchTypeException` for many different reasons, but most likely
because of a bad type mapping between a Java field and a JSON node. 
In example, assign a String to a Java int field.

`Transformer` deserializer is very powerful. Let's see some examples:

#### Deserialization examples

All examples will use these classes as reference:

```Java
    public class TestA {
        int a=8;
        String str = "123";

        TestA( int a, String str ) {
            this.a=a;
            this.str=str;
        }
    }

    public class TestB {
        long l = 0L;
        TestA a;
        TestB b;
    }

    public class TestC {
        long l = 0L;
        TestA a;
        TestB b;
        int[] arr;
    }

    public class TestD {
        ArrayList<TestA> arr;
    }

```

##### Deserialization of simple object

```Java
    void deserializeTestA() {
        JSONObject json = new JSONObject(
                "{" +
                    "\"a\":8," +
                    "\"str\":\"aaabbb\"" +
                 "}");
    
        TestA a = JSONDeserializer.Deserialize(TestA.class, json);
        // a.a = 8
        // a.str = "aaabbb"
    }
```

##### Deserialization of more complex object

```Java
    void deserializeTestB() {
        JSONObject json = new JSONObject(
                "{" +
                    "\"l\":89485," +
                    "\"a\":{" +
                        "\"a\":81," +
                        "\"str\":\"cdef\"" +
                    "}," +
                    "\"b\":null" +
                 "}");
                 
        TestB a = JSONDeserializer.Deserialize(TestB.class, json);  
          
         // a.l = 89485
         // a.b = null
         // a.a = 81
         // a.str = "cdef"
    }
```

##### Deserialization of more comples object 2

```Java
    void deserializeTest() {
        JSONObject json = new JSONObject(
            "{" +
                "\"l\":5," +
                 "\"a\":{" +
                    "\"a\":12345," +
                    "\"str\":\"1qaz\"" +
                 "}," +
                 "\"b\":null," +
                 "\"arr\":[1,2,3,4,5,6]" +
            "}");
            
        TestC a = JSONDeserializer.Deserialize( TestC.class , json);
        
        // a.l = 5
        // a.arr[0]=1
    }
```

##### Deserialization of primitive array:

```Java
    void deserializePrimitiveArray() {
        JSONArray json = new JSONArray("[0,1,2,3]");
        int[] a = JSONDeserializer.Deserialize( int[].class , json);
        
        // a[0] = 0;
        // a[1] = 1;
        // a[2] = 2;
        // a[3] = 3;
    }
```

##### Deserialization of non primitive array:

```Java
    void deserializeNonPrimitiveArray() {
    
        // use serializer to get json representation.
        TestA[] ta = new TestA[] {
                    new TestA(1234,"1qaz"),
                    new TestA(2345,"2wsx"),
                    new TestA(3456,"3edc")
            };
        String sta= JSONSerializer.Serialize(ta);
        JSONArray json = new JSONArray(sta);
        
        // deserialize array:
        TestA[] a = JSONDeserializer.Deserialize( TestA[].class , json);
            
        // a[0].a = 1234
        // a[2].str = "3edc"
    }
```

##### Deserialization of field type List<?>

```Java
    void deserializeList() {
        JSONObject json = new JSONObject(
            "{" +
                "arr:[" +
                    "{\"a\":12345,\"str\":\"1qaz\"}," +
                    "{\"a\":2345,\"str\":\"2wsx\"}," +
                    "{\"a\":34567,\"str\":\"3edc\"}," +
                    "null" +
                "]" +
            "}");

        TestD a = JSONDeserializer.Deserialize( TestD.class , json);
            
        // a.arr[0].str = "1qaz"
        // a.arr[2].a = "34567"
    }
```

##### Deserialization mapping error

```Java
    void deserializeMappingError() {
        JSONArray json = new JSONArray(
            "[" +
                "{\"a\":12345,\"str\":\"1qaz\"}," +
                "{\"a\":2345,\"str\":\"2wsx\"}," +
                "{\"a\":34567,\"str\":\"3edc\"}" +
            "]");
            
        TestB[] a = JSONDeserializer.Deserialize( TestB[].class , json);

        // error. Mapping TestA types into TestB types.
        // com.transformer.json.MatchTypeException: 
        //      Setting l in long got error: java.lang.IllegalArgumentException: 
        //      field com.hyper.ui.MainActivity$TestB.l has type long, got null
    }
```
