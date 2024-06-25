package teste6;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

public class teste6 {

    private static final String[] URLs = {
        "https://archive-api.open-meteo.com/v1/archive?latitude=-10.9111,-1.4558,-19.9208,2.8197,-15.7797&longitude=-37.0717,-48.5044,-43.9378,-60.6733,-47.9297&start_date=2024-01-01&end_date=2024-01-31&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min,temperature_2m_mean&timeformat=unixtime",
        "https://archive-api.open-meteo.com/v1/archive?latitude=-20.4428,-15.5961,-25.4278,-27.5967,-3.7172&longitude=-54.6464,-56.0967,-49.2731,-48.5492,-38.5431&start_date=2024-01-01&end_date=2024-01-31&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min,temperature_2m_mean&timeformat=unixtime",
        "https://archive-api.open-meteo.com/v1/archive?latitude=-16.6786,-16.6786,-7.115,0.0389,-9.6658&longitude=-49.2539,-49.2539,-34.8631,-51.0664,-35.7353&start_date=2024-01-01&end_date=2024-01-31&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min,temperature_2m_mean&timeformat=unixtime",
        "https://archive-api.open-meteo.com/v1/archive?latitude=-3.1019,-5.795,-10.1675,-30.0328,-8.7619&longitude=-60.025,-35.2094,-48.3277,-51.2302,-63.9039&start_date=2024-01-01&end_date=2024-01-31&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min,temperature_2m_mean&timeformat=unixtime",
        "https://archive-api.open-meteo.com/v1/archive?latitude=-8.0539,-9.9747,-22.9064,-12.9711,-2.5297&longitude=-34.8811,-67.81,-43.1822,-38.5108,-44.3028&start_date=2024-01-01&end_date=2024-01-31&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min,temperature_2m_mean&timeformat=unixtime",
        "https://archive-api.open-meteo.com/v1/archive?latitude=-23.5475,-20.3194&longitude=-46.6361,-40.3378&start_date=2024-01-01&end_date=2024-01-31&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min,temperature_2m_mean&timeformat=unixtime"
    };

    public static void main(String[] args) throws InterruptedException {
        System.out.println("\nExecutando versão de referência (thread único)...");
        runExperiment(1);

        System.out.println("\nVersão em execução com 3 threads...");
        runExperiment(3);

        System.out.println("\nVersão em execução com 9 threads...");
        runExperiment(9);

        System.out.println("\nVersão em execução com 27 threads...");
        runExperiment(27);
    }

    private static void runExperiment(int numThreads) throws InterruptedException {
        List<Long> roundTimes = new ArrayList<>();
        for (int round = 0; round < 10; round++) {
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            List<Callable<Void>> tasks = new ArrayList<>();

            for (String url : URLs) {
                tasks.add(() -> {
                    try {
                        long startTime = System.currentTimeMillis();

                        String response = makeRequest(url);
                        Map<String, Map<String, Double>> processedData = processWeatherData(response);
                        printProcessedData(processedData);

                        long endTime = System.currentTimeMillis();
                        long executionTime = endTime - startTime;
                        System.out.println("Tempo de execução para a URL: " + executionTime + " ms");

                        return null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
            }

            long roundStartTime = System.currentTimeMillis();

            
            executor.invokeAll(tasks);

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

            long roundEndTime = System.currentTimeMillis();
            long roundTime = roundEndTime - roundStartTime;
            roundTimes.add(roundTime);

            System.out.println("Tempo de execução da rodada " + (round + 1) + " com " + numThreads + " threads: " + roundTime + " ms");
        }

      
        double averageRoundTime = roundTimes.stream()
                .mapToLong(Long::valueOf)
                .average()
                .orElse(0);

        System.out.println("\nTempo médio de execução com " + numThreads + " threads: " + averageRoundTime + " ms");
    }

    private static String makeRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                return content.toString();
            }
        } else {
            return "Solicitação GET não funcionou. Código de resposta: " + responseCode;
        }
    }

    private static Map<String, Map<String, Double>> processWeatherData(String response) {
        Map<String, Map<String, Double>> dailyData = new HashMap<>();
        JSONObject jsonResponse = new JSONObject(response);

        JSONArray dailyTempMaxArray = jsonResponse.getJSONObject("diario").getJSONArray("temperatura_2m_max");
        JSONArray dailyTempMinArray = jsonResponse.getJSONObject("diario").getJSONArray("temperatura_2m_min");
        JSONArray dailyTempMeanArray = jsonResponse.getJSONObject("diario").getJSONArray("temperatura_2m_media");
        JSONArray timeArray = jsonResponse.getJSONObject("diario").getJSONArray("tempo");

        for (int i = 0; i < timeArray.length(); i++) {
            long timestamp = timeArray.getLong(i);
            double tempMax = dailyTempMaxArray.getDouble(i);
            double tempMin = dailyTempMinArray.getDouble(i);
            double tempMean = dailyTempMeanArray.getDouble(i);

            LocalDate date = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
            String dateString = date.toString();

            dailyData.putIfAbsent(dateString, new HashMap<>());
            dailyData.get(dateString).put("max", tempMax);
            dailyData.get(dateString).put("min", tempMin);
            dailyData.get(dateString).put("medio", tempMean);
        }

        return dailyData;
    }

    private static void printProcessedData(Map<String, Map<String, Double>> data) {
        for (String date : data.keySet()) {
            Map<String, Double> dailyStats = data.get(date);
            System.out.printf("Data: %s, Min: %.2f, Max: %.2f, Medio: %.2f%n", date, dailyStats.get("min"), dailyStats.get("max"), dailyStats.get("medio"));
        }
    }
}
