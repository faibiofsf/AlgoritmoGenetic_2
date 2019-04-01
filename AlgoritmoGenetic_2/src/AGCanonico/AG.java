package AGCanonico;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.Range;
import org.jfree.data.category.DefaultCategoryDataset;

class AG implements Runnable {
	
	private String objetivo;
	private ArrayList<Individuo> populacao;
	private double probCruz, probMut;
	private int numeroIteracoes, tamanhoPopulacao;
	public Individuo melhorIndividuo;
	public ArrayList<Integer> fitnessPorIteracoes;
	public ArrayList<Double> mediaFitnessPopulacao;
	private DefaultCategoryDataset ds;
	private JFreeChart grafico;
	private String nomeGrafico, tituloGrafico;

	// Construtor Entrada
	public AG(int tamanhoPopulacao, int numeroIteracoes, String objetivo, double probCruz, double probMut, String tituloGrafico, String nomeGrafico) {
		this.numeroIteracoes = numeroIteracoes;
		this.tamanhoPopulacao = tamanhoPopulacao;
		this.objetivo = objetivo;
		this.probCruz = probCruz;
		this.probMut = probMut;
		this.melhorIndividuo = new Individuo();
		this.fitnessPorIteracoes = new ArrayList<Integer>();
		this.mediaFitnessPopulacao = new ArrayList<Double>();
		this.ds = new DefaultCategoryDataset();
		this.nomeGrafico = nomeGrafico;
		this.tituloGrafico = tituloGrafico;		
	}

