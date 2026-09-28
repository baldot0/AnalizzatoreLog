import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

class RichiestaHttp {
    String ip;
    String path;
    int statusCode;
    long tempoRispostaMs;
    long timestamp;

    RichiestaHttp(String ip, String path, int statusCode, long tempoRispostaMs, long timestamp) {
        this.ip = ip;
        this.path = path;
        this.statusCode = statusCode;
        this.tempoRispostaMs = tempoRispostaMs;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return ip + " " + path + " " + statusCode + " " + tempoRispostaMs + "ms";
    }
}

class AnalizzatoreLog {
    private static final int DIMENSIONE_FINESTRA = 10;

    private final ArrayList<RichiestaHttp> storico = new ArrayList<>();
    private final LinkedList<RichiestaHttp> finestra = new LinkedList<>();
    private final HashSet<String> ipSospetti = new HashSet<>();
    private final TreeSet<Long> tempiUnivoci = new TreeSet<>();

    void registra(RichiestaHttp r) {
        storico.add(r);

        finestra.addLast(r);
        if (finestra.size() > DIMENSIONE_FINESTRA) {
            finestra.removeFirst();
        }

        if (r.statusCode >= 400) {
            ipSospetti.add(r.ip);
        }

        tempiUnivoci.add(r.tempoRispostaMs);
    }

    List<RichiestaHttp> ultimeErrori5xx(int n) {
        List<RichiestaHttp> risultato = new ArrayList<>();
        for (int i = storico.size() - 1; i >= 0 && risultato.size() < n; i--) {
            RichiestaHttp r = storico.get(i);
            if (r.statusCode >= 500) {
                risultato.add(r);
            }
        }
        return risultato;
    }

    long percentile90() {
        int k = (int) Math.ceil(0.9 * tempiUnivoci.size());
        Long valore = tempiUnivoci.first();
        for (int i = 1; i < k; i++) {
            valore = tempiUnivoci.higher(valore);
        }
        return valore;
    }

    void stampaReport() {
        long p90 = percentile90();
        SortedSet<Long> tempiLenti = tempiUnivoci.tailSet(p90, true);
        Long entro500 = tempiUnivoci.floor(500L);

        HashSet<String> ipSospettiInFinestra = new HashSet<>();
        for (RichiestaHttp r : finestra) {
            if (ipSospetti.contains(r.ip)) {
                ipSospettiInFinestra.add(r.ip);
            }
        }

        System.out.println("===== REPORT =====");
        System.out.println("Richieste totali nello storico: " + storico.size());
        System.out.println("IP sospetti totali (4xx/5xx): " + ipSospetti.size() + " " + ipSospetti);
        System.out.println("Tempi di risposta distinti: " + tempiUnivoci.size());
        System.out.println("Finestra ultime " + DIMENSIONE_FINESTRA + " richieste:");
        for (RichiestaHttp r : finestra) {
            System.out.println("  " + r);
        }
        System.out.println(ipSospettiInFinestra.size() + " IP sospetti hanno generato richieste tra le ultime "
                + finestra.size() + " in finestra " + ipSospettiInFinestra
                + "; il tempo di risposta al 90° percentile è di " + p90 + " ms");
        System.out.println("Tempi distinti >= p90: " + tempiLenti);
        System.out.println("Miglior tempo distinto entro 500 ms: " + entro500);
        System.out.println("Ultime 3 richieste con status >= 500:");
        for (RichiestaHttp r : ultimeErrori5xx(3)) {
            System.out.println("  " + r);
        }
    }
}

public class Esercizio2 {
    public static void main(String[] args) {
        String[] ips = {"10.0.0.1", "10.0.0.2", "10.0.0.3", "192.168.1.10", "192.168.1.11", "172.16.5.9"};
        String[] paths = {"/", "/login", "/api/utenti", "/api/ordini", "/immagini/logo.png", "/admin"};
        int[] status = {200, 200, 200, 200, 301, 404, 403, 500, 503, 200, 404, 502};

        Random random = new Random(42);
        AnalizzatoreLog analizzatore = new AnalizzatoreLog();
        long timestamp = 1732000000L;

        for (int i = 0; i < 35; i++) {
            RichiestaHttp r = new RichiestaHttp(
                    ips[random.nextInt(ips.length)],
                    paths[random.nextInt(paths.length)],
                    status[random.nextInt(status.length)],
                    20 + random.nextInt(900),
                    timestamp + i);
            analizzatore.registra(r);
        }

        analizzatore.stampaReport();
    }
}