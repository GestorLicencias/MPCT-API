import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ScrapeTest {
    public static void main(String[] args) {
        try {
            // Test SUNAT directly
            String ruc = "20100119227";
            URL url = new URL("https://e-consultaruc.sunat.gob.pe/cl-ti-itmrconsruc/jcrS00Alias?accion=consPorRuc&nroRuc=" + ruc);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            
            String html = content.toString();
            if(html.contains("Actividad")) {
                System.out.println("SUNAT Success: Contains Actividad");
                int index = html.indexOf("Actividad");
                System.out.println(html.substring(index, Math.min(index + 300, html.length())));
            } else {
                System.out.println("SUNAT Failed: No Actividad found. Length: " + html.length());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
