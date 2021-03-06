package java_utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import grammar.GrammarManager;
import ru.spbstu.pipeline.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.logging.Logger;

public class Manager {

    private FileInputStream inputStream;
    private FileOutputStream outputStream;
    private IPipelineStep[] pipeline;
    private boolean configured = false;
    Logger logger;

    public boolean isConfigured() {
        return configured;
    }

    Manager(String cfg, Logger logger) {
        GrammarManager g_manager = new GrammarManager();
        this.logger = logger;
        logger.info("\nConstructing pipeline manager...");
        logger.info(LoggerMessage.GetLogMessage(SemanticParse(SyntaxParser.Parse(cfg, g_manager), g_manager)));
    }

    RC SemanticParse(Map<String, String> propMap, GrammarManager g_manager) {
        if (propMap == null)
            return RC.CODE_FAILED_TO_READ;
        int instruction_count = 0;
        for (String prop : propMap.keySet()) {
            instruction_count++;
            if (prop.equals(g_manager.token(0))) {
                try {
                    logger.info("Setting manager input stream...");
                    inputStream = new FileInputStream(propMap.get(prop));
                } catch (FileNotFoundException | SecurityException exception) {
                    inputStream = null;
                    return RC.CODE_INVALID_INPUT_STREAM;
                }
                finally {
                    logger.info(LoggerMessage.GetLogMessage(RC.CODE_SUCCESS));
                }
            } else if (prop.equals(g_manager.token(1))) {
                try {
                    logger.info("Setting manager output stream...");
                    outputStream = new FileOutputStream(propMap.get(prop));
                } catch (FileNotFoundException | SecurityException exception) {
                    outputStream = null;
                    return RC.CODE_INVALID_OUTPUT_STREAM;
                }
                finally {
                    logger.info(LoggerMessage.GetLogMessage(RC.CODE_SUCCESS));
                }
            } else if (prop.equals(g_manager.token(2))) {
                logger.info("Building pipeline order...");
                String[] order = propMap.get(prop).trim().split(g_manager.DELIMITER_ORDER);
                pipeline = BuildPipeline(order, g_manager);
                if (pipeline == null) {
                    return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
                }
                logger.info(LoggerMessage.GetLogMessage(RC.CODE_SUCCESS));
            } else {
                instruction_count--;
            }
        }
        if (instruction_count != g_manager.numberTokens())
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        configured = true;
        return RC.CODE_SUCCESS;
    }

    IPipelineStep[] BuildPipeline(String[] order, GrammarManager g_manager) {
        String logMsg;
        IPipelineStep[] pipeline_tokens = new IPipelineStep[order.length];
        for (int i = 0; i < order.length; ++i) {
            String[] exec_n_config = order[i].trim().split(g_manager.DELIMITER_EXEC_AND_CFG);
            if (exec_n_config.length != 2)
                return null;
            logMsg = "Failed to create instance by its class name";
            try {
                logger.info("Creating worker...");
                Class<?> cl = Class.forName(exec_n_config[0].trim());
                // Насколько я помню, на потоке существует договоренность, что конструктор принимает только класс LoggerMessage
                pipeline_tokens[i] = (IPipelineStep) cl.getConstructor(Logger.class).newInstance(logger);
                logger.info("Configuring...");
                logMsg = LoggerMessage.GetLogMessage(((IConfigurable) pipeline_tokens[i]).setConfig(exec_n_config[1].trim()));
                logger.info(logMsg);
                if (!logMsg.equals(LoggerMessage.GetLogMessage(RC.CODE_SUCCESS))) {
                    return null;
                }

                if (i == 0) {
                    logger.info("Setting input stream...");
                    logMsg = LoggerMessage.GetLogMessage(((IReader) pipeline_tokens[i]).setInputStream(this.inputStream));
                    logger.info(logMsg);
                    if (!logMsg.equals(LoggerMessage.GetLogMessage(RC.CODE_SUCCESS)))
                        return null;

                    logger.info("Setting producer...");
                    logMsg = LoggerMessage.GetLogMessage(pipeline_tokens[i].setProducer(null));
                    logger.info(logMsg);
                    if (!logMsg.equals(LoggerMessage.GetLogMessage(RC.CODE_SUCCESS)))
                        return null;

                    continue;

                } else if (i == order.length - 1) {
                    logger.info("Setting output stream...\t");
                    logMsg = LoggerMessage.GetLogMessage(((IWriter) pipeline_tokens[i]).setOutputStream(this.outputStream));
                    logger.info(logMsg);
                    if (!logMsg.equals(LoggerMessage.GetLogMessage(RC.CODE_SUCCESS)))
                        return null;

                    logger.info("Setting producer...\t");
                    logMsg = LoggerMessage.GetLogMessage(pipeline_tokens[i].setProducer((IProducer)pipeline_tokens[i - 1]));
                    logger.info(logMsg);
                    if (!logMsg.equals(LoggerMessage.GetLogMessage(RC.CODE_SUCCESS)))
                        return null;

                } else {
                    logger.info("Setting producer...");
                    logMsg = LoggerMessage.GetLogMessage(pipeline_tokens[i].setProducer((IProducer)pipeline_tokens[i - 1]));
                    logger.info(logMsg);
                    if (!logMsg.equals(LoggerMessage.GetLogMessage(RC.CODE_SUCCESS)))
                        return null;
                }
                logger.info("Setting consumer for previous token...");
                logMsg = LoggerMessage.GetLogMessage(pipeline_tokens[i - 1].setConsumer((IConsumer)pipeline_tokens[i]));
                logger.info(logMsg);
                if (!logMsg.equals(LoggerMessage.GetLogMessage(RC.CODE_SUCCESS)))
                    return null;
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | AssertionError | NullPointerException | InvocationTargetException exception) {
                logger.severe(logMsg);
                return null;
            }
        }
        return pipeline_tokens;
    }

    public boolean Run() {
        logger.info("Initializing pipeline execution...");
        RC rc = ((IReader)pipeline[0]).execute();
        if (rc == RC.CODE_SUCCESS) {
            logger.info("Pipeline work complete!");
            return false;
        }
        return true;
    }
}