	// Iniciar
	public void run() {
		// Iniciar PopulaÃ§Ã£o
		this.populacao = this.populacaoInicial();

		// Avaliar Populacao
		for (Individuo individuo : this.populacao) {
			avalia(individuo);
		}

		// Ranqueia populaÃ§Ã£o
		this.rank();

		int iteracao = 0;
		
		while (this.numeroIteracoes > iteracao) {

			ArrayList<Individuo> novaPopulacao = new ArrayList<Individuo>();

			while (novaPopulacao.size() < populacao.size()) {

				// Selecinar cruzamento por Roleta
				Individuo pai1 = this.selecionaPai();
				Individuo pai2 = this.selecionaPai();

				// Cruzamento dos pais de acordo com a probabilidade e MutaÃ§Ã£o
				// de acordo com probabilidade
				if (Math.random() <= this.probCruz) {
					// Cruzamento
					Individuo[] filhos = this.filhosCruzamento(pai1, pai2);

					// Mutacao de acordo com probabilidade
					this.mutacao(filhos[0]);
					this.mutacao(filhos[1]);

					novaPopulacao.add(filhos[0]);
					novaPopulacao.add(filhos[1]);

				}
			}

			this.populacao = null;
			this.populacao = (ArrayList<Individuo>) novaPopulacao.clone();
			novaPopulacao.clear();

			double fitnessPopulacao = 0;			
			// Avaliar Nova Populacao
			for (Individuo individuo : this.populacao) {
				avalia(individuo);
				fitnessPopulacao += (double) individuo.getFitness();
			}
			
			
			// Ranqueia populacao
			this.rank();

			System.out.println("");
			System.out.println("Iteraçao: " + iteracao);			
			System.out.println("Melhor Individuo: " + melhorIndividuo.getCromossomo() + ": " + melhorIndividuo.getFitness());

			
			this.insereNoGrafico(fitnessPopulacao, iteracao);
			
			if(this.melhorIndividuo.getFitness() == objetivo.length()){
				insereNoGrafico(fitnessPopulacao, iteracao);
				break;
			}
			/*
			try {
				Thread.sleep(50);
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
			iteracao++;
			
		}
		
		this.salvarGrafico(""+iteracao);

	}

	private void insereNoGrafico(double fitnessPopulacao, int iteracao){
		
		ds.addValue((fitnessPopulacao / this.populacao.size()), "Media de Fitness da População", ""+iteracao);
		ds.addValue(this.melhorIndividuo.getFitness(), "Fitness do Melhor Individuo", ""+iteracao);
	}
	
	// Gerar populaÃ§Ã£o inicial
	private ArrayList<Individuo> populacaoInicial() {

		ArrayList<Individuo> populacaoInicial = new ArrayList<Individuo>();

		for (int i = 0; i < this.tamanhoPopulacao; i++) {
			String cromossomo = "";
			for (int j = 0; j < objetivo.length(); j++) {
				cromossomo += Math.random() < 0.5 ? "0" : "1";
			}
			Individuo individuo = new Individuo(cromossomo);

			avalia(individuo);

			populacaoInicial.add(individuo);
		}

		return populacaoInicial;
	}

	// SeleÃ§Ã£o
	private Individuo selecionaPai() {

		int somatorio = 0;
		double probabilidades = 0.0;
		Individuo pai = null;

		for (Individuo individuo : this.populacao) {
			somatorio += individuo.getFitness();
		}

		// Roleta
		double ponteiro = Math.random();

		for (Individuo individuo : this.populacao) {
			if (ponteiro <= ((double) individuo.getFitness() /  (double) somatorio) + probabilidades) {
				pai = new Individuo(individuo.getFitness(), individuo.getCromossomo());
				break;
			} else {
				probabilidades += ((double) individuo.getFitness() / (double) somatorio);
			}
		}

		return pai;
	}

	// Cruzamento
	private Individuo[] filhosCruzamento(Individuo pai1, Individuo pai2) {

		Individuo filho1 = new Individuo();
		Individuo filho2 = new Individuo();

		Random r = new Random();
		int posicao = r.nextInt(pai1.getCromossomo().length());
		
		if(pai1.getCromossomo().length() != pai2.getCromossomo().length()){
			System.out.println("teste");
		}

		String crom1 = pai1.getCromossomo().substring(0, posicao);
		crom1 += pai2.getCromossomo().substring(posicao, pai2.getCromossomo().length());

		String crom2 = pai2.getCromossomo().substring(0, posicao);
		crom2 += pai1.getCromossomo().substring(posicao, pai1.getCromossomo().length());

		filho1.setCromossomo(crom1);
		filho2.setCromossomo(crom2);

		Individuo[] filhos = { filho1, filho2 };

		return filhos;
	}

	// MutaÃ§Ã£o
	private void mutacao(Individuo filho) {

		char[] crom = filho.getCromossomo().toCharArray();
		
		for (int posicao = 0; posicao < crom.length; posicao++) {
			
			if (Math.random() < this.probMut) {
				if (crom[posicao] == '0') {
					crom[posicao] = '1';
				} else if (crom[posicao] == '1') {
					crom[posicao] = '0';
				}

				
			}
		}
		
		filho.setCromossomo(String.valueOf(crom));

	}

	// Avaliacao
	private void avalia(Individuo individuo) {
		char[] objetivo = this.objetivo.toCharArray();
		char[] cromossomo = individuo.getCromossomo().toCharArray();
		int pontuacao = 0;
		for (int i = 0; i < objetivo.length; i++) {
			if (objetivo[i] == cromossomo[i]) {
				pontuacao++;
			}
		}

		individuo.setFitness(pontuacao);
	}

     //Ranqueamento da populaÃ§Ã£o
	public void rank() {

		Collections.sort(this.populacao);
		
		if (this.melhorIndividuo.getFitness() < populacao.get(0).getFitness()){
			this.melhorIndividuo = new Individuo(populacao.get(0).getFitness(), populacao.get(0).getCromossomo());
		}
	}

	public JPanel getPanel() {
		this.grafico = ChartFactory.createLineChart(this.tituloGrafico, "Iteração", 
			    "Fitness", ds, PlotOrientation.VERTICAL, true, true, false);
		  return new ChartPanel(this.grafico);
	}
	
	private void salvarGrafico(String encerramento){
		this.grafico = ChartFactory.createLineChart(this.tituloGrafico+"\nEncerrado em: " + encerramento, "Iteração", 
			    "Fitness", ds, PlotOrientation.VERTICAL, true, true, false);
		OutputStream arq;
		try {
			arq = new FileOutputStream("AlgoritmoGenetic_2\\"+this.nomeGrafico+ ".png");
			ChartUtilities.writeChartAsPNG(arq, grafico, 550, 400);
			arq.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void main(String args[]) {
		
		double[] mutacoes = {0.01, 0.1};
		int[] populacoes = {10, 50};
		String[] palavras = {"1010101010", "101010101010101"}; 
		
		for (int i = 0; i < palavras.length; i++) {
			for (int j = 0; j < populacoes.length; j++) {
				for (int j2 = 0; j2 < mutacoes.length; j2++) {
					String titulo = "Evolução do AG com parametros de Mut: " + mutacoes[j2] + ", População: " + populacoes[j] + ", Palavra: " + palavras[i];
					String nomeGrafico = "Mut-" + mutacoes[j2] + "_População-" + populacoes[j] + "_Palavra-" + palavras[i];					
					AG ag = new AG(populacoes[j], 10000, palavras[i], 1.0, mutacoes[j2] , titulo, nomeGrafico);
					Thread t = new Thread(ag); //Cria a linha de execução
					t.start(); //Ativa a thread
				}
			}
		}
		
		/*
		AG ag = new AG(10, 1000, "1010101010101010", 1.0, 0.01, String titulo, String nomeGrafico);
		
		JFrame frame = new JFrame("Gráfico de Linha do Algoritmo Genético");
		frame.add(ag.getPanel());

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setSize(1024, 480);
		frame.setVisible(true);
		
		Thread t = new Thread(ag); //Cria a linha de execução
		t.start(); //Ativa a thread
	
		*/
	}

}

class Individuo implements Comparable<Individuo> {

	private int fitness = 0;
	private String cromossomo;

	public Individuo() {
	}

	public Individuo(String cromossomo) {
		this.cromossomo = cromossomo;
	}

	public Individuo(int fitness, String cromossomo) {
		super();
		this.fitness = fitness;
		this.cromossomo = cromossomo;
	}

	public int getFitness() {
		return fitness;
	}

	public void setFitness(int fitness) {
		this.fitness = fitness;
	}

	public String getCromossomo() {
		return cromossomo;
	}

	public void setCromossomo(String cromossomo) {
		this.cromossomo = cromossomo;
	}

	@Override
	public int compareTo(Individuo outroIndividuo) {
		// TODO Auto-generated method stub
		if (this.fitness > outroIndividuo.getFitness()) {
			return -1;
		}
		if (this.fitness < outroIndividuo.getFitness()) {
			return 1;
		}
		return 0;
	}

}
    