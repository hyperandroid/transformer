package com.transformer.json;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.StringTokenizer;

public class JSONDeserializer {

    public static <T> T Deserialize( Class<T> target, Object json ) throws MatchTypeException {
        if ( json instanceof JSONObject) {
            return DeserializeObject(target, (JSONObject)json);
        } else if ( json instanceof JSONArray ) {
            return DeserializeArray(target, (JSONArray) json);
        }

        return null;
    }

    private static boolean isPrimitive( Class<?> c ) {
        return  c==Character.class  || c==char.class ||
                c==Byte.class       || c==byte.class ||
                c==Short.class      || c==short.class ||
                c==Integer.class    || c==int.class ||
                c==Long.class       || c==long.class ||
                c==Float.class      || c==float.class ||
                c==Double.class     || c==double.class ||
                c==Boolean.class    || c==boolean.class ||
                c==String.class;
    }

    private static boolean isValidDeserializableField(Field field) {
        boolean isTransient= field.isAnnotationPresent(Transient.class);
        boolean validName = !field.getName().startsWith("this$");

        return !isTransient && validName;
    }

    private static <T> T DeserializeObject(@NonNull Class<T> targetClass, JSONObject json ) throws MatchTypeException {

        T target = createTarget( targetClass );
        if ( null==target ) {
            return null;
        }

        for( Field field : target.getClass().getDeclaredFields() ) {

            if ( isValidDeserializableField(field) ) {
                field.setAccessible(true);
                Class<?> field_type_class = field.getType();

                try {
                    if (isPrimitive(field_type_class)) {
                        Object json_value_for_field = json.opt(field.getName());
                        field.set(target, json_value_for_field);
                    } else {
                        Object v = json.opt(field.getName());
                        if (null != v) {
                            Object value;

                            // destination field is a List.
                            if ( List.class.isAssignableFrom(field_type_class ) ) {

                                Type type = field.getGenericType();
                                ParameterizedType ptype= (ParameterizedType)type;
                                String stype = ptype.getActualTypeArguments()[0].toString();

                                StringTokenizer st= new StringTokenizer(stype," ");
                                st.nextToken();
                                String sclass= st.nextToken();

                                Class<?> cclass = Class.forName(sclass);

                                value = DeserializeList(field_type_class, cclass, (JSONArray)v);

                            } else {
                                value = Deserialize(field_type_class, v);
                            }
                            field.set(target, value );
                        } else {
                            field.set(target, null);
                        }
                    }
                } catch (Exception x) {
                    throw new MatchTypeException("Setting " + field.getName() + " in " + field_type_class.getCanonicalName() + " got error: " + x.toString());
                }
            }
        }

        return target;
    }

    private static <T> T DeserializeArray(@NonNull Class<T> target, JSONArray json ) throws MatchTypeException {

        if (!target.isArray()) {
            throw new MatchTypeException("Assigning json array to non array type: " + target.getCanonicalName());
        }

        return (T)DeserializeArrayImpl(target.getComponentType(), json);
    }

    private static Object DeserializeArrayImpl( Class<?> component_type, JSONArray json ) throws MatchTypeException {

        Object array;

        try {
            array = Array.newInstance(component_type, json.length());
        } catch (Exception x) {
            throw new MatchTypeException("Can't create array of type: " + component_type.getCanonicalName());
        }

        for (int i = 0; i < json.length(); i++) {
            try {
                Object obj = json.get(i);

                if (isPrimitive(obj.getClass())) {
                    Array.set(array, i, json.get(i));
                } else {
                    Object v = json.get(i);
                    if (null != v) {
                        Array.set(array, i, Deserialize(component_type, v));
                    } else {
                        Array.set(array, i, null);
                    }
                }

            } catch (JSONException e) {
                throw new MatchTypeException("JSON error while deserializing array: " + e.toString());
            }
        }

        return array;
    }

    private static <T> List<T> DeserializeList( Class<?> clist, Class<?> target, JSONArray json ) throws MatchTypeException {

        try {

            Object arr = DeserializeArrayImpl( target, json );

            List list = (List)clist.getConstructor().newInstance();

            for( int i=0; i<Array.getLength(arr); i++ ) {
                list.add( Array.get(arr,i) );
            }

            return (List<T>)list;

        } catch(Exception x){
            throw new MatchTypeException("Exception deserializing list: "+x.toString());
        }
    }

    private static <T> T createTarget(Class<T> c ) {
        try {
            if ( c.isMemberClass()) {
                // solve inner class.

                Class<?> parent_class= c.getEnclosingClass();
                Object parent_object = parent_class.newInstance();

                Constructor<?> ctor = c.getDeclaredConstructor(parent_class);
                ctor.setAccessible(true);
                return (T)ctor.newInstance(parent_object);
            } else {
                return c.newInstance();
            }
        } catch( Exception e ) {
            return null;
        }
    }
}
