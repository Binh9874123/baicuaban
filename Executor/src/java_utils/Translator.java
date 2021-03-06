package java_utils;

import grammar.GrammarExecutor;
import ru.spbstu.pipeline.*;

import java.util.logging.Logger;
import java.nio.charset.Charset;
import java.util.Map;

public class Translator implements IExecutor {

    final char[] lat = new char[]{'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'r', 's', 't', 'v', 'y', 'z'};
    final char[] rus = new char[]{'б', 'ц', 'д', 'ф', 'г', 'х', 'ж', 'к', 'л', 'м', 'н', 'п', 'р', 'с', 'т', 'в', 'й', 'з'};

    final TYPE[] input_types;
    final TYPE[] output_types;
    TYPE current_input_type;
    IMediator mediator;
    Object data;
    Boolean mode;   //True - кодирование, иначе декодирование
    IProducer producer;
    IConsumer consumer;
    Logger logger;

    public Translator(Logger logger) {
        this.logger = logger;
        input_types = new TYPE[]{TYPE.BYTE, TYPE.SHORT};
        output_types = new TYPE[]{TYPE.BYTE, TYPE.SHORT};
        logger.info("\nConstructing translator...");
    }

    private RC SemanticParse(Map<String, String> propMap, GrammarExecutor g_executor) {
        if (propMap == null)
            return RC.CODE_INVALID_ARGUMENT;
        int instruction_count = 0;
        for (String prop : propMap.keySet()) {
            instruction_count++;
            if (prop.equals(g_executor.token(0))) {
                if (propMap.get(prop).equals("true"))
                    mode = true;
                else if (propMap.get(prop).equals("false"))
                    mode = false;
                else
                    return RC.CODE_INVALID_ARGUMENT;
            } else {
                instruction_count--;
            }
        }
        if (instruction_count != g_executor.numberTokens())
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        return RC.CODE_SUCCESS;
    }

    private byte[] Process(byte[] data) {
        if (data == null)
            return null;
        char[] tmpData = new String(data, Charset.forName("cp1251")).toCharArray();
        for (int i = 0; i < data.length; i++) {
            tmpData[i] = charReplace(tmpData[i]);
        }
        return new String(tmpData).getBytes((Charset.forName("cp1251")));
    }

    private char charReplace(Character symbol) {
        Character res = symbol;
        boolean caseFl = Character.isUpperCase(symbol);
        symbol = Character.toLowerCase(symbol);
        for (int i = 0; i < lat.length; i++) {
            if (mode) {
                if (symbol == rus[i]) {
                    res = lat[i];
                    break;
                }
            } else {
                if (symbol == lat[i]) {
                    res = rus[i];
                    break;
                }
            }
        }
        if (caseFl)
            return Character.toUpperCase(res);
        return res;
    }

    @Override
    public RC setConfig(String cfg) {
        if (cfg == null)
            return RC.CODE_INVALID_ARGUMENT;
        GrammarExecutor g_executor = new GrammarExecutor();
        return SemanticParse(SyntaxParser.Parse(cfg, g_executor), g_executor);
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
        if (p == null)
            return RC.CODE_INVALID_ARGUMENT;
        this.producer = p;
        current_input_type = TypeManager.MatchTypes(p.getOutputTypes(), input_types);
        mediator = p.getMediator(current_input_type);
        if (current_input_type == null)
            return RC.CODE_INVALID_ARGUMENT;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute() {

        data = mediator.getData();

        //Искусственная ситуация
        if (current_input_type == TYPE.SHORT)
            data = TypeManager.CastToByte((short[]) data);

        data = Process((byte[]) data);
        return consumer.execute();
    }

    @Override
    public TYPE[] getOutputTypes() {
        return output_types;
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
            return TypeManager.CastToShort((byte[]) TypeManager.getCopy(data));
        }
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

