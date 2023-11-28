import java.util.TreeMap;

public class Main {
    public static void main(String[] args) {
        // Простейший пример с одновременной вставкой и чтением из самописного TreeMap
        // Отсутсвие блокировок обеспечивается AtomicReference, AtomicInteger и оптимистичными методами синхронизации

        MyTreeMap<Integer, String> treeMap = new MyTreeMap<>();

        // Создаем и запускаем несколько потоков для вставки данных
        Runnable insertTask = () -> {
            for (int i = 0; i < 10; i++) {
                treeMap.put(i, "Value " + i);
            }
        };

        // Создаем и запускаем несколько потоков для чтения данных
        Runnable readTask = () -> {
            for (int i = 0; i < 10; i++) {
                System.out.println("Get = key:value | " + i + ":" + treeMap.get(i));
            }
        };

        // Запускаем потоки
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(i % 2 == 0 ? insertTask : readTask);
            threads[i].start();
        }

        // Ждем завершения всех потоков
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Finish");
    }
}
