import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        /*URL url = new URL("http://host/");
        URLConnection urlConnection = url.openConnection();
        urlConnection.setDoOutput(true);
        PrintWriter printWriter = new PrintWriter(urlConnection.getOutputStream());
        printWriter.print(key+"="+ URLEncoder.encode(value1, "UTF-8"));
        printWriter.close();*/

        String propFilename = "property.post";
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(Paths.get(propFilename))){
            properties.load(inputStream);
        }
        String urlStr = properties.remove("url").toString();
        Object userAgent = properties.remove("User-Agent").toString();
        Object redirects = properties.remove("redirects");
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        String result = doPost(new URL(urlStr), properties, userAgent == null ? null : userAgent.toString(),
                redirects == null ? -1 : Integer.parseInt(redirects.toString()));
        System.out.println(result);
    }

    public static String doPost(URL url, Map<Object, Object> property, String userAgent, int redirects) throws IOException{
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        if (userAgent != null) connection.setRequestProperty("User-Agent", userAgent);
        if (redirects>=0){
            connection.setInstanceFollowRedirects(false);
        }
        connection.setDoOutput(true);
        try(PrintWriter out = new PrintWriter(connection.getOutputStream())){
            boolean first = true;
            for(Map.Entry<Object, Object> entry : property.entrySet()){
                if (first) first=false;
                else out.print('&');
                String name = entry.getKey().toString();
                String value = entry.getValue().toString();
                out.print(name);
                out.print("=");
                out.print(URLEncoder.encode(value, "UTF-8"));
            }
        }
        String enc = connection.getContentEncoding();
        if (enc==null) enc="UTF-8";
        if (redirects>0){
            int respCode = connection.getResponseCode();
            if (respCode == HttpsURLConnection.HTTP_MOVED_PERM ||
                    respCode == HttpsURLConnection.HTTP_MOVED_TEMP ||
                    respCode == HttpsURLConnection.HTTP_SEE_OTHER){
                String location = connection.getHeaderField("Location");
                if (location != null){
                    URL base = connection.getURL();
                    connection.disconnect();
                    return doPost(new URL(base, location), property, userAgent, redirects -1);
                }
            }
        } else if (redirects == 0) {
            throw new IOException("Too many redicrects");
        }
        StringBuilder response = new StringBuilder();
        try (Scanner in = new Scanner(connection.getInputStream(), enc)){
            while (in.hasNextLine()){
                String next = in.nextLine();
                //if (next.contains("Tesla")){
                response.append(/*in.nextLine()*/next);
                response.append("\n");
            }
        }catch (IOException exception){
            InputStream err = connection.getErrorStream();
            if (err == null) throw exception;
            try(Scanner scanner = new Scanner(err)){
                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                    response.append("\n");
                }
            }
        }
        return response.toString();
    }
}

