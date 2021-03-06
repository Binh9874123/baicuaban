package java_utils;

import grammar.GrammarWriter;
import ru.spbstu.pipeline.*;
import java_utils.TypeManager;
import java.io.*;
import java.util.Map;
import java.util.logging.Logger;

public class MWriter implements IWriter {

    int buffer_size;
    Object data;
    final TYPE[] input_types;
    TYPE current_input_type;
    FileOutputStream fos;
    IProducer producer;
    IConsumer consumer;
    Logger logger;

    public MWriter(Logger logger) {
        this.logger = logger;
        input_types = new TYPE[]{TYPE.BYTE};
        logger.info("\nConstructing writer...");
    }

    private RC SemanticParse(Map<String, String> propMap, GrammarWriter g_writer) {
        if (propMap == null)
            return RC.CODE_INVALID_ARGUMENT;
        int instruction_count = 0;
        for (String prop : propMap.keySet()) {
            instruction_count++;
            if (prop.equals(g_writer.token(0))) {
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
        if (instruction_count != g_writer.numberTokens())
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        return RC.CODE_SUCCESS;
    }

    public RC WriteSlice(byte[] data) {
        if (data == null)
            return RC.CODE_SUCCESS;
        if (buffer_size > data.length)
            buffer_size = data.length;
        try {
            int tmp = 0;
            int i;
            //while (tmp <= data.length) {
            for (int j = 0; j <= data.length/buffer_size; ++j) {
                for (i = tmp; i < tmp + buffer_size; ++i){
                    if (i == data.length)
                        break;
                    this.fos.write(data[i]);
                }
                tmp += buffer_size;
            }
        } catch (IOException e) {
            return RC.CODE_FAILED_TO_WRITE;
        }
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setOutputStream(FileOutputStream fos) {
        if (fos == null)
            return RC.CODE_INVALID_OUTPUT_STREAM;
        this.fos = fos;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConfig(String cfg) {
        if (cfg == null)
            return RC.CODE_INVALID_ARGUMENT;
        GrammarWriter g_writer = new GrammarWriter();
        return SemanticParse(SyntaxParser.Parse(cfg, g_writer), g_writer);
    }

    @Override
    public RC execute() {
        data = producer.getMediator(current_input_type).getData();
        return WriteSlice((byte[])data);
    }

    @Override
    public RC setConsumer(IConsumer iConsumer) {
        this.consumer = null;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer p) {
        if (p == null)
            return RC.CODE_INVALID_ARGUMENT;
        this.producer = p;
        current_input_type = TypeManager.MatchTypes(p.getOutputTypes(),input_types);
        if (current_input_type == null)
            return RC.CODE_INVALID_ARGUMENT;
        return RC.CODE_SUCCESS;
    }
}
