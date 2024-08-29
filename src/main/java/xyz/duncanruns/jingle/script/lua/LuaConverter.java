package xyz.duncanruns.jingle.script.lua;

import com.google.common.primitives.Primitives;
import org.luaj.vm2.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class LuaConverter {
    private static final Map<Class<?>, Function<Varargs, Object>> luaToJavaMap = generateLuaToJavaMap();
    private static final Map<Class<?>, Function<Object, Varargs>> javaToLuaMap = generateJavaToLuaMap();
    private static final Map<Class<?>, String> luaTypeMap = generateLuaTypeMap();

    private LuaConverter() {
    }

    private static Map<Class<?>, String> generateLuaTypeMap() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(int.class, "number");
        map.put(LuaInteger.class, "number");
        map.put(float.class, "number");
        map.put(long.class, "number");
        map.put(double.class, "number");
        map.put(LuaNumber.class, "number");
        map.put(LuaDouble.class, "number");
        map.put(char.class, "number");
        map.put(short.class, "number");
        map.put(byte.class, "number");
        map.put(boolean.class, "boolean");
        map.put(LuaBoolean.class, "boolean");
        map.put(void.class, "nil");
        map.put(LuaNil.class, "nil");
        map.put(String.class, "string");
        map.put(LuaString.class, "string");
        map.put(LuaValue.class, "any");
        map.put(Varargs.class, "any");
        map.put(LuaFunction.class, "function");
        map.put(LuaClosure.class, "function");
        map.put(LuaTable.class, "table");
        Primitives.allWrapperTypes().forEach(clazz -> map.put(clazz, map.get(Primitives.unwrap(clazz))));
        return map;
    }

    private static Map<Class<?>, Function<Varargs, Object>> generateLuaToJavaMap() {
        Map<Class<?>, Function<Varargs, Object>> map = new HashMap<>();
        map.put(LuaNil.class, varargs -> null);
        map.put(int.class, varargs -> ((LuaValue) varargs).checknumber().toint());
        map.put(LuaInteger.class, varargs -> ((LuaValue) varargs).checknumber().toint());
        map.put(float.class, varargs -> ((LuaValue) varargs).checknumber().tofloat());
        map.put(long.class, varargs -> ((LuaValue) varargs).checknumber().tolong());
        map.put(double.class, varargs -> ((LuaValue) varargs).checknumber().todouble());
        map.put(LuaNumber.class, varargs -> ((LuaValue) varargs).checknumber().todouble());
        map.put(LuaDouble.class, varargs -> ((LuaValue) varargs).checknumber().todouble());
        map.put(char.class, varargs -> ((LuaValue) varargs).checknumber().tochar());
        map.put(short.class, varargs -> ((LuaValue) varargs).checknumber().toshort());
        map.put(byte.class, varargs -> ((LuaValue) varargs).checknumber().tobyte());
        map.put(boolean.class, varargs -> ((LuaValue) varargs).checkboolean());
        map.put(LuaBoolean.class, varargs -> ((LuaValue) varargs).checkboolean());
        map.put(String.class, varargs -> ((LuaValue) varargs).isnil() ? null : ((LuaValue) varargs).checkjstring());
        map.put(LuaString.class, varargs -> ((LuaValue) varargs).isnil() ? null : ((LuaValue) varargs).checkjstring());
        map.put(LuaValue.class, varargs -> varargs);
        map.put(Varargs.class, varargs -> varargs);
        Primitives.allWrapperTypes().forEach(clazz -> map.put(clazz, varargs -> varargs == LuaValue.NIL ? null : map.get(Primitives.unwrap(clazz)).apply(varargs)));
        return map;
    }

    private static Map<Class<?>, Function<Object, Varargs>> generateJavaToLuaMap() {
        Map<Class<?>, Function<Object, Varargs>> map = new HashMap<>();
        map.put(int.class, o -> LuaValue.valueOf((int) o));
        map.put(float.class, o -> LuaValue.valueOf((float) o));
        map.put(long.class, o -> LuaValue.valueOf((long) o));
        map.put(double.class, o -> LuaValue.valueOf((double) o));
        map.put(char.class, o -> LuaValue.valueOf((char) o));
        map.put(short.class, o -> LuaValue.valueOf((short) o));
        map.put(byte.class, o -> LuaValue.valueOf((byte) o));
        map.put(boolean.class, o -> LuaValue.valueOf((boolean) o));
        map.put(void.class, o -> LuaValue.NIL);
        map.put(String.class, o -> LuaValue.valueOf((String) o));
        Primitives.allWrapperTypes().forEach(clazz -> map.put(clazz, o -> o == null ? LuaValue.NIL : map.get(Primitives.unwrap(clazz)).apply(o)));
        return map;
    }

    public static Varargs convertToLua(Object value) {
        if (value == null) {
            return LuaValue.NIL;
        }
        if (value instanceof Varargs) {
            return (Varargs) value;
        }
        return javaToLuaMap.get(value.getClass()).apply(value);
    }

    public static Varargs convertToLua(Object value, Class<?> conversionClass) {
        if (value == null) {
            return LuaValue.NIL;
        }
        if (value instanceof Varargs) {
            return (Varargs) value;
        }
        return javaToLuaMap.get(conversionClass).apply(value);
    }

    public static Object convertToJava(Varargs value) {
        return luaToJavaMap.getOrDefault(value.getClass(), varargs -> varargs).apply(value);
    }

    public static <T> Object convertToJava(Varargs value, Class<?> conversionClass) {
        return luaToJavaMap.getOrDefault(conversionClass, LuaConverter::convertToJava).apply(value);
    }

    public static String classToLuaName(Class<?> clazz) {
        return luaTypeMap.get(clazz);
    }
}
