package java_utils;


import grammar.GrammarReader;
import ru.spbstu.pipeline.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Logger;

public class MReader implements IReader, Serializable {

    int buffer_size;
    final TYPE[] output_types;
    Object data;
    FileInputStream fis;
    IConsumer consumer;
    IProducer producer;
    Logger logger;

    public MReader(Logger logger) {
        this.logger = logger;
        output_types = new TYPE[]{TYPE.BYTE, TYPE.SHORT};
        //output_types = new TYPE[]{TYPE.SHORT};
        logger.info("\nConstructing reader...");
    }

    private RC SemanticParse(Map<String, String> propMap, GrammarReader g_reader) {
        if (propMap == null)
            return RC.CODE_INVALID_ARGUMENT;
        int instruction_count = 0;
        for (String prop : propMap.keySet()) {
            instruction_count++;
            if (prop.equals(g_reader.token(0))) {
                try {
                    buffer_size = Integer.parseInt(propMap.get(prop));
                } catch (NumberFormatException exception) {
                    buffer_size = 0;
                    return RC.CODE_INVALID_ARGUMENT;
                }
            } else {
                instruction_count--;
            }
        }
        if (instruction_count != g_reader.numberTokens())
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        return RC.CODE_SUCCESS;
    }

    public byte[] ReadSlice() {
        byte[] data;
        try {
            int maxArraySize;
            if (fis.available() == 0)
                return null;
            else maxArraySize = Math.min(fis.available(), buffer_size);
            data = new byte[maxArraySize];
            if (this.fis.read(data, 0, maxArraySize) == -1)
                return null;

        } catch (NullPointerException | IndexOutOfBoundsException | IOException exception) {
            return null;
        }
        return new String(data, Charset.forName("cp1251")).getBytes(Charset.forName("cp1251"));
    }

    @Override
    public RC setInputStream(FileInputStream fis) {
        if (fis == null)
            return RC.CODE_INVALID_INPUT_STREAM;
        this.fis = fis;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConfig(String cfg) {
        if (cfg == null)
            return RC.CODE_INVALID_ARGUMENT;
        GrammarReader g_reader = new GrammarReader();
        return SemanticParse(SyntaxParser.Parse(cfg, g_reader), g_reader);
    }

    class ByteMediator implements IMediator {

        @Override
        public Object getData() {
            if (data == null)
                return null;
            return TypeManager.getCopy(data);
        }

    }

    class ShortMediator implements IMediator {

        @Override
        public Object getData() {
            if (data == null)
                return null;
            return TypeManager.CastToShort((byte[])TypeManager.getCopy(data));
        }
    }

    @Override
    public RC execute() {

        RC codeMsg = RC.CODE_SUCCESS;
        do {
            data = ReadSlice();
            if (data == null)
                break;
            codeMsg = this.consumer.execute();
            logger.info(LoggerMessage.GetLogMessage(codeMsg));
            if (codeMsg != RC.CODE_SUCCESS)
                break;
        } while (true);
        return codeMsg;

    }

    @Override
    public RC setConsumer(IConsumer c) {
        if (c == null)
            return RC.CODE_INVALID_ARGUMENT;
        this.consumer = c;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer p) {
        producer = null;
        return RC.CODE_SUCCESS;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return output_types;
    }

    @Override
    public IMediator getMediator(TYPE type) {
        switch (type) {
            case BYTE:
                return new ByteMediator();
            case SHORT:
                return new ShortMediator();
            default:
                return null;
        }
    }

}
