package java_utils;

import ru.spbstu.pipeline.TYPE;

import java.io.*;
import java.nio.ByteBuffer;

public class TypeManager {

    static TYPE MatchTypes(TYPE[] prod_types, TYPE[] input_types) {
        for (TYPE prod_type : prod_types) {
            for (TYPE possible_type : input_types) {
                if (prod_type == possible_type) {
                    return prod_type;
                }
            }
        }
        return null;
    }

    static short[] CastToShort(byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        short[] new_data = new short[data.length / 2];
        for (int i = 0; i < new_data.length; ++i)
            new_data[i] = byteBuffer.getShort();
        return new_data;
    }

//    static short[] CastToShort(char[] data) {
//        short[] new_data = new short[data.length / 2];
//        for (int i = 0; i < new_data.length; ++i)
//            new_data[i] = (short) data[i];
//        return new_data;
//    }

    static char[] CastToChar(byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        char[] new_data = new char[data.length / 2];
        for (int i = 0; i < new_data.length; ++i)
            new_data[i] = byteBuffer.getChar();
        return new_data;
    }

//    static char[] CastToChar(short[] data) {
//        char[] new_data = new char[data.length / 2];
//        for (int i = 0; i < new_data.length; ++i)
//            new_data[i] = (char) data[i];
//        return new_data;
//    }

    static byte[] CastToByte(short[] data) {
        byte[] new_data = new byte[data.length * 2];
        int j = 0;
        for (short datum : data) {
            new_data[j + 1] = (byte) (datum & 0xFF);
            new_data[j] = (byte) ((datum >> 8) & 0xFF);
            j += 2;
        }
        return new_data;
    }


    static Object getCopy(Object data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream ous = new ObjectOutputStream(baos);
            ous.writeObject(data);
            ous.close();
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            baos.close();
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object copy = ois.readObject();
            bais.close();
            ois.close();
            return copy;
        } catch (IOException | ClassNotFoundException exception) {
            exception.printStackTrace();
            return null;
        }
    }

}
