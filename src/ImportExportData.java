import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class ImportExportData {
    public static String gamersFilePath = "gamers.json";
    public static String schemaFilePath = "schema.json";

    private static String readUsingBufferedReader(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    public static void exportGamersToJson(ArrayList<Gamer> gamers) {
        Gson gson = new Gson();
        String jsonGamer = null;
        try (FileWriter writer = new FileWriter(gamersFilePath, false)) {
            for (Gamer gamer : gamers) {
                jsonGamer = gson.toJson(gamer);
                writer.write(jsonGamer);
                writer.append('\n');
                //writer.flush();
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private static ProcessingReport validateJson(String jsonGamerSchema, String jsonGamer) throws IOException, ProcessingException {
        JsonNode schemaGamerNode = JsonLoader.fromString(jsonGamerSchema);
        JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        JsonSchema jsonSchema = factory.getJsonSchema(schemaGamerNode);
        JsonNode jsonGamerNode = JsonLoader.fromString(jsonGamer);
        return jsonSchema.validate(jsonGamerNode);
    }

    private static void extractMessages(ProcessingReport report) {
        ProcessingMessage message;
        Iterator itr = report.iterator();
        while (itr.hasNext()) {
            message = (ProcessingMessage) itr.next();
            System.out.println("Message" + message.asJson().get("message").asText());
        }
    }

    public static ArrayList<Gamer> importGamersFromJson() {
        ArrayList<Gamer> gamers = new ArrayList<Gamer>();
        Gson gson = new Gson();

        try {
            String schema = readUsingBufferedReader(schemaFilePath);
            File file = new File(gamersFilePath);
            FileReader fr = new FileReader(file);
            BufferedReader reader = new BufferedReader(fr);
            String jsonGamer = reader.readLine();
            while (jsonGamer != null) {
                ProcessingReport report = validateJson(schema, jsonGamer);
                if (!report.isSuccess()) {
                    extractMessages(report);
                    throw new Exception("Validation is failed");
                }
                //валидация пройдена успешно; добавляем игрока в массив
                Gamer gamer = gson.fromJson(jsonGamer, Gamer.class);
                gamers.add(gamer);
                jsonGamer = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ProcessingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return gamers;
    }
}
