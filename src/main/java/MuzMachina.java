import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class MuzMachina implements MetaEventListener {

    JPanel panelGlowny;
    ArrayList<JCheckBox> listaPolWyboru;
    Sequencer sekwenser;
    Sequence sekwencja;
    Track sciezka;
    JFrame ramkaGlowna;

    String[] nazwyInstrumentow = {"Bass Drum", "Closed Hi-Hat",
            "Open Hi-Hat","Acoustic Snare", "Crash Cymbal", "Hand Clap",
            "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga",
            "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo",
            "Open Hi Conga"};

    int[] instrumenty = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

    public static void main (String[] args) {
        new MuzMachina().tworzGUI();
    }

    public void tworzGUI() {
        ramkaGlowna = new JFrame("Muzyczna Machina");
        ramkaGlowna.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout uklad = new BorderLayout();
        JPanel panelZew = new JPanel(uklad);
        panelZew.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); //pusta ramka

        listaPolWyboru = new ArrayList<JCheckBox>();
        Box obszarPrzyciskow = new Box(BoxLayout.Y_AXIS);

        //tworzenie gui
        JButton start = new JButton("Start");
        start.addActionListener(new MojStartListener());
        obszarPrzyciskow.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MojStopListener());
        obszarPrzyciskow.add(stop);

        JButton tempoUp = new JButton("Szybciej");
        tempoUp.addActionListener(new MojTempoUpListener());
        obszarPrzyciskow.add(tempoUp);

        JButton tempoDown = new JButton("Wolniej");
        tempoDown.addActionListener(new MojTempoDownListener());
        obszarPrzyciskow.add(tempoDown);

        Box obszarNazw = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            obszarNazw.add(new Label(nazwyInstrumentow[i]));
        }

        panelZew.add(BorderLayout.EAST, obszarPrzyciskow);
        panelZew.add(BorderLayout.WEST, obszarNazw);

        ramkaGlowna.getContentPane().add(panelZew);

        GridLayout siatkaPolWyboru = new GridLayout(16, 16);
        siatkaPolWyboru.setVgap(1);
        siatkaPolWyboru.setHgap(2);
        panelGlowny = new JPanel(siatkaPolWyboru);
        panelZew.add(BorderLayout.CENTER, panelGlowny);

        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            listaPolWyboru.add(c);
            panelGlowny.add(c);
        } //koniec tworzenia gui

        konfigurujMidi();

        ramkaGlowna.setBounds(50, 50, 300, 300);
        ramkaGlowna.pack(); //sizes the frame so that all its contents are at or above their preferred sizes
        ramkaGlowna.setVisible(true);
    }

    public void konfigurujMidi() {
        try {
            sekwenser = MidiSystem.getSequencer();
            sekwenser.open();
            sekwencja = new Sequence(Sequence.PPQ, 4);
            sciezka = sekwencja.createTrack();
            sekwenser.setTempoInBPM(120);
        } catch(Exception e) {e.printStackTrace();}
    }

    public void utworzSciezkeIOdtworz() {
        int[] listaSciezki = null;

        sekwencja.deleteTrack(sciezka);
        sciezka = sekwencja.createTrack(); //usuń starą ścieżkę i stwórz nową

        for (int i = 0; i < 16; i++) {
            listaSciezki = new int[16];

            int klucz = instrumenty[i]; //określa jaki instument jest używany

            for (int j = 0; j < 16; j++) {
                JCheckBox jc = (JCheckBox) listaPolWyboru.get(j + (16 * i)); //jesli pole wyboru w danym takcie jest zaznaczone to instrumrnt gra, jesli nie to zapisz 0
                if (jc.isSelected()) {
                    listaSciezki[j] = klucz;
                } else {
                    listaSciezki[j] = 0;
                }
            }

            utworzSciezke(listaSciezki);
            sciezka.add(tworzZdarzenie(176, 1, 127, 0, 16));
        }

        sciezka.add(tworzZdarzenie(192, 9, 1, 0, 15)); //nich w ostatnuim takie zawsze będzie jakies zdarzenie
        try {
            sekwenser.setSequence(sekwencja);
            sekwenser.setLoopCount(sekwenser.LOOP_CONTINUOUSLY); //odtwarzaj w nieskonczonosc
            sekwenser.start(); //zacznij grać
            sekwenser.setTempoInBPM(120);
        } catch(Exception e) {e.printStackTrace();}
    }

    public class MojStartListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            utworzSciezkeIOdtworz();
        }
    }

    public class MojStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            sekwenser.stop();
        }
    }

    public class MojTempoUpListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float modTempa = sekwenser.getTempoFactor();
            sekwenser.setTempoFactor((float) (modTempa * 1.03)); //zwiększ tempo o 3%
        }
    }

    public class MojTempoDownListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float modTempa = sekwenser.getTempoFactor();
            sekwenser.setTempoFactor((float) (modTempa * .97)); //zmniejsz tempo o 3%
        }
    }


    public void utworzSciezke(int[] lista) { //metoda otrzymuje tablice dla instrumentu i tam gdzie znajdzie wartość inną niż 0 tworzy zdarzenie dla tego instrumentu (i dodajemy je do ścieżki)
        for (int i = 0; i < 16; i++) {
            int klucz = lista[i];
            if (klucz != 0) {
                sciezka.add(tworzZdarzenie(144, 9, klucz, 100, i));
                sciezka.add(tworzZdarzenie(128, 9, klucz, 100, i + 1));
            }
        }
    }

    public static MidiEvent tworzZdarzenie (int plc, int kanal, int jeden, int dwa, int takt) {
        MidiEvent zdarzenie = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(plc, kanal, jeden, dwa);
            zdarzenie = new MidiEvent(a, takt);
        } catch(Exception e) {e.printStackTrace(); }
        return zdarzenie;
    }

    public void meta(MetaMessage arg0) {
    }

}
