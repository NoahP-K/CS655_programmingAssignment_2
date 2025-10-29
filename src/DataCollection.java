import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DataCollection {
    //simulator configs
    static int nsim = 1000;
    static int delay = 1000;
    static int trace = 0;
    static int windowsize = 8;
    static double timeout = 30.0;

    public final static void main(String[] argv) {
        //collect loss data in text file to be moved to a csv later
        String fileName = "lossData.csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            //for each loss probability...
            for(double i = 0; i<=.5; i+= 0.05) {
                writer.write("=========" + String.valueOf(i)+ "\r\n");
                StringBuilder rttResults = new StringBuilder();
                StringBuilder commsResults = new StringBuilder();
                StringBuilder retransmissionResults = new StringBuilder();
                //for each seed...
                for(int j = 1; j <= 10; j++){
                    StudentNetworkSimulator simulator = new StudentNetworkSimulator(nsim, i, 0.0, delay,
                            trace, j, windowsize, timeout);
                    simulator.runSimulator();
                    double[] results = simulator.getTimeStats();
                    rttResults.append(results[0]);
                    commsResults.append(results[1]);
                    retransmissionResults.append(results[2]);
                    if(j<10) {
                        rttResults.append(",");
                        commsResults.append(",");
                        retransmissionResults.append(",");
                    }
                }
                writer.write(rttResults + "\r\n");
                writer.write(commsResults + "\r\n");
                writer.write(retransmissionResults + "\r\n");
                writer.write("\r\n");
            }
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
        }

        //repeat with corruption data
        fileName = "corruptionData.csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            //for each corruption probability...
            for(double i = 0; i<=.5; i+= 0.05) {
                writer.write("=========" + String.valueOf(i)+ "\r\n");
                StringBuilder rttResults = new StringBuilder();
                StringBuilder commsResults = new StringBuilder();
                StringBuilder retransmissionResults = new StringBuilder();
                //for each seed...
                for(int j = 1; j <= 10; j++){
                    StudentNetworkSimulator simulator = new StudentNetworkSimulator(nsim, 0, i, delay,
                            trace, j, windowsize, timeout);
                    simulator.runSimulator();
                    double[] results = simulator.getTimeStats();
                    rttResults.append(results[0]);
                    commsResults.append(results[1]);
                    retransmissionResults.append(results[2]);
                    if(j<10) {
                        rttResults.append(",");
                        commsResults.append(",");
                        retransmissionResults.append(",");
                    }
                }
                writer.write(rttResults + "\r\n");
                writer.write(commsResults + "\r\n");
                writer.write(retransmissionResults + "\r\n");
                writer.write("\r\n");
            }
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }
}
