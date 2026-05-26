import java.io.*;
import java.net.*;
import java.util.*;

public class ScrapeSunat {
    public static void main(String[] args) {
        try {
            String ruc = "20100119227";
            String urlParameters = "accion=consPorRuc&nroRuc=" + ruc + "&actReturn=1&numRnd=" + Math.random();
            byte[] postData = urlParameters.getBytes("UTF-8");
            
            URL url = new URL("https://e-consultaruc.sunat.gob.pe/cl-ti-itmrconsruc/jcrS00Alias");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
            
            try(DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData);
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            String html = response.toString();
            System.out.println("Length: " + html.length());
            
            if(html.contains("Actividad(es) Econ")) {
                int index = html.indexOf("Actividad(es) Econ");
                System.out.println("FOUND: " + html.substring(index, Math.min(index + 500, html.length())));
            } else {
                System.out.println("Not found. Check HTML.");
            }
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
