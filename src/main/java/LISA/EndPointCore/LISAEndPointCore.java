/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LISA.EndPointCore;

import LISA.LISAEndPoint;
import LISA.ServiceCore.LISAServiceCore;
import LISA.ServiceCore.LISAServiceCore.ServiceState;
import LISA.Utils.Config.Config;
import LISA.Utils.Config.ConfigFunctions;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;

/**
 *
 * @author Linus
 */
public class LISAEndPointCore implements Runnable {

    private static LISAEndPointCore instance = null;
    private static String url;
    private static ActiveMQConnectionFactory connectionFactory = null;
    private static Connection connection = null;
    private static Config config = null;
    public static final Thread endpointThread = new Thread(new LISAEndPointCore());
    public static HashMap<String, LISAServiceCore> services = new HashMap<String, LISAServiceCore>();

    protected LISAEndPointCore() {
        config = ConfigFunctions.getConfig();
        setUrl("tcp://" + config.getIp() + ":" + config.getPort());
    }

    public static LISAEndPointCore getInstanceCore() {
        if (instance == null) {
            synchronized (LISAEndPointCore.class) {
                instance = new LISAEndPointCore();
            }
        }
        return instance;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static Connection createConnection() {
        try {
            connectionFactory = new ActiveMQConnectionFactory(url);
            connection = connectionFactory.createConnection();
            connection.start();

        } catch (JMSException ex) {
            Logger.getLogger(LISAEndPointCore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return connection;
    }

    @Override
    public void run() {
        while (true) {
            for (Entry<String, LISAServiceCore> entry : services.entrySet()) {
                String key = entry.getKey();
                LISAServiceCore s = entry.getValue();
                ServiceState state = s.getState();
                if (state.equals(ServiceState.INIT)) {
                    s.init();
                    s.setState(ServiceState.ACTION);
                }
                if (state.equals(ServiceState.ACTION)) {
                    if (s.action()) {
                        s.setState(ServiceState.END);
                    }
                }
                if (state.equals(ServiceState.END)) {
                    s.end();
                    s.setState(ServiceState.WAITING);
                }

            }
        }
    }
}