import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.google.gson.Gson;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Client {

    //сокет для общения
    private static Socket clientSocket;

    // нам нужен ридер читающий с консоли, иначе как
    // мы узнаем что хочет сказать клиент?
    private static BufferedReader reader;

    // поток чтения из сокета
    private static BufferedReader in;

    // поток записи в сокет
    private static BufferedWriter out;

    public static void main(String[] args) {
        //работа с Gson
        Gson gson = new Gson();

        //получаем игроков
        //если файл импорта существует, то берем состояние игроков в виде json оттуда, предварительно
        //валидируя json на сответствие схеме
        ArrayList<Gamer> gamers;
        File file = new File(ImportExportData.gamersFilePath);
        if (file.exists()) {
            System.out.println("Импорт: попытка отвалидировать данные игроков");
            gamers = ImportExportData.importGamersFromJson();
            if (gamers.size() < 3) {
                System.out.println("Импорт: попытка отвалидировать данные игроков провалилась");
                Deck deck = new Deck();
                deck.createAndMixCards();
                gamers = deck.createGamers();
                System.out.println("Игроки получены путем раздачи карт");
            } else {
                System.out.println("Импорт: игроки успешно получены и отвалидированы");
            }
        } else {
            Deck deck = new Deck();
            deck.createAndMixCards();
            gamers = deck.createGamers();
            System.out.println("Игроки получены путем раздачи карт");
        }

        Integer count;

        //количество попыток побить каждую карту
        HashMap<String, Integer> attemptCount = new HashMap();
        //начинаем игру с сервером
        try {
            try {
                loop:
                while (true) {
                    for (Gamer gamer : gamers) {
                        //создаем сокет на соединение с сервером
                        clientSocket = new Socket("localhost", 4004);

                        // читать соообщения с сервера
                        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                        // писать на сервер
                        out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                        //Пишем в консоль служебную информацию об игре
                        System.out.println("Игрок " + gamer.getName() + " вступает в игру");

                        // Получаем json последней карты на сервере
                        String lastJsonCard = in.readLine();
                        //Преобразовываем ее в обьект
                        Card lastCard = gson.fromJson(lastJsonCard, Card.class);

                        if (attemptCount.containsKey(lastCard.toString())) {
                            count = attemptCount.get(lastCard.toString());
                            attemptCount.put(lastCard.toString(), ++count);
                        } else {
                            count = 1;
                            attemptCount.put(lastCard.toString(), count);
                        }

                        //и пытаемся покрыть эту карту картой игрока
                        Card tryCard = gamer.tryHit(lastCard);

                        //отправляем карту на сервер
                        String jsonTryCard = gson.toJson(tryCard);
                        out.write(jsonTryCard + "\n");
                        out.flush();

                        if (gamer.isWinner()) {
                            System.out.println(gamer.getName() + " - победитель. Игра окончена");
                            // отправляем сообщение на сервер
                            out.write("stop_game\n");
                            out.flush();
                            break loop;
                        } else if (count > 10) {
                            System.out.println("В игре нет победителя. Игра окончена");
                            // отправляем сообщение на сервер
                            out.write("stop_game\n");
                            out.flush();
                            break loop;
                        } else {
                            out.write("continue_game\n");
                            out.flush();
                        }

                        //сохраняем состояние клиента
                        ImportExportData.exportGamersToJson(gamers);
                        System.out.println("Ход следующего игрока");
                        clientSocket.close();
                        in.close();
                        out.close();
                    }
                }
            } finally {
                // в любом случае необходимо закрыть сокет и потоки
                if (clientSocket != null) {
                    clientSocket.close();
                }
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}