package java_utils;

import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) {
        if (Validator.isValid(args)) {
            boolean flag = true;
            Logger logger = Logger.getLogger(Main.class.getName());
            Manager manager = new Manager(args[0], logger);
            if (manager.isConfigured()) {
                int iteration = 1;
                logger.info("\nInitializing cycle...");
                while (flag) {
                    // logger.info("\nPipeline runs " + iteration++ + " iteration");
                    flag = manager.Run();
                }
            }
            else
                logger.severe("failed to construct manager");
        }
    }
}