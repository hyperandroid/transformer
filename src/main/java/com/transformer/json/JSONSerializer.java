package com.transformer.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONSerializer {

    public static String Serialize( Object obj ) throws CyclicObjectException {
        return Serialize( obj, new HashMap<>() ); // could be SparseArray
    }

    private static String Serialize(Object obj, HashMap<Integer,Object> visited ) throws CyclicObjectException {

        if ( null==obj ) {
            return "null";
        }

        if ( obj instanceof String ) {
            return SerializeString( (String)obj );
        }

        Class c = obj.getClass();

        if (    c.isPrimitive() ||
                c==Boolean.class ||
                c==Short.class ||
                c==Integer.class ||
                c==Long.class ||
                c==Float.class ||
                c==Double.class ||
                c==Byte.class ||
                c==Character.class ) {

            return SerializePrimitive(obj);
        }

        if ( visited.get( obj.hashCode() )!=null ) {
            throw new CyclicObjectException( "Cyclic dependency on "+obj.getClass().getCanonicalName()+". "+obj.toString());
        }
        visited.put( obj.hashCode(), obj );

        if (obj instanceof List) {
            List l = (List)obj;
            return SerializeArray( l.toArray(new Object[l.size()] ), visited );
        }

        if ( obj instanceof Map) {
            return SerializeMap( (Map)obj, visited );
        }

        if (c.isArray()) {
            return SerializeArray(obj, visited);
        }

        return SerializeObject(obj, visited);
    }


    private static String SerializeString(String str ) {
        return "\"" + str + "\"";
    }

    private static String SerializePrimitive( Object obj ) {
        return obj.toString();
    }


    private static String SerializeMap(Map obj, HashMap<Integer,Object> visited ) {
        StringBuilder sb= new StringBuilder();
        boolean comma = false;

        sb.append("{");
        for( Object oentry : obj.entrySet()) {
            Map.Entry e = (Map.Entry)oentry;

            String name = e.getKey().toString();
            Object value = e.getValue();

            if (comma) {
                sb.append(",");
            }
            sb.append(SerializeString(name));
            sb.append(":");
            sb.append(Serialize(value,visited));
            comma = true;

        }
        sb.append("}");

        return sb.toString();
    }

    private static String SerializeObject(Object obj, HashMap<Integer,Object> visited ) {

        StringBuilder sb= new StringBuilder();

        sb.append("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean comma = false;
        for( Field field : fields ) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                String name = field.getName();

                boolean isTransient= field.isAnnotationPresent(Transient.class);

                if ( !name.startsWith("this$") && ( null==value || !isTransient ) ) {

                    if (comma) {
                        sb.append(",");
                    }
                    sb.append(SerializeString(name));
                    sb.append(":");
                    sb.append(Serialize(value,visited));
                    comma = true;

                }
            } catch (IllegalAccessException e) {
                // field value exception.
            }
        }
        sb.append("}");

        return sb.toString();
    }

    private static String SerializeArray(Object arr, HashMap<Integer,Object> visited ) {
        StringBuilder sb= new StringBuilder();
        sb.append("[");

        int l = Array.getLength(arr);
        for( int i=0; i<l; i++ ) {
            sb.append( Serialize(Array.get(arr,i), visited ) );
            if ( i<l-1 ) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();
    }

}
